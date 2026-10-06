# paper와 calendar 스케줄이 공유하는 RunTask 호출 실패용 DLQ입니다.
# RunTask가 수락된 뒤 발생한 Task 시작 실패, 컨테이너 종료, 애플리케이션 오류는 저장하지 않습니다.
resource "aws_sqs_queue" "scheduler_dlq" {
  name                      = "${local.name_prefix}-scheduler-dlq"
  message_retention_seconds = 1209600
  sqs_managed_sse_enabled   = true
}

resource "aws_scheduler_schedule_group" "main" {
  name = local.name_prefix
}

resource "aws_scheduler_schedule" "paper_trading" {
  name        = "${local.name_prefix}-paper-trading"
  group_name  = aws_scheduler_schedule_group.main.name
  description = "Run the paper-trading task at 09:10 on weekdays"
  state       = var.schedules_enabled ? "ENABLED" : "DISABLED"
  # cron은 한국 거래 시간 자체로 읽을 수 있게 timezone을 고정합니다.
  # 공휴일 여부는 Scheduler가 아니라 애플리케이션이 최종 판단하도록 했습니다.
  schedule_expression          = "cron(10 9 ? * MON-FRI *)"
  schedule_expression_timezone = "Asia/Seoul"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_ecs_cluster.main.arn
    role_arn = aws_iam_role.scheduler.arn

    dead_letter_config {
      arn = aws_sqs_queue.scheduler_dlq.arn
    }

    retry_policy {
      # Scheduler의 RunTask 호출 재시도 정책이며
      # ECS가 요청을 수락했지만 응답이 유실된 경우 재시도로 동일 거래일 Task가
      # 중복 실행될 수 있어 자동 재시도를 하지 않게 설정합니다.
      # 따라서 DLQ 확인과 수동 복구가 필요합니다.
      #
      # 또한 재시도 0회는 exactly-once를 보장하지 않고, 중복 실행은 애플리케이션에서 막아야 합니다.
      maximum_event_age_in_seconds = 60 // 동작하지는 않지만 AWS 허용 최솟값 세팅
      maximum_retry_attempts       = 0
    }

    ecs_parameters {
      # - family ARN(revision 생략): RunTask 호출 시점의 최신 ACTIVE revision 자동 해석
      # - Task Definition의 유일한 관리 주체인 Terraform이 새 revision을 등록해도
      # Scheduler 수정 없이 다음 예약 실행부터 자동 반영
      # 따라서 Scheduler를 변경하지 않아도 다음 예약 실행부터 새 revision이 사용됩니다.

      task_definition_arn     = aws_ecs_task_definition.paper_trading.arn_without_revision
      launch_type             = "FARGATE"
      platform_version        = "1.4.0"
      task_count              = 1
      enable_ecs_managed_tags = true
      propagate_tags          = "TASK_DEFINITION"

      network_configuration {
        # 배치 작업을 위해 NGW를 상시 운영하지 않습니다.
        # Public IP는 outbound KIS/AWS 접근에만 사용하며 paper SG에는 inbound rule이 없습니다.
        assign_public_ip = true
        subnets          = [for subnet in aws_subnet.public : subnet.id]
        security_groups  = [aws_security_group.ecs_task["paper"].id]
      }
    }
  }
}

resource "aws_scheduler_schedule" "calendar_refresh" {
  name        = "${local.name_prefix}-calendar-refresh"
  group_name  = aws_scheduler_schedule_group.main.name
  description = "Refresh the trading calendar every Thursday after market close"
  state       = var.schedules_enabled ? "ENABLED" : "DISABLED"

  # 목요일 장 종료 이후 마다 거래 캘린더를 갱신합니다.(2주치)
  schedule_expression          = "cron(10 16 ? * THU *)"
  schedule_expression_timezone = "Asia/Seoul"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_ecs_cluster.main.arn
    role_arn = aws_iam_role.scheduler.arn

    dead_letter_config {
      arn = aws_sqs_queue.scheduler_dlq.arn
    }

    retry_policy {
      maximum_event_age_in_seconds = 60
      maximum_retry_attempts       = 0
    }

    ecs_parameters {
      task_definition_arn     = aws_ecs_task_definition.calendar_refresh.arn_without_revision
      launch_type             = "FARGATE"
      platform_version        = "1.4.0"
      task_count              = 1
      enable_ecs_managed_tags = true
      propagate_tags          = "TASK_DEFINITION"

      network_configuration {
        assign_public_ip = true
        subnets          = [for subnet in aws_subnet.public : subnet.id]
        security_groups  = [aws_security_group.ecs_task["calendar"].id]
      }
    }
  }
}
