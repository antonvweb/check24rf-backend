package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Service
public class SystemMetricsService {

    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private long[] prevTicks;

    @Autowired
    private DataSource dataSource;

    public SystemMetricsService() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        this.processor = hal.getProcessor();
        this.memory = hal.getMemory();
        this.prevTicks = processor.getSystemCpuLoadTicks();
    }

    public double getCpuLoad() {
        long[] oldTicks = this.prevTicks;
        this.prevTicks = processor.getSystemCpuLoadTicks();

        return processor.getSystemCpuLoadBetweenTicks(oldTicks) * 100;
    }

    public Map<String, Long> getMemLoad() {
        long usedMemory = memory.getTotal() - memory.getAvailable();
        long totalMemory = memory.getTotal();

        return Map.of(
                "used", usedMemory / 1024 / 1024,
                "total", totalMemory / 1024 / 1024
        );
    }

    public Map<String, Object> getConnectionBD() {
        try (Connection conn = dataSource.getConnection()) {
           return Map.of("dbStatus", conn.isValid(1) ? "UP" : "DOWN");
        } catch (SQLException e) {
            return Map.of("dbStatus", "DOWN");
        }
    }

    public Map<String, Object> getStatusBackend(){
        return Map.of("backendStatus", "UP");
    }

    public Map<String, Object> getBackendLogs(){
        try {
            Path logPath = Paths.get("/var/log/check24rfAPI-dev.log");
            List<String> logs = Files.readAllLines(logPath);
            int from = Math.max(0, logs.size() - 10);
            return Map.of("logs", logs.subList(from, logs.size()));
        } catch (IOException e) {
            return Map.of("logs", List.of("Не удалось прочитать логи"));
        }
    }

    public Map<String, Object> getFrontendLogs(){
        try {
            Path logPath = Paths.get("/var/log/check24rf-ip.log");
            List<String> logs = Files.readAllLines(logPath);
            int from = Math.max(0, logs.size() - 10);
            return Map.of("logs", logs.subList(from, logs.size()));
        } catch (IOException e) {
            return Map.of("logs", List.of("Не удалось прочитать логи"));
        }
    }
}
