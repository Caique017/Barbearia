resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "random_password" "origem" {
  length  = 40
  special = false
}

resource "aws_ssm_parameter" "jwt" {
  name  = "/${var.projeto}/api-security-token-secret"
  type  = "SecureString"
  value = random_password.jwt.result
}

resource "aws_ssm_parameter" "origem" {
  name  = "/${var.projeto}/origem-segredo"
  type  = "SecureString"
  value = random_password.origem.result
}

resource "aws_ssm_parameter" "front_url" {
  name  = "/${var.projeto}/front-url"
  type  = "String"
  value = "https://${aws_cloudfront_distribution.site.domain_name}"
}

resource "aws_ssm_parameter" "mail_remetente" {
  name  = "/${var.projeto}/mail-remetente"
  type  = "String"
  value = var.email_remetente
}

resource "aws_ssm_parameter" "mail_usuario" {
  name  = "/${var.projeto}/mail-username"
  type  = "SecureString"
  value = "PREENCHER-COM-O-USUARIO-SMTP-DO-SES"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "mail_senha" {
  name  = "/${var.projeto}/mail-password"
  type  = "SecureString"
  value = "PREENCHER-COM-A-SENHA-SMTP-DO-SES"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_sesv2_email_identity" "remetente" {
  email_identity = var.email_remetente
}
