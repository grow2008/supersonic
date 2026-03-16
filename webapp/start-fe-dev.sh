#!/bin/bash

node_version=$(node -v)
node -e "try { process.binding('http_parser'); } catch (error) { console.error('Current Node.js ' + process.version + ' is incompatible with the Supersonic frontend toolchain because webpack-dev-server depends on the removed internal module http_parser. Please use Node.js 18 or 20 LTS.'); process.exit(1); }"
if [ $? -ne 0 ]; then
    exit 1
fi

major_version=$(echo $node_version | cut -d'.' -f1 | tr -d 'v')

if [ $major_version -ge 17 ]; then
  export NODE_OPTIONS=--openssl-legacy-provider
fi

if ! command -v pnpm >/dev/null 2>&1; then
  npm i -g pnpm
fi

rm -rf ./packages/supersonic-fe/src/.umi ./packages/supersonic-fe/src/.umi-production

cd ./packages/chat-sdk

pnpm i

pnpm run build
if [ $? -ne 0 ]; then
    echo "Failed to build chat sdk."
    exit 1
fi
pnpm link --global

cd ../supersonic-fe

pnpm link ../chat-sdk

pnpm i

pnpm start
