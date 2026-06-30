@echo off
where git >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Git is not recognized on your system.
    echo Please download and install Git from: https://git-scm.com/download/win
    echo After installing, restart this window and run this file again.
    pause
    exit /b
)

if not exist .git (
    echo Initializing Git repository...
    git init
)

echo Staging files...
git add .

echo Committing files...
git commit -m "Initial commit of Inventory Management System"

git branch -M main

:: Reset remote just in case
git remote remove origin >nul 2>nul
git remote add origin https://github.com/rajunayak2006/Inventory-management-system-.git

echo Pushing to GitHub...
echo If prompted, please authenticate in the GitHub popup window that appears.
git push -u origin main
pause
