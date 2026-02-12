package org.example.mcoService.service;

import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.enums.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис для работы с шаблонами уведомлений.
 * Заполняет шаблоны из NotificationType реальными значениями.
 */
@Slf4j
@Service
public class NotificationTemplateService {

    /**
     * Паттерн для поиска плейсхолдеров вида {variableName}
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)}");

    /**
     * Заполняет шаблон значениями из карты переменных.
     *
     * @param template  шаблон с плейсхолдерами вида {count}, {amount}
     * @param variables карта переменных для подстановки
     * @return заполненная строка
     */
    public String fillTemplate(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.getOrDefault(variableName, matcher.group(0));
            // Экранируем специальные символы для replaceAll
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Получить заполненный заголовок уведомления.
     *
     * @param type      тип уведомления
     * @param variables переменные для подстановки
     * @return заполненный заголовок
     */
    public String getTitle(NotificationType type, Map<String, String> variables) {
        return fillTemplate(type.getTitleTemplate(), variables);
    }

    /**
     * Получить заполненное полное сообщение уведомления.
     *
     * @param type      тип уведомления
     * @param variables переменные для подстановки
     * @return заполненное сообщение
     */
    public String getMessage(NotificationType type, Map<String, String> variables) {
        return fillTemplate(type.getMessageTemplate(), variables);
    }

    /**
     * Получить заполненное короткое сообщение для PUSH.
     *
     * @param type      тип уведомления
     * @param variables переменные для подстановки
     * @return заполненное короткое сообщение
     */
    public String getShortMessage(NotificationType type, Map<String, String> variables) {
        return fillTemplate(type.getShortMessageTemplate(), variables);
    }

    /**
     * Получить категорию уведомления (всегда GENERAL для нашего сервиса).
     *
     * @param type тип уведомления
     * @return категория уведомления
     */
    public String getCategory(NotificationType type) {
        return type.getCategory();
    }

    /**
     * Создать полный набор заполненных текстов уведомления.
     *
     * @param type      тип уведомления
     * @param variables переменные для подстановки
     * @return массив из [title, message, shortMessage]
     */
    public String[] getFilledTexts(NotificationType type, Map<String, String> variables) {
        return new String[]{
                getTitle(type, variables),
                getMessage(type, variables),
                getShortMessage(type, variables)
        };
    }
}
