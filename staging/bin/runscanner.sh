#!/bin/sh
echo "Calling scanner with params: $@"

java -cp "../lib/*" gov.nrel.consumer.Main $@ 
