# evaluation-service

Each reviewer's filled-in rubric for one team and one assessment, stored in MongoDB:

- a level from 0 to 4 and an optional comment for each criterion
- an overall comment

An evaluation is a draft until it's submitted. After that it's locked for good.

This service is part of **Assay**, a final-year-project grading platform. The architecture is described in
[fyp-grading-docker-compose/docs/architecture.md](https://github.com/FirasKhalife/fyp-grading-docker-compose/blob/master/docs/architecture.md).

## API

| Method and path | Who | What |
|---|---|---|
| `GET /api/my-evaluations/{team}/{assessment}` | reviewer | My evaluation, or a blank form from the current rubric |
| `PUT /api/my-evaluations/{team}/{assessment}` | reviewer | Autosave the full form state (the first save creates the draft) |
| `POST /api/my-evaluations/{team}/{assessment}/submit` | reviewer | Submit and lock. A 422 lists the unscored criteria |
| `GET /api/evaluations?teamId=&assessment=` | admin; reviewers once the grade is final | Everyone's levels and comments for that team |
| `GET /api/evaluations/{id}/history` | admin | The draft audit trail |
| `GET /internal/evaluations/summary`, `/internal/evaluations/submitted` | admin-service | Statuses, and the details of released results |

## Events and calls

- **Publishes** `assay.activity` when a draft starts, a level changes or an evaluation is submitted.
- **Calls** admin-service:
  - To check that the reviewer holds the assessment's role on the team.
  - To hand over the grade on submit. The hand-off happens before the lock and is idempotent, so a failure leaves
    an editable draft and a retry is safe.
- **Calls** rubric-service for the criteria snapshot a new draft starts from.

## Rules it enforces

- Score = Σ(level × criterion weight %) ÷ 4, on 0 to 100.
- The score is computed here, never taken from the client. A draft shows its score so far.
- The criteria are a snapshot of the rubric taken when the draft starts, so editing a rubric never changes an
  existing evaluation.

## Run it

The whole stack runs from [fyp-grading-docker-compose](https://github.com/FirasKhalife/fyp-grading-docker-compose).
To run this service on its own (Java 25, Maven):

```bash
./mvnw spring-boot:run
```

It needs MongoDB, RabbitMQ and Keycloak, plus admin-service and rubric-service through Eureka.

## Test

```bash
./mvnw test
```

`ScoringTest` checks the brief's example (levels 4, 3, 2 on weights 20/40/40 give 70.0) and the rounding.

`EvaluationApiTest` runs on a real MongoDB, with the other services stubbed. It covers:

- the blank form, the draft, and a submit with unscored criteria
- submitting, locking, and safe retries
- the audit trail and the activity events
- a reviewer without the role on the team
- admin-service being down
- isolation between copies Docker must be running for the Testcontainers tests.

## Notes for reviewers

- Spring Boot 4.0 and Spring Cloud 2025.1 on Java 25. The image is layered and runs as a non-root user.
- Every call needs a Keycloak token and an `X-Demo-Session` header.
- Each visitor gets a private copy of the data:
  - Every row is scoped to the visitor's session (every query names the session).
  - A visitor's copy is created from `src/main/resources/demo/seed.json`.
  - Every copy is deleted at midnight (Europe/Paris).
- Errors are RFC 9457 problem details with a stable `code`.
- Roles are checked with method security. Identity always comes from the token.
- Actuator exposes only health, info and prometheus, and the gateway doesn't route it.
- The shared API contract lives in the workspace (`contract/openapi.yaml`). This service publishes its own part
  at `/v3/api-docs`.

## Lint and CI

- **Formatting:** Spotless with palantir-java-format. `./mvnw spotless:apply` fixes it.
- **Lint:** Checkstyle (`checkstyle.xml`) catches likely bugs and sloppy code.
- The build checks both in the `validate` phase, so `./mvnw verify` runs the lint, the build and every test.
- **GitHub Actions** (`.github/workflows/ci.yml`) runs on every pull request and on pushes to `master`:
  - the lint
  - `verify`, including the Testcontainers tests
  - the Docker image: built, then scanned with Trivy (the report goes to GitHub code scanning; a HIGH or CRITICAL
    finding with a fix fails the build). From `master` it is pushed to `ghcr.io/firaskhalife/fyp-grading-evaluation-service`
    (tags `latest` and the short commit SHA) with an SBOM and build provenance, and signed with cosign
- **Dependabot** proposes weekly updates for Maven, the Dockerfile's base images and the GitHub Actions, all pinned
  (base images by digest, actions by commit SHA).

The `Dockerfile` (the same in every service repo) builds a small Java 25 runtime with `jlink` on Alpine, runs as
an unprivileged user, and ships an AOT cache made by a training run at build time, so the service starts in
about half the time. Accepted unfixable scan findings, if any, are listed in `.trivyignore` with a reason.
