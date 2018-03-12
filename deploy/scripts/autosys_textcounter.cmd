::Autosys Text Counter 
::searches the specified plain text file for the specified text pattern. 
:: Returns a 0 if at least one instance is found, otherwise returns a 1
::optionally it can store the count in an autosy global variable if one is given

::syntax ..\autosys_textcounter.cmd -f <path to text file> -p <pattern to search for, surround pattern with single quotes> -g [global variable name]]

SET CLASSPATH="D:\autosys\services\autosys_opssupport\bin\*;D:\autosys\services\autosys_opssupport\lib\*;"
SET HOME=D:\autosys\services\autosys_opssupport
SET EXIT_CODE=0

goto runJavaApp

:loop
SET PARAMS=%PARAMS% %~1
if "%~1"=="" goto runJavaApp
SHIFT
goto loop



:printHelp
@echo Syntax is syntax ..\autosys_textcounter.cmd -f <path to text file> -p <pattern to search for, surround pattern with single quotes> -g [global variable name]]
@echo Example "autosysy_textcounter.cmd  -f D:\autosys\services\conf\ga_schip_c_ul_infile.BZP.properties -p '.*\.txt' 
goto end

:runJavaApp
@echo Starting OpsSupport App with this command line - java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication com.psi.autosys.services.AutosysTextCounter %*
java -Djava.library.path=%HOME%\lib -cp "%CLASSPATH%" com.psi.autosys.AutosysApplication com.psi.autosys.services.AutosysTextCounter %*

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



