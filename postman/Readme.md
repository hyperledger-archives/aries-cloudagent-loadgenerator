# Postman Load Generator

This folder offers a Postman collection and environment configuration. The 
collection includes a variety of requests to test the load test setup.

## Usage
1. install [Postman]()
1. import [AcaPy_Load_Generator.postman_collection.json](./AcaPy_Load_Generator.postman_collection.json)
1. import [AcaPy_Load_Generator.postman_environment.json](./AcaPy_Load_Generator.postman_environment.json)
1. choose the Postman environment `Acapy_Load_Generator`
1. select the Postman collection `Acapy Load Generator` and click on `Run` to 
  automatically execute all requests

## Load Generator

### Test Local Setup
This folder includes a handful of requests to test if the local load test 
setup works as expected and if all components are able to communicate with 
each other. For a variety of actions are run including:
- creating a connection between the Issuer-Verifier and Holder agent
- register a schema and credential definition (for a revocable credential) on 
  the ledger
- issue a credential
- request a credential proof with a non-revocation check

