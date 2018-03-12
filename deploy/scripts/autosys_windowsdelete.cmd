@echo off
::syntax autosys_windowsdelete.cmd from to credentials
::Get the username and password from 
setLocal EnableDelayedExpansion
SET EXIT_CODE=0
@echo the given command line parameters are %0 %*

::get the file mask/path of files to delete
SET WD_TARGET=%1
SHIFT

::get the path to the credentials file
SET WC_CREDFILE=%1
if "%WC_CREDFILE%"=="" ( 
@echo No Credentials File supplied
SET /A WC_CREDS=0
) else (
@echo Credentials File supplied
SET /A WC_CREDS=1
)

if %WC_CREDS% equ 1 (
@echo Going to Get Fileshare
goto get_fileshare
)
@echo Going to Delete Files
goto delete_files

:get_fileshare
@echo Starting get fileshare
SHIFT
SET WC_FILESHARE=%1

if "%WC_FILESHARE%"=="" (
@echo WC_FILESHARE not supplied
SET EXIT_CODE=1
goto end
)
goto :get_creds


:get_creds
@echo Starting get creds
if not exist %WC_CREDFILE% (
@echo Credentials File does not exist
SET EXIT_CODE=1
goto end
) 

@echo Reading Credentials from file

for /f "tokens=* delims= " %%a in (%WC_CREDFILE%) do (
set /a N+=1
set v!N!="%%a"
)
setLocal DisableDelayedExpansion
SET WC_USER=%v1%
SET WC_PWD=%v2%

@echo Going to Connect
goto connect



:connect
@echo Starting Connect
@echo Authenticating to the PSUNITY domain
net use %WC_FILESHARE% /user:%WC_USER% %WC_PWD%
set EXIT_CODE=%errorlevel%
if not "%EXIT_CODE%"=="0" (
@echo Failed to connect
SET EXIT_CODE=1
goto end
)
goto delete_files


:delete_files
@echo Session established. Copying Files
@echo Issuing Command "del /F /Q %WD_TARGET%"  
del /F /Q %WD_TARGET%  
SET EXIT_CODE=%ERRORLEVEL%
@echo exit after the Delete command was %EXIT_CODE%
if not "%EXIT_CODE%"=="0" (
@echo Failed to delete files.
SET EXIT_CODE=1
goto end
)
if %WC_CREDS% equ 0 goto end
goto disconnect


:disconnect
@echo Disconnecting from Domain
net use /delete %WC_FILESHARE% 
SET EXIT_CODE=%errorlevel%
if not "%EXIT_CODE%"=="0" (
@echo Failed to disconnect.
goto end
)
goto end


:end
@echo Starting End
@echo exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%
@echo Ending End