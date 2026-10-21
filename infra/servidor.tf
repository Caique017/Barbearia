data "aws_ssm_parameter" "ami_al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "aws_cloudwatch_log_group" "api" {
  name              = "/${var.projeto}/api"
  retention_in_days = 30
}

data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "api" {
  name               = "${var.projeto}-api-ec2"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.api.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "api" {
  statement {
    sid       = "LerImagem"
    actions   = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"]
    resources = [aws_ecr_repository.api.arn]
  }

  statement {
    sid       = "LoginNoEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid       = "LerParametros"
    actions   = ["ssm:GetParameter", "ssm:GetParameters"]
    resources = ["arn:aws:ssm:${var.regiao}:${data.aws_caller_identity.atual.account_id}:parameter/${var.projeto}/*"]
  }

  statement {
    sid       = "LerSenhaDoBanco"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_db_instance.banco.master_user_secret[0].secret_arn]
  }

  statement {
    sid       = "EscreverLogs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.api.arn}:*"]
  }
}

resource "aws_iam_role_policy" "api" {
  name   = "${var.projeto}-api"
  role   = aws_iam_role.api.id
  policy = data.aws_iam_policy_document.api.json
}

resource "aws_iam_instance_profile" "api" {
  name = "${var.projeto}-api"
  role = aws_iam_role.api.name
}

resource "aws_instance" "api" {
  ami                    = data.aws_ssm_parameter.ami_al2023.value
  instance_type          = var.tipo_instancia
  subnet_id              = aws_subnet.publica[0].id
  vpc_security_group_ids = [aws_security_group.api.id]
  iam_instance_profile   = aws_iam_instance_profile.api.name

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 20
    encrypted   = true
  }

  user_data = templatefile("${path.module}/modelos/user_data.sh.tftpl", {
    regiao         = var.regiao
    projeto        = var.projeto
    imagem         = aws_ecr_repository.api.repository_url
    segredo_banco  = aws_db_instance.banco.master_user_secret[0].secret_arn
    endereco_banco = aws_db_instance.banco.endpoint
    grupo_logs     = aws_cloudwatch_log_group.api.name
  })

  lifecycle {
    ignore_changes = [ami]
  }

  tags = { Name = "${var.projeto}-api" }
}

resource "aws_eip" "api" {
  instance = aws_instance.api.id
  domain   = "vpc"
}
