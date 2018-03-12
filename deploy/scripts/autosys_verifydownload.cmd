::autosys_verifydownload
::by John Gooch
::Created on 7/10/2009

SET EXIT_CODE=0
SET ARGCOUNT=0
SET FILECOUNT=0
SET PARAMS=%*
goto countargs


::COUNT the number of parameters passed in
:countargs
@echo Counting the number of supplied parameters
:count
IF NOT "%1"==""  ( 
   SET VAR%ARGCOUNT%=%1	
   SET /A ARGCOUNT+=1
   SHIFT
   goto count
) 
@echo After counting - ARGCOUNT=%ARGCOUNT%
goto checkargcount


:checkargcount
IF %ARGCOUNT% EQU 0 (
	@echo ERROR Not enough parameters supplied
	SET EXIT_CODE=1
	goto printhelp
)

IF %ARGCOUNT% GTR 2 (
	@echo ERROR Too many parameters supplied
	SET EXIT_CODE=1
	goto printhelp

)

goto setvars


:setvars

IF %ARGCOUNT% GEQ 1 (
	@echo VAR0=%VAR0%	
	SET LOGFILEPATH=%VAR0%
)

IF %ARGCOUNT% EQU 2 (
	@echo VAR1=%VAR1%	
	SET GLOBALVAR=%VAR1%
)
goto parselogfile 


:parselogfile
::Check if the any files were downloaded
find /C "successfully downloaded" %LOGFILEPATH%
IF NOT "%ERRORLEVEL%"=="0" (
@echo ERROR Problem with the log file parsing.  Errorlevel %ERRORLEVEL%
SET EXIT_CODE=1
goto end
)


for /F "tokens=3 delims= " %%a IN ('find /C "successfully downloaded" %LOGFILEPATH%') do (
	SET FILECOUNT=%%a
)
IF %ARGCOUNT% EQU 2 goto setglobalvar
goto end


:setglobalvar
::SET GLOBALVAR=%GLOBALVAR: =%
for /F "tokens=2 delims= " %%a IN (%GLOBALVAR%) DO (
echo "Replacement GV Value is  %%a"
)

SET COMMAND=%GLOBALVAR%
SET COMMAND="%COMMAND%="
SET COMMAND=%COMMAND%%FILECOUNT%
@echo COMMAND is %COMMAND%
@echo "GLOBLVAR %GLOBALVAR% after spaces have been removed."
sendevent -E SET_GLOBAL -G "%GLOBALVAR%=%FILECOUNT%"
IF NOT "%ERRORLEVEL%"=="0" (
	@echo Error Failed to update global variable %GLOBALVAR%
	SET EXIT_CODE=1
	goto end
)
goto end

:printhelp
@echo Correct syntax is
@echo autosys_verifydownload.cmd <path to autosys_ftp log file>
goto end


:end
@echo Exiting with Exit Code %EXIT_CODE% 
exit %EXIT_CODE%