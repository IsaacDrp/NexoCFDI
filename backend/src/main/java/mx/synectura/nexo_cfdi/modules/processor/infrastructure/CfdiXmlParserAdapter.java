package mx.synectura.nexo_cfdi.modules.processor.infrastructure;

import lombok.extern.slf4j.Slf4j;
import mx.synectura.nexo_cfdi.modules.processor.api.CfdiParserPort;
import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiData;
import mx.synectura.nexo_cfdi.modules.processor.domain.CfdiParseException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
public class CfdiXmlParserAdapter implements CfdiParserPort {

    private static final DocumentBuilderFactory DBF;

    static {
        DBF = DocumentBuilderFactory.newInstance();
        DBF.setNamespaceAware(true);
        try {
            DBF.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DBF.setFeature("http://xml.org/sax/features/external-general-entities", false);
            DBF.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DBF.setXIncludeAware(false);
            DBF.setExpandEntityReferences(false);
        } catch (Exception e) {
            throw new IllegalStateException("Configuración de seguridad XML fallida", e);
        }
    }

    @Override
    public CfdiData parse(byte[] xmlContent) {
        log.debug("CFDI_PARSE_INICIO bytes={}", xmlContent.length);
        try {
            DocumentBuilder db  = DBF.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlContent));
            doc.getDocumentElement().normalize();

            String version = attrOrNull(doc.getDocumentElement(), "Version");
            log.debug("CFDI_VERSION version={}", version);

            String rfcReceptor = requireAttr(doc, "Receptor", "Rfc");
            String rfcEmisor   = requireAttr(doc, "Emisor",   "Rfc");
            NodeList emisorNodes = doc.getElementsByTagNameNS("*", "Emisor");
            String nombreEmisor = emisorNodes.getLength() > 0
                    ? attrOrNull(emisorNodes.item(0), "Nombre") : null;
            if (nombreEmisor != null) nombreEmisor = nombreEmisor.trim();

            NodeList tfdNodes = doc.getElementsByTagNameNS("*", "TimbreFiscalDigital");
            String uuid = tfdNodes.getLength() > 0
                    ? attrOrNull(tfdNodes.item(0), "UUID")
                    : null;

            if (uuid == null) {
                log.warn("CFDI_SIN_TIMBRE rfcReceptor={} rfcEmisor={} — puede ser borrador o CFDI cancelado",
                        rfcReceptor, rfcEmisor);
            }

            LocalDateTime fecha = null;
            String fechaStr = attrOrNull(doc.getDocumentElement(), "Fecha");
            if (fechaStr != null) {
                try { fecha = LocalDateTime.parse(fechaStr); } catch (Exception ex) {
                    log.debug("CFDI_FECHA_PARSE_FAIL value={}", fechaStr);
                }
            }

            BigDecimal subtotal = parseBigDecimal(attrOrNull(doc.getDocumentElement(), "SubTotal"));
            BigDecimal total    = parseBigDecimal(attrOrNull(doc.getDocumentElement(), "Total"));

            // Root-level Impuestos carries TotalImpuestosTrasladados; concept-level ones don't.
            BigDecimal iva = null;
            NodeList impNodes = doc.getElementsByTagNameNS("*", "Impuestos");
            for (int i = 0; i < impNodes.getLength(); i++) {
                String ivaStr = attrOrNull(impNodes.item(i), "TotalImpuestosTrasladados");
                if (ivaStr != null) {
                    iva = parseBigDecimal(ivaStr);
                    break;
                }
            }

            log.info("CFDI_PARSE_OK version={} rfcReceptor={} rfcEmisor={} nombreEmisor={} uuid={} fecha={} subtotal={} iva={} total={}",
                    version, rfcReceptor, rfcEmisor, nombreEmisor, uuid, fecha, subtotal, iva, total);

            return new CfdiData(rfcEmisor, rfcReceptor, uuid, fecha, subtotal, iva, total, nombreEmisor);

        } catch (CfdiParseException e) {
            log.warn("CFDI_PARSE_FAIL causa={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("CFDI_PARSE_FAIL causa={}", e.getMessage(), e);
            throw new CfdiParseException("Error al parsear CFDI XML: " + e.getMessage(), e);
        }
    }

    private String requireAttr(Document doc, String localName, String attrName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            throw new CfdiParseException("Elemento <" + localName + "> no encontrado en el CFDI");
        }
        String value = attrOrNull(nodes.item(0), attrName);
        if (value == null || value.isBlank()) {
            throw new CfdiParseException("Atributo " + attrName + " vacío en <" + localName + ">");
        }
        return value.trim().toUpperCase();
    }

    private String attrOrNull(org.w3c.dom.Node node, String attrName) {
        org.w3c.dom.Node attr = node.getAttributes().getNamedItem(attrName);
        return attr == null ? null : attr.getNodeValue();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); } catch (NumberFormatException ex) {
            log.debug("CFDI_DECIMAL_PARSE_FAIL value={}", value);
            return null;
        }
    }
}
