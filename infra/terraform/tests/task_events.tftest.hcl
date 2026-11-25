mock_provider "aws" {
  override_during = plan

  mock_resource "aws_db_instance" {
    defaults = {
      master_user_secret = [{
        kms_key_id    = "arn:aws:kms:ap-northeast-2:123456789012:key/00000000-0000-0000-0000-000000000000"
        secret_arn    = "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:rds-mock"
        secret_status = "active"
      }]
    }
  }

  mock_data "aws_availability_zones" {
    defaults = {
      names = ["ap-northeast-2a", "ap-northeast-2c"]
    }
  }

  mock_data "aws_caller_identity" {
    defaults = {
      account_id = "123456789012"
    }
  }

  mock_data "aws_iam_policy_document" {
    defaults = {
      json = jsonencode({
        Version   = "2012-10-17"
        Statement = []
      })
    }
  }
}

variables {
  github_oidc_subject = "repo:owner@123/repository@456:environment:production"
}

run "cluster_task_history_preserves_events_with_scoped_log_access" {
  command = plan

  override_resource {
    target          = aws_ecs_cluster.main
    override_during = plan
    values = {
      arn = "arn:aws:ecs:ap-northeast-2:123456789012:cluster/autotrader-prod"
    }
  }

  override_resource {
    target          = aws_cloudwatch_log_group.task_stopped
    override_during = plan
    values = {
      arn = "arn:aws:logs:ap-northeast-2:123456789012:log-group:/aws/events/autotrader-prod/ecs-task-stopped"
    }
  }

  override_resource {
    target          = aws_cloudwatch_event_rule.task_stopped
    override_during = plan
    values = {
      arn = "arn:aws:events:ap-northeast-2:123456789012:rule/autotrader-prod-ecs-task-stopped"
    }
  }

  assert {
    condition = (
      jsondecode(aws_cloudwatch_event_rule.task_stopped.event_pattern).source == ["aws.ecs"]
      && jsondecode(aws_cloudwatch_event_rule.task_stopped.event_pattern)["detail-type"] == ["ECS Task State Change"]
      && jsondecode(aws_cloudwatch_event_rule.task_stopped.event_pattern).detail.clusterArn == [aws_ecs_cluster.main.arn]
      && jsondecode(aws_cloudwatch_event_rule.task_stopped.event_pattern).detail.lastStatus == ["STOPPED"]
    )
    error_message = "Only stopped task events from this ECS cluster must be recorded."
  }

  assert {
    # 정상 종료와 exitCode가 없는 기동 실패도 수집해야 하므로 실패 조건을 붙이지 않습니다.
    condition = (
      toset(keys(jsondecode(aws_cloudwatch_event_rule.task_stopped.event_pattern).detail))
      == toset(["clusterArn", "lastStatus"])
    )
    error_message = "Recording must include all families, successful exits, and startup failures without exit codes."
  }

  assert {
    condition = (
      aws_cloudwatch_event_target.task_stopped.arn == aws_cloudwatch_log_group.task_stopped.arn
      && aws_cloudwatch_event_target.task_stopped.role_arn == null
      && aws_cloudwatch_event_target.task_stopped.input == null
      && aws_cloudwatch_event_target.task_stopped.input_path == null
      && length(aws_cloudwatch_event_target.task_stopped.input_transformer) == 0
    )
    error_message = "The original event must reach the dedicated log group without transformation or a RoleArn."
  }

  assert {
    condition = (
      # Statement 순서와 분할 개수에 의존하지 않고 허용 작업과 권한 범위를 검증합니다.
      toset(flatten([
        for statement in jsondecode(aws_cloudwatch_log_resource_policy.task_stopped.policy_document).Statement :
        statement.Action
      ])) == toset(["logs:CreateLogStream", "logs:PutLogEvents"])
      && alltrue([
        for statement in jsondecode(aws_cloudwatch_log_resource_policy.task_stopped.policy_document).Statement :
        statement.Effect == "Allow"
        && length(flatten([statement.Principal.Service])) > 0
        && alltrue([
          for principal in flatten([statement.Principal.Service]) :
          contains(["events.amazonaws.com", "delivery.logs.amazonaws.com"], principal)
        ])
        && alltrue([
          for resource in flatten([statement.Resource]) :
          contains([
            "${aws_cloudwatch_log_group.task_stopped.arn}:*",
            "${aws_cloudwatch_log_group.task_stopped.arn}:*:*",
          ], resource)
        ])
        && (
          !contains(flatten([statement.Action]), "logs:PutLogEvents")
          || try(statement.Condition.ArnEquals["aws:SourceArn"] == aws_cloudwatch_event_rule.task_stopped.arn, false)
        )
      ])
    )
    error_message = "Only EventBridge log writes to this log group may be allowed, with PutLogEvents restricted to this rule."
  }
}
