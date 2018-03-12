::Syntax setglobalvariable.cmd <search directory> <file name pattern> <global variable name>
::Written by John Gooch Jan 14,2009
::Updated Jan 14,2009
@echo on

SET /A EXIT_CODE=0
SET CLASSPATH="D:\autosys\services\autosys_opssupport\bin\*;D:\autosys\services\autosys_opssupport\lib\*;"
SET CLASSNAME=com.psi.autosys.services.AutosysGetFilename
SET HOME=D:\autosys\services\autosys_opssupport



::Check the search directory
@echo Checking the search directory
if "%1"=="" (
@echo No search directory supplied.
SET /A EXIT_CODE=1
goto end
)
SET SEARCHDIR=%1
SHIFT


::Check the File name pattern
@echo Checking the File name pattern
if "%1"=="" (
@echo File name pattern supplied.
SET /A EXIT_CODE=1
goto end
)
SET PATTERN=%1
SHIFT


::Check the Target Global Variable Name
@echo Checking the Global Variable Name
if "%1"=="" (
@echo No Global Variable Name supplied.
SET /A EXIT_CODE=1
goto end
)
SET GLOBALVAR=%1
SHIFT



::Check the existence of the search dir
if not exist %SEARCHDIR% (
	@echo The Search dir %SEARCHDIR% does not exist
	SET /A EXIT_CODE=1
	goto end
)




::Search for a file matching the filename pattern
@echo searching %SEARCHDIR% for files matching pattern %PATTERN%
::@echo Executing command java -jar GetFileName.jar getFileName -p %2 -n $3 > %AUTO_JOB_NAME%_temp.txt & set /p FILENAME="" < %AUTO_JOB_NAME%_temp.txt
::java -jar GetFileName.jar getFileName -p %2 -n %3 > %AUTO_JOB_NAME%_temp.txt && set /p FILENAME=""<%AUTO_JOB_NAME%_temp.txt
@echo " syntax java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication %CLASSNAME% -p %SEARCHDIR% -n %PATTERN% > %AUTO_JOB_NAME%_temp.txt & set /p FILENAME="" < %AUTO_JOB_NAME%_temp.txt"
@echo Java command with output on the screen 
java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication %CLASSNAME% -p %SEARCHDIR% -n %PATTERN%

@echo Java command without put redirected
::java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication %CLASSNAME% -p %SEARCHDIR% -n %PATTERN% > %AUTO_JOB_NAME%_temp.txt & set /p FILENAME="" < %AUTO_JOB_NAME%_temp.txt
java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication %CLASSNAME% -p %SEARCHDIR% -n %PATTERN% > %AUTO_JOB_NAME%_temp.txt 

SET /A EXITCODE=%ERRORLEVEL%
if not "%EXIT_CODE%"=="0" ( 
	@echo getFilName Failed.
	@echo Return code is %EXIT_CODE%
	goto end
)


::read in the filename from the text file
For /f "tokens=1,2 delims==" %%a in ( %AUTO_JOB_NAME%_temp.txt ) Do (
	If "%%a"=="FILENAME" SET FILENAME=%%b
)
goto deletetmp

:deletetmp
::Delete the tmp file used to store the file name
@echo Deleting the temporary variable file %AUTO_JOB_NAME%_temp.txt
del %AUTO_JOB_NAME%_temp.txt
SET /A EXITCODE=%errorlevel%
if not "%EXIT_CODE%"=="0" ( 
	@echo del %AUTO_JOB_NAME%_temp.txt  Failed 
	@echo FILENAME is %FILENAME%
	@echo Return code is %EXIT_CODE%
	goto end
)


::Check if the filename is empty
@echo Checking if the filename is empty or not
if "%FILENAME%"=="" (
	@echo File name not found.
	SET /A EXIT_CODE=1
	goto end
	
)
 
 
 
::Set the Global Variable to discovered filename
@echo Setting the global variable 

::@echo setting the -G argument
:set globalvar=
@echo Executing sendevent -E SET_GLOBAL -G "%GLOBALVAR%=%FILENAME%"
sendevent -E SET_GLOBAL -G "%GLOBALVAR%=%FILENAME%"

SET /A EXIT_CODE=%errorlevel%
if not "%EXIT_CODE%"=="0" (
	@echo Global variable %GLOBALVAR% name not set.
	@echo FILENAME is %FILENAME%
	@echo Return code is %EXIT_CODE%
	goto end
)


@echo FILENAME is %FILENAME%
@echo EXITCODE is %EXIT_CODE%

  
:end 
@echo Exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%