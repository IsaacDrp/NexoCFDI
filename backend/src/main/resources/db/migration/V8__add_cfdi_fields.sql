ALTER TABLE ingested_emails
    ADD COLUMN cfdi_rfc_emisor VARCHAR(13),
    ADD COLUMN cfdi_fecha      TIMESTAMP,
    ADD COLUMN cfdi_subtotal   NUMERIC(15,2),
    ADD COLUMN cfdi_iva        NUMERIC(15,2),
    ADD COLUMN cfdi_total      NUMERIC(15,2);
