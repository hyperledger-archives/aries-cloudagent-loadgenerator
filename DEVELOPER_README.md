# Developer Readme for the "Aries Cloud Agent Load Generator"

This readme outlines how the load generator can be started from the IDE during development. It is written for developers
who want to extend/adopt the code of the load generator.

## Technology

The Load Generator is a Spring Boot application written in Kotlin.

## Startup a Test Environment

To start the environment without the Load Generator container, run:

```
./setup/manage.sh debug
```

## Configuration via application.yml

If the load generator is started via the IDE it is configured
via [application.yml](./src/main/resources/application.yml).

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
        thread-pool-size: 4

test-flows:
    full-flow:
        active: true
        use-revocable-credentials: true
        revocation-registry-size: 500
        check-non-revoked: true
        use-oob-instead-of-connection: false
    issuer-flow:
        active: false
        use-revocable-credentials: true
        revocation-registry-size: 500
        use-oob-credential-issuance: false
    connection-request-flow:
        active: false
    credential-issuance-flow:
        active: false
        use-revocable-credentials: true
        revocation-registry-size: 500
        use-oob-credential-issuance: false
    proof-request-flow:
        active: false
        check-non-revoked: true
        use-oob-proof-requests: false
```

Only one Test Runner and one Test Flow should be set to `active: true` at a time. The Test Flow will **automatically be
executed** by the Test Runner once the application is started.

## Grafana Configuration

The data sources, as well as dashboards, are provisioned automatically when running the Grafana container.

Data sources are configured manually
in [./setup/grafana/grafana-provisioning/datasources/](./setup/grafana/grafana-provisioning/datasources/) using YAML
files. Dashboards are configured in [./setup/grafana/dashboards](./setup/grafana/dashboards) using JSON files.
Dashboards can be created via the Grafana Web UI and exported as JSON afterwards.

## E2E Test Cases

- Start whole system with multitenancy disabled
- Start whole system with multitenancy enabled
- Down the whole system using `manage.sh down`
- Restart the whole system using `manage.sh restart`
- Start whole system with postgres cluster
- Start whole system without the load generator using `manage.sh debug` and start the load generator via the IDE
- Run it on MacOS
- Run it on Linux
