:: By John Gooch
::copyright 2008
::Requires Autosys

::@echo %*
set /A COUNT=0
set /A EXIT_CODE=0
::See if there any files to move
for /f "tokens=*" %%a in ('dir %1\GA*.zip /b /a-d' ) do set /A COUNT=%COUNT% + 1

if %COUNT%==0 ( 
set /A EXIT_CODE=1	
goto end 
)

::locate the oldest zip file
for /f "tokens=*" %%a in ('dir %1\GA*.zip /b /a-d' ) do @set OLDEST=%%a

@echo The oldest zip file is %OLDEST%


::Set the global variable containing the path to the file path
sendevent -E SET_GLOBAL -G "EPROSFILE=%OLDEST%"


::move the oldest zip file from incoming to outgoing.
::move /Y %1\%OLDEST% %2\
::if errorlevel 0 echo DoNE > %2\done.txt
::if not errorlevel 0 set /A EXIT_CODE=%errorlevel% 

:end
@echo EXIT_CODE=%EXIT_CODE%
exit %EXIT_CODE%