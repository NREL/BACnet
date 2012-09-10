#!/bin/sh

javac -cp lib/*.jar -sourcepath bacnet/src  -d bacnet/bin bacnet/src/Scan.java bacnet/src/BASender.java
