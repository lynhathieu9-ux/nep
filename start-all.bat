@echo off
chcp 65001 >nul
title NEP 环保公众监督系统 - 一键启动
echo ============================================
echo   NEP 环保公众监督系统 一键启动
echo ============================================
echo.
echo [前置] 请确保 MySQL(3306) 与 Nacos(8848) 已启动
echo   Nacos 启动: nacos\nacos\bin\startup.cmd -m standalone
echo.

set BASE=%~dp0

echo [1/5] 启动 用户服务 (8082) ...
start "NEP-user"     cmd /c "java -jar "%BASE%nep-service-user\target\nep-service-user-1.0.0.jar" > "%BASE%logs\run-user.log" 2>&1"
timeout /t 25 /nobreak >nul

echo [2/5] 启动 反馈服务 (8083) ...
start "NEP-feedback" cmd /c "java -jar "%BASE%nep-service-feedback\target\nep-service-feedback-1.0.0.jar" > "%BASE%logs\run-feedback.log" 2>&1"

echo [3/5] 启动 内容服务 (8084) ...
start "NEP-content"  cmd /c "java -jar "%BASE%nep-service-content\target\nep-service-content-1.0.0.jar" > "%BASE%logs\run-content.log" 2>&1"
timeout /t 30 /nobreak >nul

echo [4/5] 启动 网关 (9000) ...
start "NEP-gateway"  cmd /c "java -jar "%BASE%nep-gateway\target\nep-gateway-1.0.0.jar" > "%BASE%logs\run-gateway.log" 2>&1"
timeout /t 15 /nobreak >nul

echo [5/5] 启动 前端 (3000) ...
start "NEP-frontend" cmd /c "cd /d "%BASE%nep-ui" && npm run dev"

echo.
echo ============================================
echo   启动完成！各服务在独立窗口运行
echo   前端:   http://localhost:3000
echo   网关:   http://localhost:9000
echo   Nacos:  http://localhost:8848/nacos (nacos/nacos)
echo   接口文档: http://localhost:9000/doc.html
echo ============================================
echo   关闭各 NEP-* 窗口即可停止对应服务
pause
