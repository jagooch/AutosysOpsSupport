#!/bin/bash
cd $1
FILENAME=`java -jar GetFileName.jar getFileName -p $2 -n $3`
EXITCODE=$?
if [ $EXITCODE -ne 0 ] 
then
	echo getFilName Failed.
	echo FILENAME is $FILENAME
	echo Return code is $EXITCODE
	exit $EXITCODE
fi

sendevent -E SET_GLOBAL -G "$4=$FILENAME"
EXITCODE=$?

if [ $EXITCODE -ne 0 ] 
then
	echo FILENAME is $FILENAME
	echo Return code is $EXITCODE
	exit $EXITCODE
fi


echo FILENAME is $FILENAME
echo EXITCODE is $EXITCODE

  
