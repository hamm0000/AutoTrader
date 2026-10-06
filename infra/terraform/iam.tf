data "aws_iam_policy_document" "ecs_task_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}


# Execution role은 ECS/Fargate agent가 image pull, log 전송,
# Task Definition에 선언된 secret 주입을 수행할 때 사용합니다.
# 애플리케이션 컨테이너에서는 이 role의 권한을 사용할 수 없습니다.
# paper와 calendar가 서로의 KIS secret에 접근하지 못하도록 실행 목적별로 role을 분리합니다.
resource "aws_iam_role" "ecs_execution" {
  for_each = toset(["paper", "calendar"])

  name               = "${local.name_prefix}-${each.key}-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  for_each = aws_iam_role.ecs_execution

  role       = each.value.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

locals {
  # RDS master secret은 두 task가 공통으로 사용하지만
  # KIS secret은 실행 목적에 맞는 하나만 허용합니다.
  ecs_execution_secret_arns = {
    paper = [
      aws_secretsmanager_secret.paper_trading.arn,
      aws_db_instance.main.master_user_secret[0].secret_arn,
    ]
    calendar = [
      aws_secretsmanager_secret.calendar_refresh.arn,
      aws_db_instance.main.master_user_secret[0].secret_arn,
    ]
  }
}

data "aws_iam_policy_document" "ecs_execution_secrets" {
  for_each = local.ecs_execution_secret_arns

  statement {
    sid       = "ReadRuntimeSecrets"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = each.value
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  for_each = data.aws_iam_policy_document.ecs_execution_secrets

  name   = "read-runtime-secrets"
  role   = aws_iam_role.ecs_execution[each.key].id
  policy = each.value.json
}

# Task role은 애플리케이션 컨테이너가 AWS API를 호출할 때 사용합니다.
# 현재는 직접 호출하지 않으므로 정책을 연결하지 않습니다.
# ECR, Secrets Manager, CloudWatch Logs 접근은 위 execution role이 담당합니다.
resource "aws_iam_role" "ecs_task" {
  name               = "${local.name_prefix}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

data "aws_iam_policy_document" "scheduler_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["scheduler.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "scheduler" {
  name               = "${local.name_prefix}-scheduler"
  assume_role_policy = data.aws_iam_policy_document.scheduler_assume_role.json
}

data "aws_iam_policy_document" "scheduler" {
  # Scheduler가 지정된 cluster에서 허용된 Task Definition family만 실행하도록 제한합니다.
  # revision wildcard는 Terraform이 등록한 최신 ACTIVE revision을 실행하기 위해 사용합니다.
  statement {
    sid     = "RunLatestTaskDefinitionRevision"
    actions = ["ecs:RunTask"]
    resources = [
      "${aws_ecs_task_definition.paper_trading.arn_without_revision}:*",
      "${aws_ecs_task_definition.calendar_refresh.arn_without_revision}:*",
    ]

    condition {
      test     = "ArnEquals"
      variable = "ecs:cluster"
      values   = [aws_ecs_cluster.main.arn]
    }
  }

  statement {
    sid     = "PassEcsRoles"
    actions = ["iam:PassRole"]
    resources = concat(
      [for role in values(aws_iam_role.ecs_execution) : role.arn],
      [aws_iam_role.ecs_task.arn],
    )

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }

  statement {
    sid       = "WriteSchedulerDeadLetterQueue"
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.scheduler_dlq.arn]
  }
}

resource "aws_iam_role_policy" "scheduler" {
  name   = "run-scheduled-ecs-tasks"
  role   = aws_iam_role.scheduler.id
  policy = data.aws_iam_policy_document.scheduler.json
}

resource "aws_iam_openid_connect_provider" "github" {
  count = var.github_oidc_provider_arn == null ? 1 : 0

  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

data "aws_iam_policy_document" "github_actions_assume_role" {
  # production 환경과 immutable ID가 포함된 OIDC subject가
  # 정확히 일치할 때만 role 사용을 허용해 Repo Jacking이나 타 워크플로 재사용으로 인한 비인가 AssumeRole을 원천 차단합니다.
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [var.github_oidc_subject]
    }
  }
}

resource "aws_iam_role" "github_release" {
  name               = "${local.name_prefix}-github-release"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json
}

data "aws_iam_policy_document" "github_release" {
  # ECR 인증 토큰 발급 API는 resource-level 권한 제한을 지원하지 않아 "*"를 사용합니다.
  # 이미지 push 권한은 아래 statement에서 두 ECR repository로 제한합니다.
  statement {
    sid       = "GetEcrAuthorizationToken"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PushImages"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = [aws_ecr_repository.app.arn, aws_ecr_repository.strategy.arn]
  }
}

resource "aws_iam_role_policy" "github_release" {
  name   = "publish-release-images"
  role   = aws_iam_role.github_release.id
  policy = data.aws_iam_policy_document.github_release.json
}
