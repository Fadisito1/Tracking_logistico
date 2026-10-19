-- Datos de ejemplo (perfil dev / H2). Reproducen los 12 envíos de muestra del front (src/data.ts).
-- Los ids se asignan por orden de inserción (las tablas arrancan vacías).

-- Personal logístico (el id 1 es el operador por defecto de la API: operadorId=1)
INSERT INTO personal_logistico (nombre, email_corporativo, rol) VALUES
('María Rodríguez', 'maria.rodriguez@fedex.com', 'ADMINISTRADOR'),
('Javier López', 'javier.lopez@fedex.com', 'OPERADOR');

-- Clientes (remitentes y destinatarios)
INSERT INTO clientes (nombre, telefono, email) VALUES
('Valentina Torres', '+56 9 8812 3344', 'v.torres@empresa.cl'),
('Carlos Mendoza', '+34 612 345 678', 'c.mendoza@gmail.com'),
('Rodrigo Fuentes', '+56 9 7731 0022', 'rfuentes@logistica.cl'),
('Ana Martínez', '+52 55 1234 5678', 'ana.martinez@outlook.com'),
('Sofía Herrera', '+57 300 456 7890', 's.herrera@correo.co'),
('Miguel Ángel Reyes', '+51 987 654 321', 'ma.reyes@peru.pe'),
('Lucía Navarro', '+56 9 6622 8833', 'l.navarro@tienda.cl'),
('Fernanda Castillo', '+55 11 99887 7665', 'fcastillo@brasil.com'),
('Andrés Morales', '+56 9 5500 1122', 'amorales@corp.cl'),
('Elena Vásquez', '+34 699 112 233', 'evasquez@spain.es'),
('Patricia Romero', '+593 98 765 4321', 'promero@ec.com'),
('James Wilson', '+1 646 555 0199', 'j.wilson@ny.com'),
('Diego Muñoz', '+58 412 888 9900', 'd.munoz@vzla.com'),
('Roberto Paz', '+507 6677 8899', 'rpaz@panama.pa'),
('Camila Ríos', '+56 9 4411 5566', 'c.rios@latam.cl'),
('Yuki Tanaka', '+81 90 1234 5678', 'y.tanaka@jp.com'),
('Beatriz Salinas', '+56 9 3322 7788', 'bsalinas@shop.cl'),
('Kofi Asante', '+233 24 456 7890', 'k.asante@ghana.gh'),
('Felipe Contreras', '+56 9 9900 2233', 'f.contreras@biz.cl'),
('Olga Petrov', '+7 916 123 4567', 'o.petrov@ru.com'),
('Isabel Heredia', '+56 9 7788 4455', 'i.heredia@export.cl'),
('Marco Ferretti', '+39 02 1234 5678', 'm.ferretti@it.com'),
('Tomás Aguilar', '+56 9 6644 9900', 't.aguilar@cargo.cl'),
('Priya Sharma', '+91 98765 43210', 'p.sharma@in.com');

-- Direcciones de destino (el front solo captura la de destino)
INSERT INTO direcciones (calle, numero, ciudad) VALUES
('Calle Gran Vía', '42', 'Madrid'),
('Av. Insurgentes', '1001', 'Ciudad de México'),
('Av. Larco', '340', 'Lima'),
('Rua Oscar Freire', '88', 'São Paulo'),
('C/ Velázquez', '12', 'Barcelona'),
('5th Avenue', '350', 'Nueva York'),
('Av. Balboa', '200', 'Panamá'),
('Shinjuku-ku', '1-2-3', 'Tokio'),
('Liberation Road', '45', 'Accra'),
('Tverskaya', '15', 'Moscú'),
('Via Montenapoleone', '7', 'Milán'),
('MG Road', '200', 'Bangalore');

