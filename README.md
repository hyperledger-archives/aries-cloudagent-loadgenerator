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

The load generator as well as the test case to run is configured via the [application.yml](
./src/main/resources/application.yml). Alternatively, you can set the same parameters using environment variables.

```
test-cases:
  full-process:
    active: true
    number-of-iterations: 10
    number-of-parallel-iterations: 2
```

Only one test case should be set to `active: true` at a time. This test case will **automatically be executed** once the
application is started.

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
- **Grafana Loki:** is used to collect logs from services like the Load Generator

### View Test Results in Grafana

Grafana runs on http://localhost:3000. It comes preconfigured with dashboards to visualize the test results from the
load tests. You can for example open http://localhost:3000/d/0Pe9llbnz/full-process to the test results for
the `Full Process` test.

To see any data on the dashboard, ensure to select the right time range in Grafana for which data has been collected.

### Grafana Configuration

The data sources, as well as dashboards, are provisioned automatically when running the Grafana container.

Data sources are configured manually
in [./setup/grafana/grafana-provisioning/datasources/](./setup/grafana/grafana-provisioning/datasources/) using YAML
files. Dashboards are configured in [./setup/grafana/dashboards](./setup/grafana/dashboards) using JSON files.
Dashboards can be created via the Grafana Web UI and exported as JSON afterwards.


