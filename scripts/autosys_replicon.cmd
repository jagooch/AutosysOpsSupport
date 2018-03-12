SET EXIT_CODE=0
c:
cd "C:\Program Files\Pervasive\Cosmos9\Common" 
djengine %*

SET EXIT_CODE=%ERRORLEVEL%

if NOT "%EXIT_CODE%"=="0" (
	@echo Error encountered executing djengine
	SET EXIT_CODE=1
	goto end
)
goto end


:end
@echo Exiting with Exit Code %EXIT_CODE%
exit %EXIT_CODE%
