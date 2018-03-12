::Ops Autosys Application Wrapper 
:: By John Gooch
:: 03/30/2009
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
dir

::@echo original pct 1 = %1

SET CLASSNAME=%~1
if "%CLASSNAME%"=="" (
	@echo Classname parameter not set
	SET EXIT_CODE=1
	goto printHelp
)
SHIFT


:loop
SET PARAMS=%PARAMS% %~1
if "%~1"=="" goto runJavaApp
SHIFT
goto loop



:printHelp
@echo Syntax is autosys_opssupport.cmd <fully qualified class name> <parameters>
@echo Example "autosysy_opssupport.cmd com.psi.autosys.AutosysApplication com.psi.autosys.project.nmsn.Autosys_NMSN -c c:\test_deploy\conf\autosys_ganmsn.properties -l INFO"
goto end

:runJavaApp
@echo Starting OpsSupport App with this command line - java -cp %CLASSPATH% com.psi.autosys.AutosysApplication %CLASSNAME% %PARAMS%
::java -Djava.library.path=\lib -cp %CLASSPATH% com.psi.autosys.AutosysApplication %CLASSNAME% %PARAMS%
java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication %CLASSNAME% %PARAMS%


SET EXIT_CODE=%errorlevel%
if not "%EXIT_CODE%"=="0" (
	@echo Error Running Java application
)
goto end



:end 
@echo Exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%
