#!/bin/bash



function printHelp {
   echo "Syntax is autosys_replacetext <directory> <matching text> <replacement text>"
}


if [ $# -ne 3 ]
 then
    echo Not enought parameters.  - $# 
    printHelp
    exit 1 
fi

echo Parameters are
echo 1 = $1
echo 2 = $2
echo 3 = $3


if [ -d  $1 ] 
then
    cd $1 
    echo The command is ls *$2*  
    echo "while read i do echo Processing $i;" 
    echo "new file name"
    echo $i | sed 's/$2/$3/g'
    
    echo  "mv -v $i `echo $i | sed 's/"$2"/"$3"/g'`;"
    echo "done;"
    ls *$2* | while read i;do echo Processing $i; echo new file name `echo $i | sed "s/$2/$3/g"`; mv -v "$i" `echo $i | sed "s/$2/$3/g"`; done;
else
   exit 1
fi