-- Envíos (dirección de origen sin informar)
INSERT INTO envios (tracking_number, prioridad, peso, dimensiones, costo, fecha_registro, fecha_estimada_entrega, estado_operativo, id_remitente, id_destinatario, id_direccion_destino, id_empleado_creador) VALUES
('LOG-38472915-5', 'EXPRES', 14.2, '40x30x20', 53400.00, '2026-09-04 08:30:00', '2026-09-05 20:30:00', 'EN_TRANSITO', 1, 2, 1, 1),
('LOG-38472916-6', 'ESTANDAR', 3.8, '25x20x15', 17600.00, '2026-08-30 10:00:00', '2026-09-03 10:00:00', 'ENTREGADO', 3, 4, 2, 1),
('LOG-92813047-2', 'ESTANDAR', 22, '60x40x35', 54000.00, '2026-09-02 07:15:00', '2026-09-06 07:15:00', 'EN_CENTRO_DISTRIBUCION', 5, 6, 3, 1),
('LOG-10293847-1', 'PROGRAMADO', 8.5, '35x25x25', 32000.00, '2026-09-03 11:30:00', '2026-09-10 00:00:00', 'RECOGIDO', 7, 8, 4, 1),
('LOG-55512349-0', 'EXPRES', 1.2, '20x15x10', 27400.00, '2026-09-06 07:00:00', '2026-09-07 19:00:00', 'PENDIENTE_RECOGIDA', 9, 10, 5, 1),
('LOG-77841230-4', 'EXPRES', 5.6, '30x22x18', 36200.00, '2026-09-01 14:00:00', '2026-09-03 02:00:00', 'ENTREGADO', 11, 12, 6, 1),
('LOG-33928471-7', 'ESTANDAR', 9, '45x35x30', 28000.00, '2026-08-28 09:00:00', '2026-09-01 09:00:00', 'INCIDENCIA', 13, 14, 7, 1),
('LOG-48193027-3', 'ESTANDAR', 31.4, '80x60x50', 72800.00, '2026-09-03 06:00:00', '2026-09-07 06:00:00', 'EN_TRANSITO', 15, 16, 8, 1),
('LOG-61834920-6', 'EXPRES', 7.3, '33x28x22', 39600.00, '2026-09-02 13:00:00', '2026-09-04 01:00:00', 'EN_RUTA_ENTREGA', 17, 18, 9, 1),
('LOG-29047381-8', 'PROGRAMADO', 4.1, '28x20x12', 23200.00, '2026-09-06 09:00:00', '2026-09-20 00:00:00', 'PENDIENTE_RECOGIDA', 19, 20, 10, 1),
('LOG-84730192-9', 'EXPRES', 2.9, '22x18x14', 30800.00, '2026-09-04 15:00:00', '2026-09-06 03:00:00', 'EN_CENTRO_DISTRIBUCION', 21, 22, 11, 1),
('LOG-57301928-0', 'ESTANDAR', 18.7, '55x45x40', 47400.00, '2026-09-05 08:00:00', '2026-09-09 08:00:00', 'RECOGIDO', 23, 24, 12, 1);

