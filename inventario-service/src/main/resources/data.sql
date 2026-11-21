-- OYL Frutos Secos
-- Inventario inicial de demostracion.
-- Los ID corresponden al catalogo inicial
-- del microservicio Productos.

-- Almendras naturales
INSERT INTO inventario
(producto_id, stock_disponible, stock_reservado)
SELECT 1, 25, 0
    WHERE NOT EXISTS (
    SELECT 1 FROM inventario WHERE producto_id = 1
);

-- Nueces mariposa
INSERT INTO inventario
(producto_id, stock_disponible, stock_reservado)
SELECT 2, 18, 0
    WHERE NOT EXISTS (
    SELECT 1 FROM inventario WHERE producto_id = 2
);

-- Semillas de chia
INSERT INTO inventario
(producto_id, stock_disponible, stock_reservado)
SELECT 3, 40, 0
    WHERE NOT EXISTS (
    SELECT 1 FROM inventario WHERE producto_id = 3
);

-- Pasas rubias
INSERT INTO inventario
(producto_id, stock_disponible, stock_reservado)
SELECT 4, 12, 0
    WHERE NOT EXISTS (
    SELECT 1 FROM inventario WHERE producto_id = 4
);
