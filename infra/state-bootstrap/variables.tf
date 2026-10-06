variable "aws_region" {
  description = "AWS region that stores the production Terraform state."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name included in the globally unique state bucket name."
  type        = string
  default     = "autotrader"
}

variable "environment" {
  description = "Environment name included in the state bucket name."
  type        = string
  default     = "prod"
}
