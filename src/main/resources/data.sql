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

INSERT INTO productos (nombre, descripcion, precio, stock)
SELECT * FROM (SELECT 'Gallinazo a la brasa', 'Especialidad de nuestro chef kingpepa', 67.00, 15) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Gallinazo a la brasa');

-- Fuerza la actualización de la descripción si el gallinazo ya existía en la base de datos de H2
UPDATE productos SET descripcion = 'Especialidad de nuestro chef kingpepa' WHERE nombre = 'Gallinazo a la brasa';

INSERT INTO productos (nombre, descripcion, precio, stock)
SELECT * FROM (SELECT 'Lagarto al sillao', 'Lagarto sideral bien bañado al sillao. Especialidad de la casa.', 45.00, 10) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Lagarto al sillao');

-- Fuerza actualización por si acaso
UPDATE productos SET descripcion = 'Lagarto sideral bien bañado al sillao. Especialidad de la casa.' WHERE nombre = 'Lagarto al sillao';

INSERT INTO clientes (nombre, telefono, email, direccion)
SELECT * FROM (SELECT 'Cliente de Mostrador (Genérico)', '000000000', 'mostrador@local.com', 'Local') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE telefono = '000000000');

-- Renombrar forzosamente si la base de datos ya tenía a "Juan Pérez" guardado
UPDATE clientes SET nombre = 'Cliente de Mostrador (Genérico)', telefono = '000000000', email = 'mostrador@local.com', direccion = 'Local' WHERE telefono = '999888777';

-- =========================================================
-- Cuentas de prueba (avance 3: usuarios y roles).
-- Contraseña de cada una = el texto antes de "@polleria.com" + "123!"
-- en mayúscula inicial, p. ej. admin@polleria.com / Admin123!
-- Los hashes son BCrypt reales (no se guarda ninguna contraseña en texto
-- plano ni siquiera en este archivo de datos de ejemplo).
-- =========================================================

INSERT INTO usuarios (nombre_completo, email, password_hash, rol, email_verificado, activo, fecha_creacion)
SELECT * FROM (SELECT 'Gustavo Fring' AS nombre_completo, 'admin@polleria.com' AS email,
    '$2b$10$evqWYdV6E0oZHUejQ3tA..70EUA/DoJsqzXScq1ttd0WtbNiGtC2u' AS password_hash,
    'ADMINISTRADOR' AS rol, TRUE AS email_verificado, TRUE AS activo, CURRENT_TIMESTAMP AS fecha_creacion) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'admin@polleria.com');

INSERT INTO usuarios (nombre_completo, email, password_hash, rol, email_verificado, activo, fecha_creacion)
SELECT * FROM (SELECT 'Cocinero de Prueba' AS nombre_completo, 'cocina@polleria.com' AS email,
    '$2b$10$mZmlVOP1OEOkZz1fHG4lwedaGmZGFnk54dkZmsiyA.Ov533v8j5By' AS password_hash,
    'COCINERO' AS rol, TRUE AS email_verificado, TRUE AS activo, CURRENT_TIMESTAMP AS fecha_creacion) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'cocina@polleria.com');

INSERT INTO usuarios (nombre_completo, email, password_hash, rol, email_verificado, activo, fecha_creacion)
SELECT * FROM (SELECT 'Repartidor de Prueba' AS nombre_completo, 'reparto@polleria.com' AS email,
    '$2b$10$ylQGt01yCFuLQO9Z/M0SVeyzPgPwJwQmdvc7ps6G/CziA0aIKSFie' AS password_hash,
    'REPARTIDOR' AS rol, TRUE AS email_verificado, TRUE AS activo, CURRENT_TIMESTAMP AS fecha_creacion) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'reparto@polleria.com');

INSERT INTO usuarios (nombre_completo, email, password_hash, rol, email_verificado, activo, fecha_creacion)
SELECT * FROM (SELECT 'Cajera de Prueba' AS nombre_completo, 'caja@polleria.com' AS email,
    '$2b$10$EFz.ke.TjHDlfJHP9FIvdOLYLEOIdIzOtQ4xaXoQrzLVE/cUby/v2' AS password_hash,
    'CAJERA' AS rol, TRUE AS email_verificado, TRUE AS activo, CURRENT_TIMESTAMP AS fecha_creacion) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'caja@polleria.com');

INSERT INTO usuarios (nombre_completo, email, password_hash, rol, email_verificado, activo, fecha_creacion)
SELECT * FROM (SELECT 'Cliente de Prueba' AS nombre_completo, 'cliente@polleria.com' AS email,
    '$2b$10$JmlyryZZR2AAH0VCA9Fum.X6Rx4Sl5GNJ8zXhVZft7/C28cMVvy6K' AS password_hash,
    'CLIENTE' AS rol, TRUE AS email_verificado, TRUE AS activo, CURRENT_TIMESTAMP AS fecha_creacion) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'cliente@polleria.com');

-- Cliente de contacto enlazado a la cuenta cliente@polleria.com (para que ya
-- tenga un clienteId listo y probar POST /api/pedidos sin registrarse de cero).
INSERT INTO clientes (nombre, telefono, email, direccion, usuario_id)
SELECT 'Cliente de Prueba', '999000111', 'cliente@polleria.com', 'Jr. de Prueba 456, Lima',
    (SELECT id FROM usuarios WHERE email = 'cliente@polleria.com')
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE email = 'cliente@polleria.com')
  AND EXISTS (SELECT 1 FROM usuarios WHERE email = 'cliente@polleria.com');
  
UPDATE usuarios SET nombre_completo = 'Gustavo Fring' WHERE email = 'admin@polleria.com'; 
