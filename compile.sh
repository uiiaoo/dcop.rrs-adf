#!/bin/sh
JAR_NAME='rrs-adf-dcop-lib.jar'
BLD_DIR='build'
LIB_DIR='library'
SRC_DIR='src'

cd `dirname $0`

rm -rf $BLD_DIR
rm -rf $JAR_NAME

mkdir $BLD_DIR

CP=`find $PWD/$LIB_DIR/ -name '*.jar' | awk -F '\n' -v ORS=':' '{print}'`

cd $SRC_DIR
SRC_FILES=`find . -name '*.java'`
javac -encoding UTF-8 -classpath "${CP}." -d ../$BLD_DIR $SRC_FILES && echo "[OK] Build class files."

cd ..
cd $BLD_DIR
jar cvf ../$JAR_NAME ./ >/dev/null && echo "[OK] Build jar file."
