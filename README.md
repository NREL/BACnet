Building Agent BACnet Scraper
===============

This library provides scanning and data scraping functionality for devices on a [BACnet](http://www.bacnet.org/) network.  It provides standalone functionality but is intended to work with the [Stateful BACnet Scraper](https://github.com/nrel/bacnet-state) application to provide modular and stateful discovery and scraping functionality.

Project funded by NREL's Commercial Building LDRD Project for Building Agent.  Read more about Building Agent:

* [NREL-Developed Software Tackles Building Efficiency and Offers Cost Savings](http://www.nrel.gov/news/press/2013/5301.html)
* [Progress on Enabling an Interactive Conversation Between Commercial Building Occupants and Their Building To Improve Comfort and Energy Efficiency](http://www.nrel.gov/buildings/pdfs/55197.pdf)
* [Building Agent Software: Occupancy Feedback for Building Controls](http://techportal.eere.energy.gov/technology.do/techID=1068)

Compiling
---------

[gradle](http://www.gradle.org/) is used for building the project. To build:

```sh
git clone https://github.com/NREL/BACnet.git
cd BACnet
gradle build
```
Options
---------
The -dev option is required.  If the -databus option is not set to false, databus configuration (-k,-u,-U,-p) must be provided.  

````
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
                                      config/filter.json
 -F,--oid-file <arg>                  JSON oid file to use for the slave
                                      device configuration, default:
                                      config/example_oid.json
 -i,--id <arg>                        Device ID of this software, default:
                                      11234
 -k,--databus-key <arg>               Key for sending to Databus, default:
                                      941RCGC.B2.1WWXM5WZVA5YL
 -l,--logging-properties-file <arg>   File for loading logger
                                      configuration, default:
                                      config/logging.properties
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
````

Slave Device
------------

In addition to the scraping and scanning features, the tool can act as a BACnet slave device. See the `--slave-device` and `--oid-file` for more information.
