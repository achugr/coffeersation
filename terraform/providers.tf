terraform {
  required_version = ">= 1.5.5"

  required_providers {
    google-beta = ">= 4.78.0"
    google      = ">= 4.78.0"
  }
}

provider "google" {
  credentials = file(var.tf_sa_key_location)
  project = var.project_id
}

provider "google-beta" {
  credentials = file(var.tf_sa_key_location)
  project = var.project_id
}