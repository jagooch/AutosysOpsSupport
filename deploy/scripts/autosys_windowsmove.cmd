@echo on

@echo Parameters passed are %*


SET /A COUNT=0
SET EXIT_CODE=0

if "%1"=="" (
@echo No From parameter given
SET EXIT_CODE=1
goto end
)


if "%2"=="" (
@echo No TO parameter given
SET EXIT_CODE=1
goto end
)



@echo off
@echo Check if %1 exists
if not exist %1 (
@echo Directory %1 does not exist.
SET EXIT_CODE=1
goto end
) else (
@echo Directory %1 exists
)


@echo Check if directory %2 exists.
if not exist %2 (
@echo Directory %2 does not exist.
SET EXIT_CODE=1
goto end
) else (
@echo Directory %2 exists
)


echo Executing command for /f %%a in ('dir /b %1') do SET /A COUNT=%COUNT%+1
for /f %%a in ('dir /b %1') do SET /A COUNT=%COUNT%+1
@echo COUNT=%COUNT%
if "%COUNT%"=="0" exit 0



@echo Executing command move /Y %1 %2
move /Y %1 %2

:end
@echo Exiting with EXIT_CODE=%EXIT_CODE%
exit %EXIT_CODE%