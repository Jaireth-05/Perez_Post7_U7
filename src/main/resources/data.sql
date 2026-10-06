INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES
    (1, 'Valentina VIP',        'VIP',       NULL),
    (2, 'Felipe Frecuente',     'FRECUENTE', NULL),
    (3, 'Mario Moroso',         'MOROSO',    NULL),
    (4, 'Elena Estandar',       'ESTANDAR',  NULL),
    (5, 'Comercial Andes SAS',  'ESTANDAR',  '900123456-1'),
    (6, 'Fabiola Fiel',         'FRECUENTE', NULL),
    (7, 'Cliente sin tipo',     NULL,        NULL);

INSERT INTO productos (id, nombre, precio) VALUES
    (1, 'Teclado mecanico', 100000),
    (2, 'Monitor 27 pulgadas', 250000),
    (3, 'Mouse inalambrico', 50000);

INSERT INTO inventario (producto_id, stock) VALUES (1, 1000), (2, 500), (3, 5);

INSERT INTO facturas (cliente_id, monto, pagada) VALUES (3, 300000, FALSE), (3, 50000, TRUE);

-- Historial: cliente 2 con 4 pedidos previos (>3), cliente 6 con 11 (>10)
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado)
    SELECT 2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO' FROM SYSTEM_RANGE(1, 4);
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado)
    SELECT 6, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO' FROM SYSTEM_RANGE(1, 11);
