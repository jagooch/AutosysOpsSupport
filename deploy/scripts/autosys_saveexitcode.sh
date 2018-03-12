##Autosys SaveExitCode 
##by TheGooch
##Copyright (lol ) 12/16/2008

#autorep -J $1 -w | grep 'SU ' | sendevent -E SET_GLOBAL -G  "$2=$(awk -F"/" '{print $2}')"
IFS=" "
count=0
line=$(autorep -J $1 -w | grep $1)
echo LINE=$line
for i in $line
do 
last=$i
count=`expr $count + 1`
echo "I counted $count fields"
done
echo LAST=$last

if [ $count = 8 ]
then
	echo sendevent -E SET_GLOBAL -G "$2=$last"
	sendevent -E SET_GLOBAL -G "$2=$last"
elif [ $count = 4 ]
then
	echo "Job $1 cannot be found. Check the name and try again."
	exit 1
else
	echo sendevent -E SET_GLOBAL -G "$2=0"
	sendevent -E SET_GLOBAL -G "$2=0"	
fi

  
#echo EXIT_CODE=$EXIT_CODE
