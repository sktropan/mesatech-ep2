# Despliegue EC2 — MesaTech (Nico)

Topología EV1: **3 instancias** (DB + MS + BFF). Ver [`TOPOLOGIA.md`](TOPOLOGIA.md) e IPs en [`docs/arquitectura/diagrama.md`](../../docs/arquitectura/diagrama.md).

## Checklist rápido

| # | Tarea | Evidencia informe |
| --- | --- | --- |
| 1 | Instancia Ubuntu + IP pública | NIC-01, NIC-02 |
| 2 | Java 21 + Docker | `java -version`, `docker ps` |
| 3 | MySQL 8.4 con compose EC2 | NIC-03, NIC-04 |
| 4 | Tres JARs corriendo | NIC-05 |
| 5 | SG: 8081/8082 cerrados a Internet | NIC-08, NIC-09 |
| 6 | BFF sin JWT → 401 | NIC-07 |
| 7 | Entregar URL BFF a Ninna | `http://<IP>:8080` |

## 1. Variables en la EC2

Copiar plantilla (no commitear valores reales):

```bash
cp infra/ec2/env.ec2.example ~/mesatech.env
nano ~/mesatech.env
# Rellenar ~/mesatech.env según infra/ec2/env.ec2.example
source ~/mesatech.env
```

Pedir a **Ari**: `ENTRA_ISSUER_URI`, `ENTRA_AUDIENCE`.

## 2. MySQL 8.4 y RabbitMQ

RabbitMQ queda arriba con el mismo Compose (vhost `mesatech`). `ms-catalogo` publica y `ms-solicitudes` consume: cada unit de systemd lleva su `RABBITMQ_USER`. Pruebas: [`docs/pruebas-rabbitmq.md`](../../docs/pruebas-rabbitmq.md).

```bash
cd ~/duoc-mesatech/infra
cp .env.example .env
# Editar .env si la clave de producción no es la de ejemplo
docker compose -f docker-compose.yml -f docker-compose.ec2.yml up -d
docker compose ps
```

El override deja MySQL (`3306`) y RabbitMQ (`5672`, `15672`) solo en `127.0.0.1`.

## 3. Compilar JARs (en tu PC o en EC2)

Windows (PowerShell, raíz del repo):

```powershell
.\infra\scripts\build-jars.ps1
```

Git Bash / Linux:

```bash
./infra/scripts/build-jars.sh
```

Subir a EC2:

```bash
scp -i mesatech.pem infra/out/*.jar ubuntu@<IP>:~/apps/
```

## 4. Arrancar servicios (Ubuntu)

Con variables exportadas (`source ~/mesatech.env`):

```bash
java -jar ~/apps/bff-*.jar &
java -jar ~/apps/ms-solicitudes-*.jar &
java -jar ~/apps/ms-catalogo-*.jar &
ss -tlnp | grep 808
```

Producción: usar unidades systemd en [`../scripts/systemd/`](../scripts/systemd/) (opcional).

## 5. Security groups (resumen)

Ver [`SECURITY-GROUPS.md`](SECURITY-GROUPS.md).

## 6. CORS en BFF

En EC2 exportar orígenes del React (y localhost para pruebas):

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:3000,https://<invoke-id>.execute-api.<region>.amazonaws.com
```

(Ninna confirma URL del front; Gateway suele ser el origen del browser si el front llama al Invoke URL.)

## 7. Pruebas

```bash
curl -i http://127.0.0.1:8080/v1/solicitudes/mias          # 401
curl -i http://<IP_PUBLICA>:8081/v1/solicitudes/mias       # timeout o rechazado
```

Con JWT válido (Skarlet/Ari): `curl -H "Authorization: Bearer <token>" http://127.0.0.1:8080/api/usuario`
