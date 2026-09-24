# Changelog - calisat-ms-catalogo

## [1.3.0] - 2026-09-24

### Added
- RBAC con Azure Entra ID: bean `JwtAuthenticationConverter` que extrae el claim `roles` del JWT con prefijo `ROLE_` (normalizado a mayúsculas)
- Escrituras del catálogo (`POST`, `PUT`, `DELETE` incluido `/{sku}/reactivar`) y `GET /inactivos` restringidas a `ADMINISTRADOR`
- Lectura pública de productos activos (`GET /api/v1/catalogo`, `/{sku}`, `/categoria/{categoria}`) mantiene `permitAll`
- Versión pom.xml actualizada a 1.3.0

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

## [1.0.12] - 2026-09-06

- Fix permiso de ejecución del wrapper mvnw en CI (asunto del commit `0b6beba`).

## [1.0.11] - 2026-09-06

- Prohibir cambio de SKU en actualizar (identificador canónico inmutable) (asunto del commit `110eecd`).

## [1.0.10] - 2026-09-06

- Documentar API con springdoc-openapi y validar `imagenUrl` (asunto del commit `cf1803f`).

## [1.0.9] - 2026-09-06

- Test de integración con PostgreSQL real (Testcontainers) (asunto del commit `af62b7e`).

## [1.0.8] - 2026-09-06

- CI/CD: workflow GitHub Actions con `mvn verify` y build de imagen (asunto del commit `dbd9358`).

## [1.0.7] - 2026-09-06

- Infra sin .env: defaults inline, `init/01_create_producto.sql` y `JPA_DDL_AUTO=validate` (asunto del commit `ca7c41e`).

## [1.0.6] - 2026-09-06

- Endurecer config JWT (fail-fast) y limpiar rutas públicas muertas (asunto del commit `617d80e`).

## [1.0.5] - 2026-09-06

- Unificar contrato por SKU: `PUT/DELETE /catalogo/{sku}` (asunto del commit `3ddb82a`).

## [1.0.4] - 2026-09-06

- Corregir errores HTTP: 400 para input inválido, 404 para rutas inexistentes y logging en `GlobalExceptionHandler` (asunto del commit `387aadd`).

## [1.0.3] - 2026-09-05

- Fases 2-4 del plan: `ProductoService`/`Repository`, `ProductoController` (6 rutas `/api/v1/catalogo`), seguridad JWT con lectura pública y suite de 30 tests (asunto del commit `7f1d3e6`).

## [1.0.2] - 2026-09-04

- Alinear basefiles con calisat-ms-inventario y eliminar Flyway (asunto del commit `504a366`).

## [1.0.1] - 2026-09-04

- Fase 1: capa de datos Producto (entidad JPA + migración Flyway V1) (asunto del commit `045c83b`).

## [1.0.0] - 2026-08-28

### Added
- Microservicio calisat-ms-catalogo con Spring Boot 4.1.0 y Java 21
- Docker Compose con PostgreSQL 15 y app Spring Boot
- Entidad JPA Producto con validaciones Jakarta
- ProductoRepository con JPA
- ProductoService con lógica CRUD completa
- ProductoController con endpoints GET, POST, PUT, DELETE
- SecurityConfig para validacion JWT de Azure AD
- CORS configurado para API Gateway
- Health check via Spring Actuator

[1.3.0]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.17...v1.1.0
[1.0.17]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.16...v1.0.17
[1.0.16]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.15...v1.0.16
[1.0.15]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.14...v1.0.15
[1.0.14]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.13...v1.0.14
[1.0.13]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.12...v1.0.13
[1.0.12]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.11...v1.0.12
[1.0.11]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.10...v1.0.11
[1.0.10]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.9...v1.0.10
[1.0.9]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.8...v1.0.9
[1.0.8]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.7...v1.0.8
[1.0.7]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.6...v1.0.7
[1.0.6]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.5...v1.0.6
[1.0.5]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/DavNat13/calisat-ms-catalogo/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/DavNat13/calisat-ms-catalogo/releases/tag/v1.0.0
