# TaskFlow DevOps

Docker image, CircleCI pipeline, and Railway notes for Module 4 Assignment. Stretch goals are covered here:

- a pipeline that tests, builds, and publishes the image to Docker Hub on every push to `main`
- semver and commit-SHA image tags, with `latest` kept on the newest main build
- a dependency scan in CI, with reports stored as artifacts: Trivy on the runtime jars, and OWASP Dependency-Check when an NVD API key is set, plus a Trivy image scan

`README.md` is left for the submission write-up. Use this file to run the tools, then summarise the pipeline URL, Docker Hub tags, and scan results in the README.

The Maven `pom.xml` version stays `0.0.1-SNAPSHOT`. Image tags do not follow that number. `pom.xml` and `src/` are changed in [PR #2](https://github.com/debighosh/module3-taskflow-api/pull/2) and are not edited here.

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

A local run uses the default Spring profile and the in-memory H2 database. It does not need Postgres or `SPRING_PROFILES_ACTIVE`. On this branch the task list may be `[]`. After PR #2, a fresh database includes six sample tasks.

The Dockerfile is the assignment's multi-stage build, with three small changes:

- Maven dependencies are downloaded in their own layer before `src/` is copied, and Maven runs with `-B` so the build does not prompt. Railway still compiles the JAR inside the image, because `target/` is not in git.
- The runtime image records `APP_VERSION` as an OCI label. CircleCI passes the release version. Railway can omit it; the label then stays `unversioned`.
- The JRE process runs as the non-root user `taskflow`. With `PORT` unset it listens on 8080. After PR #2 the app uses `PORT` when Railway sets it, and still falls back to 8080.

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
2. Edit `VERSION` to the next number and push that commit to `main`.

The previous semver tag stays on Docker Hub. `latest` and the new semver tag point at the new image. The previous `sha-...` tag still identifies the old build. Use a patch bump for a fix, a minor bump for a compatible change, or a major bump for a breaking API change.

### Recommended release order

The following order was used so Docker Hub keeps two versions and the scan artifacts are a before/after pair for stretch goal 5.

1. Add the CircleCI Docker Hub variables before anything is merged to `main`.
2. Merge [PR #1](https://github.com/debighosh/module3-taskflow-api/pull/1) first. That `main` push publishes `1.0.0`, `sha-<commit>`, and `latest` from Spring Boot 3.2.2. Download `trivy-dependencies.txt` from the `dependency-check` job and `trivy-image.txt` from the `docker-image` job. If `NVD_API_KEY` is set, also download the Dependency-Check HTML report. Those artifacts are the baseline scan.
3. Update [PR #2](https://github.com/debighosh/module3-taskflow-api/pull/2) with `main`, set `VERSION` to `1.1.0` on that branch, then merge it. PR #2 upgrades Spring Boot to 3.5.16. That `main` push publishes `1.1.0`, `sha-<commit>`, and `latest`.
4. Compare the `1.1.0` scan artifacts with the `1.0.0` artifacts in the submission README.

`VERSION` has to be `1.1.0` in the commit that merges PR #2. A merge that still contains `1.0.0` publishes `1.0.0` again and replaces the Spring Boot 3.2.2 image on that tag. The earlier `sha-` tag would still point at the baseline build.

Deploy to Railway after PR #2 is on `main`. That release is the one that listens on Railway's `PORT`.

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
2. `dependency-check` — in parallel with the tests. Copies the runtime jars and scans them with Trivy 0.76.0. If `NVD_API_KEY` is set, it also runs OWASP Dependency-Check CLI 13.0.0. No plugin is added to `pom.xml`.
3. `docker-image` — after tests pass, build the image, scan it with Trivy 0.76.0, and publish only from `main` or a release tag.

`docker-image` waits only for `test`. A skipped or failed dependency scan does not block the Docker Hub publish.

### What I had to do in CircleCI

1. Sign in at [circleci.com](https://circleci.com) with the GitHub account that owns `debighosh/module3-taskflow-api`.
2. Create or select the project for this repository. CircleCI reads `.circleci/config.yml`.
3. In **Project Settings → Environment Variables**, add:

   | Name | Value |
   | --- | --- |
   | `DOCKERHUB_USERNAME` | `debighosh` |
   | `DOCKERHUB_TOKEN` | Docker Hub access token (below) |
   | `NVD_API_KEY` | Optional. Leave it unset until you have a key. A blank value is treated as unset. |

   These are injected into the jobs. They are not in the repository. A CircleCI context works too: create the context, add the same variables, then add `context: <name>` to the `test`, `dependency-check`, and `docker-image` jobs. The config does not name a context, so a missing context cannot fail the pipeline. Project environment variables are enough.

4. Set the variables before the first push to `main` that should publish. Without them, tests and the image build still run, and the publish step fails.
5. Leave **Pass secrets to builds from forked pull requests** off. Fork builds do not need Docker Hub credentials, and this config does not push from pull-request branches.
6. Allow pushes to `main` to build. If the project is set to build pull requests only, a merge to `main` will not publish.

Create the Docker Hub token:

1. Sign in at [hub.docker.com](https://hub.docker.com) as `debighosh`.
2. Open **Account settings → Personal access tokens**.
3. Create a token with Read and Write access (public repositories are enough for `debighosh/taskflow-api`).
4. Copy the token once into the CircleCI variable `DOCKERHUB_TOKEN`. Do not commit it, and do not put it in Railway.

`NVD_API_KEY` is optional. I was having trouble getting the key as the site was overloaded for a couple of days. So I decide to leave it unset until the NVD request site gave me a key. I did not add the variable with an empty value as I found out that a blank value is treated the same as unset.

Without a key, Dependency-Check 13.0.0 is skipped and the job stays green. That release exits 13 when the key is missing: `NvdApiException: Invalid API Key, length of 0 too short`, then "Unable to continue dependency-check analysis" because no NVD documents were downloaded. The CLI ships with an empty `nvd.api.key` ([dependency-check issue 8715](https://github.com/dependency-check/DependencyCheck/issues/8715)). The fix is merged and is not in 13.0.0, which is still the latest release. The job writes `SKIPPED.txt`. The dependency findings for the README are in `trivy-dependencies.txt`.

When you add a non-empty `NVD_API_KEY` (request one from [nvd.nist.gov/developers/request-an-api-key](https://nvd.nist.gov/developers/request-an-api-key)), the same job runs Dependency-Check on the next build. It passes the key with `--nvdApiKey` and `--failOnCVSS 11`, and uploads the HTML and JSON reports. The NVD data directory is cached for 7 days (`--nvdValidForHours 168`). If that cache upload is too large for the CircleCI plan, the reports are already stored. Remove the "Save OWASP Dependency-Check data" step.

Trivy does not call the NVD API. The first run downloads Trivy's vulnerability database and its Java index. Later runs reuse the cache. Maven dependencies are cached as well.

### Scans

Findings do not fail the build. The README has to summarise them, and a newly published CVE in Alpine or a transitive dependency should not block the coursework publish.

Without `NVD_API_KEY`:

- The `dependency-check` job copies runtime jars (`compile` and `runtime`, not test-only jars) and scans that directory with `trivy rootfs`. `trivy fs` does not open JARs. Reports are `trivy-dependencies.txt` and `trivy-dependencies.json`.
- `SKIPPED.txt` on that job explains why Dependency-Check did not run.
- The `docker-image` job writes `trivy-image.txt` and `trivy-image.json` for the built image, including Alpine packages and the application JAR.

With `NVD_API_KEY`:

- The same Trivy reports are produced.
- Dependency-Check also runs with `--failOnCVSS 11` and writes `dependency-check-report.html` and `dependency-check-report.json`. CVSS scores stop at 10, so findings keep a successful exit code.
- If the NVD download fails, or the key is rejected, the `dependency-check` job fails. The Trivy jar report is still uploaded. The Docker Hub publish does not wait on this job.

Every Trivy command uses `--exit-code 0`, so findings do not fail the job. If the Trivy binary or its database cannot be downloaded, that step fails.

Open the job in CircleCI and use the **Artifacts** tab. For the README, summarise `trivy-dependencies.txt` and `trivy-image.txt`. When the key is set, also summarise the Dependency-Check HTML report: what was found, what you changed, and what you left (for example an Alpine package CVE that would require leaving the base image from the assignment).

To fail the build later, lower `--failOnCVSS` or drop `--exit-code 0` and pass `--severity HIGH,CRITICAL --exit-code 1` to Trivy.

## Railway

Deploy the service from this GitHub repository. Railway detects the Dockerfile in the root and builds it. `target/` is not in git; the image build runs Maven.

1. Create a project at [railway.app](https://railway.app) and choose **Deploy from GitHub repo**.
2. Select this repository. Deploy after PR #2 is on `main`, so the service includes the prod profile and `PORT` binding.
3. Add a Postgres database in the same project.
4. On the API service, set the variables below. For `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD`, use Railway's variable reference to the Postgres service (the same values the Postgres plugin injects).
5. When the deployment is up, open the service → **Settings → Networking → Generate Domain**.
6. Check `https://<your-domain>/api/tasks`.

The app honours `PORT` and falls back to 8080. Railway sets `PORT`. Point the public domain at that same port. Local `docker run -p 8080:8080` leaves `PORT` unset, so the process listens on 8080.

CircleCI publishes to Docker Hub. Railway rebuilds from GitHub when `main` changes. The assignment deploy path is this Dockerfile.

Local Docker and CI stay on the default profile and H2. Production settings belong on the Railway service. [PR #2](https://github.com/debighosh/module3-taskflow-api/pull/2) reads these variables from `application-prod.properties` and `application.properties`:

| Variable | What to set |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `ALLOWED_ORIGINS` | The Netlify origin, for example `https://your-site.netlify.app`. Separate extra origins with commas. |
| `PGHOST` | Reference from the Railway Postgres service |
| `PGPORT` | Reference from the Railway Postgres service |
| `PGDATABASE` | Reference from the Railway Postgres service |
| `PGUSER` | Reference from the Railway Postgres service |
| `PGPASSWORD` | Reference from the Railway Postgres service |

`PGPASSWORD` and the other `PG*` values are referenced from the Postgres service. I did not commit them. If `ALLOWED_ORIGINS` is unset, the app allows `http://localhost:5173` only, so the Railway service needs the Netlify origin. The prod profile turns the H2 console off. Do not set `SPRING_PROFILES_ACTIVE=prod` in CircleCI: the tests stay on H2 and do not need a database.
