#!/bin/bash
#########################################
#    Script to stop perf analyzer
#########################################
bin=`dirname "$0"`
cd $bin

APPNAME="MySQL Perf Analyzer"
echo "check and stop $APPNAME"
curdir=`pwd`
PID_FILE=$curdir/myperf.pid

#softshutdown first
touch "myserver.shutdown"

if [ -e $PID_FILE ]; then
    FILE_PID=`cat $PID_FILE`
    OS_PID=`ps -p $FILE_PID -o pid=`

    if [ "x$OS_PID" = "x" ]; then
        OS_PID=-1
    fi

    if [ $OS_PID != $FILE_PID ]; then
        echo "$APPNAME is not running, removing orphaned $PID_FILE file"
        rm $PID_FILE
    else
    
        echo "$APPNAME is running, stop it"
        kill -9 $FILE_PID
        # Wait for the app process to exit before returning.

        OS_PID=$FILE_PID

        until [ $OS_PID != $FILE_PID ]; do
                sleep 1
                OS_PID=`ps -p $FILE_PID -o pid=`

                if [ "x$OS_PID" = "x" ]; then
                        OS_PID=-1
                fi
        done

        rm $PID_FILE

    fi
else
    echo "$APPNAME is not running"
fi