-- Historial de movimientos
INSERT INTO movimientos_envio (envio_id, estado, descripcion, ubicacion, fecha_hora) VALUES
(1, 'PENDIENTE_RECOGIDA', 'Envío registrado en sistema', 'Santiago, CL', '2026-09-04 08:30:00'),
(1, 'RECOGIDO', 'Paquete recogido por DHL', 'Santiago, CL', '2026-09-04 14:15:00'),
(1, 'EN_TRANSITO', 'En vuelo SCL→MAD', 'Aeropuerto SCL', '2026-09-05 06:00:00'),
(2, 'PENDIENTE_RECOGIDA', NULL, 'Valparaíso, CL', '2026-08-30 10:00:00'),
(2, 'RECOGIDO', NULL, 'Valparaíso, CL', '2026-08-30 16:00:00'),
(2, 'EN_TRANSITO', NULL, 'Santiago, CL', '2026-09-01 09:00:00'),
(2, 'EN_CENTRO_DISTRIBUCION', NULL, 'Ciudad de México', '2026-09-03 14:30:00'),
(2, 'EN_RUTA_ENTREGA', NULL, 'Col. Condesa, CDMX', '2026-09-04 08:00:00'),
(2, 'ENTREGADO', NULL, 'Av. Insurgentes 1001', '2026-09-04 11:45:00'),
(3, 'PENDIENTE_RECOGIDA', NULL, 'Bogotá, CO', '2026-09-02 07:15:00'),
(3, 'RECOGIDO', NULL, 'Bogotá, CO', '2026-09-02 13:00:00'),
(3, 'EN_TRANSITO', NULL, 'Vuelo BOG→LIM', '2026-09-03 20:00:00'),
(3, 'EN_CENTRO_DISTRIBUCION', 'Revisión en aduana', 'Lima, PE', '2026-09-04 05:30:00'),
(4, 'PENDIENTE_RECOGIDA', NULL, 'Santiago, CL', '2026-09-03 11:30:00'),
(4, 'RECOGIDO', NULL, 'Santiago, CL', '2026-09-05 09:00:00'),
(5, 'PENDIENTE_RECOGIDA', NULL, 'Montevideo, UY', '2026-09-06 07:00:00'),
(6, 'PENDIENTE_RECOGIDA', NULL, 'Quito, EC', '2026-09-01 14:00:00'),
(6, 'RECOGIDO', NULL, 'Quito, EC', '2026-09-01 18:30:00'),
(6, 'EN_TRANSITO', NULL, 'Vuelo UIO→JFK', '2026-09-02 12:00:00'),
(6, 'EN_CENTRO_DISTRIBUCION', NULL, 'JFK, Nueva York', '2026-09-03 09:00:00'),
(6, 'EN_RUTA_ENTREGA', NULL, 'Manhattan, NY', '2026-09-03 14:00:00'),
(6, 'ENTREGADO', NULL, '5th Ave 350', '2026-09-03 16:20:00'),
(7, 'PENDIENTE_RECOGIDA', NULL, 'Caracas, VE', '2026-08-28 09:00:00'),
(7, 'RECOGIDO', NULL, 'Caracas, VE', '2026-08-28 15:00:00'),
(7, 'EN_TRANSITO', NULL, 'Vuelo CCS→PTY', '2026-08-29 10:00:00'),
(7, 'INCIDENCIA', 'Documentación incompleta. Paquete retenido.', 'Aeropuerto PTY', '2026-08-30 08:00:00'),
(8, 'PENDIENTE_RECOGIDA', NULL, 'Lima, PE', '2026-09-03 06:00:00'),
(8, 'RECOGIDO', NULL, 'Lima, PE', '2026-09-03 12:00:00'),
(8, 'EN_TRANSITO', NULL, 'Vuelo LIM→NRT', '2026-09-04 22:00:00'),
(9, 'PENDIENTE_RECOGIDA', NULL, 'Santiago, CL', '2026-09-02 13:00:00'),
(9, 'RECOGIDO', NULL, 'Santiago, CL', '2026-09-02 17:00:00'),
(9, 'EN_TRANSITO', NULL, 'Vuelo SCL→ACC', '2026-09-03 21:00:00'),
(9, 'EN_CENTRO_DISTRIBUCION', NULL, 'Accra, GH', '2026-09-05 06:00:00'),
(9, 'EN_RUTA_ENTREGA', NULL, 'Accra, GH', '2026-09-06 08:30:00'),
(10, 'PENDIENTE_RECOGIDA', NULL, 'Concepción, CL', '2026-09-06 09:00:00'),
(11, 'PENDIENTE_RECOGIDA', NULL, 'Santiago, CL', '2026-09-04 15:00:00'),
(11, 'RECOGIDO', NULL, 'Santiago, CL', '2026-09-04 19:00:00'),
(11, 'EN_TRANSITO', NULL, 'Vuelo SCL→MXP', '2026-09-05 23:00:00'),
(11, 'EN_CENTRO_DISTRIBUCION', NULL, 'Milán, IT', '2026-09-06 16:00:00'),
(12, 'PENDIENTE_RECOGIDA', NULL, 'La Serena, CL', '2026-09-05 08:00:00'),
(12, 'RECOGIDO', NULL, 'La Serena, CL', '2026-09-05 14:30:00');

-- Auditoría de ediciones
INSERT INTO registros_auditoria (envio_id, usuario_modificador, campos_modificados, fecha_hora) VALUES
(1, 'María Rodríguez', 'peso; dimensiones', '2026-09-04 09:00:00'),
(3, 'Javier López', 'dirección destinatario', '2026-09-02 08:00:00');
