#!/bin/bash

# @author Michal Karm Babacek

# Debug logging
echo "STAT: `networkctl status`" >> /opt/sinkit/ip.log
echo "STAT ${SINKIT_NIC:-eth0}: `networkctl status ${SINKIT_NIC:-eth0}`" >> /opt/sinkit/ip.log

# Wait for the interface to wake up
TIMEOUT=20
MYIP=""
while [[ "${MYIP}X" == "X" ]] && [[ "${TIMEOUT}" -gt 0 ]]; do
    echo "Loop ${TIMEOUT}" >> /opt/sinkit/ip.log
    MYIP="`networkctl status ${SINKIT_NIC:-eth0} | awk '{if($1~/Address:/){printf($2);}}'`"
    export MYIP
    let TIMEOUT=$TIMEOUT-1
    if [[ "${MYIP}" == ${SINKIT_ADDR_PREFIX:-10}* ]]; then
        break;
    else 
        MYIP=""
        sleep 1;
    fi
done
echo -e "MYIP: ${MYIP}\nMYNIC: ${SINKIT_NIC:-eth0}" >> /opt/sinkit/ip.log
if [[ "${MYIP}X" == "X" ]]; then 
    echo "${SINKIT_NIC:-eth0} Interface error. " >> /opt/sinkit/ip.log
    exit 1
fi

# Replace NIC
sed -i "s/@SINKITNIC@/${SINKIT_NIC:-eth0}/g" ${WF_CONFIG}

# Replace TCPPING config
sed -i "s/@SINKIT_TCPPING_INITIAL_HOSTS@/${SINKIT_TCPPING_INITIAL_HOSTS:-[${HOSTNAME}]7800}/g" ${WF_CONFIG}
sed -i "s/@SINKIT_TCPPING_TIMEOUT@/${SINKIT_TCPPING_TIMEOUT:-60000}/g" ${WF_CONFIG}
sed -i "s/@SINKIT_TCPPING_NUM_MEMBERS@/${SINKIT_TCPPING_NUM_MEMBERS:-1}/g" ${WF_CONFIG}

# Replace Logging level
sed -i "s/@SINKITLOGGING@/${SINKIT_LOGLEVEL:-INFO}/g" ${WF_CONFIG}

#CONTAINER_NAME=`echo ${DOCKERCLOUD_CONTAINER_FQDN}|sed 's/\([^\.]*\.[^\.]*\).*/\1/g'`

if [ "`echo \"${HOSTNAME}\" | wc -c`" -gt 24 ]; then
    echo "ERROR: HOSTNAME ${HOSTNAME} must be up to 24 characters long."
    exit 1
fi

sed -i "s/<core-environment>/<core-environment node-identifier=\"${HOSTNAME}\">/g" ${WF_CONFIG}

/opt/sinkit/wildfly/bin/standalone.sh \
 -b ${MYIP} \
 -c standalone-ha.xml \
 -Djava.net.preferIPv4Stack=true \
 -Djboss.modules.system.pkgs=org.jboss.byteman \
 -Djava.awt.headless=true \
 -Djboss.bind.address.management=${MYIP} \
 -Djboss.bind.address=${MYIP} \
 -Djboss.bind.address.private=${MYIP} \
 -Djgroups.udp.mcast_addr=228.6.7.8 \
 -Djgroups.udp.mcast_port=46655 \
 -Djgroups.bind_addr=${MYIP} \
 -Djgroups.tcp.address=${MYIP} \
 -Djboss.default.multicast.address=230.0.0.4 \
 -Djboss.node.name="${HOSTNAME}" \
 -Djboss.host.name="${HOSTNAME}" \
 -Djboss.qualified.host.name="${HOSTNAME}" \
 -Djboss.as.management.blocking.timeout=1800
