# TaskFlow DevOps

Docker image, CircleCI pipeline, and Railway notes for Module 4. Stretch goals covered here:

- a pipeline that tests, builds, and publishes the image to Docker Hub on every push to `main`
- semver and commit-SHA image tags, with `latest` kept on the newest main build
- OWASP Dependency-Check plus a Trivy image scan, with the reports stored as CI artifacts

`README.md` is left for the submission write-up. Use this file to run the tools, then summarise the pipeline URL, Docker Hub tags, and scan results in the README.

The Maven `pom.xml` version stays `0.0.1-SNAPSHOT`. Image tags do not follow that number. `pom.xml` and `src/` are owned by the concurrent backend change and are not edited here.

## Build and run locally

From the repository root:

```bash
docker build -t taskflow:latest .
docker run -p 8080:8080 taskflow:latest
```

In a second terminal:

```bash
curl http://localhost:8080/api/tasks
```

The container listens on port 8080. Stop it with `docker stop` on the container id from `docker ps`.

A local run uses the default Spring profile and the in-memory H2 database. It does not need Postgres or `SPRING_PROFILES_ACTIVE`. If the backend does not seed tasks yet, the curl response is a JSON array and may be `[]`.

The Dockerfile is the assignment's multi-stage build, with three small changes:

- Maven dependencies are downloaded in their own layer before `src/` is copied, and Maven runs with `-B` so the build does not prompt. Railway still compiles the JAR inside the image, because `target/` is not in git.
- The runtime image records `APP_VERSION` as an OCI label. CircleCI passes the release version. Railway can omit it; the label then stays `unversioned`.
- The JRE process runs as the non-root user `taskflow`. It still binds to port 8080.

The base images are unchanged: `maven:3.9-eclipse-temurin-21` to build, `eclipse-temurin:21-jre-alpine` to run.

## Versioning

`VERSION` in the repository root is the release number. It is `MAJOR.MINOR.PATCH` with no `v` prefix. The file currently contains `1.0.0`.

On a push to `main`, CircleCI pushes all of these to Docker Hub:

| Tag | Example | When it moves |
| --- | --- | --- |
| semver from `VERSION` | `debighosh/taskflow-api:1.0.0` | Each time that version is published. Pushing the same version again overwrites this tag. |
| commit SHA | `debighosh/taskflow-api:sha-92b4785abcde` | Every publish. The suffix is `git rev-parse --short=12 HEAD`. |
| `latest` | `debighosh/taskflow-api:latest` | Only on a push to `main`. A tag-only pipeline does not move it. |

The repository name comes from `DOCKERHUB_USERNAME` plus `/taskflow-api`. With that variable set to `debighosh`, the image is `debighosh/taskflow-api`.

### Cut the next version

1. Publish `1.0.0` by merging to `main` with the Docker Hub variables set. That push tags `1.0.0`, `sha-<commit>`, and `latest`.
2. Edit `VERSION` to the next number, for example `1.1.0`. Commit and push to `main`.

The `1.0.0` tag stays on Docker Hub. `latest` and `1.1.0` point at the new image. The previous `sha-...` tag still identifies the old build. Use `1.0.1` instead of `1.1.0` for a fix, or `2.0.0` for a breaking API change.

Optional git tag, if you also want the version visible on GitHub:

```bash
git tag v1.1.0
git push origin v1.1.0
```

A tag must be exactly `vMAJOR.MINOR.PATCH`, such as `v1.1.0`. That pipeline uses the tag as the image version even if `VERSION` disagrees, and it does not move `latest`. Prefer the `VERSION` file plus a push to `main` so one pipeline both publishes the semver tag and moves `latest`. Update `VERSION` when you create the git tag, or the next main build will publish the old number again.

Do not put tokens, passwords, or API keys in `VERSION` or anywhere else in the repository.

## CircleCI

The config is `.circleci/config.yml`. Two workflows share the same jobs:

- `ci` runs on every branch push, including `main`. It does not run for git tags.
- `release` runs only for tags that match `vMAJOR.MINOR.PATCH`.

Each workflow runs:

1. `test` — `mvn test` on H2. No database service is started.
2. `dependency-check` — OWASP Dependency-Check CLI 13.0.0, in parallel with the tests. This does not add a plugin to `pom.xml`.
3. `docker-image` — after tests pass, build the image, scan it with Trivy 0.76.0, and publish only from `main` or a release tag.

`dependency-check` does not block the image. A slow NVD download will not hold up the Docker Hub push. If that job fails, the workflow is still marked failed, so fix or rerun it before you treat the pipeline as green.

### What you have to do in CircleCI

