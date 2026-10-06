resource "aws_db_instance" "main" {
  identifier = "${local.name_prefix}-postgres"

  engine         = "postgres"
  engine_version = "17"
  instance_class = "db.t4g.micro"

  allocated_storage     = 20
  max_allocated_storage = 0
  storage_type          = "gp2"
  storage_encrypted     = true

  db_name                     = var.db_name
  username                    = var.db_username
  manage_master_user_password = true
  port                        = 5432

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period = 7
  backup_window           = "17:00-18:00"
  maintenance_window      = "Sun:18:00-Sun:19:00"

  auto_minor_version_upgrade = true
  apply_immediately          = false
  deletion_protection        = true
  skip_final_snapshot        = false
  final_snapshot_identifier  = "${local.name_prefix}-postgres-final"
  copy_tags_to_snapshot      = true

  performance_insights_enabled = false
  monitoring_interval          = 0

  tags = {
    Name = "${local.name_prefix}-postgres"
  }
}

