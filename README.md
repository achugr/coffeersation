# coffeersation - A Minimalistic Coffee Talk Bot for Slack (yet another one)

The primary goal of coffeersation is to provide a simple and easy-to-deploy coffee talk bot for Slack. It also serves as an excellent platform to practice GCP, Kotlin, and GraalVM.

## Technical Specifications

- **Development Language**: Kotlin.
- **Frameworks Used**:
  - Slack SDK for obtaining user and channel information.
  - [Bolt](https://slack.dev/java-slack-sdk/guides/getting-started-with-bolt), a bot framework.
- **Web Server**: Ktor.
- **Cloud Computing Platform**: GraalVM for native image. The project aims to achieve a startup time of 3 seconds on cloud run for responding to Slack's webhooks. Although it starts in approximately 100 milliseconds locally, achieving the same in CloudRun is still in progress.

### Platform

The bot operates on Google Cloud Platform (GCP):
- Cloud Run for running the application.
- Cloud Task for scheduling the next introduction round and other asynchronous tasks.
- Datastore for maintaining the introduction state.

## Future Improvements
- Integrate Terraform for resource configuration to reduce manual work.
- Document the application deployment steps.
