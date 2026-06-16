@echo off
chcp 65001 >nul
title LinkChat 服务启动器

echo.
echo   ╔══════════════════════════════════════╗
echo   ║     LinkChat 服务端一键启动          ║
echo   ╚══════════════════════════════════════╝
echo.

set "TOOLS=C:\Users\彭勋豪\linkchat-tools"
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot"

echo [1/3] 启动 Redis...
start "LinkChat-Redis" /MIN "%TOOLS%\redis\redis-server.exe" --maxheap 256MB
timeout /t 1 /nobreak >nul
"%TOOLS%\redis\redis-cli.exe" ping >nul 2>&1
if %errorlevel%==0 (
    echo         Redis    已启动 [OK]
) else (
    echo         Redis    启动失败!
)

echo [2/3] 启动 MinIO...
set MINIO_ROOT_USER=minioadmin
set MINIO_ROOT_PASSWORD=minioadmin
start "LinkChat-MinIO" /MIN "%TOOLS%\minio.exe" server "%TOOLS%\minio-data" --console-address ":9001"
timeout /t 2 /nobreak >nul
echo         MinIO    已启动 [OK]

echo [3/3] 启动 Spring Boot 后端...
cd /d "d:\GitHub\item\chat-1\linkchat-server"
start "LinkChat-Backend" /MIN java -jar "target\linkchat-server-1.0.0.jar"
echo         后端     正在启动... 等待约 10 秒
timeout /t 8 /nobreak >nul

echo.
echo   ╔══════════════════════════════════════╗
echo   ║  全部服务启动完成！                  ║
echo   ║                                      ║
echo   ║  后端 API:  http://localhost:8080/api ║
echo   ║  MinIO管理: http://localhost:9001     ║
echo   ║                                      ║
echo   ║  前端另启: cd linkchat-web ^&^& npm run dev ║
echo   ╚══════════════════════════════════════╝
echo.
echo 按任意键关闭此窗口...
pause >nul
