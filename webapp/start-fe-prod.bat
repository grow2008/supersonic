setlocal
for /f "delims=" %%i in ('node -v') do set "node_version=%%i"
node -e "try { process.binding('http_parser'); } catch (error) { console.error('Current Node.js ' + process.version + ' is incompatible with the Supersonic frontend toolchain because webpack-dev-server depends on the removed internal module http_parser. Please use Node.js 18 or 20 LTS.'); process.exit(1); }"
if errorlevel 1 (
  exit /b 1
)
for /f "tokens=2 delims=v." %%i in ("%node_version%") do set "major_version=%%i"
if %major_version% GEQ 17 (
  set "NODE_OPTIONS=--openssl-legacy-provider"
  echo Node.js version is greater than or equal to 17. NODE_OPTIONS has been set to --openssl-legacy-provider.
)
where /q pnpm
if errorlevel 1 (
  echo pnpm is not installed. Installing...
  npm install -g pnpm
  if errorlevel 1 (
    echo Failed to install pnpm. Please check if npm is installed and the network connection is working.
  ) else (
    echo pnpm installed successfully.
  )
) else (
  echo pnpm is already installed.
)

if exist .\supersonic-webapp.tar.gz del /q .\supersonic-webapp.tar.gz

rmdir /S /Q .\packages\supersonic-fe\src\.umi
rmdir /S /Q .\packages\supersonic-fe\src\.umi-production
cd ./packages/chat-sdk
call pnpm i
if errorlevel 1 (
  exit /b 1
)
call pnpm run build
if errorlevel 1 (
  exit /b 1
)
call pnpm link --global
if errorlevel 1 (
  exit /b 1
)
cd ../supersonic-fe
call pnpm link ../chat-sdk
if errorlevel 1 (
  exit /b 1
)
call pnpm i
if errorlevel 1 (
  exit /b 1
)
call pnpm run build:os-local
if errorlevel 1 (
  exit /b 1
)
tar -zcvf supersonic-webapp.tar.gz supersonic-webapp
if errorlevel 1 (
  exit /b 1
)
move supersonic-webapp.tar.gz ..\..\
if errorlevel 1 (
  exit /b 1
)
cd ..
endlocal
