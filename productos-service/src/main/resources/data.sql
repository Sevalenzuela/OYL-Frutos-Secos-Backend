-- OYL Frutos Secos
-- Datos sinteticos para el microservicio Productos.
-- Cada producto inicial se identifica por su ID.
-- No sobrescribir productos que ya existen.

-- Producto inicial 1
INSERT INTO productos
(nombre, categoria, precio, descripcion, presentacion, imagen_uri, activo)
SELECT
    'Almendras naturales',
    'Frutos secos',
    4500,
    'Almendras naturales de excelente calidad.',
    'Bolsa de 250 g',
    NULL,
    TRUE
    WHERE NOT EXISTS (
    SELECT 1 FROM productos WHERE id = 1
);

-- Producto inicial 2
INSERT INTO productos
(nombre, categoria, precio, descripcion, presentacion, imagen_uri, activo)
SELECT
    'Nueces mariposa',
    'Frutos secos',
    5200,
    'Nueces seleccionadas para consumo directo.',
    'Bolsa de 250 g',
    NULL,
    TRUE
    WHERE NOT EXISTS (
    SELECT 1 FROM productos WHERE id = 2
);

-- Producto inicial 3
INSERT INTO productos
(nombre, categoria, precio, descripcion, presentacion, imagen_uri, activo)
SELECT
    'Semillas de chia',
    'Semillas',
    2900,
    'Semillas de chia para distintas preparaciones.',
    'Bolsa de 200 g',
    NULL,
    TRUE
    WHERE NOT EXISTS (
    SELECT 1 FROM productos WHERE id = 3
);

-- Producto inicial 4
INSERT INTO productos
(nombre, categoria, precio, descripcion, presentacion, imagen_uri, activo)
SELECT
    'Pasas rubias',
    'Deshidratados',
    2500,
    'Pasas rubias ideales para snacks y reposteria.',
    'Bolsa de 250 g',
    NULL,
    TRUE
    WHERE NOT EXISTS (
    SELECT 1 FROM productos WHERE id = 4
);