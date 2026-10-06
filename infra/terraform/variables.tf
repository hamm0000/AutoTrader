variable "aws_region" {
  description = "AWS region for the deployment."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Name prefix used for AWS resources."
  type        = string
  default     = "autotrader"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "prod"
}

variable "vpc_cidr" {
  description = "CIDR block for the AutoTrader VPC."
  type        = string
  default     = "10.42.0.0/16"
}

variable "db_name" {
  description = "PostgreSQL database name."
  type        = string
  default     = "autotrader"
}

variable "db_username" {
  description = "PostgreSQL master username. The password is managed by RDS in Secrets Manager."
  type        = string
  default     = "autotrader_admin"
}

variable "app_image_digest" {
  description = "Immutable app-live ECR digest. Leave null only while bootstrapping ECR with disabled schedules."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition     = var.app_image_digest == null ? true : can(regex("^sha256:[0-9a-f]{64}$", var.app_image_digest))
    error_message = "app_image_digest must be null or a sha256 digest returned by ECR."
  }
}

variable "strategy_image_digest" {
  description = "Immutable zscore-strategy ECR digest. Leave null only while bootstrapping ECR with disabled schedules."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition     = var.strategy_image_digest == null ? true : can(regex("^sha256:[0-9a-f]{64}$", var.strategy_image_digest))
    error_message = "strategy_image_digest must be null or a sha256 digest returned by ECR."
  }
}

variable "schedules_enabled" {
  description = "Enables scheduled ECS tasks only after images and secret values are ready."
  type        = bool
  default     = false
}

variable "github_oidc_subject" {
  description = "Exact GitHub OIDC sub claim for the protected production environment."
  type        = string

  validation {
    condition = (
      startswith(var.github_oidc_subject, "repo:")
      && endswith(var.github_oidc_subject, ":environment:production")
      && !strcontains(var.github_oidc_subject, "OWNER_ID")
      && !strcontains(var.github_oidc_subject, "REPOSITORY_ID")
    )
    error_message = "Set the exact immutable GitHub OIDC subject for the production environment."
  }
}

variable "github_oidc_provider_arn" {
  description = "ARN of an existing GitHub Actions OIDC provider. Leave null to create one."
  type        = string
  default     = null
  nullable    = true
}

variable "tags" {
  description = "Additional tags applied to AWS resources."
  type        = map(string)
  default     = {}
}
