@echo off
setlocal

rem Set Visual Studio paths
set "VSINSTALLDIR=C:\Program Files (x86)\Microsoft Visual Studio\18\BuildTools"
set "VCINSTALLDIR=%VSINSTALLDIR%\VC"
set "VCToolsInstallDir=%VCINSTALLDIR%\Tools\MSVC\14.50.35717"
set "WindowsSdkDir=C:\Program Files (x86)\Windows Kits\10\"
set "WindowsSDKVersion=10.0.26100.0"

rem Set INCLUDE
set "INCLUDE=%VCToolsInstallDir%\include;%WindowsSdkDir%Include\%WindowsSDKVersion%\ucrt;%WindowsSdkDir%Include\%WindowsSDKVersion%\um;%WindowsSdkDir%Include\%WindowsSDKVersion%\shared"

rem Set LIB
set "LIB=%VCToolsInstallDir%\lib\x64;%WindowsSdkDir%Lib\%WindowsSDKVersion%\ucrt\x64;%WindowsSdkDir%Lib\%WindowsSDKVersion%\um\x64"

rem Set PATH with MSVC tools first
set "PATH=%VCToolsInstallDir%\bin\Hostx64\x64;%WindowsSdkDir%bin\%WindowsSDKVersion%\x64;%PATH%"

rem Set 改成自己的路径
set "JAVA_HOME=D:\jdk\graalvm-jdk-21.0.7+8.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d D:\my-project\kcode
mvn -Pnative native:compile
endlocal
