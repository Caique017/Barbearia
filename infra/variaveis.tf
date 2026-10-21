variable "regiao" {
  description = "Região da AWS. sa-east-1 (São Paulo) fica perto dos clientes."
  type        = string
  default     = "sa-east-1"
}

variable "projeto" {
  description = "Prefixo dos nomes dos recursos."
  type        = string
  default     = "barbearia"
}

variable "repo_api" {
  description = "Repositório da API no GitHub (dono/nome), autorizado a fazer deploy."
  type        = string
  default     = "Caique017/Barbearia"
}

variable "repo_web" {
  description = "Repositório do front no GitHub (dono/nome), autorizado a publicar o site."
  type        = string
  default     = "Caique017/Barbearia-web"
}

variable "tipo_instancia" {
  description = "Tipo da EC2 que roda a API."
  type        = string
  default     = "t3.small"
}

variable "classe_banco" {
  description = "Classe da instância RDS."
  type        = string
  default     = "db.t4g.micro"
}

variable "email_remetente" {
  description = "E-mail que envia a redefinição de senha. O SES manda um link de verificação pra ele."
  type        = string
}

variable "email_alertas" {
  description = "E-mail que recebe alertas (servidor fora, muitos logins com falha, orçamento estourado)."
  type        = string
}

variable "orcamento_mensal_usd" {
  description = "Limite de gasto mensal em dólares; passou de 80% vira alerta por e-mail."
  type        = number
  default     = 40
}
