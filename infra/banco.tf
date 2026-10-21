resource "aws_db_subnet_group" "banco" {
  name       = var.projeto
  subnet_ids = aws_subnet.privada[*].id
}

resource "aws_db_parameter_group" "banco" {
  name   = "${var.projeto}-postgres17"
  family = "postgres17"

  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }
}

resource "aws_db_instance" "banco" {
  identifier     = var.projeto
  engine         = "postgres"
  engine_version = "17"
  instance_class = var.classe_banco

  db_name                     = "barbearia"
  username                    = "barbearia"
  manage_master_user_password = true

  allocated_storage     = 20
  max_allocated_storage = 50
  storage_type          = "gp3"
  storage_encrypted     = true

  db_subnet_group_name   = aws_db_subnet_group.banco.name
  vpc_security_group_ids = [aws_security_group.banco.id]
  parameter_group_name   = aws_db_parameter_group.banco.name
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period    = 7
  backup_window              = "06:00-07:00"
  maintenance_window         = "sun:07:30-sun:08:30"
  auto_minor_version_upgrade = true
  copy_tags_to_snapshot      = true

  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.projeto}-final"
}
