# Check if git is installed
if (!(Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "Git is not recognized on your system." -ForegroundColor Red
    Write-Host "Please download and install Git from: https://git-scm.com/download/win" -ForegroundColor Yellow
    Write-Host "After installing Git, please restart this terminal window and run this script again." -ForegroundColor Yellow
    exit
}

# Initialize git if not already done
if (!(Test-Path .git)) {
    Write-Host "Initializing Git repository..." -ForegroundColor Cyan
    git init
}

# Add all files
Write-Host "Staging files..." -ForegroundColor Cyan
git add .

# Check if there are changes to commit
$status = git status --porcelain
if ($status) {
    Write-Host "Committing files..." -ForegroundColor Cyan
    git commit -m "Initial commit of Inventory Management System"
} else {
    Write-Host "No new changes to commit." -ForegroundColor Yellow
}

# Setup main branch
git branch -M main

# Add remote if not exists
$remoteExists = git remote
if ($remoteExists -notcontains "origin") {
    Write-Host "Adding remote origin..." -ForegroundColor Cyan
    git remote add origin https://github.com/rajunayak2006/Inventory-management-system-.git
} else {
    # Update remote just in case it was set to something else
    git remote set-url origin https://github.com/rajunayak2006/Inventory-management-system-.git
    Write-Host "Remote origin set to: https://github.com/rajunayak2006/Inventory-management-system-.git" -ForegroundColor Yellow
}

# Push to main
Write-Host "Pushing code to GitHub..." -ForegroundColor Cyan
Write-Host "If prompted, please authenticate in the GitHub popup window that appears." -ForegroundColor Green
git push -u origin main
