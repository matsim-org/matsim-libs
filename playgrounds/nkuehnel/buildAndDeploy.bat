cd /D %~dp0
call ant clean
call ant dist
copy /Y dist\josmMatsimPlugin.jar   "%APPDATA%\JOSM\plugins\josmMatsimPlugin.jar"
echo BAT - After Ant run
