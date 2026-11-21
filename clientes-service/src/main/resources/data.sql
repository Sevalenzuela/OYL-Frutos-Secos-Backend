-- OYL Frutos Secos
-- Cliente sintetico para demostracion.
-- Cuenta inactiva con un hash de ejemplo.
-- No se utiliza para iniciar sesion.

INSERT INTO clientes (
    nombre,
    nombre_usuario,
    correo,
    telefono,
    direccion,
    password_hash,
    rol,
    activo
)
SELECT
    'Cliente de demostracion',
    'cliente_demo',
    'cliente@example.com',
    '900000000',
    'Direccion de demostracion',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'CLIENTE',
    FALSE
    WHERE NOT EXISTS (
    SELECT 1 FROM clientes
);