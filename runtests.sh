#!/usr/bin/env bash

# Env vars

# If you wnat to attach debugger to Unit tests, use the undermentioned MAVEN_OPTS with debug opts:
#export MAVEN_OPTS="-DforkCount=0 -Xmx3200m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1661"
export MAVEN_OPTS="-DforkCount=0 -Xmx3200m"
# If you want to attach to Arquillian integration tests inside Wildfly,  use:
#export JVM_ARGS_DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1661"
export SINKIT_ACCESS_TOKEN="user"
export SINKIT_ADDR_PREFIX="127"
export SINKIT_DNS_REQUEST_LOGGING_ENABLED="True"
export SINKIT_ELASTIC_CLUSTER="archive"
export SINKIT_ELASTIC_HOST="127.0.0.1"
export SINKIT_ELASTIC_PORT="9300"
export SINKIT_ELASTIC_REST_PORT="9200"
export SINKIT_HOTROD_HOST="127.0.0.1"
export SINKIT_HOTROD_PORT="11322"
export SINKIT_HOTROD_CONN_TIMEOUT_S="300"
export SINKIT_IOC_ACTIVE_HOURS="1"
export SINKIT_IOC_DEACTIVATOR_SKIP="False"
export SINKIT_LOCAL_CACHE_LIFESPAN="1000"
export SINKIT_LOCAL_CACHE_SIZE="10"
export SINKIT_LOGLEVEL="ALL"
export SINKIT_MS_RAM="256m"
export SINKIT_MX_RAM="512m"
export SINKIT_NIC="lo"
export SINKIT_SINKHOLE_IP="127.0.0.1"
export SINKIT_SINKHOLE_IPV6="::1"
export SINKIT_VERIFY_CLIENT="REQUIRED"
export SINKIT_VIRUS_TOTAL_SKIP="True"
export SINKIT_WHITELIST_VALID_HOURS="1"
export SINKIT_MGMT_USER="user"
export SINKIT_MGMT_PASS="user"

mvn clean test

docker-compose -f ./integration-tests/docker-compose.yml down -v
docker-compose -f ./integration-tests/docker-compose.yml rm -fv
docker volume ls -qf "name=integration-tests*" | xargs docker volume rm
docker network ls -qf "name=integration-tests*" | xargs docker network rm
docker ps -aqf "name=integration-tests*" | xargs docker rm
docker-compose -f ./integration-tests/docker-compose.yml pull

echo -e "Waiting for Elastic ports to clear up...\c"
until $(test `netstat -tupan 2> /dev/null | grep -c ':9300 '` -eq 0); do echo -e ".\c";sleep 1;done
until $(test `netstat -tupan 2> /dev/null | grep -c ':9200 '` -eq 0); do echo -e ".\c";sleep 1;done
echo "Elastic ports cleared."

docker-compose -f ./integration-tests/docker-compose.yml up -d

echo -e "Waiting for Elastic...\c"
until $(curl --silent --output /dev/null --fail --head http://127.0.0.1:9200); do echo -e ".\c";sleep 1;done
echo "Elastic started."
echo -e "Waiting for Infinispan...\c"
until $(curl --silent --output /dev/null --fail --head http://127.0.0.1:9991/error); do echo -e ".\c";sleep 1;done
echo "Infinispan started."
curl -XPUT 127.0.0.1:9200/_template/iocs -d @integration-tests/src/test/resources/elastic_iocs.json
curl -XPUT 127.0.0.1:9200/_template/logs -d @integration-tests/src/test/resources/elastic_logs.json
curl -XPUT 127.0.0.1:9200/_template/passivedns -d @integration-tests/src/test/resources/elastic_passivedns.json

mvn integration-test -Parq-wildfly-managed -Dhotrod_host=127.0.0.1 -Dhotrod_port=11322 -Djvm.args.debug="${JVM_ARGS_DEBUG}"-Djdk.net.URLClassPath.disableClassPathURLCheck=true

docker-compose -f ./integration-tests/docker-compose.yml down -v
