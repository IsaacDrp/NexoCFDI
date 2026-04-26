package mx.synectura.nexo_cfdi.modules.storage.api;

import java.io.InputStream;

public interface DocumentStoragePort {
    String upload(String fileName, InputStream content, String contentType);
    InputStream download(String fileId);
    void delete(String fileId);
}
