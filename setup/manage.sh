#!/usr/bin/env bash

export MSYS_NO_PATHCONV=1
set -e


# getting script path
SCRIPT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# export environment variables from .env
export $(grep -v '^#' $SCRIPT_HOME/.env | xargs)


# ignore orphans warning
export COMPOSE_IGNORE_ORPHANS=True

# =================================================================================================================
# Usage:
# -----------------------------------------------------------------------------------------------------------------
usage() {
  cat <<-EOF

      Usage: $0 [command]

      Commands:

      start - Creates the application containers from the built images
              and starts the services based on the docker-compose.yml files and configuration supplied in the .env.

      restart - First, "down" is executed. Then, "start" is run.

      stop - Stops and remove the services.
             The volumes are not deleted so they will be reused the next time you run start.

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
    docker-compose -f docker-agents.yml logs -f
  fi
}

function startLoadGenerator() {
  echo "Starting Load Generator ..."
  docker-compose -f docker-load-generator.yml up -d --build
}

function startDashboard() {
  echo "Starting dashboard and logging containers ..."

  if [ $SYSTEM_ISSUER_POSTGRES_DB ] && [ $SYSTEM_ISSUER_POSTGRES_DB = "true"]
  then
    if [ $SYSTEM_ISSUER_POSTGRES_DB_CLUSTER ] && [ $SYSTEM_ISSUER_POSTGRES_DB_CLUSTER  = "true" ]
    then
      docker-compose -f ./dashboard/docker-compose.yml --profile cluster up -d
    else
      docker-compose -f ./dashboard/docker-compose.yml --profile non-cluster up -d
    fi;
  else
    docker-compose -f ./dashboard/docker-compose.yml up -d
  fi
}

function createBlockchainNetwork() {
  echo "Starting the VON Network ..."
  git submodule update --init --recursive
  ./von-network/manage build
  ./von-network/manage start
  echo "Waiting for the ledger to start... (sleeping 30 seconds)"
  sleep 30

  echo "Starting tails server"
  docker-compose -f ./docker-tails-server.yml up -d

  echo "Registering issuer DID..."
  curl -d "{\"role\": \"ENDORSER\", \"seed\":\"$ISSUER_DID_SEED\"}" -H "Content-Type: application/json" -X POST $LEDGER_REGISTER_DID_ENDPOINT
}

function provisionAcaPyAgents() {
  echo "Start provisioning AcaPys..."
  docker-compose -f docker-agents.yml --profile issuer-verifier-provisioning up -d

  echo "Provisioning AcaPys... (sleeping 15 seconds)"
  sleep 15

  echo "Starting all AcaPy related docker containers ..."
  docker-compose -f docker-agents.yml --profile issuer-verifier --profile holder up -d --scale issuer-verifier-acapy=$NUMBER_OF_ISSUER_VERIFIER_ACAPY_INSTANCES
}

function startPostgresSingleInstance() {
  echo "Starting Issuer Wallet DB as single instance... (sleeping 15 seconds)"
  docker-compose -f issuer-walletdb.yml --profile non-cluster up -d;
  sleep 15
}

# cluster is built using Patroni technologies: https://github.com/zalando/patroni
# details about toy environment using Docker: https://github.com/zalando/patroni/tree/master/docker
function startPostgresCluster() {
  echo "Starting Issuer Wallet DB as the Postgres Patroni cluster..."
  git submodule update --init --recursive

  cd "$SCRIPT_HOME/patroni";
  docker build -t postgres-cluster-node --build-arg PG_MAJOR=13 .;

  cd $SCRIPT_HOME;
  docker-compose -f issuer-walletdb.yml --profile cluster up -d;

  echo "Starting Postgres HA Cluster... (sleeping 45 seconds)"
  sleep 45
}

function downAll() {
  echo "Stopping the VON Network and deleting ledger data ..."
  ./von-network/manage down

  echo "Stopping and removing tails-server"
  docker-compose -f ./docker-tails-server.yml down -v

  echo "Stopping and removing dashboard and logging containers as well as volumes ..."
  docker-compose -f ./dashboard/docker-compose.yml down -v

  echo "Stopping load generator ..."
  docker-compose -f docker-load-generator.yml down -v

  echo "Stopping and removing any running AcaPy containers as well as volumes ..."
  docker-compose -f docker-agents.yml down -v

  echo "Stopping and removing any walletdb containers"
  docker-compose -f issuer-walletdb.yml down -v
}

function startAll() {
  if [ $SYSTEM_LEDGER ] && [ $SYSTEM_LEDGER  = "true" ]
  then
    createBlockchainNetwork
  fi;

  if [ $SYSTEM_ISSUER_POSTGRES_DB ] && [ $SYSTEM_ISSUER_POSTGRES_DB = "true"]
  then
    if [ $SYSTEM_ISSUER_POSTGRES_DB_CLUSTER ] && [ $SYSTEM_ISSUER_POSTGRES_DB_CLUSTER  = "true" ]
    then
      startPostgresCluster
    else
      startPostgresSingleInstance
    fi;
  fi;

  if [ $SYSTEM_METRICS_DASHBOARD ] && [ $SYSTEM_METRICS_DASHBOARD  = "true" ]
  then
    startDashboard
  fi;

  if [ $SYSTEM_AGENTS ] && [ $SYSTEM_AGENTS  = "true" ]
  then
    provisionAcaPyAgents
  fi;

  if [ $SYSTEM_LOAD_GENERATOR ] && [ $SYSTEM_LOAD_GENERATOR  = "true" ]
  then
    startLoadGenerator
  fi;
}

case "${COMMAND}" in
start)
  startAll
  ;;
restart)
  downAll
  startAll
  ;;
stop)
  echo "Stopping load generator"
  docker-compose -f docker-load-generator rm -f -s

  echo "Stopping and removing any running AcaPy containers ..."
  docker-compose -f docker-agents.yml rm -f -s

  echo "Stopping and removing dashboard and logging containers ..."
  docker-compose -f ./dashboard/docker-compose.yml rm -f -s

  echo "Stopping the VON Network ..."
  ./von-network/manage stop

  echo "Stopping the Tails Server ..."
  docker-compose -f ./docker-tails-server.yml rm -f -s

  echo "Stopping the Wallet DB ..."
  docker-compose -f ./issuer-walletdb.yml rm -f -s
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
