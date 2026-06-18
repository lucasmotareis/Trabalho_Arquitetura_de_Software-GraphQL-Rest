@echo off
cd /d "%~dp0"
set steam_master_ipc_name_override=
node .\node_modules\vite\bin\vite.js --host 0.0.0.0 --port 5173
