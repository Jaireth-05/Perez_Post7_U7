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

## Decisiones de diseño — Parte 1

**Antipatrón identificado:** God Object + Spaghetti Code en `GestorPedidos.procesarPedido()` (evidencia en el diagnóstico de arriba: 6 responsabilidades en 106 líneas, hasta 3 niveles de anidamiento, SQL, reglas y formato de texto en el mismo método).

**Estructura resultante (4 capas cohesivas):**
`ValidadorStock → ValidadorCliente` (paquete `validacion/`), `EstrategiaDescuento` + `SelectorEstrategiaDescuento` (paquete `descuento/`), `PedidoRepository` / `ProductoRepository` (persistencia), `NotificacionPedidoService` (aviso) y un `GestorPedidos` de 61 líneas que solo orquesta.

**Patrón 1 — Chain of Responsibility para las validaciones.** Las validaciones tienen una dependencia real de orden y de corte anticipado: si el stock falla, no tiene sentido consultar la mora del cliente. *Alternativa descartada:* un método `validarTodo()` con una lista de `Predicate<ContextoPedido>`; evalúa todos los predicados aunque el primero falle y no permite que un validador decida no delegar al siguiente.

**Patrón 2 — Strategy para el descuento.** El descuento no depende de un orden: siempre se aplica exactamente una regla según el tipo de cliente. *Alternativa descartada:* modelarlo como un eslabón más de la cadena; habría exigido un mecanismo artificial para garantizar que solo un eslabón module el descuento. Un mapa de selección directa resuelve el problema con menos indirección y sin condicionales.

**Ajustes necesarios para conservar el comportamiento original** (detectados al comparar con las pruebas de caracterización):
- `ValidadorStock` conserva el rechazo "El pedido no contiene items"; sin él, un pedido sin ítems provocaría un `NullPointerException`.
- `encadenar()` devuelve el eslabón *recibido*, no el receptor. Por eso `GestorPedidos` guarda `stock` como inicio de la cadena en lugar de asignar el valor devuelto; de lo contrario la cadena empezaría en `ValidadorCliente` y nunca se validaría el stock.
- El cálculo de precios usa un pequeño `ProductoRepository`, para que `GestorPedidos` no tenga SQL ni `JdbcTemplate`.
- Se conservó deliberadamente el comportamiento heredado ante un cliente inexistente (excepción de acceso a datos): el objetivo de esta parte es refactorizar sin cambiar el comportamiento observable.

**Mejoras que quedan fuera de alcance:** transacción explícita sobre `PedidoRepository.guardar`, y inyectar un `Clock` en `ValidadorCliente` para probar el horario de corte sin depender de la hora del sistema.

## Diagnóstico de la Parte 2 — las tres campañas como eslabones de la cadena

Código analizado (commit `feat: agregar 3 campanas...`): `PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen`, el campo `descuentoCampana` de `ContextoPedido` y el constructor de `GestorPedidos` que ahora encadena 5 eslabones.

### Antipatrón: Golden Hammer
Se reutilizó Chain of Responsibility porque funcionó en la Parte 1, sin comprobar si el nuevo problema tenía la misma forma. La comprobación contra las propiedades que justificaban la cadena da este resultado:

| Propiedad que justifica la cadena | `ValidadorStock` / `ValidadorCliente` | `PromocionBlackFriday` / `Corporativo` / `Volumen` |
|---|---|---|
| Dependencia de orden | **Sí**: el stock debe validarse antes que la mora del cliente | **No**: `aplicarDescuentoCampana` toma el máximo (operación conmutativa); ejecutar `PromocionVolumen` antes que `PromocionCorporativo` produce el mismo resultado |
| Corte anticipado (puede rechazar) | **Sí**: llaman a `contexto.rechazar(...)` | **No**: ninguna de las tres llama jamás a `rechazar()`; el propio comentario de `PromocionBlackFriday` lo admite ("nunca rechaza") |
| Cumple el contrato `ValidadorPedido` ("decidir si el pedido continúa o se rechaza") | Sí | No: son *calculadoras de descuento* disfrazadas de validadores |

**Evidencia adicional de que no era la herramienta adecuada:**
- Los eslabones nuevos se comunican escribiendo en un **campo mutable compartido** (`descuentoCampana`), efecto lateral en lugar de un valor de retorno.
- La regla "el mayor descuento gana" está escondida dentro de `ContextoPedido.aplicarDescuentoCampana`, no en un lugar que exprese la regla de negocio. Si dos campañas debieran **sumarse**, el campo compartido y la cadena no lo permiten sin ambigüedad (¿quién suma?, ¿en qué orden?).
- `GestorPedidos` ahora combina **dos mecanismos distintos** para el mismo concepto de "descuento" (`selector` + `descuentoCampana`), y su constructor pasó de 6 a 9 parámetros.
- El comentario de `PromocionBlackFriday` ("se agregó a la cadena porque los eslabones ya sabían conectarse entre sí") muestra que la razón fue la comodidad de lo conocido, no el análisis del problema.
- `PromocionVolumen` recalcula desde el `request` un dato (unidades totales) que el contexto no expone: otra señal de que el contexto de *validación* no es el lugar de este cálculo.

Las tres campañas tienen exactamente la forma de `DescuentoVip` o `DescuentoFrecuente`: calculan un porcentaje a partir de datos del pedido o del cliente, sin orden de evaluación. Corresponde a **Strategy**, no a la cadena.

### Verificación de equivalencia
Se escribieron `CampanasTest` y `CampanasBlackFridayTest` (9 pedidos de campaña) en el mismo commit que la versión con eslabones, para fijar su resultado antes de corregir. Se mantienen **sin modificar** en el commit de la corrección.

> Observación sobre el código de partida: en el fragmento original `primerValidador = stock.encadenar(cliente).encadenar(...)` asignaría el *último* eslabón (porque `encadenar` devuelve el eslabón recibido). En este repositorio se guarda `stock` como inicio de la cadena, tal como en la Parte 1.
