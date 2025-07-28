mvn clean package
Copy-Item "C:\Users\hdoan3\code\PhasorJ\target\PhasorJ-1.0-SNAPSHOT.jar" "C:\Users\hdoan3\code\Fiji\jars\" -Force
Start-Process "C:\Users\hdoan3\code\Fiji\fiji-windows-x64.exe"
