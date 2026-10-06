CREATE TABLE clientes (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    tipo_cliente VARCHAR(20),
    nit VARCHAR(30)
);
CREATE TABLE productos (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    precio DOUBLE NOT NULL
);
CREATE TABLE inventario (
    producto_id BIGINT PRIMARY KEY,
    stock INT NOT NULL
);
CREATE TABLE facturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    monto DOUBLE NOT NULL,
    pagada BOOLEAN NOT NULL
);
CREATE TABLE pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    subtotal DOUBLE,
    descuento DOUBLE,
    impuesto DOUBLE,
    total DOUBLE,
    fecha TIMESTAMP,
    estado VARCHAR(20)
);
CREATE TABLE detalle_pedido (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL
);
