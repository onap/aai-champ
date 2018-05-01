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

PROPS="-DAPP_HOME=$APP_HOME"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
PROPS="$PROPS -DKEY_STORE_PASSWORD=$KEY_STORE_PASSWORD"
JVM_MAX_HEAP=${MAX_HEAP:-1024}

set -x
exec java -Xmx${JVM_MAX_HEAP}m $PROPS -Dloader.path="${GRAPHIMPL_DEPS}" -jar "${APP_HOME}/champ-service.jar"
