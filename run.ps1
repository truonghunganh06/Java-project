Write-Host "Dang khoi dong ung dung web Spring Boot..." -ForegroundColor Cyan
$env:JAVA_HOME = "C:\Users\Windows\.jdks\openjdk-26.0.2.1"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw.cmd spring-boot:run
