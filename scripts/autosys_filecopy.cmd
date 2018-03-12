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
goto run


:run
java -cp %CLASSPATH% com.psi.autosys.AutosysApplication com.psi.autosys.services.Autosys_FileCopy %*
SET EXIT_CODE=%ERRORLEVEL%
goto end

:end 
@echo Exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%
 

