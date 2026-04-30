@echo off
echo Running build task...
call gradlew build

echo Build completed. Checking for jar files...
dir build\libs\*.jar /s

pause