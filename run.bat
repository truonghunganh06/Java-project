@echo off
title Topic Management Web Server
echo Dang khoi dong ung dung web Spring Boot...
set JAVA_HOME=C:\Users\Windows\.jdks\openjdk-26.0.2.1
set PATH=%JAVA_HOME%\bin;%PATH%
call mvnw.cmd spring-boot:run
pause
