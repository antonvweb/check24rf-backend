package org.example.authService.utils;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Утилиты валидации данных
 * 
 * Предоставляет методы для валидации:
 * - Телефонов (российские и международные форматы)
 * - Email адресов
 * - Идентификаторов (телефон или email)
 * 
 * Также предоставляет методы нормализации телефонов
 */
@Component
public class ValidationUtils {
    
    // Паттерн для телефонов: + и 10-15 цифр
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    
    // Паттерн для email: стандартный RFC 5322 (упрощённый)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    /**
     * Проверка валидности телефонного номера
     * 
     * @param phone Телефонный номер для проверки
     * @return true если номер валиден
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        // Очищаем от пробелов, дефисов, скобок
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
    
    /**
     * Проверка валидности email адреса
     * 
     * @param email Email адрес для проверки
     * @return true если email валиден
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Проверка валидности идентификатора (телефон ИЛИ email)
     * 
     * @param identifier Идентификатор для проверки
     * @return true если это валидный телефон или email
     */
    public boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }
        return isValidPhone(identifier) || isValidEmail(identifier);
    }
    
    /**
     * Нормализация телефонного номера к единому формату
     * 
     * Преобразует:
     * - 8XXXXXXXXXX -> +7XXXXXXXXXX
     * - XXXXXXXXXX -> +7XXXXXXXXXX (для российских номеров)
     * - +7XXXXXXXXXX -> +7XXXXXXXXXX (без изменений)
     * 
     * @param phone Телефонный номер для нормализации
     * @return Нормализованный номер в формате +7XXXXXXXXXX
     */
    public String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        
        // Очищаем от пробелов, дефисов, скобок
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Если начинается с 8 и длина 11 цифр (российский формат)
        if (cleanPhone.startsWith("8") && cleanPhone.length() == 11) {
            cleanPhone = "+7" + cleanPhone.substring(1);
        } 
        // Если не начинается с + и длина 10 цифр (без кода страны)
        else if (!cleanPhone.startsWith("+") && cleanPhone.length() == 10) {
            // Предполагаем российский код +7
            cleanPhone = "+7" + cleanPhone;
        }
        // Если не начинается с +, добавляем +
        else if (!cleanPhone.startsWith("+") && cleanPhone.length() >= 10) {
            cleanPhone = "+" + cleanPhone;
        }
        
        return cleanPhone;
    }
    
    /**
     * Очистка телефонного номера от форматирования
     * 
     * @param phone Телефонный номер
     * @return Номер только с цифрами и знаком +
     */
    public String cleanPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        return phone.replaceAll("[\\s\\-\\(\\)]", "");
    }
}