1. Sign in at [circleci.com](https://circleci.com) with the GitHub account that owns `debighosh/module3-taskflow-api`.
2. Create or select the project for this repository. CircleCI reads `.circleci/config.yml`.
3. In **Project Settings → Environment Variables**, add:

   | Name | Value |
   | --- | --- |
   | `DOCKERHUB_USERNAME` | `debighosh` |
   | `DOCKERHUB_TOKEN` | Docker Hub access token (below) |
   | `NVD_API_KEY` | Optional. NVD API key. |

   These are injected into the jobs. They are not in the repository. A CircleCI context works too: create the context, add the same variables, then add `context: <name>` to the `test`, `dependency-check`, and `docker-image` jobs. The config does not name a context, so a missing context cannot fail the pipeline. Project environment variables are enough.

4. Set the variables before the first push to `main` that should publish. Without them, tests and the image build still run, and the publish step fails.
5. Leave **Pass secrets to builds from forked pull requests** off. Fork builds do not need Docker Hub credentials, and this config does not push from pull-request branches.
6. Allow pushes to `main` to build. If the project is set to build pull requests only, a merge to `main` will not publish.

Create the Docker Hub token:

1. Sign in at [hub.docker.com](https://hub.docker.com) as `debighosh`.
2. Open **Account settings → Personal access tokens**.
3. Create a token with Read and Write access (public repositories are enough for `debighosh/taskflow-api`).
4. Copy the token once into the CircleCI variable `DOCKERHUB_TOKEN`. Do not commit it, and do not put it in Railway.

Request an NVD key from [nvd.nist.gov/developers/request-an-api-key](https://nvd.nist.gov/developers/request-an-api-key). Dependency-Check calls the NVD API. Without a key the first run is rate-limited and can take a long time. The key is optional. The NVD data directory is cached between CircleCI runs for 7 days (`--nvdValidForHours 168`). Maven dependencies and the Trivy databases are cached as well. The first Trivy run also downloads a large Java vulnerability database; later runs reuse the cache. If the NVD cache upload fails because the database is large for the CircleCI plan, the HTML report is still an artifact. Remove the "Save OWASP Dependency-Check data" step and rerun; the scan itself does not depend on that cache.

### Scans

Findings do not fail the build. The README has to summarise them, and a newly published CVE in Alpine or a transitive dependency should not block the coursework publish.

- Dependency-Check runs with `--failOnCVSS 11`. CVSS scores stop at 10, so the exit code stays successful when vulnerabilities are found. The HTML and JSON reports are artifacts on the `dependency-check` job.
- Trivy runs with `--exit-code 0` and writes `trivy-image.txt` and `trivy-image.json` on the `docker-image` job.
- If the CLI cannot be downloaded, or the NVD update fails, that job fails. That is a broken scan, not a finding.

The dependency scan covers runtime-scoped jars (`compile` and `runtime`) copied with `mvn dependency:copy-dependencies`. It does not scan test-only jars. Trivy scans the image that is actually published, including Alpine packages and the application JAR.

Open the job in CircleCI and use the **Artifacts** tab. For the README, summarise the Dependency-Check HTML report and `trivy-image.txt`: what was found, what you changed, and what you left (for example an Alpine package CVE that would require leaving the base image from the assignment).

To fail the build later, lower `--failOnCVSS` or drop `--exit-code 0` and pass `--severity HIGH,CRITICAL --exit-code 1` to Trivy.

## Railway

Deploy the service from this GitHub repository. Railway detects the Dockerfile in the root and builds it. `target/` is not in git; the image build runs Maven.

1. Create a project at [railway.app](https://railway.app) and choose **Deploy from GitHub repo**.
2. Select this repository.
3. When the deployment is up, open the service → **Settings → Networking → Generate Domain**.
4. Set the public port to **8080**.
5. Check `https://<your-domain>/api/tasks`.

This path is separate from CircleCI. CircleCI publishes to Docker Hub. Railway rebuilds from GitHub. The assignment's deploy path is the GitHub Dockerfile, not the Docker Hub image.

Local Docker and CI do not set a Spring profile. Production variables belong on the Railway service. The concurrent backend change makes production read the variables below. Confirm the exact names, and any additions, against that pull request before you rely on this list:

| Variable | What to set |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `ALLOWED_ORIGINS` | The Netlify origin allowed by CORS, for example `https://<site>.netlify.app` |
| `PGHOST` | Railway Postgres host |
| `PGPORT` | Railway Postgres port |
| `PGDATABASE` | Database name |
| `PGUSER` | Database user |
| `PGPASSWORD` | Database password |

Attaching a Postgres plugin often injects the `PG*` variables for you. Do not commit any of these values. Do not set `SPRING_PROFILES_ACTIVE=prod` in CircleCI: the tests are written to stay on H2 and do not need a database.
