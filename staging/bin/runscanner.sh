#!/bin/sh
echo "Calling scanner with params: $@"

nohup java -cp "../lib/*" gov.nrel.consumer.Main $@ 
