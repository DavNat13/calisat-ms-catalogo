# Changelog - calisat-ms-catalogo

## [1.2.0] - 2026-09-23

### Added
- Paginación con `Page` en la búsqueda por categoría: `GET /api/v1/catalogo/categoria/{categoria}` ahora acepta `page`, `size` y `sort` (tamaño por defecto 20, orden `id`)
- Nuevo endpoint `GET /api/v1/catalogo/inactivos`: lista productos dados de baja (`activo=false`), paginado, protegido con JWT
- Nuevo endpoint `POST /api/v1/catalogo/{sku}/reactivar`: reactiva un producto (`activo=true`); idempotente (200 si ya estaba activo), 404 si el SKU no existe; protegido con JWT
- `SecurityConfig`: `/api/v1/catalogo/inactivos` exige autenticación (antes de la regla pública `GET /api/v1/catalogo/**`)
- Tests unitarios: `ProductoServiceTest` (10 tests) y `ProductoControllerTest` (9 tests) — 19 en total
- Maven wrapper (`.mvn/`, `mvnw`, `mvnw.cmd`) para construir sin Maven instalado
- Workflow de CI (`.github/workflows/ci.yml`): compila y ejecuta tests unitarios con `./mvnw` en push/PR a `main`

### Changed
- `ProductoRepository.findByCategoriaAndActivoTrue` y `ProductoService.listarPorCategoria` ahora devuelven `Page<Producto>` en lugar de `List<Producto>`
- Versión pom.xml incrementada a 1.2.0

## [1.1.0] - 2026-09-07

### Changed
- Versión pom.xml incrementada a 1.1.0
- Confirmación de endpoints CRUD completos: GET, GET/{sku}, GET/categoria/{cat}, POST, PUT/{sku}, DELETE/{sku}
- Documentación Swagger actualizada con todas las operaciones

## [1.0.17] - 2026-09-07

### Fixed
- Corregido host de conexión a BD: `postgres` → `calisat-db-catalogo` (evita UnknownHostException)

## [1.0.16] - 2026-09-07

### Changed
- Mapeo de puertos en docker-compose.yml cambiado de 8083 a 8081 (alineado con API Gateway)

## [1.0.15] - 2026-09-07

### Changed
- Renombrado paquete base de `com.califorge.mscatalogo` a `com.calisat.mscatalogo`
- Actualizados todos los imports en todas las clases Java

## [1.0.14] - 2026-09-05

### Changed
- Hardcodeado todos los valores de configuración para modo académico
- Eliminadas variables de entorno de docker-compose.yml
- Corregido SecurityConfig con `withIssuerLocation` para autodescubrimiento correcto de JWK Set
- Implementado `DelegatingOAuth2TokenValidator` para combinar issuer + audience validators
- Eliminados archivos innecesarios (tests, maven wrapper, init scripts)

## [1.0.13] - 2026-09-05

### Changed
- Eliminado workflow CI/CD (no requerido)

## [1.0.0] - 2026-08-28

### Added
- Microservicio calisat-ms-catalogo con Spring Boot 4.1.0 y Java 21
- Docker Compose con PostgreSQL 15 y app Spring Boot
- Entidad JPA Producto con validaciones Jakarta
- ProductoRepository con JPA
- ProductoService con lógica CRUD completa
- ProductoController con endpoints GET, POST, PUT, DELETE
- SecurityConfig para validación JWT de Azure AD
- CORS configurado para API Gateway
- Health check via Spring Actuator

[1.2.0]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.1.0...v1.2.0
