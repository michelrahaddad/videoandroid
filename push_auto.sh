#!/bin/bash

echo "📦 Iniciando automação Git..."

cd ~/Desktop/subir || exit

git add .
git commit -m "${1:-Atualização automática via script}"
git pull origin main --rebase
git push origin main

echo "✅ Código sincronizado com o GitHub!"
chmod +x ~/Desktop/subir/push_auto.sh
