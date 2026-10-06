output "aws_account_id" {
  description = "AWS account that owns the deployment."
  value       = data.aws_caller_identity.current.account_id
}

output "aws_region" {
  value = var.aws_region
}

output "ecr_app_repository_name" {
  value = aws_ecr_repository.app.name
}

output "ecr_app_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "ecr_strategy_repository_name" {
  value = aws_ecr_repository.strategy.name
}

output "ecr_strategy_repository_url" {
  value = aws_ecr_repository.strategy.repository_url
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "public_subnet_ids" {
  value = [for subnet in aws_subnet.public : subnet.id]
}

output "paper_task_security_group_id" {
  value = aws_security_group.ecs_task["paper"].id
}

output "calendar_task_security_group_id" {
  value = aws_security_group.ecs_task["calendar"].id
}

output "paper_task_family" {
  value = aws_ecs_task_definition.paper_trading.family
}

output "calendar_task_family" {
  value = aws_ecs_task_definition.calendar_refresh.family
}

output "rds_endpoint" {
  description = "Private RDS endpoint; reachable only from the paper and calendar task security groups."
  value       = aws_db_instance.main.endpoint
}

output "paper_log_group_name" {
  value = aws_cloudwatch_log_group.paper_app.name
}

output "calendar_log_group_name" {
  value = aws_cloudwatch_log_group.calendar_refresh.name
}

output "paper_secret_arn" {
  description = "Secret container to populate with the four KIS paper-trading values."
  value       = aws_secretsmanager_secret.paper_trading.arn
}

output "calendar_secret_arn" {
  description = "Secret container to populate with the two KIS holiday-query values."
  value       = aws_secretsmanager_secret.calendar_refresh.arn
}

output "github_release_role_arn" {
  value = aws_iam_role.github_release.arn
}

output "scheduler_state" {
  value = var.schedules_enabled ? "ENABLED" : "DISABLED"
}
