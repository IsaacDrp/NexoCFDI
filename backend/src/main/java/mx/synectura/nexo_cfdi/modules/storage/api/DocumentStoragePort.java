package mx.synectura.nexo_cfdi.modules.storage.api;

public interface DocumentStoragePort {
    /** Sube contenido al blob store y devuelve el objectKey usado. */
    String store(String objectKey, byte[] content, String contentType);
    void remove(String objectKey);
}
