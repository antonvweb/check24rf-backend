package org.example.controller;

import org.example.service.SystemMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Autowired
    private SystemMetricsService systemMetricsService;

    @GetMapping
    public Map<String, Object> getMetrics() {
        Map<String, Object> result = new HashMap<>();

        // CPU load (округляем до 1 знака)
        double cpuLoad = systemMetricsService.getCpuLoad();
        result.put("cpu", Math.round(cpuLoad * 10.0) / 10.0);

        // Память (used, total)
        result.put("memory", systemMetricsService.getMemLoad());

        // Статус подключения к БД
        result.putAll(systemMetricsService.getConnectionBD());

        // Статус backend
        result.putAll(systemMetricsService.getStatusBackend());

        // Логи backend и frontend
        result.put("backendLogs", systemMetricsService.getBackendLogs().get("logs"));
        result.put("frontendLogs", systemMetricsService.getFrontendLogs().get("logs"));

        return result;
    }
}

