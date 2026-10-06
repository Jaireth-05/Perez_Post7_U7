# Post-contenido — Unidad 6: Antipatrones de Diseño

Proyecto Spring Boot `pedidos-service` (Java 17, Spring JDBC, H2).

## Diagnóstico de la Parte 1 — `GestorPedidos` (antes de refactorizar)

Archivo analizado: `GestorPedidos.java` (237 líneas en este repositorio; `procesarPedido()` ocupa las líneas 30–135, es decir, 106 líneas en un solo método).

### Antipatrón: God Object + Spaghetti Code

**Evidencia de God Object (la clase hace demasiado).** `procesarPedido()` mezcla 6 responsabilidades, cada una con una razón de cambio distinta:

| # | Responsabilidad | Líneas | Razón por la que cambiaría |
|---|---|---|---|
| 1 | Validación de stock | 33–46 | Cambian las políticas de inventario |
| 2 | Validación de cliente y mora | 48–67 | Cambian las reglas de crédito o el horario de corte |
| 3 | Cálculo de subtotal | 69–75 | Cambia la fuente de precios |
| 4 | Cálculo de descuento e impuesto | 77–98 | Cambian las reglas comerciales o la tasa de IVA |
| 5 | Persistencia JDBC (pedido, detalle, inventario) | 100–115 | Cambia el esquema o el motor de BD |
| 6 | Notificación (armado del texto + envío) | 117–131 | Cambia el formato o el canal de aviso |

La clase además inyecta `JdbcTemplate` y `EmailService` directamente (líneas 24–28), por lo que conoce SQL y detalles de correo. Los seis métodos privados del final (líneas 141–235) no son invocados desde `procesarPedido()`: son código muerto que sugiere un archivo que ha ido acumulando funciones (señal de *Lava Flow*, secundaria).

**Evidencia de Spaghetti Code (el flujo es difícil de seguir y modificar).**
- **Anidamiento:** el bloque de mora (líneas 50–66) llega a **3 niveles** (`if/else if` por tipo → `if` de deuda → `if/else` de horario). El bloque de descuento (líneas 79–94) llega a 2 niveles de condicionales encadenados por tipo de cliente y por monto.
- **Niveles de abstracción mezclados:** en la misma secuencia de líneas hay SQL embebido (p. ej. líneas 39, 49, 55, 72, 88, 101), reglas de negocio (descuentos, mora), cálculo numérico (línea 97–98) y formateo de texto del correo (líneas 118–123).
- **Consultas dentro de bucles:** stock (línea 39) y precio (línea 72) hacen una consulta SQL *por ítem*.
- **Dependencia del reloj:** la regla de mora llama a `LocalTime.now()` (línea 59), lo que impide probarla de forma determinista sin cambiar la hora del sistema.
- **Sin transacción:** el INSERT del pedido, los detalles y el UPDATE de inventario (líneas 101–115) no están en una transacción explícita; un fallo intermedio deja datos a medias.

**Costo de cambio (pregunta guía 4).** Para agregar un nuevo tipo de cliente con descuento propio habría que modificar el bloque `if/else if` de las líneas 79–94 *dentro* de `procesarPedido()`, releyendo el método completo para no romper validaciones, persistencia ni notificación: toda modificación toca la clase que ya concentra las otras 5 responsabilidades.

**Defecto encontrado durante el análisis.** `jdbcTemplate.queryForObject(...)` lanza `EmptyResultDataAccessException` cuando no hay fila, por lo que para un cliente con id inexistente el mensaje "Cliente no registrado" (línea 53) **nunca se alcanza**: solo se alcanza si la fila existe con `tipo_cliente` nulo. Las pruebas de caracterización (`PedidoCaracterizacionTest`) documentan ambos comportamientos.

### Pruebas de caracterización
`PedidoCaracterizacionTest` ejecuta 11 pedidos sobre las tres rutas de validación (stock, mora, cliente) y los descuentos VIP (5 %, 10 %, 15 %), FRECUENTE (4 %, 8 %) y ESTANDAR (0 %). Se escribieron **antes** de refactorizar para fijar el comportamiento observable.
