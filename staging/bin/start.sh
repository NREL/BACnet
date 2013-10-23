#!/bin/bash

#Launch the polling program...

#Old Methods
##nohup java -cp "../lib/*" gov.nrel.consumer.Main 2>&1 >../logs/out.log
##./runscanner.sh -f ../conf/filter.json -vv -dev eth4 -s -i 65001 -u https://databus.nrel.gov:5502 -r register:10768272987:b1:4814227944682770547 -g bacnet > ../logs/output.log 2>&1 &
##./runscanner.sh -dev eth1 -S -i 51243 -vv -F ../conf/example_oid.json

# Client
##./runscanner.sh -dev eth1 -s -i 65010 -vv -f ../conf/filter.json

# Server
##./runscanner.sh -dev eth1 -S -vv -F ../conf/example_oid.json -i 45678

# Both
./runscanner.sh -dev eth5 -s -i 65010 -vv -f ../conf/filter.json -S -F ../conf/example_oid.json