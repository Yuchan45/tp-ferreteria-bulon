package com.bulon.stock.movimientos.estrategias;

import com.bulon.stock.common.exception.BusinessException;
import com.bulon.stock.movimientos.MovimientoStockDTO;
import com.bulon.stock.movimientos.TipoMovimiento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada estrategia por separado y la fábrica, sin Spring ni base de datos. */
class EstrategiasMovimientoTests {

    private final FabricaMovimientos fabrica = new FabricaMovimientos(List.of(
            new IngresoCompra(), new SalidaVenta(), new DevolucionCliente(),
            new DevolucionProveedor(), new Rotura(), new Ajuste()));

    @Test
    void laFabricaEntregaLaEstrategiaDeCadaTipo() {
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            assertThat(fabrica.estrategiaPara(tipo).tipo()).isEqualTo(tipo);
        }
    }

    @Test
    void laFabricaNoArrancaSiFaltaUnaEstrategia() {
        assertThatThrownBy(() -> new FabricaMovimientos(List.of(new IngresoCompra())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falta la estrategia");
    }

    @Test
    void ingresosSumanYSalidasRestan() {
        BigDecimal diez = new BigDecimal("10");

        assertThat(delta(TipoMovimiento.INGRESO_COMPRA, diez)).isEqualByComparingTo("10");
        assertThat(delta(TipoMovimiento.DEVOLUCION_CLIENTE, diez)).isEqualByComparingTo("10");
        assertThat(delta(TipoMovimiento.SALIDA_VENTA, diez)).isEqualByComparingTo("-10");
        assertThat(delta(TipoMovimiento.DEVOLUCION_PROVEEDOR, diez)).isEqualByComparingTo("-10");
        assertThat(delta(TipoMovimiento.ROTURA, diez)).isEqualByComparingTo("-10");
    }

    @Test
    void elAjusteAplicaLaCantidadConSuSigno() {
        assertThat(delta(TipoMovimiento.AJUSTE, new BigDecimal("-3"))).isEqualByComparingTo("-3");
        validar(TipoMovimiento.AJUSTE, "-3", "Recuento", null); // negativo permitido
        assertThatThrownBy(() -> validar(TipoMovimiento.AJUSTE, "0", "Recuento", null))
                .hasMessageContaining("no puede ser 0");
    }

    @Test
    void losDemasTiposExigenCantidadPositiva() {
        assertThatThrownBy(() -> validar(TipoMovimiento.SALIDA_VENTA, "-5", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void cadaTipoExigeSuMotivoYSuProveedor() {
        assertThatThrownBy(() -> validar(TipoMovimiento.ROTURA, "1", " ", null))
                .hasMessageContaining("El motivo es obligatorio");
        assertThatThrownBy(() -> validar(TipoMovimiento.INGRESO_COMPRA, "1", null, null))
                .hasMessageContaining("El proveedor es obligatorio");
        assertThatThrownBy(() -> validar(TipoMovimiento.DEVOLUCION_PROVEEDOR, "1", null, 1L))
                .hasMessageContaining("El motivo es obligatorio");
        assertThatThrownBy(() -> validar(TipoMovimiento.SALIDA_VENTA, "1", null, 1L))
                .hasMessageContaining("solo se indica en compras");

        validar(TipoMovimiento.SALIDA_VENTA, "1", null, null);
        validar(TipoMovimiento.INGRESO_COMPRA, "1", null, 1L);
        validar(TipoMovimiento.DEVOLUCION_PROVEEDOR, "1", "Fallado", 1L);
    }

    private BigDecimal delta(TipoMovimiento tipo, BigDecimal cantidad) {
        return fabrica.estrategiaPara(tipo).delta(cantidad);
    }

    private void validar(TipoMovimiento tipo, String cantidad, String motivo, Long proveedorId) {
        fabrica.estrategiaPara(tipo).validar(new MovimientoStockDTO(null, 1L, 1L, 1L, tipo,
                new BigDecimal(cantidad), null, motivo, null, null, proveedorId));
    }
}
