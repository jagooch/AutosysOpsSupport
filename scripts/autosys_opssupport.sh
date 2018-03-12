#!/bin/bash
##cho on
CLASSPATH=/home/autosys/services/autosys_opssupport/lib/*:/home/autosys/services/autosys_opssupport/bin/*
JAVA_HOME=/home/autosys/services/autosys_opssupport/java
PATH=$JAVA_HOME/bin:$PATH


##check that sufficient arguments were given
if [ $# >= 1 ]; then
   echo "Syntax autosys_opssupport.sh <utility class name> <arguments>"   
   exit 1
fi


export CLASSPATH JAVA_HOME PATH
export CLASSNAME=$1
SHIFT

echo CLASSNAME = $CLASSNAME 

##echo "Environment setup complete, here it is"
##env | sort


##echo Java Version is 
##java -version
##the java command w/output is 
##java -cp $CLASSPATH com.psi.autosys.AutosysApplication com.psi.autosys.services.AutosysTimeStamp "MMddyyyy_HHmmdd" 


java -cp $CLASSPATH com.psi.autosys.AutosysApplication $CLASSNAME $* 

RETVALUE=$?
export RETVALUE
if [ $RETVALUE -eq 0 ]; then
echo Success
exit 0
fi 


echo Failure
exit 1




##SET EXIT_CODE=%ERRORLEVEL%#
###if NOT "%EXIT_CODE%"=="0" (
##	SET EXIT_CODE=1
##	goto end
##) 


##:end
##@echo Exiting with Exit Code %EXIT_CODE%
##exit %EXIT_CODE%



