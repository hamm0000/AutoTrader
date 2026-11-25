# Task가 끝나면 ECS가 보내는 이벤트를 로그 그룹에 저장합니다.
# 정상 실행의 종료 코드를 확인하고, 실패 원인은 Task ARN으로 찾습니다.

resource "aws_cloudwatch_log_group" "task_stopped" {
  name              = "/aws/events/${local.name_prefix}/ecs-task-stopped"
  retention_in_days = 90
}

# ECS 이벤트는(AWS 내부 서비스여서) default 버스로 들어오므로 Rule도 같은 버스에 둡니다.
resource "aws_cloudwatch_event_rule" "task_stopped" {
  name           = "${local.name_prefix}-ecs-task-stopped"
  description    = "Record all stopped tasks in this cluster, including successful runs"
  event_bus_name = "default"

  # 정상 종료와 컨테이너가 시작하지 못한 경우도 모두 기록합니다.
  # 따라서 종료 코드나 stopCode는 조건에 넣지 않습니다.
  # 같은 클러스터이면 이후 추가되는 Task family도 기록합니다.
  event_pattern = jsonencode({
    source        = ["aws.ecs"]
    "detail-type" = ["ECS Task State Change"]
    detail = {
      clusterArn = [aws_ecs_cluster.main.arn]
      lastStatus = ["STOPPED"]
    }
  })
}

# CloudWatch Logs는 RoleArn 없이 로그 리소스 정책으로 기록 권한을 줍니다.
# 정책은 계정과 리전에 등록하고, Resource로 이 로그 그룹만 허용합니다.
# Task role과 Scheduler 역할에는 이 권한을 추가하지 않습니다.
resource "aws_cloudwatch_log_resource_policy" "task_stopped" {
  policy_name = "${local.name_prefix}-ecs-task-stopped"
  policy_document = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # 이 로그 그룹 안에 스트림을 만드는 권한
        Sid    = "CreateTaskStoppedLogStreams"
        Effect = "Allow"
        Principal = {
          Service = ["events.amazonaws.com", "delivery.logs.amazonaws.com"]
        }
        Action   = ["logs:CreateLogStream"]
        Resource = "${aws_cloudwatch_log_group.task_stopped.arn}:*"
      },
      {
        # 로그 스트림에 이벤트를 쓰는 권한
        # 이 Rule에서 보낸 요청만 허용합니다.
        Sid    = "WriteTaskStoppedEvents"
        Effect = "Allow"
        Principal = {
          Service = ["events.amazonaws.com", "delivery.logs.amazonaws.com"]
        }
        Action   = ["logs:PutLogEvents"]
        Resource = "${aws_cloudwatch_log_group.task_stopped.arn}:*:*"
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_cloudwatch_event_rule.task_stopped.arn
          }
        }
      },
    ]
  })
}

# stoppedReason에 따옴표나 개행이 있어도 기록이 깨지지 않게 원본 JSON을 보냅니다.
# 컨테이너별 결과와 시작, 종료 시각도 원본에 있는 대로 남습니다.
# exitCode가 없으면 정상 종료로 보지 않습니다.
# RunTask의 environment override도 기록될 수 있으므로 비밀값은 secrets로만 주입합니다.
resource "aws_cloudwatch_event_target" "task_stopped" {
  event_bus_name = aws_cloudwatch_event_rule.task_stopped.event_bus_name
  rule           = aws_cloudwatch_event_rule.task_stopped.name
  target_id      = "RecordStoppedTask"
  arn            = aws_cloudwatch_log_group.task_stopped.arn

  # 재시도는 로그 전달에만 적용됩니다. 매매 Task를 재시도xxx

  # 권한 설정 전에 로그를 보내지 않도록 정책 생성 후 Target을 연결합니다.
  depends_on = [aws_cloudwatch_log_resource_policy.task_stopped]
}
