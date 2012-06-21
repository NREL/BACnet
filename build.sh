#!/bin/sh

javac -cp lib/bacnet4J.jar:lib/seroUtils.jar:bacnet/bin:lib/gson-2.1.jar:lib/commons-cli-1.2.jar -sourcepath bacnet/src  -d bacnet/bin bacnet/src/Scan.java bacnet/src/SDISender.java bacnet/src/BASender.java
