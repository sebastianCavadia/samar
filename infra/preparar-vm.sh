#!/usr/bin/env bash
# Prepara una VM Ubuntu recién creada para correr SAMAR. Se corre una sola vez:
#   bash infra/preparar-vm.sh
# Instala Docker, crea 2 GB de swap (para compilar sin quedarse sin memoria) y abre 80/443.
set -euo pipefail

if ! command -v docker >/dev/null; then
  echo "== Instalando Docker (script oficial de get.docker.com)"
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
  echo "   Docker instalado. Cierra la sesión SSH y vuelve a entrar para usarlo sin sudo."
else
  echo "== Docker ya está instalado: $(docker --version)"
fi

if [ "$(swapon --show | wc -l)" = 0 ]; then
  echo "== Creando 2 GB de swap"
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile >/dev/null
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
else
  echo "== Ya hay swap"
fi

echo "== Abriendo SSH, 80 y 443 en el firewall de la VM"
if command -v ufw >/dev/null; then
  sudo ufw allow OpenSSH >/dev/null
  sudo ufw allow 80,443/tcp >/dev/null
  sudo ufw --force enable >/dev/null
fi
# Las imágenes de Ubuntu en Oracle Cloud traen reglas de iptables propias que bloquean 80/443.
if sudo iptables -S INPUT 2>/dev/null | grep -q 'REJECT'; then
  sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
  sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
  sudo sh -c 'command -v netfilter-persistent >/dev/null && netfilter-persistent save' || true
fi

echo
echo "Listo. Memoria: $(free -h | awk '/Mem:/{print $2}') de RAM, $(free -h | awk '/Swap:/{print $2}') de swap."
echo "Siguiente paso: git clone del repositorio, cp .env.example .env y bash desplegar.sh"
