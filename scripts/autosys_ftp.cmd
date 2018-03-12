::Ops Autosys FTP Application Wrapper 
:: By John Gooch
:: 06/25/2009
@echo Parameters given are  %*
@echo Environment Vars are
@echo AUTOSERV %AUTOSERV%
@echo AUTOROOT %AUTOROOT%
@echo AUTOUSER %AUTOUSER%
@echo COMPUTERNAME %COMPUTERNAME%
java -version

SET CLASSPATH="D:\autosys\services\autosys_opssupport\bin\*;D:\autosys\services\autosys_opssupport\lib\*;"

SET HOME=D:\autosys\services\autosys_opssupport

SET EXIT_CODE=0


:loop
SET PARAMS=%PARAMS% %~1
if "%~1"=="" goto runJavaApp
SHIFT
goto loop



:printHelp
@echo Syntax is autosys_ftp.cmd  <parameters>
@echo Example "autosysy_ftp.cmd  -c D:\autosys\services\conf\ga_schip_c_ul_infile.BZP.properties -l INFO"
goto end

:runJavaApp
@echo Starting OpsSupport App with this command line - java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication com.psi.autosys.services.,Autosys_Ftp %PARAMS%
java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication com.psi.autosys.services.Autosys_Ftp %PARAMS%

SET EXIT_CODE=%ERRORLEVEL%
if not "%EXIT_CODE%"=="0" (
	@echo Error Running Java application
	SET EXIT_CODE=1
	goto end
)
goto end



:end 
@echo Exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%
