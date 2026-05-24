package mx.synectura.nexo_cfdi.modules.storage.api;

public interface DocumentStoragePort {
    /** Sube contenido al blob store y devuelve el objectKey usado. */
    String store(String objectKey, byte[] content, String contentType);
    void remove(String objectKey);
    /** Devuelve una URL temporal para visualizar/descargar el objeto. */
    String getPresignedUrl(String objectKey);
    /** Descarga el contenido completo de un objeto. */
    byte[] fetch(String objectKey);
}
