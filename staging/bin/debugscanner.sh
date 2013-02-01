#!/bin/sh
echo "Calling scanner with params: $@"

java -Xdebug -Xrunjdwp:transport=dt_socket,address=9680,server=y,suspend=y -cp "../lib/*" gov.nrel.bacnet.Scan $@
