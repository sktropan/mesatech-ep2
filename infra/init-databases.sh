#!/bin/bash
# Se ejecuta una sola vez, cuando el volumen de MySQL está vacío.
# La imagen ya crea MYSQL_DATABASE (catalogo) y el usuario MYSQL_USER.
# Aquí se asegura la segunda base y el permiso sobre las dos.
set -euo pipefail

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS catalogo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS solicitudes CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON catalogo.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON solicitudes.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL
