package mx.synectura.nexo_cfdi.modules.processor.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CfdiData(
        String rfcEmisor,
        String rfcReceptor,
        String uuid,
        LocalDateTime fecha,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        String nombreEmisor
) {}
