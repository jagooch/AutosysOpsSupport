SET EXIT_CODE=0
SET AUTOROOT=D:\PROGRA~1\CA\UNICEN~1.BZP
SET AUTOSERV=BZP
SET AUTOSYS=D:\PROGRA~1\CA\UNICEN~1.BZP\autosys
SET AUTOUSER=D:\PROGRA~1\CA\UNICEN~1.BZP\autouser


net start "CA Unicenter AutoSys Remote Agent (BZP)"
net start "CA Unicenter AutoSys Event Processor (BZP)"

@echo Verifying that all necessary Autosys services are started
net start | find /C "AutoSys" | find "3"
if not errorlevel 0 (
   goto fail_mail
) else (
  goto success_mail 
)

:fail_mail
::D:\autosys\services\autosys_email\autosys_email.cmd D:\autosys\services\autosys_email C:\scripts C:\scripts\autosys_email.fail.properties C:\scripts\log4j.properties
D:\Autosys\services\blat\blat.exe -to ProductionSupport@policy-studies.com -s "Autosyse EP Startup FAILED" -body "Bus Ops - Production. Check T-Autosys-App2 EP service and check database dboraprd001"  

SET EXIT_CODE=1
goto end


:success_mail
::D:\autosys\services\autosys_email\autosys_email.cmd D:\autosys\services\autosys_email C:\scripts C:\scripts\autosys_email.success.properties C:\scripts\log4j.properties
D:\Autosys\services\blat\blat.exe -to ProductionSupport@policy-studies.com -s "Autosyse EP Started Successfully" -body "Bus Ops - Autosys EP service started."  

SET EXIT_CODE=0
goto end



:end
@Echo Exiting with exit code %EXIT_CODE%
exit %EXIT_CODE%




