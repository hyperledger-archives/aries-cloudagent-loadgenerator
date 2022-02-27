#!/usr/bin/env bash

export MSYS_NO_PATHCONV=1
set -e


# getting script path
SCRIPT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# export environment variables from .env
export $(grep -v '^#' $SCRIPT_HOME/.env | xargs)

# ignore orphans warning
export COMPOSE_IGNORE_ORPHANS=True

# load all required git submodules
git submodule update --init --recursive

# =================================================================================================================
# Usage:
# -----------------------------------------------------------------------------------------------------------------
usage() {
  cat <<-EOF

      Usage: $0 [command]

      Commands:

      start - Creates the application containers from the built images
              and starts the services based on the docker-compose.yml file.

      startWithoutLoadGenerator - Starts all containers but the load generator.
                                  Can be used for running the load generator via the IDE.

      startclusterednetwork - Starts network with Walled DB in a Patroni Postgres Cluster

      startloadgenerator - Starts only Spring service to generate load on the network

      startdashboard - Starts dashboard infrastructure to collect and present metrics

      restart - First, "down" is executed. Then, "start" is run.

      down - Brings down the services and removes the volumes (storage) and containers.

      logs - To tail the logs of running containers (ctrl-c to exit).
             Possible to print logs for acapy or ledger related containers.
             Examples:
              $0 logs --acapy
              $0 logs --ledger
EOF
  exit 1
}

toLower() {
  echo $(echo ${@} | tr '[:upper:]' '[:lower:]')
}

pushd ${SCRIPT_HOME} >/dev/null
COMMAND=$(toLower ${1})
shift || COMMAND=usage

function initEnv() {
  for arg in "$@"; do
    # Remove recognized arguments from the list after processing.
    shift
    case "$arg" in
      *=*)
        export "${arg}"
        ;;
      --ledger)
        LOG_LEDGER=1
        ;;
      --acapy)
        LOG_ACAPY=1
        ;;
      *)
        # If not recognized, save it for later procesing ...
        set -- "$@" "$arg"
        ;;
    esac
  done
}

function logs() {
  if [ ! -z "${LOG_LEDGER}" ]; then
    docker-compose -p von -f von-network/docker-compose.yml logs -f
  elif [ ! -z "${LOG_ACAPY}" ]; then
    docker-compose -f docker-compose.yml logs -f
  fi
}

function startLoadGenerator() {
  echo "Starting Load Generator ..."
  docker-compose -f docker-compose-load-generator.yml up -d --build
}

function startDashboard() {
  echo "Starting dashboard and logging containers ..."
  docker-compose -f ./dashboard/docker-compose.yml up -d
}

function startIndyNetwork() {
  echo "Starting the VON Network ..."
  ./von-network/manage build
  ./von-network/manage start

  echo "Waiting for the ledger to start... (sleeping 30 seconds)"
  sleep 30

  echo "Registering issuer DID..."
  curl -d "{\"role\": \"ENDORSER\", \"seed\":\"$ISSUER_DID_SEED\"}" -H "Content-Type: application/json" -X POST $LEDGER_REGISTER_DID_ENDPOINT
}

function startAgents() {
  docker-compose -f ./agents/docker-compose-agents.yml --profile issuer-verifier-provisioning up -d

  echo "Provisioning AcaPys... (sleeping 15 seconds)"
  sleep 15

  echo "Starting all AcaPy related docker containers ..."
  docker-compose -f ./agents/docker-compose-agents.yml --profile all-but-load-generator up -d --scale issuer-verifier-acapy=$NUMBER_OF_ISSUER_VERIFIER_ACAPY_INSTANCES

  echo "Waiting for all the agents to start... (sleeping 15 seconds)"
  sleep 15
}

function startPostgresSingleInstance() {
  docker-compose -f ./agents/docker-compose-issuer-verifier-walletdb.yml --profile single-instance up -d;

  echo "Starting Issuer Wallet DB as single instance... (sleeping 15 seconds)"
  sleep 15
}

# cluster is built using Patroni technologies: https://github.com/zalando/patroni
# details about toy environment using Docker: https://github.com/zalando/patroni/tree/master/docker
function startPostgresCluster() {
  cd "$SCRIPT_HOME/agents/patroni";
  docker build -t postgres-cluster-node --build-arg PG_MAJOR=13 .;

  cd $SCRIPT_HOME;
  docker-compose -f ./agents/docker-compose-issuer-verifier-walletdb.yml --profile cluster up -d;

  echo "Starting Postgres HA Cluster... (sleeping 45 seconds)"
  sleep 45
}

function startAllWithoutLoadGenerator() {
  startIndyNetwork
  startDashboard
  startPostgresSingleInstance
  startAgents
}

function startAllWithClusteredWalletDB() {
  startIndyNetwork
  startDashboard
  startPostgresCluster
  startAgents
}

function startAll() {
  startAllWithoutLoadGenerator
  startLoadGenerator
}

function downAll() {
  echo "Stopping the VON Network and deleting ledger data ..."
  ./von-network/manage down

  echo "Stopping and removing dashboard and logging containers as well as volumes ..."
  docker-compose -f ./dashboard/docker-compose.yml down -v

  echo "Stopping load generator ..."
  docker-compose -f docker-compose-load-generator.yml down -v

  echo "Stopping and removing any running AcaPy containers as well as volumes ..."
  docker-compose -f ./agents/docker-compose-agents.yml down -v

  echo "Stopping and removing any Wallet-DB containers as well as volumes ..."
  docker-compose -f ./agents/docker-compose-issuer-verifier-walletdb.yml down -v
}

case "${COMMAND}" in
start)
  startAll
  ;;
startwithoutloadgenerator)
  startAllWithoutLoadGenerator
  ;;
startloadgenerator)
  startLoadGenerator
  ;;
startdashboard)
  startDashboard
  ;;
startpostgrescluster)
  startPostgresCluster
  ;;
startclusterednetwork)
  startAllWithClusteredWalletDB
  ;;
restart)
  downAll
  startAll
  ;;
down)
  downAll
  ;;
logs)
  initEnv "$@"
  logs
  ;;
*)
  usage
  ;;
esac

popd >/dev/null
