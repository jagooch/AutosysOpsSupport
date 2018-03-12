cd $1
ls *,* | while read i;do echo Processing $i; echo new file name `echo $i | sed 's/,//g'`; mv -v "$i" `echo $i | sed 's/,//g'`; done;

