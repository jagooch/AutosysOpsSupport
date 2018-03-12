#!/bin/bash
##cho on
CLASSPATH=/home/autosys/services/autosys_opssupport/lib/*:/home/autosys/services/autosys_opssupport/bin/*
JAVA_HOME=/home/autosys/services/autosys_opssupport/java
PATH=$JAVA_HOME/bin:$PATH


##check that sufficient arguments were given
if [ $# != 2 ]; then
   echo "Syntax autosys_archive.sh <working directory> <step name>"   
   exit 1
fi


#Check that the target directory exits
if [ ! -d $1 ]; then
   echo "Specified working directory does not exist or access is denied. Please check the path."
   exit 1
fi

export CLASSPATH JAVA_HOME PATH
export JOBROOT=$1
export JOBNAME=$2

echo JOBROOT = $1 JOBNAME= $2

##echo "Environment setup complete, here it is"
##env | sort


##echo Java Version is 
##java -version
##the java command w/output is 
##java -cp $CLASSPATH com.psi.autosys.AutosysApplication com.psi.autosys.services.AutosysTimeStamp "MMddyyyy_HHmmdd" 



##get the current time
java -cp $CLASSPATH com.psi.autosys.AutosysApplication com.psi.autosys.services.AutosysTimeStamp "MMddyyyy_HHmmdd" |
while IFS="=" read f1 f2
do
if [ "$f1"="TIMESTAMP" ]; then
echo "$f1=$f2"
TIMESTAMP=$f2
export TIMESTAMP
fi 
done



##SET DATETIMESTAMP=%%a
##)
##echo DATETIMESTAMP=%DATETIMESTAMP%

echo java -cp $CLASSPATH com.psi.autosys.AutosysApplication com.psi.autosys.services.Autosys_Zip -a CREATE -s "$JOBROOT/workingdir/input;$JOBROOT/workingdir/output;$JOBROOT/workingdir/log;$JOBROOT/joboutput" -d "$JOBROOT/archive" -z ${JOBNAME}_arc_${TIMESTAMP}.zip -c -l INFO

java -cp $CLASSPATH com.psi.autosys.AutosysApplication com.psi.autosys.services.Autosys_Zip -a CREATE -s "$JOBROOT/workingdir/input;$JOBROOT/workingdir/output;$JOBROOT/workingdir/log;$JOBROOT/joboutput" -d "$JOBROOT/archive" -z ${JOBNAME}_arc_${TIMESTAMP}.zip -c -l INFO 

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



