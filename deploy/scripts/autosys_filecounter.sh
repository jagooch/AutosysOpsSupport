#!/bin/bash
EXIT_CODE=0
COUNT=0
FILEPATH=$1
PATTERN=$2

echo Pct 1 = $1
echo Pct 2 = $2
echo FILEPATH = $FILEPATH
echo PATTERN = $PATTERN


echo Executing dir command for testing
echo find $FILEPATH -type f -iname "$2"
find $FILEPATH -type f -iname "$2"
echo Executing dir command test complete

COUNT=`find $FILEPATH -type f -iname "$2" | wc -w` 
exit $COUNT
