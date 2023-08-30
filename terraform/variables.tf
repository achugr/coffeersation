variable "project_id" {
  default = "coffeersation"
}

variable "cloudbuild_location" {
  type    = string
  default = "us-central1"
}

variable "app_location" {
  type    = string
  default = "us-east1"
}

variable "tf_sa_key_location" {
  type    = string
  default = "./gcp-key.json"
}

variable "webhook_secret" {
  type      = string
  sensitive = true
}

variable "github_cloudbuild_app_installation_id" {
  type    = string
  default = "40798843"
}

variable "github_repo_url" {
  type    = string
  default = "https://github.com/achugr/coffeersation.git"
}
variable "github_token" {
  type      = string
  sensitive = true
}

variable "slack_signing_secret" {
  type      = string
  sensitive = true
}

variable "slack_bot_token" {
  type      = string
  sensitive = true
}

variable "coffeersation_url" {
  type      = string
  sensitive = true
}
