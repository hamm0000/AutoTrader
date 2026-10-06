locals {
  name_prefix = "${var.project_name}-${var.environment}"
  azs         = slice(data.aws_availability_zones.available.names, 0, 2)

  common_tags = merge(var.tags, {
    Application = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  })

  github_oidc_provider_arn = coalesce(
    var.github_oidc_provider_arn,
    try(aws_iam_openid_connect_provider.github[0].arn, null)
  )

  # 최초 apply는 ECR repository와 비활성 Scheduler를 만들기 위한 bootstrap 단계입니다.
  # release 이후에는 ECR digest를 입력해 같은 Terraform 코드가 Task Definition revision도 소유합니다.
  app_image      = var.app_image_digest == null ? "${aws_ecr_repository.app.repository_url}:bootstrap" : "${aws_ecr_repository.app.repository_url}@${var.app_image_digest}"
  strategy_image = var.strategy_image_digest == null ? "${aws_ecr_repository.strategy.repository_url}:bootstrap" : "${aws_ecr_repository.strategy.repository_url}@${var.strategy_image_digest}"
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}
