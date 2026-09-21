-- Datos de ejemplo para probar el flujo de pedidos rápidamente.
-- Se cargan solo si las tablas están vacías (evita duplicar en cada reinicio
-- gracias a la condición WHERE NOT EXISTS).

INSERT INTO productos (nombre, descripcion, precio, stock)
SELECT * FROM (SELECT 'Pollo a la brasa entero' AS nombre, 'Con papas y ensalada' AS descripcion, 65.00 AS precio, 20 AS stock) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pollo a la brasa entero');

INSERT INTO productos (nombre, descripcion, precio, stock)
SELECT * FROM (SELECT '1/4 de pollo', 'Con papas y ensalada', 18.00, 40) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = '1/4 de pollo');

INSERT INTO productos (nombre, descripcion, precio, stock)
SELECT * FROM (SELECT 'Gaseosa 1.5L', 'Inca Kola / Coca Cola', 9.00, 50) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Gaseosa 1.5L');

INSERT INTO productos (nombre, descripcion, precio, stock)
SELECT * FROM (SELECT 'Combo familiar', 'Pollo entero + 2 gaseosas + papas extra', 89.00, 0) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Combo familiar');

INSERT INTO clientes (nombre, telefono, email, direccion)
SELECT * FROM (SELECT 'Juan Pérez', '999888777', 'juan.perez@example.com', 'Av. Los Álamos 123, Lima') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE telefono = '999888777');
