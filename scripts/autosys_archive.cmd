@echo on
SET CLASSPATH="D:\autosys\services\autosys_opssupport\lib\*;D:\autosys\services\autosys_opssupport\bin\*"
SET JOBROOT=%1
SET JOBNAME=%2


::the java command w/output is 
java -cp %CLASSPATH% com.psi.autosys.AutosysApplication com.psi.autosys.services.AutosysTimeStamp "MMddyyyy_HHmmdd"


::get the current time
for /f "tokens=2 delims==" %%a in ('java -cp %CLASSPATH% com.psi.autosys.AutosysApplication com.psi.autosys.services.AutosysTimeStamp "MMddyyyy_HHmmdd"') do (
SET DATETIMESTAMP=%%a
)
echo DATETIMESTAMP=%DATETIMESTAMP%



java -cp %CLASSPATH% com.psi.autosys.AutosysApplication com.psi.autosys.services.Autosys_Zip -a CREATE -s "%JOBROOT%\workingdir\input;%JOBROOT%\workingdir\output;%JOBROOT%\workingdir\log;%JOBROOT%\joboutput" -d "%JOBROOT%\archive" -z %JOBNAME%_arc_%DATETIMESTAMP%.zip -c -l INFO 


SET EXIT_CODE=%ERRORLEVEL%
if NOT "%EXIT_CODE%"=="0" (
	SET EXIT_CODE=1
	goto end
) 


:end
@echo Exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%



