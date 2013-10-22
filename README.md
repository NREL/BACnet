BACnet Scraper
===============

This application is designed to scan all devices on a BACnet system (http://www.bacnet.org/) to allow for data scraping from the devices. 

The system pulls down all the system metadata by running "discovery" once a week.  After the discovery, the system does point-by-point pulling of data of each device based on a filter file.

Project funded by NREL's Commercial Building LDRD Project for Building Agent.

-------------------------------

BACnet Scraper requires a recent version of JRuby. The logic and BACnet portions of the project are written in Java while the front end is developed in Ruby, for flexibility.

Compiling
---------

[gradle](http://www.gradle.org/) is used for building the project. To build:

```sh
git clone https://github.com/NREL/BACnet.git
cd BACnet
gradle build
```

This will compile the BACnet Scraper and prepare some example files for execution. 

Execution
---------

A jruby script, run.rb, is staged for execution (note this file is overwritten each time you compile). The script gives examples for logging, data storage and retrieval, and use of Sinatra to provide a web interface to the BACnet process.

To run the application as a scraper do the following:

```sh
cd build/bacnet/bin
bundle exec jruby run.rb -dev en0 -s -i 65010 -vv -f ../conf/filter.json
```

To run the application as a slave device do the following:

```sh
cd build/bacnet/bin
bundle exec jruby run.rb -dev en0 -S -vv -F ../conf/example_oid.json -i 65010
```

To run both the application as a scraper and a slave device do the following:

```sh
cd build/bacnet/bin
bundle exec jruby run.rb -dev en0 -s -i 65010 -vv -f ../conf/filter.json -S -F ../conf/example_oid.json
```

For more options use --help

```sh
cd build/bacnet/bin
bundle exec jruby run.rb --help


usage: Syntax: [-D <arg>] [-d <arg>] [-databus <arg>] [-f <arg>] [-F                              
       <arg>] [-i <arg>] [-k <arg>] [-l <arg>] [-M <arg>] [-m <arg>] [-p                          
       <arg>] [-s] [-S] [-T <arg>] [-t <arg>] [-U <arg>] [-u <arg>] [-v]                          
       [-vv]                                                                                      
 -D,--device-id <arg>                 device ID to scan, exclusive of                             
                                      min-device-id and max-device-id                             
 -d,--dev <arg>                       Network device to use for                                   
                                      broadcasts, default: eth0                                   
 -databus,--databus-enabled <arg>     Enable writing to databus. default:                         
                                      true                                                        
 -f,--filter-file <arg>               JSON filter file to use during                              
                                      scanning, default:                                          
                                      ../conf/filter.json
 -F,--oid-file <arg>                  JSON oid file to use for the slave
                                      device configuration, default:
                                      ../conf/example_oid.json
 -i,--id <arg>                        Device ID of this software, default:
                                      11234
 -k,--databus-key <arg>               Key for sending to Databus, default:
                                      941RCGC.B2.1WWXM5WZVA5YL
 -l,--logging-properties-file <arg>   File for loading logger
                                      configuration, default:
                                      ../conf/logging.properties
 -M,--max-device-id <arg>             Maximum device ID to scan for,
                                      default: -1
 -m,--min-device-id <arg>             Minimum device ID to scan for,
                                      default: -1
 -p,--databus-port <arg>              Databus port for sending to
                                      Database, default: 5502
 -s,--scan                            Enable scanning feature, default:
                                      true
 -S,--slave-device                    Enable slave device feature,
                                      default: false
 -T,--slave-device-interval <arg>     Number of seconds between updates to
                                      slave device values, default: 10
 -t,--scan-interval <arg>             Amount of time (in ms) to wait
                                      between finishing one scan and
                                      starting another. default: 168
 -U,--databus-user <arg>              Databus username for sending to
                                      Database, default: robot-bacnet
 -u,--databus-url <arg>               Databus URL to send data to,
                                      default: databus.nrel.gov
 -v,--verbose                         Verbose logging (Info Level).
                                      Default is warning and error
                                      logging. default: false
 -vv,--very-verbose                   Very verbose logging (All Levels).
                                      Default is warning and error
                                      logging. default: false
```

Slave Device
------------

In addition to the scraping and scanning features, the tool can act as a BACnet slave device. See the `--slave-device` and `--oid-file` for more information.
