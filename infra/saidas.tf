output "site_url" {
  description = "Endereço público (front + /api). Também é o FRONT_URL do e-mail de redefinição."
  value       = "https://${aws_cloudfront_distribution.site.domain_name}"
}

output "github_api_variaveis" {
  description = "Variáveis do repositório da API (Settings → Secrets and variables → Actions → Variables)."
  value = {
    AWS_REGION      = var.regiao
    AWS_ROLE_ARN    = aws_iam_role.github_api.arn
    ECR_REPOSITORY  = aws_ecr_repository.api.name
    EC2_INSTANCE_ID = aws_instance.api.id
  }
}

output "github_web_variaveis" {
  description = "Variáveis do repositório do front."
  value = {
    AWS_REGION    = var.regiao
    AWS_ROLE_ARN  = aws_iam_role.github_web.arn
    S3_BUCKET     = aws_s3_bucket.site.bucket
    CLOUDFRONT_ID = aws_cloudfront_distribution.site.id
  }
}

output "parametros_smtp" {
  description = "Preencha com as credenciais SMTP do SES e as chaves do Stripe."
  value       = concat([aws_ssm_parameter.mail_usuario.name, aws_ssm_parameter.mail_senha.name], [for p in aws_ssm_parameter.stripe : p.name])
}
