#!/bin/sh

echo "Calling scanner with params: $@"
java -classpath lib/seroUtils.jar:lib/bacnet4J.jar:bacnet/bin:lib/gson-2.1.jar:lib/commons-cli-1.2.jar gov.nrel.bacnet.Scan $@

