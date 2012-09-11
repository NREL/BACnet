#!/bin/sh
echo "Calling scanner with params: $@"

export CLASSPATH=
for f in lib/*.jar
do
    export CLASSPATH=$f:$CLASSPATH
done

export CLASSPATH=bacnet/bin:$CLASSPATH

java -Xdebug -Xrunjdwp:transport=dt_socket,address=9680,server=y,suspend=y -classpath $CLASSPATH gov.nrel.bacnet.Scan $@
