#!/usr/bin/env bash

bin=`dirname "$0"`
cd $bin

defport=9092
PORT=${1:-$defport} 

rm logs/myperf.log.*.lck
#nohup java -Xms128m -classpath  myperfserver-executable-jar-with-dependencies.jar com.yahoo.dba.tools.myperfserver.App -j $bin -p $PORT -w myperf.war -k work -c /myperf &
nohup java -Xms128m -classpath  myperfserver-executable-jar-with-dependencies.jar com.yahoo.dba.tools.myperfserver.App -f config_default.properties &