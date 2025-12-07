@echo off
call mvnw.cmd clean compile > build-output.txt 2>&1
echo Build complete. Check build-output.txt for results.

