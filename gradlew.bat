@echo off
setlocal
set "ROOT=%~dp0"
set "WRAPPER=%ROOT%gradle\wrapper\gradle-wrapper.jar"
if not exist "%WRAPPER%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$p='%WRAPPER%'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.13-wrapper.jar' -OutFile $p; if ((Get-FileHash $p -Algorithm SHA256).Hash.ToLower() -ne '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f') { Remove-Item $p; throw 'Gradle wrapper checksum mismatch' }"
  if errorlevel 1 exit /b 1
)
java -classpath "%WRAPPER%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %errorlevel%
