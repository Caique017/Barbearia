terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.69"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.regiao

  default_tags {
    tags = {
      Projeto    = var.projeto
      Gerenciado = "terraform"
    }
  }
}

data "aws_caller_identity" "atual" {}

data "aws_availability_zones" "disponiveis" {
  state = "available"
}
