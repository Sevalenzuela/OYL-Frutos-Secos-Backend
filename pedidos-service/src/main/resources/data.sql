-- OYL Frutos Secos
-- Pedido historico ficticio para demostracion.
-- Se encuentra cancelado y no afecta el inventario.

INSERT INTO pedidos (
    numero,
    fecha,
    cliente_id,
    nombre_cliente,
    telefono,
    modalidad,
    direccion,
    total,
    estado
)
SELECT
    'OYL-DEMO-001',
    1767225600000,
    1,
    'Cliente de demostracion',
    '900000000',
    'RETIRO',
    '',
    14200,
    'CANCELADO'
    WHERE NOT EXISTS (
    SELECT 1
    FROM pedidos
    WHERE numero = 'OYL-DEMO-001'
);

-- Detalle 1: Almendras naturales
INSERT INTO detalles_pedido (
    pedido_id,
    producto_id,
    nombre,
    cantidad,
    precio_unitario
)
SELECT
    p.id,
    1,
    'Almendras naturales',
    2,
    4500
FROM pedidos p
WHERE p.numero = 'OYL-DEMO-001'
  AND NOT EXISTS (
    SELECT 1
    FROM detalles_pedido d
    WHERE d.pedido_id = p.id
      AND d.producto_id = 1
);

-- Detalle 2: Nueces mariposa
INSERT INTO detalles_pedido (
    pedido_id,
    producto_id,
    nombre,
    cantidad,
    precio_unitario
)
SELECT
    p.id,
    2,
    'Nueces mariposa',
    1,
    5200
FROM pedidos p
WHERE p.numero = 'OYL-DEMO-001'
  AND NOT EXISTS (
    SELECT 1
    FROM detalles_pedido d
    WHERE d.pedido_id = p.id
      AND d.producto_id = 2
);