# Aries Cloudagent Load Generator

A simple load generator to test the performance of the ACA-PY agent.

## Local setup

### Building the application

To compile and package the application use the following command:

```sh
./mvnw package
```

### Running the application locally

Run the `main` method in the `com.bka.ssi.generator.GeneratorApplication` from your
IDE.

Alternatively you can use
the [Spring Boot Maven plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html)
like so:

```sh
./mvnw spring-boot:run
```

### Open the SwaggerUI
As automatically generarted SwaggerUI is available after startup under http://localhost:8080/swagger-ui.html 

## Docker setup

### Building the application

To build the docker image run the following command from the root directory:

```sh
docker build -t loadgenerator .
```

### Running the application with docker

```sh
docker run --rm -p 8080:8080 loadgenerator
```

## Testing

### Running the Unit Tests

The unit test can be run through your preferred IDE.

IntelliJ: https://www.jetbrains.com/help/idea/performing-tests.html

Alternatively the unit test can also be run using the following command:

```sh
./mvnw test
```

### Coverage

The coverage report can be generated through your preferred IDE.

IntelliJ: https://www.jetbrains.com/help/idea/running-test-with-coverage.html

