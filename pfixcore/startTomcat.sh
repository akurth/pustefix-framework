#!/bin/sh

if [ "$1" == "-d" ] ; then 
   cmd="jpda run"
else
   cmd="run"
fi

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:`pwd`/jni/build
export JAVA_OPTS="-mx200M"
echo "---------------------------"
echo "catalina.sh $cmd"
echo "Using JAVA_OPTS:       $JAVA_OPTS"
echo "Using JPDA_TRANSPORT:  $JPDA_TRANSPORT"
echo "Using JPDA_ADDRESS:    $JPDA_ADDRESS"
echo "Using JPDA_OPTS:       $JPDA_OPTS"
echo "---------------------------"

cd ./example/servletconf/tomcat/
LANG=C ./bin/catalina.sh $cmd



