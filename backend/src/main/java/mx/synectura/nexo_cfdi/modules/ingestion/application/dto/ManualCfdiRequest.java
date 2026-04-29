package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ManualCfdiRequest {
    private String cfdiUuid;
    private String rfcEmisor;
    private String nombreEmisor;
    /** ISO LocalDateTime: yyyy-MM-ddTHH:mm:ss */
    private String fecha;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;
}
