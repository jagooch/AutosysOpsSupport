::Syntax setglobalvariable.cmd <search directory> <file name pattern> <global variable name>
::Written by John Gooch Jan 14,2009
::Updated Jan 14,2009
::syntax is autosys_getfilename <search directory path> <file name pattern> <destination global variable for filename>   
@echo on

SET /A EXIT_CODE=0
SET CLASSPATH="D:\autosys\services\autosys_opssupport\bin\*;D:\autosys\services\autosys_opssupport\lib\*"
SET CLASSNAME=com.psi.autosys.services.AutosysGetFilename
SET HOME=D:\autosys\services\autosys_opssupport
SET /A ARGCOUNT=1
SET FILENAME=

:goto argcounter

::Count the number of arguments
:argcounter
for %%a in (%*) do (
SET /A ARGCOUNT+=1
@echo Counting Argument %%a = %ARGCOUNT%
@echo check for another one
)

goto checkargcount

:checkargcount
@echo ARGCOUNT after counting is %ARGCOUNT%
if %ARGCOUNT% lss 2 (
	@echo Not enough arguments supplied.
	goto printhelp
)
if %ARGCOUNT% gtr 3 (
	@echo Too many arguments supplied.
	goto printhelp
)
goto :chksearchdir


:printhelp 
SET EXIT_CODE=1
@echo "syntax is autosys_getfilename <search directory path> <file name pattern> <destination global variable for filename>"
@echo Your supplied %*
goto end


:continue
:chksearchdir
::Check the search directory
@echo Checking the search directory
SET SEARCHDIR=%1
::Check the existence of the search dir
if not exist %SEARCHDIR% (
	@echo The Search dir %SEARCHDIR% does not exist
	SET /A EXIT_CODE=1
	goto end
)
SHIFT
goto chkpattern


:chkpattern
::Check the File name pattern
@echo Checking the File name pattern
SET PATTERN=%1
SHIFT


if %ARGCOUNT% equ 3 goto chkglobalvar
goto getfname

:chkglobalvar
::Check the Target Global Variable Name
@echo Checking the Global Variable Name
SET GLOBALVAR=%1
goto getfname

:getfname
@echo free form java command output into a file
java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication %CLASSNAME% -p %SEARCHDIR% -n %PATTERN%

@echo free form java command output into a file
java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication %CLASSNAME% -p %SEARCHDIR% -n %PATTERN%>%AUTO_JOB_NAME%_temp.txt

::read the file output into a variable
for /F "tokens=1,2 delims==" %%a in (%AUTO_JOB_NAME%_temp.txt) do (
	if "%%a"=="FILENAME" (
		@echo found filename output
		@echo FILENAME=%%b
		SET FILENAME=%%b
	) 
)

@echo filename after running java command is FILENAME=%FILENAME%




@echo searching %SEARCHDIR% for files matching pattern %PATTERN%


SET /A EXITCODE=%ERRORLEVEL%
if not "%EXIT_CODE%"=="0" ( 
	@echo getFilName Failed.
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
 
if %ARGCOUNT equ 2 (
	@echo FILENAME=%FILENAME%
 	goto end
)
goto setglobalvar

 
:setglobalvar 
::Set the Global Variable to discovered filename
@echo Setting the global variable 

::@echo setting the -G argument
::set globalvar=
@echo Executing sendevent -E SET_GLOBAL -G "%GLOBALVAR%=%FILENAME%"
sendevent -E SET_GLOBAL -G "%GLOBALVAR%=%FILENAME%"

SET /A EXIT_CODE=%ERRORLEVEL%
if not "%EXIT_CODE%"=="0" (
	@echo Global variable %GLOBALVAR% name not set.
	@echo FILENAME is %FILENAME%
	@echo Return code is %EXIT_CODE%
	goto end
)
@echo FILENAME is %FILENAME%
@echo EXITCODE is %EXIT_CODE%
:goto end
  
  

  
:end 
@echo Exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%