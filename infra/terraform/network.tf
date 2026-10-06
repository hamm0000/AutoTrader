locals {
  # 같은 ECS 클러스터에서 실행되지만 paper Task는 KIS 모의투자 29443를 사용하고
  # calendar Task는 실전 환경의 국내휴장일 조회 9443를 사용합니다.
  # Task별 SG에서 허용할 KIS 포트를 분리해서 Task Definition이 잘못 변경되어도
  # paper task가 실전 환경의 KIS API에 연결되는 것을 네트워크 계층에서 차단합니다.
  ecs_task_networks = {
    paper = {
      name_suffix     = "paper-task"
      kis_port        = 29443
      kis_description = "KIS paper-trading API"
    }
    calendar = {
      name_suffix     = "calendar-task"
      kis_port        = 9443
      kis_description = "KIS production read-only calendar API"
    }
  }
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true // VPC 내부에서 DNS 이름을 IP 주소로 조회할 수 있게 함
  enable_dns_hostnames = true // public IP가 있는 리소스에 AWS DNS 호스트 이름을 사용할 수 있게 함

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

resource "aws_subnet" "public" {
  for_each = { for index, az in local.azs : az => index }

  vpc_id            = aws_vpc.main.id
  availability_zone = each.key
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, each.value)

  # Subnet의 리소스에는 public IP를 자동 할당하지 않는다.
  # 예약된 Fargate Task만 Scheduler에서 assign_public_ip=true로 실행해 public IP를 할당받고
  # IGW를 통해 KIS에 접근 및 AWS의 각종 작업(이미지 pull,Secrets Manager,CloudWatch)을 수행하여 NAT Gateway의 상시 비용을 없앰.
  map_public_ip_on_launch = false

  tags = {
    Name = "${local.name_prefix}-public-${each.value + 1}"
    Tier = "public"
  }
}

resource "aws_subnet" "private" {
  for_each = { for index, az in local.azs : az => index }

  vpc_id            = aws_vpc.main.id
  availability_zone = each.key
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, each.value + 10)

  tags = {
    Name = "${local.name_prefix}-private-${each.value + 1}"
    Tier = "private"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${local.name_prefix}-public"
  }
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

# RDS private subnet이 VPC 기본(main)route table 변경의 영향을 받지 않도록
# VPC 내부 local 경로만 있는 전용 route table을 명시적으로 연결
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-private"
  }
}

resource "aws_route_table_association" "private" {
  for_each = aws_subnet.private

  subnet_id      = each.value.id
  route_table_id = aws_route_table.private.id
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db"
  subnet_ids = [for subnet in aws_subnet.private : subnet.id]

  tags = {
    Name = "${local.name_prefix}-db"
  }
}

resource "aws_security_group" "ecs_task" {
  for_each = local.ecs_task_networks

  name        = "${local.name_prefix}-${each.value.name_suffix}"
  description = "No inbound access; controlled outbound access for ${each.key} Fargate task"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-${each.value.name_suffix}"
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds"
  description = "PostgreSQL access from AutoTrader ECS tasks only"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-rds"
  }
}


# 예약된 Fargate Task는 외부 요청을 받지 않으므로 inbound rule을 설정하지 않습니다.
# Public IP는 인터넷 outbound에 사용하며, 외부에서 시작한 연결은 SG가 차단합니다.
# SG는 stateful이므로 Task가 시작한 연결의 응답은 별도 inbound rule 없이 허용됩니다.
# AmazonProvidedDNS는 SG의 필터 대상이 아니므로 53번 egress rule을 추가하지 않습니다.

# 두 Task는 VPC Endpoint 대신 IGW를 통해
# AWS의 각종 작업(이미지 pull,Secrets Manager,CloudWatch logs) public endpoint에 접근합니다.
# SG는 도메인을 조건으로 제한할 수 없으므로 모든 IPv4 목적지의 TCP 443 outbound를 허용합니다.
resource "aws_vpc_security_group_egress_rule" "task_https" {
  for_each = local.ecs_task_networks

  security_group_id = aws_security_group.ecs_task[each.key].id
  description       = "AWS HTTPS APIs required to start and operate the Fargate task"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_egress_rule" "task_kis" {
  for_each = local.ecs_task_networks

  security_group_id = aws_security_group.ecs_task[each.key].id
  description       = each.value.kis_description
  ip_protocol       = "tcp"
  from_port         = each.value.kis_port
  to_port           = each.value.kis_port
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_egress_rule" "task_postgres" {
  for_each = local.ecs_task_networks

  security_group_id            = aws_security_group.ecs_task[each.key].id
  description                  = "PostgreSQL in the private subnets"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = aws_security_group.rds.id
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_task" {
  for_each = local.ecs_task_networks

  security_group_id            = aws_security_group.rds.id
  description                  = "PostgreSQL from the ${each.key} ECS task"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = aws_security_group.ecs_task[each.key].id
}
