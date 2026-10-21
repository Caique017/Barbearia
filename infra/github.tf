resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1", "1c58a3a8518e8759bf075b76b750d4f2df264fcd"]
}

data "aws_iam_policy_document" "github_api_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.repo_api}:environment:producao"]
    }
  }
}

resource "aws_iam_role" "github_api" {
  name               = "${var.projeto}-github-deploy-api"
  assume_role_policy = data.aws_iam_policy_document.github_api_assume.json
}

data "aws_iam_policy_document" "github_api" {
  statement {
    sid       = "LoginNoEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EnviarImagem"
    actions = [
      "ecr:BatchCheckLayerAvailability", "ecr:BatchGetImage", "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload", "ecr:PutImage", "ecr:UploadLayerPart",
    ]
    resources = [aws_ecr_repository.api.arn]
  }

  statement {
    sid     = "RodarDeployNaInstancia"
    actions = ["ssm:SendCommand"]
    resources = [
      aws_instance.api.arn,
      "arn:aws:ssm:${var.regiao}::document/AWS-RunShellScript",
    ]
  }

  statement {
    sid       = "AcompanharDeploy"
    actions   = ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_api" {
  name   = "${var.projeto}-github-deploy-api"
  role   = aws_iam_role.github_api.id
  policy = data.aws_iam_policy_document.github_api.json
}

data "aws_iam_policy_document" "github_web_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.repo_web}:environment:producao"]
    }
  }
}

resource "aws_iam_role" "github_web" {
  name               = "${var.projeto}-github-deploy-web"
  assume_role_policy = data.aws_iam_policy_document.github_web_assume.json
}

data "aws_iam_policy_document" "github_web" {
  statement {
    sid       = "PublicarSite"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.site.arn]
  }

  statement {
    sid       = "ArquivosDoSite"
    actions   = ["s3:PutObject", "s3:DeleteObject", "s3:GetObject"]
    resources = ["${aws_s3_bucket.site.arn}/*"]
  }

  statement {
    sid       = "LimparCache"
    actions   = ["cloudfront:CreateInvalidation"]
    resources = [aws_cloudfront_distribution.site.arn]
  }
}

resource "aws_iam_role_policy" "github_web" {
  name   = "${var.projeto}-github-deploy-web"
  role   = aws_iam_role.github_web.id
  policy = data.aws_iam_policy_document.github_web.json
}
