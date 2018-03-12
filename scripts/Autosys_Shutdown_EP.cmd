@echo off
SET EXIT_CODE=0
SET AUTOROOT=D:\PROGRA~1\CA\UNICEN~1.BZP
SET AUTOSERV=BZP
SET AUTOSYS=D:\PROGRA~1\CA\UNICEN~1.BZP\autosys
SET AUTOUSER=D:\PROGRA~1\CA\UNICEN~1.BZP\autouser


::net stop "CA Unicenter AutoSys Event Processor (BZP)"
sendevent -E STOP_DEMON
net start | find /C "AutoSys" | find "3"
if errorlevel 0 (
goto fail_mail
) else (
goto success_mail
)


:fail_mail
D:\Autosys\services\blat\blat.exe -to ProductionSupport@policy-studies.com -s "Autosyse EP Shutdown FAILED" -body "Bus Ops - Production. Check T-Autosys-App2 EP service and check database dboraprd001"  
SET EXIT_CODE=1
goto end

:goto success_mail
D:\Autosys\services\blat\blat.exe -to ProductionSupport@policy-studies.com -s "Autosyse EP Shutdown Successful" -body "Bus Ops - Production. Check T-Autosys-App2 EP service to see why the shutdown failed."  
goto end

:end
@echo Exiting with exit code %EXIT_CODE%
exit %EXIT_CODE%
