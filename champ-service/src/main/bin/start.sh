#!/bin/sh
#
# ============LICENSE_START==========================================
# org.onap.aai
# ===================================================================
# Copyright © 2017 AT&T Intellectual Property. All rights reserved.
# Copyright © 2017 Amdocs
# ===================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END============================================
# ECOMP is a trademark and service mark of AT&T Intellectual Property.
#

APP_HOME="/opt/app/champ-service"
GRAPH_DEPS_HOME="${APP_HOME}/graph-deps"

if [ -z "$CONFIG_HOME" ]; then
    echo "CONFIG_HOME must be set in order to start up process"
    exit 1
fi

if [ -z "$KEY_STORE_PASSWORD" ]; then
    echo "KEY_STORE_PASSWORD must be set in order to start up process"
    exit 1
fi

if [ -z "$SERVICE_BEANS" ]; then
    echo "SERVICE_BEANS must be set in order to start up process"
    exit 1
fi

for dir in $( find ${GRAPH_DEPS_HOME}/* -maxdepth 0 -type d ); do
    CURRIMPL=$(basename $dir)
    if [ "x$GRAPHIMPL" = "x$CURRIMPL" ]; then
        GRAPHIMPL_DEPS="${GRAPH_DEPS_HOME}/${GRAPHIMPL}"
        echo "Setting up graph implementation to $GRAPHIMPL"
    else
        SUPPORTED_GRAPHIMPL="$SUPPORTED_GRAPHIMPL $CURRIMPL"
    fi
done

if [ -z "$GRAPHIMPL_DEPS" ]; then
    echo "Configured graph implementation '$GRAPHIMPL' is not supported. Acceptable implementations are one of: $SUPPORTED_GRAPHIMPL"
    exit 1
fi

# Changes related to:AAI-2175
# Change aai champ container processes to run as non-root on the host
USER_ID=${LOCAL_USER_ID:-9001}
GROUP_ID=${LOCAL_GROUP_ID:-9001}
CHAMP_LOGS=/var/log/onap/AAI-CHAMP

if [ $(cat /etc/passwd | grep aaiadmin | wc -l) -eq 0 ]; then

        groupadd aaiadmin -g ${GROUP_ID} || {
                echo "Unable to create the group id for ${GROUP_ID}";
                exit 1;
        }
        useradd --shell=/bin/bash -u ${USER_ID} -g ${GROUP_ID} -o -c "" -m aaiadmin || {
                echo "Unable to create the user id for ${USER_ID}";
                exit 1;
        }
fi;

chown -R aaiadmin:aaiadmin ${MICRO_HOME}
chown -R aaiadmin:aaiadmin ${APP_HOME}
chown -R aaiadmin:aaiadmin ${CHAMP_LOGS}

find ${MICRO_HOME}  -name "*.sh" -exec chmod +x {} +

gosu aaiadmin ln -s /logs $MICRO_HOME/logs
JAVA_CMD="exec gosu aaiadmin java";
PROPS="-DAPP_HOME=$APP_HOME"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
PROPS="$PROPS -Dlogging.config=$APP_HOME/bundleconfig/etc/logback.xml"
PROPS="$PROPS -DKEY_STORE_PASSWORD=$KEY_STORE_PASSWORD"

if [ ! -z "$TRUST_STORE_PASSWORD" ]; then
   PROPS="$PROPS -DTRUST_STORE_PASSWORD=${TRUST_STORE_PASSWORD}"
fi

if [ ! -z "$TRUST_STORE_LOCATION" ]; then
   PROPS="$PROPS -DTRUST_STORE_LOCATION=${TRUST_STORE_LOCATION}"
fi

JVM_MAX_HEAP=${MAX_HEAP:-1024}

set -x
${JAVA_CMD} -Xmx${JVM_MAX_HEAP}m $PROPS -Dloader.path="${GRAPHIMPL_DEPS}" -jar "${APP_HOME}/champ-service.jar"
