cd /D %~dp0
call ant clean
call ant install
echo BAT - After Ant run
