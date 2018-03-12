#!/bin/bash
##Syntax setglobalvariable.cmd <search directory> <file name pattern> <global variable name>
##Written by John Gooch Jun 30,2009
##Updated Jun 30,2009
##syntax is autosys_getfilename <search directory path> <file name pattern> <destination global variable for filename>   
echo on

EXIT_CODE=0
CLASSPATH=/home/autosys/services/autosys_opssupport/bin/*:/home/autosys/services/autosys_opssupport/lib/*: 
CLASSNAME=com.psi.autosys.services.AutosysGetFilename 
JOBHOME=/home/autosys/services/autosys_opssupport
ARGCOUNT=$# 
ARGS=$*
PATTERN=
SEARCHPATH=
GLOBALVARIABLE=
JAVA_HOME=/home/autosys/services/autosys_opssupport/java
PATH=$JAVA_HOME/bin:$PATH
export PATH JAVA_HOME CLASSPATH CLASSNAME JOBHOME ARGCOUNT ARGS PATTERN SEARCHPATH GLOBALVARIABLE

echo command line arguments $*



printhelp() { 
   EXIT_CODE=1
   echo "syntax is autosys_getfilename <search directory path> <file name pattern> <destination global variable for filename>"
   echo Your supplied $ARGCOUNT arguments which where -  $ARGS
   end
}



checkargcount() {
	echo ARGCOUNT after counting is $ARGCOUNT
	if [ $ARGCOUNT -lt 2 ];then 
		echo Not enough arguments supplied.
		return 1
	fi
	if [ $ARGCOUNT -gt 3 ]; then
		echo Too many arguments supplied.
		return 1
	fi
	return 0
}

chksearchdir() {
	##Check the search directory
	echo Checking the search directory
	SEARCHPATH=$1
	#Check the existence of the search dir
	if [ ! -d $SEARCHPATH ]; then 
		echo The Search dir $SEARCHPATH does not exist
		return 1
	fi
	echo SEARCHPATH set to $SEARCHPATH
	return 0
}


chkpattern() {
	##Check the File name pattern
	echo Checking the File name pattern
	PATTERN=$1
	echo PATTERN set to $PATTERN
	return 0
}



getfname() {
	echo Getting File Name
	#echo Printing out sorted environment variables.
	#env | sort

	#echo Environment printout complete
	#echo free form java command sending out to display
        #echo java -Djava.library.path=$JOBHOME/lib -cp "$CLASSPATH" com.psi.autosys.AutosysApplication $CLASSNAME -p $SEARCHPATH -n $PATTERN
	java -Djava.library.path=$JOBHOME/lib -cp "$CLASSPATH" com.psi.autosys.AutosysApplication $CLASSNAME -p $SEARCHPATH -n $PATTERN
	if [  $? -ne 0 ]; then
		echo Did not find any files;
		return 1	
	fi
	#echo free form java command sending output into a file
	OUTPUT=$(java -Djava.library.path=$JOBHOME/lib -cp "$CLASSPATH" com.psi.autosys.AutosysApplication $CLASSNAME -p $SEARCHPATH -n $PATTERN)
	#echo Output of Java Command is $OUTPUT
	#SAVEIFS=$IFS
	#declare -a FNAME=($OUTPUT)
	for x in $OUTPUT
		do 
			if grep -q FILENAME <<<$x;  then
				#echo I found the line with FILENAME, it is $x
				#FILENAME=${x#*=}
				SAVEIFS=$IFS
				IFS="="
				declare -a FNAME=($x)
				FILENAME=${FNAME[1]}
				#echo FILENAME=$FILENAME
			fi
		done
	EXIT_CODE=$?
	return $EXIT_CODE
}


chkglobalvar() {
	echo Checking the Global Variable Name $1
	GLOBALVARIABLE=$3
	return 0
}

#Exit the application / scripts
end() {  
echo Exiting  with Exit Code $EXIT_CODE
exit $EXIT_CODE
}



main() {
	#Check that the correct number of arguments were given
	echo Counting the Supplied Arguments
	checkargcount

	if [ $? -ne 0 ]; then 
		printhelp
		EXIT_CODE=1;export EXIT_CODE
		end
	fi 

	#Check that directory to be searched actually exists
	echo Checking the target search directory
	chksearchdir $1

	if [ $? -ne 0 ]; then 
		printhelp
		EXIT_CODE=1;export EXIT_CODE
		end
	fi 
	
	#Check that a valid pattern was supplied
	echo Checking the file name pattern
	chkpattern $2

	if [ $? -ne 0 ]; then 
		printhelp
		EXIT_CODE=1
		end
	fi 


	if [ $ARGCOUNT -eq 3 ]; then 
		echo Getting the Global Variable Name
		chkglobalvar $3
        	if [ $? -ne 0 ]; then
                	printhelp
                	EXIT_CODE=1
                	end
        	fi
	fi 
	echo Retrieving the File Name
	getfname
	
	if [ $? -ne 0 ]; then
		printhelp
		EXIT_CODE=1
		end
	fi


	if [ $ARGCOUNT -eq 3 ]; then 
		sendevent -E SET_GLOBAL -G "$GLOBALVARIABLE=$FILENAME"
		if [ $? -ne 0  ]; then
			EXIT_CODE=1
			end
		fi
	fi 
	
	
	echo Filename = $FILENAME
	end
}
main $ARGS





 


