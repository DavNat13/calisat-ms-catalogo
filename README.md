# calisat-ms-catalogo

> Microservicio Spring Boot que expone el catálogo de productos de Calisat: consulta pública paginada y administración (CRUD) protegida con JWT.

![Versión](https://img.shields.io/badge/version-1.2.0-2563EB)
![Java](https://img.shields.io/badge/Java-21-F89820?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)
![Estado](https://img.shields.io/badge/estado-modo%20acad%C3%A9mico-FACC15)

**Versión actual: `1.2.0`** (definida en `pom.xml` · historial en [`CHANGELOG.md`](CHANGELOG.md))

---

## 📑 Índice

- [📋 Descripción general](#-descripción-general)
- [✨ Características principales](#-características-principales)
- [🏗️ Arquitectura](#-arquitectura)
- [🚀 Requisitos](#-requisitos)
- [⚙️ Configuración](#-configuración)
- [▶️ Ejecución local](#-ejecución-local)
- [📡 Endpoints principales](#-endpoints-principales)
- [🗃️ Modelo de datos](#-modelo-de-datos)
- [🔒 Seguridad](#-seguridad)
- [🧪 Tests](#-tests)
- [📦 Despliegue](#-despliegue)
- [🔗 Microservicios relacionados](#-microservicios-relacionados)
- [📄 Licencia y modo académico](#-licencia-y-modo-académico)

---

## 📋 Descripción general

**calisat-ms-catalogo** es el microservicio encargado del **catálogo de productos** de la plataforma Calisat (equipamiento de calistenia). Expone una API REST versionada bajo `/api/v1/catalogo` con identificador canónico **SKU**:

- **Lectura pública**: el listado paginado de productos activos, el detalle por SKU y el filtro por categoría están disponibles sin autenticación (para el frontend anónimo).
- **Escritura protegida**: crear, actualizar, dar de baja (borrado lógico) y reactivar productos exigen un **JWT de Microsoft Entra ID**.
- **Sin RBAC**: cualquier usuario autenticado puede administrar el catálogo (*modo académico*).

Incluye **OpenAPI 3 + Swagger UI** (springdoc 3.1.0) y **Actuator** para health checks.

## ✨ Características principales

- 📖 **Consulta pública paginada** de productos activos (`Page`, tamaño 20 por defecto, orden `id`).
- 🔎 **Búsqueda por SKU** como identificador canónico del recurso (p. ej. `ANILLAS-001`).
- 🏷️ **Filtro por categoría** con paginación.
- ✍️ **CRUD completo** con validación (`@Valid`) y códigos HTTP precisos (`201` + `Location`, `400`, `404`).
- 🧾 **Baja lógica y reactivación**: `DELETE` marca `activo=false`; `POST /{sku}/reactivar` lo restaura (idempotente).
- 📚 **Inactivos listables**: `GET /inactivos` devuelve productos dados de baja para su posterior reactivación (requiere JWT).
- 📕 **OpenAPI 3 + Swagger UI** anotados con `@Operation` y `@Tag`.
- 🩺 **Actuator**: `GET /actuator/health` e `info` públicos.
- 🧪 **Testcontainers**: integraciones con PostgreSQL real vía Failsafe (`*IT.java`).
- 🐳 **Docker multi-stage** con usuario no root y *health check*.

## 🏗️ Arquitectura

```mermaid
flowchart LR
    F[calisat-frontend] -->|GET público / sin token| API[calisat-ms-catalogo<br/>:8082]
    C[calisat-ms-carrito] -->|GET /{sku} · JWT| API
    F -->|POST/PUT/DELETE · JWT| API
    API --> PG[(PostgreSQL<br/>calisat_catalogo)]
    API --> SW[Swagger UI / OpenAPI]
    API --> AC[/actuator/health]
```

### Estructura de paquetes

```
com.calisat.mscatalogo
├── config/        # SecurityConfig, AudienceValidator, CORS
├── controller/    # ProductoController
├── dto/           # ProductoRequest, ProductoResponse
├── exception/     # GlobalExceptionHandler, SkuDuplicadoException, ...
├── model/         # Producto (JPA)
├── repository/    # ProductoRepository (Spring Data)
└── service/       # ProductoService (@Transactional)
```

## 🚀 Requisitos

| Requisito | Versión mínima |
|-----------|----------------|
| JDK | **21+** (validado por `maven-enforcer-plugin`) |
| Maven | 3.6.3+ (o usa el wrapper `./mvnw`) |
| Docker + Docker Compose | 24+ (para BD y despliegue) |
| Node.js | 20+ (solo si usas el frontend hermano) |

## ⚙️ Configuración

Valores de `src/main/resources/application.yaml` y `docker-compose.yml`:

| Parámetro | Valor |
|-----------|-------|
| **Puerto del servicio** | **`8082`** (publicado por Docker Compose: `8082:8080`; la app en contenedor escucha en `8080`) |
| Base de datos | PostgreSQL · `calisat_catalogo` |
| Host de BD (Compose) | `calisat-db-catalogo:5432` |
| Usuario / contraseña BD | `postgres` / `postgres` *(solo académico)* |
| `ddl-auto` | `update` (esquema gestionado por Hibernate) |
| JWT *issuer* | `https://login.microsoftonline.com/e5372bf0-c5e3-4286-887c-79069f209c1f/v2.0` |
| JWT *audience* | `d221f0d2-1a7c-4872-ad6c-367a1f0717ec` |
| Origen CORS permitido | `https://ezeh839whh.execute-api.us-east-1.amazonaws.com` |
| Actuator | `health`, `info` |

> ⚠️ **Modo académico**: issuer, audience, credenciales de BD y CORS están **hardcodeados** a propósito. En producción deben externalizarse (variables de entorno / secret manager).

> 💡 **Puerto en local**: `application.yaml` declara `8080`; el ecosistema Calisat consume este servicio en **`8082`** (`CALISAT_CATALOGO_URL=http://localhost:8082` en ms-carrito). Si lo ejecutas fuera de Docker, arráncalo con `SERVER_PORT=8082` o ajusta esa variable en los consumidores.

## ▶️ Ejecución local

### 1. Base de datos

```bash
docker compose up -d postgres-db
```

Levanta PostgreSQL 15 (`calisat-db-catalogo`, BD `calisat_catalogo`) con health check y volumen persistente.

### 2. Aplicación (Maven Wrapper)

```bash
# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8082

# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

### 3. Todo con Docker Compose

```bash
docker compose up --build
```

Servicio disponible en `http://localhost:8082` (Swagger: `http://localhost:8082/swagger-ui.html`).

## 📡 Endpoints principales

Base: `http://localhost:8082/api/v1/catalogo`

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/catalogo` | Listar productos **activos** paginados | Pública |
| `GET` | `/api/v1/catalogo/{sku}` | Detalle de producto por SKU (404 si no existe/inactivo) | Pública |
| `GET` | `/api/v1/catalogo/categoria/{categoria}` | Productos activos por categoría (paginado) | Pública |
| `GET` | `/api/v1/catalogo/inactivos` | Productos dados de baja (`activo=false`) | JWT |
| `POST` | `/api/v1/catalogo` | Crear producto (SKU único; `201` + `Location`) | JWT |
| `POST` | `/api/v1/catalogo/{sku}/reactivar` | Reactivar producto (idempotente) | JWT |
| `PUT` | `/api/v1/catalogo/{sku}` | Actualizar producto (SKU inmutable) | JWT |
| `DELETE` | `/api/v1/catalogo/{sku}` | Baja lógica (`activo=false`, idempotente) | JWT |

**Total: 8 endpoints** · *Swagger UI*: `/swagger-ui.html` · *OpenAPI JSON*: `/v3/api-docs`

### Ejemplo

```bash
# Listado público (sin token)
curl http://localhost:8082/api/v1/catalogo?page=0&size=20

# Crear producto (con JWT)
curl -X POST http://localhost:8082/api/v1/catalogo \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sku":"BARRAS-001","nombre":"Barra de dominadas","precio":89.90,"categoria":"Barras"}'
```

## 🗃️ Modelo de datos

### Entidad `Producto` (tabla `producto`)

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | `Long` | PK, `IDENTITY` |
| `sku` | `String(64)` | **Único**, `NOT NULL` — identificador canónico |
| `nombre` | `String(120)` | `NOT NULL` |
| `descripcion` | `String(1000)` | Opcional |
| `precio` | `BigDecimal(10,2)` | `NOT NULL`, `> 0` |
| `categoria` | `String(64)` | `NOT NULL` |
| `imagen_url` | `String(500)` | Opcional |
| `activo` | `boolean` | Default `true` (baja lógica) |
| `fecha_creacion` | `LocalDateTime` | `@PrePersist` = ahora |

## 🔒 Seguridad

- **JWT (OAuth2 Resource Server)** de **Microsoft Entra ID**: validación de *issuer* + *audience* con `AudienceValidator` y `DelegatingOAuth2TokenValidator`.
- **Sin RBAC / roles**: un único nivel autenticado; el *scope* por usuario no aplica (catálogo compartido).
- **Usuario genérico**: cualquier cuenta válida del tenant académico puede escribir.
- **Rutas públicas**: `GET /api/v1/catalogo/**` (excepto `/inactivos`), `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.
- **CORS**: restringido al origen del despliegue (API Gateway); CSRF deshabilitado (API stateless con Bearer).
- **Headers**: solo `Authorization` y `Content-Type` permitidos.

## 🧪 Tests

```bash
# Tests unitarios (Surefire)
./mvnw test

# Tests de integración con Testcontainers (Failsafe)
./mvnw verify
```

| Suite | Archivos | Tests |
|-------|----------|-------|
| Unitarios | `ProductoServiceTest`, `ProductoControllerTest` | **19** |
| Integración (`*IT`) | configurados vía Failsafe + Testcontainers | — |

## 📦 Despliegue

### Docker

```bash
docker build -t calisat-ms-catalogo:1.2.0 .
docker run -p 8082:8080 --name calisat-ms-catalogo calisat-ms-catalogo:1.2.0
```

**Dockerfile multi-stage:**

1. `maven:3.9.6-eclipse-temurin-21-alpine` → `mvn clean package`.
2. `eclipse-temurin:21-jre-alpine` → JAR ejecutado con usuario `spring` (no root), `MaxRAMPercentage=75`, `HEALTHCHECK` contra `/actuator/health`.

### Docker Compose (recomendado)

```bash
docker compose up --build
```

Levanta PostgreSQL 15 + la app en la red `calisat-net`, con el puerto **8082** publicado en el host.

## 🔗 Microservicios relacionados

| Repositorio | Relación |
|-------------|----------|
| [calisat-frontend](https://github.com/DavNat13/calisat-frontend) | Consumidor principal (lectura pública + CRUD con JWT) |
| [calisat-ms-carrito](https://github.com/DavNat13/calisat-ms-carrito) | Valida SKU/precio vía `CatalogoClient` (`CALISAT_CATALOGO_URL`, defecto `:8082`) |
| [calisat-ms-usuarios](https://github.com/DavNat13/calisat-ms-usuarios) | Perfil y direcciones (puerto 8081) |
| [calisat-ms-inventario](https://github.com/DavNat13/calisat-ms-inventario) | Stock por SKU (puerto 8083) |
| [calisat-ms-orden](https://github.com/DavNat13/calisat-ms-orden) | Órdenes de compra (puerto 8085) |
| [calisat-ms-envios](https://github.com/DavNat13/calisat-ms-envios) | Envíos y seguimiento (puerto 8086) |
| [calisat-ms-notificaciones](https://github.com/DavNat13/calisat-ms-notificaciones) | Notificaciones (puerto 8087) |

## 📄 Licencia y modo académico

Proyecto desarrollado en **modo académico**; sin licencia open source formal. Credenciales, *issuer* y *audience* de Entra ID están *hardcodeados* con fines educativos.

- **Versión actual**: `1.2.0`
- **Historial de cambios**: [`CHANGELOG.md`](CHANGELOG.md)
- **Plan de trabajo**: consulta el `CHANGELOG.md` para el detalle por fases (paginación, inactivos, reactivación, Testcontainers).
