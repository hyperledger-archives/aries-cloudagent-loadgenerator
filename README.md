# Aries Cloudagent Load Generator

A simple load generator to test the performance of the ACA-PY agent.

## Startup a Test Environment

This repository comes with an automated testing setup consisting of:

- Issuer/Verifier AcaPy (+ Postgres Wallet DB)
- Holder AcaPy (+ Postgres Wallet DB)
- Tails Server
- VON Network
- Analysis Tools ([see below](#analysis-tools))

To start it run:

```
./setup/manage.sh start
```

For more details check this [README](./setup/README.md).

## Startup the Load Generator

### Configuration

The load generator is configured via the [application.yml](./src/main/resources/application.yml). Alternatively, you can
set the same parameters using environment variables.

Two things need to be chosen by setting `active: true`:

1. **Test Runner** (each Test Runner generates the load differently)
2. **Test Flow** (each Test Flow corresponds to a different test scenario)

```
test-runners:
    max-parallel-iterations-runner:
        active: false
        number-of-total-iterations: 100
        number-of-parallel-iterations: 5
    constant-load-runner:
        active: true
        number-of-total-iterations: 100
        number-of-iterations-per-minute: 30
        core-thread-pool-size: 4

test-flows:
    full-flow:
        active: true
        use-revocable-credentials: true
        revocation-registry-size: 500
        check-non-revoked: true
        use-oob-proof-requests: false
        use-oob-credential-issuance: false
    issuer-flow:
        active: false
        use-revocable-credentials: true
        revocation-registry-size: 500
        use-oob-credential-issuance: false
    connection-request-flow:
        active: false
    credential-issuance-flow:
        active: false
        use-revocable-credentials: false
        revocation-registry-size: 500
        use-oob-credential-issuance: false
    proof-request-flow:
        active: false
        check-non-revoked: false
        use-oob-proof-requests: false
```

Only one Test Runner and one Test Flow should be set to `active: true` at a time. The Test Flow will **automatically be
executed** by the Test Runner once the application is started.

### Local setup

#### Building the application

To compile and package the application use the following command:

```sh
./mvnw package
```

#### Running the application locally

Run the `main` method in the `com.bka.ssi.generator.GeneratorApplication` from your
IDE.

Alternatively you can use
the [Spring Boot Maven plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html)
like so:

```sh
./mvnw spring-boot:run
```

### Docker setup

#### Building the application

To build the docker image run the following command from the root directory:

```sh
docker build -t loadgenerator .
```

#### Running the application with docker

```sh
docker run --rm -p 8080:8080 loadgenerator
```

### Open the SwaggerUI

An automatically generated SwaggerUI is available after startup under http://localhost:8080/swagger-ui.html

## Analyse the Test Results

This project includes a setup for analysing and visualizing the test results. The whole analysis setup is started
automatically when starting the test environment.

### Analysis Tools

- **Grafana:** is used to visualize the collected data on a dashboard
- **Grafane Image Renderer:** used to render Grafana graphs as images to export them to a PDF (uses
  the [Image Renderer Plugin](https://grafana.com/grafana/plugins/grafana-image-renderer/))
- **Grafana PDF Exporter:** used to export a Grafana dashboard as a PDF (
  uses [IzakMarais/reporter](https://github.com/IzakMarais/reporter))
- **Grafana Loki:** is used to collect logs from services like the Load Generator

### View Test Results in Grafana

Grafana runs on http://localhost:3000. It comes preconfigured with dashboards to visualize the test results from the
load tests. You can for example open http://localhost:3000/d/0Pe9llbnz/test-results to the test results.

To see any data on the dashboard, ensure to select the right time range in Grafana for which data has been collected.

### Export Grafana Dashboard as PDF

Using [IzakMarais/reporter](https://github.com/IzakMarais/reporter) it is possible to export a dashboard as a PDF. For
this a link exists in the top right corner of the dashboards. The PDF generation can take multiple minutes depending on
the Dashboard complexity. Check the logs of the `grafana-pdf-exporter` container in case you want to see the progress of
the PDF generation.

### Grafana Configuration

The data sources, as well as dashboards, are provisioned automatically when running the Grafana container.

Data sources are configured manually
in [./setup/grafana/grafana-provisioning/datasources/](./setup/grafana/grafana-provisioning/datasources/) using YAML
files. Dashboards are configured in [./setup/grafana/dashboards](./setup/grafana/dashboards) using JSON files.
Dashboards can be created via the Grafana Web UI and exported as JSON afterwards.


