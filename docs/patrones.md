# Patrones de diseño en el código

Cómo quedó implementado cada patrón comprometido en la [Entrega 2](informes/Grupo%206%20-%20Entrega%202%20-%20Ferreter%C3%ADa%20Bul%C3%B3n.pdf).
Rutas relativas a `backend/src/main/java/com/bulon/stock/`.

| Patrón | Problema | Clases |
|--------|----------|--------|
| Singleton | Una sola configuración y pool de conexiones a SQL Server | `DataSource` (HikariCP) administrado por Spring |
| Factory Method | Crear los 6 tipos de movimiento sin condicionales en el servicio | `movimientos/factory/MovimientoStockCreator` y subclases |
| Strategy | Validar y aplicar cada tipo de movimiento a su manera | `movimientos/strategy/MovimientoStrategy` y estrategias |
| Observer | Avisar del stock bajo sin que el movimiento conozca a los interesados | `movimientos/StockObserver`, `alertas/EncargadoDepositoObserver` |
| Decorator | Combinar canales de aviso (registro, log, email) | `alertas/Alerta`, `AlertaBase`, `AlertaDecorator`, `LogAlertaDecorator`, `EmailAlertaDecorator` |
| Adapter | Exportar reportes con librerías de interfaces distintas | `reportes/export/ReporteExporter`, `PdfReporteAdapter` (OpenPDF), `ExcelReporteAdapter` (fastexcel) |

## Singleton

No hay una clase `DatabaseConnectionManager` escrita a mano. Spring Boot crea un único bean `DataSource`
(pool HikariCP) y lo comparten todos los repositorios a través de JPA. Es un Singleton administrado por el contenedor:
la configuración vive en un solo lugar (`application.yml`) y nadie abre conexiones por su cuenta.
Una clase propia con `getInstance()` saltearía el pool, las transacciones de Spring y Flyway.

## Factory Method + Strategy: registrar un movimiento

`MovimientoStockService.registrar` resuelve las entidades y delega en dos objetos elegidos por tipo:

1. **Creator** (Factory Method). `MovimientoStockCreator.registrar` es el método plantilla y `crearMovimiento` es el
   método fábrica que cada subclase redefine. `MovimientoSimpleCreator` cubre ingreso, venta, devolución y rotura
   (cantidad positiva, sin destino). `AjusteCreator` exige motivo y admite signo. `TrasladoCreator` exige un destino
   distinto del origen.
2. **Strategy**. `validar` comprueba que el stock alcance y `actualizarStock` lo aplica. `IngresoStrategy` suma,
   `SalidaStrategy` resta, `AjusteStrategy` suma con signo y `TrasladoStrategy` resta en origen y suma en destino
   en la misma transacción.

Cada creator y cada estrategia declara sus `tipos()`. El servicio arma un `EnumMap` al iniciar y falla si a algún
`TipoMovimiento` le falta su par. Así, agregar un tipo no toca el servicio (Open/Closed).

## Observer + Decorator: alertas de reposición

`MovimientoStockService` es el Subject. Después de cada movimiento calcula el stock total del producto y llama a
`stockActualizado` en cada `StockObserver`. Spring suscribe automáticamente todos los beans que implementan la interfaz.

`EncargadoDepositoObserver` compara el stock con el mínimo y el punto de pedido (`productos/EstadoStock`):
abre una alerta al llegar al punto de pedido, la escala si baja del mínimo y la cierra al reponer.
Para avisar usa un `Alerta`, armado en `alertas/AlertaConfig` como cadena de decoradores:

```
EmailAlertaDecorator (si hay SMTP y ALERTAS_EMAIL)
  └─ LogAlertaDecorator
       └─ AlertaBase (guarda la alerta: la ven /alertas y el panel)
```

Sumar un canal (por ejemplo, WhatsApp) es un decorador nuevo y una línea en `AlertaConfig`.

## Adapter: exportación de reportes

`ReporteService` arma un `Reporte` (título, columnas, filas) sin saber de formatos y le pide el archivo al
`ReporteExporter` del formato pedido. `PdfReporteAdapter` traduce ese modelo a la API de OpenPDF
(`Document`, `PdfPTable`) y `ExcelReporteAdapter` a la de fastexcel (`Workbook`, `Worksheet`).
Cambiar de librería toca un solo adaptador.

## Otras decisiones

- **Bloqueo optimista:** `stock/Existencia` tiene `@Version`. Si dos ventas leen la misma existencia, la segunda en
  guardar falla y la API responde 409. Lo prueba `MovimientosTests.ventasSimultaneasNuncaDejanStockNegativoNiDescuadrado`.
- **Estado de órdenes de compra:** `compras/EstadoOrdenCompra.puedePasarA` define las transiciones válidas en un solo lugar.
