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
              and starts the services based on the docker-compose.yml file.

      startWithoutLoadGenerator - Starts all containers but the load generator.
                                  Can be used for running the load generator via the IDE.

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
    docker-compose -f docker-compose.yml logs -f
  fi
}

function startVonNetworkDashboardLogging() {
  echo "Starting the VON Network ..."
  git submodule update --init --recursive
  ./von-network/manage build
  ./von-network/manage start
  echo "Waiting for the ledger to start... (sleeping 30 seconds)"
  sleep 30

  echo "Registering issuer DID..."
  curl -d "{\"role\": \"ENDORSER\", \"seed\":\"$ISSUER_DID_SEED\"}" -H "Content-Type: application/json" -X POST $LEDGER_REGISTER_DID_ENDPOINT

  echo "Starting dashboard and logging containers ..."
  docker-compose -f ./dashboard/docker-compose.yml up -d
}

case "${COMMAND}" in
start)
  startVonNetworkDashboardLogging

  echo "Provisioning AcaPys and Wallet DBs ..."
  docker-compose -f docker-compose.yml --profile all up -d --build

  echo "Waiting for the Wallet DB to be provisioned... (sleeping 5 seconds)"
  sleep 10

  echo "Starting all AcaPy related docker containers ..."
  docker-compose -f docker-compose.yml --profile all up -d --scale issuer-verifier-acapy=2
  ;;
startwithoutloadgenerator)
  startVonNetworkDashboardLogging

  echo "Provisioning AcaPys and Wallet DBs ..."
  docker-compose -f docker-compose.yml up -d

  echo "Waiting for the Wallet DB to be provisioned... (sleeping 5 seconds)"
  sleep 10

  echo "Starting all AcaPy related docker containers ..."
  docker-compose -f docker-compose.yml up -d --scale issuer-verifier-acapy=2
  ;;
stop)
  echo "Stopping the VON Network ..."
  ./von-network/manage stop

  echo "Stopping and removing dashboard and logging containers ..."
  docker-compose -f ./dashboard/docker-compose.yml rm -f -s

  echo "Stopping and removing any running AcaPy containers ..."
  docker-compose -f docker-compose.yml rm -f -s
  ;;
down)
  echo "Stopping the VON Network and deleting ledger data ..."
  ./von-network/manage down

  echo "Stopping and removing dashboard and logging containers as well as volumes ..."
  docker-compose -f ./dashboard/docker-compose.yml down -v

  echo "Stopping and removing any running AcaPy containers as well as volumes ..."
  docker-compose -f docker-compose.yml down -v
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
