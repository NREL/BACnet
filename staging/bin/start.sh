#!/bin/bash

#Launch the polling program...

nohup java -cp "../lib/*" gov.nrel.consumer.Main 2>&1 >../logs/out.log

##./runscanner.sh -f ../conf/filter.json -vv -dev eth4 -s -i 65001 -u https://databus.nrel.gov:5502 -r register:10768272987:b1:4814227944682770547 -g bacnet > ../logs/output.log 2>&1 &


