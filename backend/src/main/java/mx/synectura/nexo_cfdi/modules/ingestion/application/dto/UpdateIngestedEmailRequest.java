package mx.synectura.nexo_cfdi.modules.ingestion.application.dto;

import java.math.BigDecimal;

/**
 * Campos editables de un ingested_email. Todos son opcionales salvo que se quiera
 * forzar un valor (null borra el campo en BD).
 */
public class UpdateIngestedEmailRequest {
    private String cfdiUuid;
    private String rfcEmisor;
    private String nombreEmisor;
    private String fecha;      // ISO-8601 LocalDateTime: "2024-03-15T12:00:00"
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal total;

    public String getCfdiUuid()      { return cfdiUuid; }
    public String getRfcEmisor()     { return rfcEmisor; }
    public String getNombreEmisor()  { return nombreEmisor; }
    public String getFecha()         { return fecha; }
    public BigDecimal getSubtotal()  { return subtotal; }
    public BigDecimal getIva()       { return iva; }
    public BigDecimal getTotal()     { return total; }

    public void setCfdiUuid(String cfdiUuid)          { this.cfdiUuid = cfdiUuid; }
    public void setRfcEmisor(String rfcEmisor)        { this.rfcEmisor = rfcEmisor; }
    public void setNombreEmisor(String nombreEmisor)  { this.nombreEmisor = nombreEmisor; }
    public void setFecha(String fecha)                { this.fecha = fecha; }
    public void setSubtotal(BigDecimal subtotal)      { this.subtotal = subtotal; }
    public void setIva(BigDecimal iva)                { this.iva = iva; }
    public void setTotal(BigDecimal total)            { this.total = total; }
}
