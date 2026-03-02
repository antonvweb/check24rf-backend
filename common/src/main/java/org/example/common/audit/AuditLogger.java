package org.example.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Компонент аудиторского логирования для безопасности
 * 
 * Логирует все важные события безопасности:
 * - Успешные аутентификации
 * - Неудачные попытки входа
 * - Выход из системы
 * - Подозрительные события
 * 
 * Логи пишутся в отдельный файл audit.log для последующего анализа
 */
@Slf4j
@Component
public class AuditLogger {
    
    // Отдельный logger для аудита - пишет в audit.log
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    // Формат даты для логов (Московское время)
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Europe/Moscow"));
    
    /**
     * Логирование успешной аутентификации
     * 
     * @param userId ID пользователя
     * @param identifier Идентификатор (телефон/email)
     * @param ip IP адрес клиента
     */
    public void logAuthSuccess(String userId, String identifier, String ip) {
        auditLogger.info("AUTH_SUCCESS | userId={} | identifier={} | ip={} | time={}", 
            userId, maskIdentifier(identifier), ip, formatter.format(Instant.now()));
    }
    
    /**
     * Логирование неудачной попытки аутентификации
     * 
     * @param identifier Идентификатор (телефон/email)
     * @param reason Причина отказа
     * @param ip IP адрес клиента
     */
    public void logAuthFailure(String identifier, String reason, String ip) {
        auditLogger.warn("AUTH_FAILURE | identifier={} | reason={} | ip={} | time={}", 
            maskIdentifier(identifier), reason, ip, formatter.format(Instant.now()));
    }
    
    /**
     * Логирование выхода из системы
     * 
     * @param userId ID пользователя
     * @param ip IP адрес клиента
     */
    public void logLogout(String userId, String ip) {
        auditLogger.info("LOGOUT | userId={} | ip={} | time={}", 
            userId, ip, formatter.format(Instant.now()));
    }
    
    /**
     * Логирование событий безопасности
     * 
     * @param eventType Тип события (например, "RATE_LIMIT_EXCEEDED", "INVALID_TOKEN")
     * @param details Детали события
     * @param ip IP адрес клиента
     */
    public void logSecurityEvent(String eventType, String details, String ip) {
        auditLogger.warn("SECURITY_EVENT | type={} | details={} | ip={} | time={}", 
            eventType, details, ip, formatter.format(Instant.now()));
    }
    
    /**
     * Логирование подозрительной активности
     * 
     * @param eventType Тип события
     * @param userId ID пользователя (если известен)
     * @param identifier Идентификатор
     * @param ip IP адрес
     * @param details Детали
     */
    public void logSuspiciousActivity(String eventType, String userId, String identifier, String ip, String details) {
        auditLogger.error("SUSPICIOUS_ACTIVITY | type={} | userId={} | identifier={} | ip={} | details={} | time={}", 
            eventType, userId, maskIdentifier(identifier), ip, details, formatter.format(Instant.now()));
    }
    
    /**
     * Логирование изменений прав доступа
     * 
     * @param userId ID пользователя
     * @param action Действие (GRANT, REVOKE)
     * @param role Роль
     * @param adminId ID администратора
     */
    public void logRoleChange(String userId, String action, String role, String adminId) {
        auditLogger.info("ROLE_CHANGE | userId={} | action={} | role={} | adminId={} | time={}", 
            userId, action, role, adminId, formatter.format(Instant.now()));
    }
    
    /**
     * Маскировка идентификатора для безопасности (частичное скрытие)
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "***";
        }
        
        if (identifier.contains("@")) {
            // Email: user***@domain.com
            int atIndex = identifier.indexOf("@");
            String visiblePart = identifier.substring(0, Math.min(3, atIndex));
            return visiblePart + "***" + identifier.substring(atIndex);
        } else {
            // Phone: +7***1234
            if (identifier.length() > 7) {
                return identifier.substring(0, 3) + "***" + identifier.substring(identifier.length() - 4);
            }
            return "***";
        }
    }
}
