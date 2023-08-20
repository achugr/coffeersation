data "google_project" "project" {}

resource "google_project_service" "cloud_task_api" {
  service = "cloudtasks.googleapis.com"

  timeouts {
    create = "30m"
    update = "40m"
  }

  disable_dependent_services = true
}

resource "google_project_service" "iam_api" {
  service = "iam.googleapis.com"

  timeouts {
    create = "30m"
    update = "40m"
  }
}

resource "google_project_service" "datastore_api" {
  service            = "datastore.googleapis.com"

  timeouts {
    create = "30m"
    update = "40m"
  }
}

resource "google_project_service" "run_api" {
  service = "run.googleapis.com"

  timeouts {
    create = "30m"
    update = "40m"
  }
}

resource "google_service_account" "cloud_run_service_account" {
  project      = var.project_id
  account_id   = "cloud-run-service-account"
  display_name = "Service account for Cloud Run"
}

resource "google_secret_manager_secret_iam_member" "internal_sa_key_access" {
  secret_id  = google_secret_manager_secret.coffeersation_internal_sa_key_secret.id
  role       = "roles/secretmanager.secretAccessor"
  member     = "serviceAccount:${google_service_account.cloud_run_service_account.email}"
  depends_on = [google_secret_manager_secret.coffeersation_internal_sa_key_secret]
}

resource "google_secret_manager_secret_iam_member" "slack_signing_secret_access" {
  secret_id  = google_secret_manager_secret.slack_signing_secret.id
  role       = "roles/secretmanager.secretAccessor"
  member     = "serviceAccount:${google_service_account.cloud_run_service_account.email}"
  depends_on = [google_secret_manager_secret.slack_signing_secret]
}

resource "google_secret_manager_secret_iam_member" "slack_bot_token_access" {
  secret_id  = google_secret_manager_secret.slack_bot_token.id
  role       = "roles/secretmanager.secretAccessor"
  member     = "serviceAccount:${google_service_account.cloud_run_service_account.email}"
  depends_on = [google_secret_manager_secret.slack_bot_token]
}

resource "google_cloud_run_v2_service" "app_cloud_run" {
  name     = "coffeersation"
  location = var.app_location
  template {
    service_account = google_service_account.cloud_run_service_account.email
    containers {
      image = "gcr.io/coffeersation/app"
      volume_mounts {
        mount_path = "/creds"
        name       = "GCP_CREDENTIALS_FILE"
      }
      env {
        name = "SLACK_BOT_TOKEN"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.slack_bot_token.secret_id
            version = "latest"
          }
        }
      }
      env {
        name = "SLACK_SIGNING_SECRET"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.slack_signing_secret.secret_id
            version = "latest"
          }
        }
      }
      env {
        name  = "GCP_PROJECT"
        value = var.project_id
      }
      env {
        name  = "WEBHOOK_BASE_URL"
        value = var.coffeersation_url
      }
      env {
        name  = "WEBHOOK_SECRET"
        value = var.webhook_secret
      }
      env {
        name  = "TRIGGER_QUEUE"
        value = google_cloud_tasks_queue.tasks_queue.name
      }
      env {
        name  = "INIT_QUEUE"
        value = google_cloud_tasks_queue.tasks_queue.name
      }
      env {
        name  = "WEBHOOK_QUEUE_REGION"
        value = google_cloud_tasks_queue.tasks_queue.location
      }
      env {
        name  = "WEBHOOK_AUDIENCE"
        value = var.coffeersation_url
      }
      env {
        name  = "WEBHOOK_SERVICE_ACCOUNT"
        value = google_service_account.coffeersation_internal_sa.email
      }
      env {
        name  = "GCP_CREDENTIALS_FILE"
        value = "/creds/queue-sa-creds.json"
      }
    }
    volumes {
      name = "GCP_CREDENTIALS_FILE"
      secret {
        secret = google_secret_manager_secret.coffeersation_internal_sa_key_secret.secret_id
        items {
          version = "latest"
          path    = "queue-sa-creds.json"
          mode    = 0 # use default 0444
        }
      }
    }
  }
  depends_on = [google_project_service.run_api, google_service_account.cloud_run_service_account]
}

resource "google_service_account_iam_binding" "cloud_run_sa_as_internal_sa" {
  service_account_id = google_service_account.coffeersation_internal_sa.name
  role               = "roles/iam.serviceAccountUser"
  members            = [
    "serviceAccount:${google_service_account.coffeersation_internal_sa.email}"
  ]
}

resource "google_cloud_run_service_iam_binding" "noauth_iam_policy" {
  location = google_cloud_run_v2_service.app_cloud_run.location
  project  = google_cloud_run_v2_service.app_cloud_run.project
  service  = google_cloud_run_v2_service.app_cloud_run.name
  role     = "roles/run.invoker"
  members  = [
    "allUsers"
  ]
}

resource "google_firestore_database" "firestore_database" {
  project     = var.project_id
  location_id = var.app_location
  name        = "(default)"
  type        = "DATASTORE_MODE"
}

resource "google_cloud_tasks_queue" "tasks_queue" {
  name     = "tasks"
  location = "us-east1"
}

resource "google_service_account" "coffeersation_internal_sa" {
  project    = var.project_id
  account_id = "coffeersation-internal-sa"
}

resource "google_project_iam_member" "queue_sa_access" {
  project  = var.project_id
  for_each = toset([
    "roles/cloudtasks.enqueuer",
    "roles/run.invoker",
    "roles/datastore.user"
  ])
  role   = each.key
  member = "serviceAccount:${google_service_account.coffeersation_internal_sa.email}"
}

resource "google_service_account_key" "coffeersation_internal_sa_key" {
  service_account_id = google_service_account.coffeersation_internal_sa.name
}

resource "google_secret_manager_secret" "coffeersation_internal_sa_key_secret" {
  project   = var.project_id
  secret_id = "coffeersation_internal_sa_key"

  replication {
    automatic = true
  }
}

resource "google_secret_manager_secret_version" "queue_sa_key_secret_version" {
  secret = google_secret_manager_secret.coffeersation_internal_sa_key_secret.id

  secret_data = base64decode(google_service_account_key.coffeersation_internal_sa_key.private_key)
}

resource "google_secret_manager_secret" "slack_bot_token" {
  project   = var.project_id
  secret_id = "slack_bot_token"

  replication {
    automatic = true
  }
}

resource "google_secret_manager_secret_version" "slack_bot_token_version" {
  secret = google_secret_manager_secret.slack_bot_token.id

  secret_data = var.slack_bot_token
}

resource "google_secret_manager_secret" "slack_signing_secret" {
  project   = var.project_id
  secret_id = "slack_signing_secret"

  replication {
    automatic = true
  }
}

resource "google_secret_manager_secret_version" "slack_signing_secret_version" {
  secret = google_secret_manager_secret.slack_signing_secret.id

  secret_data = var.slack_signing_secret
}