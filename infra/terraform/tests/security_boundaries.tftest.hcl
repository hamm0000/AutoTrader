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

run "scheduled_tasks_keep_environment_boundaries" {
  command = plan

  override_resource {
    target          = aws_security_group.ecs_task["paper"]
    override_during = plan
    values = {
      id = "sg-0123456789abcdef0"
    }
  }

  override_resource {
    target          = aws_security_group.ecs_task["calendar"]
    override_during = plan
    values = {
      id = "sg-0fedcba9876543210"
    }
  }

  override_resource {
    target          = aws_iam_role.ecs_execution["paper"]
    override_during = plan
    values = {
      arn = "arn:aws:iam::123456789012:role/autotrader-prod-paper-execution"
    }
  }

  override_resource {
    target          = aws_iam_role.ecs_execution["calendar"]
    override_during = plan
    values = {
      arn = "arn:aws:iam::123456789012:role/autotrader-prod-calendar-execution"
    }
  }

  assert {
    condition     = length(aws_security_group.ecs_task) == 2
    error_message = "Paper trading and calendar refresh must use separate security groups."
  }

  assert {
    condition = (
      aws_vpc_security_group_egress_rule.task_kis["paper"].from_port == 29443
      && aws_vpc_security_group_egress_rule.task_kis["paper"].to_port == 29443
    )
    error_message = "The paper-trading KIS rule must use the paper API port."
  }

  assert {
    condition = (
      aws_vpc_security_group_egress_rule.task_kis["calendar"].from_port == 9443
      && aws_vpc_security_group_egress_rule.task_kis["calendar"].to_port == 9443
    )
    error_message = "The calendar KIS rule must use the production read-only API port."
  }

  assert {
    condition = (
      length(aws_scheduler_schedule.paper_trading.target[0].ecs_parameters[0].network_configuration[0].security_groups) == 1
      && contains(
        aws_scheduler_schedule.paper_trading.target[0].ecs_parameters[0].network_configuration[0].security_groups,
        aws_security_group.ecs_task["paper"].id
      )
    )
    error_message = "The paper-trading schedule must launch with the paper security group."
  }

  assert {
    condition = (
      length(aws_scheduler_schedule.calendar_refresh.target[0].ecs_parameters[0].network_configuration[0].security_groups) == 1
      && contains(
        aws_scheduler_schedule.calendar_refresh.target[0].ecs_parameters[0].network_configuration[0].security_groups,
        aws_security_group.ecs_task["calendar"].id
      )
    )
    error_message = "The calendar schedule must launch with the calendar security group."
  }

  assert {
    condition = (
      aws_ecs_task_definition.paper_trading.execution_role_arn
      == aws_iam_role.ecs_execution["paper"].arn
    )
    error_message = "The paper-trading task must use the paper execution role."
  }

  assert {
    condition = (
      aws_ecs_task_definition.calendar_refresh.execution_role_arn
      == aws_iam_role.ecs_execution["calendar"].arn
    )
    error_message = "The calendar task must use the calendar execution role."
  }

  assert {
    condition     = aws_scheduler_schedule.paper_trading.target[0].retry_policy[0].maximum_retry_attempts == 0
    error_message = "The order-producing schedule must not retry RunTask automatically."
  }

  assert {
    condition = (
      aws_ecs_task_definition.paper_trading.track_latest != true
      && aws_ecs_task_definition.calendar_refresh.track_latest != true
    )
    error_message = "Task definitions must not opt in to tracking revisions registered outside Terraform."
  }
}

run "enabled_schedules_require_immutable_images" {
  command = plan

  variables {
    schedules_enabled = true
  }

  expect_failures = [aws_ecs_task_definition.paper_trading]
}

run "release_image_digests_change_together" {
  command = plan

  variables {
    app_image_digest = "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  }

  expect_failures = [aws_ecs_task_definition.paper_trading]
}
