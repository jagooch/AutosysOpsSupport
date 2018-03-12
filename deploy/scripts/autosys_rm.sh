#!/bin/bash


if [ $# -ne 2 ]; 
	then
	echo "Usage: autosys_rm.sh <target dir> < filename pattern >"
	exit 1

else 
	echo "- $# - arguments supplied. They are - $* -"

fi

if [ ! -d "$1" ] 
then
echo $1 is not a directory
exit 1
else
echo $1 is a directory
fi

cd $1

echo I found `ls| xargs|wc` files
if [ "$(ls|xargs)" ]; 
then
   echo Directory $1 is not empty	
   echo find /path/to/dir/delete -type f -print0 -iname '*.txt' | xargs -0 /bin/rm -f	
   find $1 -type f -print0 -iname '$2' | xargs -0 /bin/rm -f 
   if [ $? -ne 0   ]
	then
	echo Error occured while deleting files from $1
	exit 1
   else
   	echo "All files matching $1/$2 successfully deleted."
   fi
else 
   echo "Directory $1 is empty."
   echo "Nothing to do..."

fi
echo "Script completed. Exiting with Exit Code $?"
exit 0
