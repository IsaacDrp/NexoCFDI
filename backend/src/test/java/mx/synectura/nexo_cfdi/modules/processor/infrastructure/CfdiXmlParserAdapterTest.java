package mx.synectura.nexo_cfdi.modules.processor.infrastructure;

import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiData;
import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CfdiXmlParserAdapterTest {

    private CfdiXmlParserAdapter sut;

    @BeforeEach
    void setUp() {
        sut = new CfdiXmlParserAdapter();
    }

    // ------------------------------------------------------------------ helper

    private byte[] bytes(String xml) {
        return xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ valid CFDI 4.0

    private static final String CFDI_40 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante
                xmlns:cfdi="http://www.sat.gob.mx/cfd/4"
                xmlns:tfd="http://www.sat.gob.mx/TimbreFiscalDigital"
                Version="4.0"
                Fecha="2024-01-15T10:30:00"
                SubTotal="100.00"
                Total="116.00">
              <cfdi:Emisor Rfc=" aaa010101aaa " Nombre=" Empresa Emisora S.A. "/>
              <cfdi:Receptor Rfc=" bbb020202bbb "/>
              <cfdi:Impuestos TotalImpuestosTrasladados="16.00"/>
              <cfdi:Complemento>
                <tfd:TimbreFiscalDigital UUID="12345678-1234-1234-1234-123456789012"/>
              </cfdi:Complemento>
            </cfdi:Comprobante>
            """;

    @Test
    void validCfdi40_allFieldsParsedCorrectly() {
        CfdiData data = sut.parse(bytes(CFDI_40));

        assertThat(data.rfcEmisor()).isEqualTo("AAA010101AAA");
        assertThat(data.rfcReceptor()).isEqualTo("BBB020202BBB");
        assertThat(data.nombreEmisor()).isEqualTo("Empresa Emisora S.A.");
        assertThat(data.uuid()).isEqualTo("12345678-1234-1234-1234-123456789012");
        assertThat(data.fecha()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        assertThat(data.subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(data.total()).isEqualByComparingTo(new BigDecimal("116.00"));
        assertThat(data.iva()).isEqualByComparingTo(new BigDecimal("16.00"));
    }

    @Test
    void rfcsTrimmedAndUppercased() {
        // Fed lowercase RFC with surrounding spaces; requireAttr does trim().toUpperCase()
        CfdiData data = sut.parse(bytes(CFDI_40));

        assertThat(data.rfcEmisor()).isEqualTo("AAA010101AAA");
        assertThat(data.rfcReceptor()).isEqualTo("BBB020202BBB");
    }

    @Test
    void nombreEmisorTrimmed() {
        CfdiData data = sut.parse(bytes(CFDI_40));

        assertThat(data.nombreEmisor()).isEqualTo("Empresa Emisora S.A.");
        assertThat(data.nombreEmisor()).doesNotStartWith(" ");
        assertThat(data.nombreEmisor()).doesNotEndWith(" ");
    }

    // ------------------------------------------------------------------ valid CFDI 3.3

    private static final String CFDI_33 = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante
                xmlns:cfdi="http://www.sat.gob.mx/cfd/3"
                xmlns:tfd="http://www.sat.gob.mx/TimbreFiscalDigital"
                Version="3.3"
                Fecha="2023-06-01T09:00:00"
                SubTotal="200.00"
                Total="232.00">
              <cfdi:Emisor Rfc="CCC030303CCC" Nombre="Otra Empresa"/>
              <cfdi:Receptor Rfc="DDD040404DDD"/>
              <cfdi:Impuestos TotalImpuestosTrasladados="32.00"/>
              <cfdi:Complemento>
                <tfd:TimbreFiscalDigital UUID="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"/>
              </cfdi:Complemento>
            </cfdi:Comprobante>
            """;

    @Test
    void validCfdi33_parsesSuccessfully() {
        CfdiData data = sut.parse(bytes(CFDI_33));

        assertThat(data.rfcEmisor()).isEqualTo("CCC030303CCC");
        assertThat(data.rfcReceptor()).isEqualTo("DDD040404DDD");
        assertThat(data.uuid()).isEqualTo("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        assertThat(data.subtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    // ------------------------------------------------------------------ missing TimbreFiscalDigital

    private static final String CFDI_NO_TFD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante
                xmlns:cfdi="http://www.sat.gob.mx/cfd/4"
                Version="4.0"
                Fecha="2024-01-15T10:30:00"
                SubTotal="100.00"
                Total="116.00">
              <cfdi:Emisor Rfc="EEE050505EEE" Nombre="Emisor Sin Timbre"/>
              <cfdi:Receptor Rfc="FFF060606FFF"/>
              <cfdi:Impuestos TotalImpuestosTrasladados="16.00"/>
            </cfdi:Comprobante>
            """;

    @Test
    void noTimbreFiscalDigital_uuidIsNullNoExceptionThrown() {
        CfdiData data = sut.parse(bytes(CFDI_NO_TFD));

        assertThat(data.uuid()).isNull();
        assertThat(data.rfcEmisor()).isEqualTo("EEE050505EEE");
        assertThat(data.rfcReceptor()).isEqualTo("FFF060606FFF");
    }

    // ------------------------------------------------------------------ missing elements

    private static final String CFDI_NO_RECEPTOR = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante
                xmlns:cfdi="http://www.sat.gob.mx/cfd/4"
                Version="4.0"
                Fecha="2024-01-15T10:30:00"
                SubTotal="100.00"
                Total="116.00">
              <cfdi:Emisor Rfc="GGG070707GGG" Nombre="Emisor OK"/>
            </cfdi:Comprobante>
            """;

    @Test
    void missingReceptorElement_throwsCfdiParseException() {
        assertThatThrownBy(() -> sut.parse(bytes(CFDI_NO_RECEPTOR)))
                .isInstanceOf(CfdiParseException.class);
    }

    private static final String CFDI_NO_EMISOR = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante
                xmlns:cfdi="http://www.sat.gob.mx/cfd/4"
                Version="4.0"
                Fecha="2024-01-15T10:30:00"
                SubTotal="100.00"
                Total="116.00">
              <cfdi:Receptor Rfc="HHH080808HHH"/>
            </cfdi:Comprobante>
            """;

    @Test
    void missingEmisorElement_throwsCfdiParseException() {
        assertThatThrownBy(() -> sut.parse(bytes(CFDI_NO_EMISOR)))
                .isInstanceOf(CfdiParseException.class);
    }

    private static final String CFDI_BLANK_RFC = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante
                xmlns:cfdi="http://www.sat.gob.mx/cfd/4"
                Version="4.0"
                Fecha="2024-01-15T10:30:00"
                SubTotal="100.00"
                Total="116.00">
              <cfdi:Emisor Rfc="III090909III" Nombre="Emisor OK"/>
              <cfdi:Receptor Rfc="   "/>
            </cfdi:Comprobante>
            """;

    @Test
    void blankRfcOnReceptor_throwsCfdiParseException() {
        assertThatThrownBy(() -> sut.parse(bytes(CFDI_BLANK_RFC)))
                .isInstanceOf(CfdiParseException.class);
    }

    // ------------------------------------------------------------------ IVA from root-level Impuestos

    private static final String CFDI_CONCEPT_IVA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cfdi:Comprobante
                xmlns:cfdi="http://www.sat.gob.mx/cfd/4"
                xmlns:tfd="http://www.sat.gob.mx/TimbreFiscalDigital"
                Version="4.0"
                Fecha="2024-03-01T12:00:00"
                SubTotal="500.00"
                Total="580.00">
              <cfdi:Emisor Rfc="JJJ101010JJJ" Nombre="Emisor Conceptos"/>
              <cfdi:Receptor Rfc="KKK111111KKK"/>
              <cfdi:Conceptos>
                <cfdi:Concepto>
                  <!-- concept-level Impuestos has NO TotalImpuestosTrasladados -->
                  <cfdi:Impuestos/>
                </cfdi:Concepto>
              </cfdi:Conceptos>
              <!-- root-level Impuestos carries the total -->
              <cfdi:Impuestos TotalImpuestosTrasladados="80.00"/>
              <cfdi:Complemento>
                <tfd:TimbreFiscalDigital UUID="abcdef01-abcd-abcd-abcd-abcdef012345"/>
              </cfdi:Complemento>
            </cfdi:Comprobante>
            """;

    @Test
    void ivaReadFromRootLevelImpuestos_notConceptLevel() {
        CfdiData data = sut.parse(bytes(CFDI_CONCEPT_IVA));

        assertThat(data.iva()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    // ------------------------------------------------------------------ error cases

    @Test
    void malformedXml_throwsCfdiParseException() {
        assertThatThrownBy(() -> sut.parse("<not valid".getBytes()))
                .isInstanceOf(CfdiParseException.class);
    }

    @Test
    void doctypeDeclaration_throwsCfdiParseException() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <cfdi:Comprobante xmlns:cfdi="http://www.sat.gob.mx/cfd/4">
                  <cfdi:Emisor Rfc="LLL121212LLL" Nombre="Emisor"/>
                  <cfdi:Receptor Rfc="MMM131313MMM"/>
                </cfdi:Comprobante>
                """;

        assertThatThrownBy(() -> sut.parse(bytes(xxe)))
                .isInstanceOf(CfdiParseException.class);
    }

    @Test
    void invalidDecimalInSubTotal_returnsNullNotException() {
        // Per actual code: parseBigDecimal catches NumberFormatException and returns null
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <cfdi:Comprobante
                    xmlns:cfdi="http://www.sat.gob.mx/cfd/4"
                    xmlns:tfd="http://www.sat.gob.mx/TimbreFiscalDigital"
                    Version="4.0"
                    Fecha="2024-01-15T10:30:00"
                    SubTotal="abc"
                    Total="116.00">
                  <cfdi:Emisor Rfc="NNN141414NNN" Nombre="Emisor"/>
                  <cfdi:Receptor Rfc="OOO151515OOO"/>
                  <cfdi:Impuestos TotalImpuestosTrasladados="16.00"/>
                  <cfdi:Complemento>
                    <tfd:TimbreFiscalDigital UUID="11111111-2222-3333-4444-555555555555"/>
                  </cfdi:Complemento>
                </cfdi:Comprobante>
                """;

        CfdiData data = sut.parse(bytes(xml));

        // parseBigDecimal logs a warning and returns null — no exception
        assertThat(data.subtotal()).isNull();
        // other fields still parsed
        assertThat(data.rfcEmisor()).isEqualTo("NNN141414NNN");
        assertThat(data.total()).isEqualByComparingTo(new BigDecimal("116.00"));
    }
}
