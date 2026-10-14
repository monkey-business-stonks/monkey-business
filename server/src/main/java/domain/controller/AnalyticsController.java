package domain.controller;

import domain.service.AnalyticsService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyAuthority('ROLE_ANALYST','ROLE_OPERATIONS')")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(analyticsService.getReplicaHealth());
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('ROLE_ANALYST','ROLE_OPERATIONS')")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(analyticsService.getOverviewMetrics());
    }
}
