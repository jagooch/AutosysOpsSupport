::autosys_verifyupload
::by John Gooch
::Created on 6/22/2009

SET EXIT_CODE=0

::Check if a log file path was given
if "%1"=="" (
	@echo Required Parameters not given.
	SET EXIT_CODE=1
	goto printhelp
)
SET LOGFILEPATH=%1
goto parselogfile 


:parselogfile
find /C "successfully uploaded" %LOGFILEPATH%

SET EXIT_CODE=%ERRORLEVEL%

if not "%EXIT_CODE%"=="0" (
	@echo No files were uploaded. Please check job log files for more information.
	SET EXIT_CODE=1
	goto end
)
goto end


:printhelp
@echo Correct syntax is
@echo autosys_verifyupload.cmd <path to autosys_ftp log file>
goto end


:end
@echo Exiting with Exit Code %EXIT_CODE% 
exit %EXIT_CODE%