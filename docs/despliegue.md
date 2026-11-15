# Despliegue de SAMAR en la web

Al terminar tendrás SAMAR en `https://tu-nombre.duckdns.org`, con HTTPS, y cada cambio se publica con
un solo comando. Todo corre en una VM con Docker usando `docker-compose.prod.yml`:

```
Internet ──443──► Caddy (frontend + HTTPS) ──► api-gateway ──► cuentas · oferta · reservas · recomendaciones ──► Postgres
```

Solo Caddy se ve desde afuera (puertos 80 y 443). El gateway, los servicios y Postgres solo existen en
la red interna de Docker.

## Una sola vez

### 1. Subir el código a GitHub

La VM descarga el código desde GitHub. Desde tu PC, en la carpeta `samar`:

```bash
git remote add origin https://github.com/USUARIO/samar.git
git push -u origin main
```

Si el repositorio es privado, en la VM necesitarás iniciar sesión (`gh auth login`) o una *deploy key*.

### 2. Crear la VM

- **Sistema:** Ubuntu 24.04.
- **Memoria:** mínimo 2 GB de RAM; con 4 GB va holgado. Los seis contenedores usan alrededor de
  1,6 GB en reposo y compilar necesita algo más (el script de preparación agrega 2 GB de swap).
- **Puertos abiertos en el proveedor:** 22 (SSH), 80 y 443.
- **Opciones gratuitas o con crédito de estudiante** (revisa las condiciones actuales):
  - *Oracle Cloud Always Free*: VM ARM (Ampere) de hasta 4 núcleos y 24 GB, gratis. A veces no hay
    capacidad en la región; reintenta o prueba otra. Todas las imágenes que usa SAMAR funcionan en ARM.
  - *Azure for Students*: crédito sin tarjeta con el correo de la universidad. Una B2s (4 GB) basta.
  - *GitHub Student Developer Pack*: incluye crédito en DigitalOcean; un droplet de 4 GB basta.

Anota la **IP pública** de la VM.

### 3. Dominio gratis con DuckDNS

1. Entra a [duckdns.org](https://www.duckdns.org) con tu cuenta de GitHub o Google.
2. Crea un subdominio (por ejemplo `samar-unimag`) y en *current ip* pon la IP pública de la VM.
3. Comprueba desde tu PC que ya apunta: `nslookup samar-unimag.duckdns.org` debe mostrar esa IP.

Caddy pide el certificado HTTPS a Let's Encrypt la primera vez que arranca. Para eso el dominio ya debe
apuntar a la VM y el puerto 80 debe estar abierto.

### 4. Preparar la VM

```bash
ssh ubuntu@IP_DE_LA_VM          # en Azure el usuario es el que elegiste al crearla
git clone https://github.com/USUARIO/samar.git
cd samar
bash infra/preparar-vm.sh       # instala Docker, crea swap y abre 80/443
exit                            # salir y volver a entrar para usar docker sin sudo
```

### 5. Configurar los secretos

```bash
ssh ubuntu@IP_DE_LA_VM
cd samar
cp .env.example .env
nano .env
```

- `SAMAR_DOMINIO`: tu subdominio, sin `https://` (por ejemplo `samar-unimag.duckdns.org`).
- `JWT_SECRET`, `ADMIN_PASSWORD` y las cinco claves de Postgres: un valor distinto y aleatorio en
  cada uno. Genera cada valor con `openssl rand -base64 36`.
- Guarda `ADMIN_PASSWORD` en un lugar seguro: es la clave de `admin@samar.co`.

`.env` nunca se sube a GitHub (está en `.gitignore`). Las claves de Postgres solo se aplican la primera
vez que se crea la base; cambiarlas después no hace nada.

### 6. Primer despliegue

```bash
bash desplegar.sh --sin-pull                 # la primera vez tarda 10-15 min compilando
SEED_BD=1 bash pruebas/seed-demo.sh       # opcional: cuentas, servicios y reservas de demo
```

Abre `https://TU_DOMINIO` en el navegador.

### 7. Respaldo diario (recomendado)

```bash
crontab -e
# agregar al final:
0 3 * * * bash $HOME/samar/infra/respaldar.sh >> $HOME/respaldos.log 2>&1
```

Guarda una copia de las cuatro bases cada día a las 3 a. m. y conserva la última semana en `~/respaldos`.

## Cada vez que hay un cambio

En tu PC, cuando el cambio funciona en local:

```bash
git add -A
git commit -m "Descripción del cambio"
git push
```

En la VM:

```bash
ssh ubuntu@IP_DE_LA_VM
cd samar
bash desplegar.sh
```

`desplegar.sh` hace `git pull`, reconstruye las imágenes, reinicia solo los contenedores que cambiaron
y espera a que los cinco servicios respondan. Los datos se conservan: si el cambio trae una migración
nueva (`V4__...sql`), Flyway la aplica al arrancar. Por eso **nunca se edita una migración que ya
corrió en producción**; siempre se agrega una nueva.

Mientras un servicio se reinicia, sus páginas pueden fallar unos segundos.

## Comandos útiles en la VM

| Para | Comando |
|---|---|
| Ver el estado de los contenedores | `docker compose -f docker-compose.prod.yml ps` |
| Ver los logs de un servicio | `docker compose -f docker-compose.prod.yml logs -f --tail 100 reservas-service` |
| Reiniciar un servicio | `docker compose -f docker-compose.prod.yml restart oferta-service` |
| Volver a la versión anterior (si la nueva no traía migraciones) | `git checkout HASH_ANTERIOR && bash desplegar.sh --sin-pull`; cuando esté corregido, `git checkout main && bash desplegar.sh` |
| Entrar a una base | `docker exec -it samar-db psql -U postgres -d samar_reservas` |
| Memoria de cada contenedor | `docker stats --no-stream` |
| Apagar todo (conserva los datos) | `docker compose -f docker-compose.prod.yml down` |

`down -v` **borra las bases**. No lo uses en producción salvo que quieras empezar de cero.

## Problemas comunes

- **El navegador dice que el sitio no es seguro o no carga:** el certificado no se emitió. Revisa
  `docker compose -f docker-compose.prod.yml logs frontend`. Casi siempre es que el dominio todavía no
  apunta a la IP o que el puerto 80 está cerrado en el proveedor o en la VM.
- **`desplegar.sh` se queda esperando a un servicio:** mira sus logs. Si dice
  `password authentication failed`, cambiaste las claves de Postgres en `.env` después del primer arranque.
- **La compilación se detiene sin error claro (`Killed`):** falta memoria. Verifica que haya swap con
  `free -h` o usa una VM con más RAM.
- **No puedo entrar como administrador:** el admin se crea solo en el primer arranque, con el
  `ADMIN_PASSWORD` que había en ese momento.
