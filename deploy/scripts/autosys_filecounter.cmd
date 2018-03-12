if "%1"=="" goto help
if "%2"=="" goto help
SET /A EXIT_CODE = 0
SET /A %COUNT% = 0
SET FILEPATH=%1
SET PATTERN=%2
SET EXIT_CODE=0

Echo Pct 1 = %1
echo Pct 2 = %2
echo FILEPATH = %FILEPATH%
echo PATTERN = %PATTERN%


@echo Executing dir command for testing
dir /b /a-d /S %FILEPATH%
if not exist %FILEPATH% (
	echo Directory %FILEPATH% does not exist.
	SET /A EXIT_CODE=254
	goto end
)

@echo Executing dir command test complete



for /f "tokens=*" %%a in ('dir  /b /a-d /S %FILEPATH%\%PATTERN%') do @set /A COUNT+=1
echo %COUNT% files found with name pattern %PATTERN% in directory %FILEPATH%
SET /A EXIT_CODE=%COUNT%
goto end

:help
@echo Syntax: autosys_filecounter.cmd <directory> <file name pattern>
set /A EXIT_CODE=254
goto end


:end
echo Exiting with Exit Code %EXIT_CODE% 
exit %EXIT_CODE%
