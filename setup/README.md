# Aries Cloudagent Load Generator Base

The load generator application relies on third party software to work. This folder consists of the necessary building blocks to provide a working setup.

## Architecture

The architecture mainly consists of three underlying building blocks:

- [VON-Network](https://github.com/bcgov/von-network) for a local deployment of an indy ledger
- Two [ACA-Py](https://github.com/hyperledger/aries-cloudagent-python) instances representing a holder and an
  issuer/verifier (+ PostgreSQL as the wallet backend)
- [Tails Server](https://github.com/bcgov/indy-tails-server/) to support revocation

## Prerequisites

You need to have [Docker](https://docs.docker.com/get-docker/) installed and access to a basic command line.

## Setup

### .env file

Environment variables can be set via the provided `.env` file. However, the existing values are sufficient for a functioning setup.

> **NOTE**: Linux users may need to change all occurrences of `host.docker.internal` to `172.17.0.1`

### Manage Script

From this directory run:

```./manage start```

This will start all necessary components as well as registering a DID from the given seed in the `.env` file.

To stop the setup, run:

```./manage stop```

If you also want to delete all data and remove all containers, run:

```./manage down```

You can also inspect the logs of the VON-Network and the ACA-Py/Tails server in a simple way:

```./manage logs --$TYPE```

Where `--$TYPE` can either be `--ledger` or `--acapy` depending on your needs.

## Next steps

For monitoring purposes you can open http://localhost:9000/ to see the transactions written to the ledger. Additionally,
you can interact with the issuer/verifier ACA-Py directly on http://localhost:10000/api/doc or
on http://localhost:10010/api/doc for the holder ACA-Py.
