#!/bin/sh
JAR_NAME='rrs-adf-dcop-lib.jar'
LIB_DIR='library'

cd `dirname $0`

CP=`find ./${LIB_DIR}/ -name '*.jar' | awk -F '\n' -v ORS=':' '{print}'`
CP=${CP}:./${JAR_NAME}
java -classpath "${CP}" lib.adf.dcop.comlayer.VirtualCommunicationServer
