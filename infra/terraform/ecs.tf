resource "aws_ecs_cluster" "main" {
  name = local.name_prefix

  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

resource "aws_cloudwatch_log_group" "paper_app" {
  name              = "/ecs/${local.name_prefix}/paper-app"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "paper_strategy" {
  name              = "/ecs/${local.name_prefix}/paper-strategy"
  retention_in_days = 14
}

resource "aws_cloudwatch_log_group" "calendar_refresh" {
  name              = "/ecs/${local.name_prefix}/calendar-refresh"
  retention_in_days = 14
}

resource "aws_ecs_task_definition" "paper_trading" {
  family                   = "${local.name_prefix}-paper-trading"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution["paper"].arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  lifecycle {
    precondition {
      condition = (
        (var.app_image_digest == null && var.strategy_image_digest == null)
        || (var.app_image_digest != null && var.strategy_image_digest != null)
      )
      error_message = "app_image_digest and strategy_image_digest must be set together."
    }

    precondition {
      condition = (
        var.schedules_enabled == false
        || (var.app_image_digest != null && var.strategy_image_digest != null)
      )
      error_message = "Both immutable image digests must be set before schedules can be enabled."
    }
  }

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  volume {
    name = "app-tmp"
  }

  volume {
    name = "strategy-tmp"
  }

  container_definitions = jsonencode([
    {
      name                   = "zscore-strategy"
      image                  = local.strategy_image
      essential              = true
      cpu                    = 128
      memoryReservation      = 128
      readonlyRootFilesystem = true
      stopTimeout            = 30
      linuxParameters        = { initProcessEnabled = true }
      mountPoints = [{
        sourceVolume  = "strategy-tmp"
        containerPath = "/tmp"
        readOnly      = false
      }]
      healthCheck = {
        command = [
          "CMD-SHELL",
          "curl --fail --silent --show-error --max-time 1 http://127.0.0.1:9000/health || exit 1",
        ]
        interval    = 5
        timeout     = 2
        retries     = 10
        startPeriod = 2
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.paper_strategy.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
    {
      name                   = "app-live"
      image                  = local.app_image
      essential              = true
      cpu                    = 384
      memoryReservation      = 768
      readonlyRootFilesystem = true
      stopTimeout            = 30
      linuxParameters        = { initProcessEnabled = true }
      dependsOn              = [{ containerName = "zscore-strategy", condition = "HEALTHY" }]
      mountPoints = [{
        sourceVolume  = "app-tmp"
        containerPath = "/tmp"
        readOnly      = false
      }]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "paper-trading" },
        { name = "AUTOTRADER_DB_URL", value = "jdbc:postgresql://${aws_db_instance.main.address}:5432/${var.db_name}" },
        { name = "ZSCORE_SERVER_BASE_URL", value = "http://127.0.0.1:9000" },
        { name = "ZSCORE_SERVER_REQUEST_TIMEOUT", value = "2s" },
        { name = "KIS_PAPER_ACCESS_TOKEN_CACHE_ENABLED", value = "false" },
        { name = "JAVA_TOOL_OPTIONS", value = "-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError" },
      ]
      secrets = [
        { name = "AUTOTRADER_DB_USERNAME", valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:username::" },
        { name = "AUTOTRADER_DB_PASSWORD", valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:password::" },
        { name = "KIS_PAPER_APP_KEY", valueFrom = "${aws_secretsmanager_secret.paper_trading.arn}:KIS_PAPER_APP_KEY::" },
        { name = "KIS_PAPER_APP_SECRET", valueFrom = "${aws_secretsmanager_secret.paper_trading.arn}:KIS_PAPER_APP_SECRET::" },
        { name = "KIS_PAPER_ACCOUNT_NUMBER", valueFrom = "${aws_secretsmanager_secret.paper_trading.arn}:KIS_PAPER_ACCOUNT_NUMBER::" },
        { name = "KIS_PAPER_ACCOUNT_PRODUCT_CODE", valueFrom = "${aws_secretsmanager_secret.paper_trading.arn}:KIS_PAPER_ACCOUNT_PRODUCT_CODE::" },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.paper_app.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
  ])
}

resource "aws_ecs_task_definition" "calendar_refresh" {
  family                   = "${local.name_prefix}-calendar-refresh"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution["calendar"].arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  volume {
    name = "app-tmp"
  }

  container_definitions = jsonencode([
    {
      name                   = "market-calendar-refresh"
      image                  = local.app_image
      essential              = true
      cpu                    = 512
      memoryReservation      = 768
      readonlyRootFilesystem = true
      stopTimeout            = 30
      linuxParameters        = { initProcessEnabled = true }
      mountPoints = [{
        sourceVolume  = "app-tmp"
        containerPath = "/tmp"
        readOnly      = false
      }]
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "market-calendar-refresh" },
        { name = "AUTOTRADER_POSTGRES_URL", value = "jdbc:postgresql://${aws_db_instance.main.address}:5432/${var.db_name}" },
        { name = "JAVA_TOOL_OPTIONS", value = "-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError" },
      ]
      secrets = [
        { name = "POSTGRES_USER", valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:username::" },
        { name = "POSTGRES_PASSWORD", valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:password::" },
        { name = "KIS_PROD_APP_KEY", valueFrom = "${aws_secretsmanager_secret.calendar_refresh.arn}:KIS_PROD_APP_KEY::" },
        { name = "KIS_PROD_APP_SECRET", valueFrom = "${aws_secretsmanager_secret.calendar_refresh.arn}:KIS_PROD_APP_SECRET::" },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.calendar_refresh.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
  ])
}
