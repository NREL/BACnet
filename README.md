BACnet Scraper
===============

This application is designed to scan all devices on a BACnet system (http://www.bacnet.org/) to allow for data scraping from the devices. 

The system pulls down all the system metadata by running "discovery" once a week.  After the discovery, the system does point-by-point pulling of data of each device based on a filter file.

Project funded by NREL's Commercial Building LDRD Project for Building Agent.