
::autosys_verifyupload
::by John Gooch 
::Created 06/22/2009


SET EXIT_CODE=0

::Check if a source file path was given
if "%1"=="" (
	@echo Required Parameters not given.
	SET EXIT_CODE=1
	goto printhelp
)

if NOT EXIST %1 (
	@echo %1 does not exist. Required Parameters not given.
	SET EXIT_CODE=1
	goto printhelp
)
SET SOURCE=%1
SHIFT



::Check if a dest  file path was given
if "%1"=="" (
	@echo Required Ddst  Parameters not given.
	SET EXIT_CODE=1
	goto printhelp
)

if NOT EXIST %1 (
	@echo %1 does not exist. Required Destination Parametersnot given.
	SET EXIT_CODE=1
	goto printhelp
)
SET DEST=%1
SHIFT
goto robocopy 


:printhelp
@echo syntax is autosys_robocopy <source address> <destination path>
goto end


:robocopy
robocopy %SOURCE%  %DEST% /XD WORKINGDIR JOBOUTPUT ARCHIVE /XF *.out *.err *.log /XO /E


:end
@echo Exiting with Exit Code %EXIT_CODE%
exit  /b %EXIT_CODE%