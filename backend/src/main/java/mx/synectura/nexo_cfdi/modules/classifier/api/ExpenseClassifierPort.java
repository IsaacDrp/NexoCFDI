package mx.synectura.nexo_cfdi.modules.classifier.api;

import java.util.Map;

public interface ExpenseClassifierPort {
    String classify(Map<String, Object> cfdiData);
}
