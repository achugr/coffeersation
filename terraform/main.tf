terraform {
  required_version = ">= 1.5.5"

  required_providers {
    # Cloud Run support was added on 3.3.0
    google = ">= 4.51.0"
  }
}

provider "google" {
  credentials = file("./gcp-key.json")

  project = "coffeersation"
  region  = "us-east1"
}

resource "google_project_service" "cloud_task_api" {
  service = "cloudtasks.googleapis.com"

  timeouts {
    create = "30m"
    update = "40m"
  }

  disable_dependent_services = true
}

resource "google_cloud_tasks_queue" "tasks_queue" {
  name = "tasks"
  location = "us-east1"
}