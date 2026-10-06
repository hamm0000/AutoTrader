resource "aws_secretsmanager_secret" "paper_trading" {
  name                    = "${local.name_prefix}/paper-trading"
  description             = "KIS paper trading credentials; value is populated outside Terraform"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret" "calendar_refresh" {
  name                    = "${local.name_prefix}/calendar-refresh"
  description             = "KIS production read-only holiday query credentials; value is populated outside Terraform"
  recovery_window_in_days = 7
}

