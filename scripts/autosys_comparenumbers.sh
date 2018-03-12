#!/bin/bash
if [ $# -ne 2 ] 
then
echo "Invalid number of parameters. 2 numbers required. Input $*"
exit 255
fi



VAR1=$1
VAR2=$2
if [ $1 -eq $2  ] 
then
echo "$1 is equal to $2."
exit 0
else
echo "$1 is NOT equal to $2."
exit 1
fi

