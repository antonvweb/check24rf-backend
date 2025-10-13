package org.example.adminPanelService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
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

    public Map<String, Object> getBackendLogs() {
        Path logPath = Paths.get("/var/log/check24rfAPI-dev.log");
        int maxLines = 1000;
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            LinkedList<String> lastLines = new LinkedList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (lastLines.size() == maxLines) {
                    lastLines.removeFirst();
                }
                lastLines.add(line);
            }
            return Map.of("logs", lastLines);
        } catch (IOException e) {
            return Map.of("logs", List.of("Не удалось прочитать лог"));
        }
    }

    public Map<String, Object> getFrontendLogs() {
        Path logPath = Paths.get("/var/log/check24rf-ip.log");
        int maxLines = 1000;
        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            LinkedList<String> lastLines = new LinkedList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (lastLines.size() == maxLines) {
                    lastLines.removeFirst();
                }
                lastLines.add(line);
            }
            return Map.of("logs", lastLines);
        } catch (IOException e) {
            return Map.of("logs", List.of("Не удалось прочитать логи"));
        }
    }
}
