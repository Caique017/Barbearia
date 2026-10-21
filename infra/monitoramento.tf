resource "aws_sns_topic" "alertas" {
  name = "${var.projeto}-alertas"
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.alertas.arn
  protocol  = "email"
  endpoint  = var.email_alertas
}

resource "aws_cloudwatch_metric_alarm" "servidor_fora" {
  alarm_name          = "${var.projeto}-servidor-fora"
  alarm_description   = "A EC2 da API falhou no status check"
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed"
  dimensions          = { InstanceId = aws_instance.api.id }
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  alarm_actions       = [aws_sns_topic.alertas.arn]
  ok_actions          = [aws_sns_topic.alertas.arn]
}

resource "aws_cloudwatch_metric_alarm" "banco_sem_espaco" {
  alarm_name          = "${var.projeto}-banco-sem-espaco"
  alarm_description   = "Menos de 2 GB livres no RDS"
  namespace           = "AWS/RDS"
  metric_name         = "FreeStorageSpace"
  dimensions          = { DBInstanceIdentifier = aws_db_instance.banco.identifier }
  statistic           = "Minimum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 2000000000
  comparison_operator = "LessThanThreshold"
  alarm_actions       = [aws_sns_topic.alertas.arn]
}

locals {
  eventos_monitorados = {
    login_falhou       = { limite = 50, descricao = "Muitos logins com falha (forca bruta?)" }
    limite_ip_excedido = { limite = 30, descricao = "Muitas requisicoes barradas por IP" }
    acesso_negado      = { limite = 50, descricao = "Muitos acessos negados" }
  }
}

resource "aws_cloudwatch_log_metric_filter" "eventos" {
  for_each       = local.eventos_monitorados
  name           = "${var.projeto}-${each.key}"
  log_group_name = aws_cloudwatch_log_group.api.name
  pattern        = "\"evento=${each.key}\""

  metric_transformation {
    name      = each.key
    namespace = "${var.projeto}/seguranca"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "eventos" {
  for_each            = local.eventos_monitorados
  alarm_name          = "${var.projeto}-${each.key}"
  alarm_description   = each.value.descricao
  namespace           = "${var.projeto}/seguranca"
  metric_name         = each.key
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = each.value.limite
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alertas.arn]

  depends_on = [aws_cloudwatch_log_metric_filter.eventos]
}

resource "aws_cloudwatch_log_metric_filter" "erros" {
  name           = "${var.projeto}-erros"
  log_group_name = aws_cloudwatch_log_group.api.name
  pattern        = "\"ERROR\""

  metric_transformation {
    name      = "erros"
    namespace = "${var.projeto}/aplicacao"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "erros" {
  alarm_name          = "${var.projeto}-erros"
  alarm_description   = "Erros inesperados na API"
  namespace           = "${var.projeto}/aplicacao"
  metric_name         = "erros"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 10
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alertas.arn]

  depends_on = [aws_cloudwatch_log_metric_filter.erros]
}

resource "aws_budgets_budget" "mensal" {
  name         = "${var.projeto}-mensal"
  budget_type  = "COST"
  limit_amount = tostring(var.orcamento_mensal_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.email_alertas]
  }
}
