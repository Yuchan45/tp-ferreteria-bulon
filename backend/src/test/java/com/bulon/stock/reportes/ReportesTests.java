package com.bulon.stock.reportes;

import com.bulon.stock.auth.AuthDTOs;
import com.bulon.stock.reportes.Reporte.Columna;
import com.bulon.stock.reportes.adaptadores.ExcelReporteAdapter;
import com.bulon.stock.reportes.adaptadores.PdfReporteAdapter;
import com.bulon.stock.sucursales.SucursalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los adaptadores por separado y los tres reportes de punta a punta, sobre los datos del seeder. */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:reportes_test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportesTests {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final Reporte ejemplo = new Reporte("Reporte de prueba", List.of("Sucursal Central"),
            List.of(Columna.texto("Código"), Columna.numero("Stock")),
            List.of(List.of("TOR-001", new BigDecimal("2380")), Arrays.asList("CAD-001", new BigDecimal("25.5"))),
            "2 productos");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private SucursalService sucursalService;

    @Test
    void elAdapterDePdfGeneraUnPdf() {
        byte[] pdf = new PdfReporteAdapter().exportar(ejemplo);

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void elAdapterDeExcelDejaLosNumerosComoNumeros() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(new ExcelReporteAdapter().exportar(ejemplo)))) {
            Sheet hoja = libro.getSheetAt(0);
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Reporte de prueba");
            int encabezado = filaDe(hoja, "Código");
            assertThat(hoja.getRow(encabezado + 2).getCell(1).getNumericCellValue()).isEqualTo(25.5);
        }
    }

    @Test
    void elReporteDeStockTraeTodosLosProductosDeLaSucursal() throws Exception {
        MockHttpServletResponse respuesta = descargar("/api/reportes/stock?formato=excel&sucursalId=" + sucursalId());

        assertThat(respuesta.getContentType()).isEqualTo(XLSX);
        assertThat(respuesta.getHeader("Content-Disposition")).contains("attachment").contains("stock-suc-01-").contains(".xlsx");
        assertThat(filasDeDatos(respuesta, "Código")).isEqualTo(27);
    }

    @Test
    void losFiltrosSeAplicanAlReporte() throws Exception {
        // El seeder registra 2 ventas y deja 6 productos críticos
        assertThat(filasDeDatos(descargar("/api/reportes/movimientos?formato=excel&tipo=SALIDA_VENTA&sucursalId=" + sucursalId()), "Fecha"))
                .isEqualTo(2);
        assertThat(filasDeDatos(descargar("/api/reportes/stock?formato=excel&estado=CRITICO&sucursalId=" + sucursalId()), "Código"))
                .isEqualTo(6);
        assertThat(filasDeDatos(descargar("/api/reportes/stock?formato=excel&q=lampara&sucursalId=" + sucursalId()), "Código"))
                .isEqualTo(1);
    }

    @Test
    void elReporteAReponerSaleEnPdf() throws Exception {
        MockHttpServletResponse respuesta = descargar("/api/reportes/a-reponer?formato=pdf&sucursalId=" + sucursalId());

        assertThat(respuesta.getContentType()).isEqualTo("application/pdf");
        assertThat(new String(respuesta.getContentAsByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void unFormatoDesconocidoOUnaSesionFaltanteSeRechazan() throws Exception {
        mvc.perform(conToken(get("/api/reportes/stock?formato=word&sucursalId=" + sucursalId()), token()))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/reportes/stock?sucursalId=" + sucursalId()).contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletResponse descargar(String url) throws Exception {
        return mvc.perform(conToken(get(url), token())).andExpect(status().isOk()).andReturn().getResponse();
    }

    /** Cuántas filas hay entre el encabezado y la primera fila vacía. */
    private static int filasDeDatos(MockHttpServletResponse respuesta, String primerEncabezado) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(respuesta.getContentAsByteArray()))) {
            Sheet hoja = libro.getSheetAt(0);
            int fila = filaDe(hoja, primerEncabezado) + 1;
            int datos = 0;
            while (hoja.getRow(fila) != null) {
                datos++;
                fila++;
            }
            return datos;
        }
    }

    private static int filaDe(Sheet hoja, String texto) {
        for (Row row : hoja) {
            if (row.getCell(0) != null && texto.equals(row.getCell(0).getStringCellValue())) {
                return row.getRowNum();
            }
        }
        throw new AssertionError("No se encontró la fila \"" + texto + "\"");
    }

    private Long sucursalId() {
        return sucursalService.buscarPorCodigo("SUC-01").orElseThrow().getId();
    }

    private String token() throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AuthDTOs.LoginRequest("ventas", "empleado"))))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("token").asText();
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder request, String token) {
        return request.contextPath("/api").header("Authorization", "Bearer " + token);
    }
}
