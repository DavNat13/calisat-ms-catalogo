CREATE TABLE producto (
    id             BIGSERIAL PRIMARY KEY,
    sku            VARCHAR(64)  NOT NULL UNIQUE,
    nombre         VARCHAR(120) NOT NULL,
    descripcion    VARCHAR(1000),
    precio         NUMERIC(10, 2) NOT NULL,
    categoria      VARCHAR(64)  NOT NULL,
    imagen_url     VARCHAR(500),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);