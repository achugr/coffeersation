resource "google_project_service" "cloudbuild_api" {
  service = "cloudbuild.googleapis.com"
  timeouts {
    create = "30m"
    update = "40m"
  }
}

resource "google_project_service" "secretmanager" {
  service = "secretmanager.googleapis.com"
  timeouts {
    create = "30m"
    update = "40m"
  }
}

resource "google_secret_manager_secret" "github_token_secret" {
  secret_id = "github_token"

  replication {
    automatic = true
  }

  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "github_token_secret_version" {
  secret      = google_secret_manager_secret.github_token_secret.id
  secret_data = var.github_token
}


data "google_iam_policy" "serviceagent_secretAccessor" {
  binding {
    role    = "roles/secretmanager.secretAccessor"
    members = ["serviceAccount:service-${data.google_project.project.number}@gcp-sa-cloudbuild.iam.gserviceaccount.com"]
  }
}

resource "google_secret_manager_secret_iam_policy" "github_token_policy" {
  project     = google_secret_manager_secret.github_token_secret.project
  secret_id   = google_secret_manager_secret.github_token_secret.secret_id
  policy_data = data.google_iam_policy.serviceagent_secretAccessor.policy_data
}

resource "google_project_iam_member" "cloudbuild_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-cloudbuild.iam.gserviceaccount.com"
}

resource "google_project_iam_member" "cloudbuild_cloudrun_access" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${data.google_project.project.number}@cloudbuild.gserviceaccount.com"
}

resource "google_service_account_iam_member" "cloudbuild_sa_as_cloudrun_sa" {
  service_account_id = google_service_account.cloud_run_service_account.name
  role               = "roles/iam.serviceAccountUser"
  member = "serviceAccount:${data.google_project.project.number}@cloudbuild.gserviceaccount.com"
}

resource "google_cloudbuildv2_connection" "github_connection" {
  name     = "github_connection"
  location = var.cloudbuild_location
  github_config {
    app_installation_id = var.github_cloudbuild_app_installation_id
    authorizer_credential {
      oauth_token_secret_version = google_secret_manager_secret_version.github_token_secret_version.id
    }
  }
  depends_on = [google_secret_manager_secret_iam_policy.github_token_policy]
}

resource "google_cloudbuildv2_repository" "coffeersation_repository" {
  name              = "coffeersation_repository"
  location          = var.cloudbuild_location
  parent_connection = google_cloudbuildv2_connection.github_connection.name
  remote_uri        = var.github_repo_url
}

resource "google_cloudbuild_trigger" "repo-trigger" {
  location = var.cloudbuild_location
  repository_event_config {
    repository = google_cloudbuildv2_repository.coffeersation_repository.id
    push {
      branch = "add-terraform"
    }
  }

  filename = "cloudbuild.yaml"
}
