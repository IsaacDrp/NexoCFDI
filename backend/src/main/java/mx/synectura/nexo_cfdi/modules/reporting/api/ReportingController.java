package mx.synectura.nexo_cfdi.modules.reporting.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reporting")
public class ReportingController {

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return Map.of("message", "Reporting summary endpoint");
    }
}
