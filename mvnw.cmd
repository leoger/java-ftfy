@ECHO OFF
SETLOCAL
SET BASE_DIR=%~dp0
SET WRAPPER_DIR=%BASE_DIR%.mvn\wrapper
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
SET PROPS_FILE=%WRAPPER_DIR%\maven-wrapper.properties

IF NOT EXIST "%WRAPPER_JAR%" (
  FOR /F "tokens=1,* delims==" %%A IN (%PROPS_FILE%) DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
  )
  IF NOT "%WRAPPER_URL%"=="" (
    POWERSHELL -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-WebRequest -UseBasicParsing '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%' } catch {}"
  )
)

IF EXIST "%WRAPPER_JAR%" (
  java -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
  EXIT /B %ERRORLEVEL%
)

mvn %*
