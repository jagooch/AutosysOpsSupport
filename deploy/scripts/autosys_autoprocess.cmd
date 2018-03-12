@echo off
@SET WORKING_DIR=
@SET /A OUTPUT_FILE_COUNT=0
@SET AP_PARAMS=
@SET /A EXIT_CODE=0
@SET /A COUNT=0

@echo Setting Parameters
if "%1"=="" (
	goto print_help
)
@SET WORKING_DIR=%1
@SHIFT

if "%1"=="" goto print_help

::@SET /A OUTPUT_FILE_COUNT=%1
::@SHIFT


::Now get the params to pass to autoprocess
if "%1"=="" goto print_help
goto ap_params
GOTO END
 

:ap_params
@echo running ap_params
SET AP_PARAMS=%AP_PARAMS% %1
SHIFT
if not "%1"=="" (
	goto ap_params
	)
goto joblogger_param



::create joblogger parameter value and add it in front of other to AP_PARAMS
:joblogger_param
@echo running joblogger_param
if not exist %WORKING_DIR% (
	@echo Working directory %WORKING_DIR% does not exist.
	set /A EXIT_CODE=254
	echo EXIT_CODE set to 1 due to missing directory
	goto end
	)
set JOB_LOGGER=%WORKING_DIR%\log\%AUTO_JOB_NAME%.log
set AP_PARAMS=joblogger=%JOB_LOGGER% %AP_PARAMS%
goto run_ap


::run autoprocess now
:run_ap
@echo running run_ap
@echo Running AP command - autoprocess %AP_PARAMS%
autoprocess %AP_PARAMS%
::check the return code to see if AP was successful or not
set /A EXIT_CODE=%errorlevel%

if not %EXIT_CODE% == 0 (
	echo EXIT_CODE set to 1 due to AP's exit code not being 0
	set /A EXIT_CODE=254
	goto end 
) 
goto end 


::check if there should be output files
:chk_output
@echo Running Chk_Output
::Check the expected number of files, if 0, goto end
::if %OUTPUT_FILE_COUNT%==0 goto end

SET /A COUNT=0
@echo Executing dir command for testing
dir %WORKING_DIR%\output /b /a-d /S
@echo Executing dir command test complete


for /f "tokens=*" %%a in ('dir %WORKING_DIR%\output /b /a-d /S') do @set /A COUNT+=1

::if not %OUTPUT_FILE_COUNT%==%COUNT% (
::goto end
::)

if %COUNT% > 0 echo DONE > %WORKING_DIR%\output\done.txt
set /A EXIT_CODE=%COUNT%
goto end


:print_help
@echo Running print help
@echo syntax: autosys_autoprocess (working directory path) (space-separated autoprocess job id's)
set /A EXIT_CODE = 254
echo EXIT_CODE set to 1 due to incorrect syntax
goto end

:end
@echo Running End
@echo .
@echo .
@echo WORKING_DIR=%WORKING_DIR%
@echo AP_PARAMS=%AP_PARAMS%
@echo OUTPUT_FILE_COUNT=%OUTPUT_FILE_COUNT%
@echo COUNT=%COUNT%
@echo EXIT_CODE = %EXIT_CODE%
exit %EXIT_CODE%