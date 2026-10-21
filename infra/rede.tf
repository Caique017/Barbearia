locals {
  zonas = slice(data.aws_availability_zones.disponiveis.names, 0, 2)
}

resource "aws_vpc" "principal" {
  cidr_block           = "10.20.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = var.projeto }
}

resource "aws_internet_gateway" "principal" {
  vpc_id = aws_vpc.principal.id
}

resource "aws_subnet" "publica" {
  count                   = 2
  vpc_id                  = aws_vpc.principal.id
  cidr_block              = cidrsubnet(aws_vpc.principal.cidr_block, 8, count.index)
  availability_zone       = local.zonas[count.index]
  map_public_ip_on_launch = false

  tags = { Name = "${var.projeto}-publica-${count.index}" }
}

resource "aws_subnet" "privada" {
  count             = 2
  vpc_id            = aws_vpc.principal.id
  cidr_block        = cidrsubnet(aws_vpc.principal.cidr_block, 8, count.index + 10)
  availability_zone = local.zonas[count.index]

  tags = { Name = "${var.projeto}-privada-${count.index}" }
}

resource "aws_route_table" "publica" {
  vpc_id = aws_vpc.principal.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.principal.id
  }
}

resource "aws_route_table_association" "publica" {
  count          = 2
  subnet_id      = aws_subnet.publica[count.index].id
  route_table_id = aws_route_table.publica.id
}

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "api" {
  name        = "${var.projeto}-api"
  description = "API: so recebe trafego do CloudFront"
  vpc_id      = aws_vpc.principal.id

  ingress {
    description     = "CloudFront para a API"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]
  }

  egress {
    description = "Saida para ECR, SSM, SES e banco"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "banco" {
  name        = "${var.projeto}-banco"
  description = "Postgres: so a API acessa"
  vpc_id      = aws_vpc.principal.id

  ingress {
    description     = "API para o Postgres"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.api.id]
  }
}
