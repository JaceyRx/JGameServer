@echo off

start "GM" cmd /k call "run gm.bat"
TIMEOUT /T 10 /NOBREAK

start "Logic" cmd /k call "run logic.bat"
start "Battle" cmd /k call "run battle.bat"
start "Gateway" cmd /k call "run gateway.bat"
start "Chat" cmd /k call "run chat.bat"