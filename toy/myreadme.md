###
Richard's team load generator

```
https://github.com/My-DIGI-ID/aries-cloudagent-loadgenerator

https://github.com/lissi-id/acapy-load-test-results
```


### Run local indy network
```
git clone https://github.com/bcgov/von-network.git
./von-network/mange up

git clone https://github.com/bcgov/indy-tails-server.git
./indy-tails-server/docker/manage up
```

### Run nodes

```
./run_demo faber --wallet-type indy \
  --wallet-name wallet_db \
  --wallet-storage-type postgres_storage \
  --wallet-storage-config '{"url":"host.docker.internal:5432","max_connections":100}' \
  --wallet-storage-creds '{"account":"postgres","password":"postgres","admin_account":"postgres","admin_password":"postgres"}'
```


### Get helper

```
../scripts/run_docker start --help
```

### Run Issuer-Verifier

```
docker-compose -f docker-issuer.yml up -d --scale issuer-verifier-acapy=2
```


### Postgres

```
docker-compose -f docker-wallet-postgres up -d
```

#### Login in DB
```
docker exec -it issuer-verifier-wallet-db bash
```

Work with database
```
psql -U postgres
\l+
\c wallet_db # connect to database
\dt+ # show all tables


```

Dump database

```
pg_dump -U postgres wallet_db > dump.sql
docker cp issuer-verifier-wallet-db:/dump.sql ./wallet-db-dump.sql

select count(*) from items;
```

```
http://localhost:10000/api/doc#/connection/post_connections_create_invitation
```

### Postgres Cluster

Implementation of Postgres Cluster is done through Patroni:
```
https://github.com/zalando/patroni/tree/master/docker

Run it localy
psql -h localhost -p 5000 -U postgres -W
```
