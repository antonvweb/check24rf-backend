package org.example.mcoService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.entity.Receipt;
import org.example.mcoService.entity.User;
import org.example.mcoService.repository.ReceiptRepository;
import org.example.mcoService.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Сохранить чеки из MCO API в БД
     * Автоматически создает пользователей и проверяет дубликаты
     *
     * @param receiptsFromMco список чеков от MCO API
     * @return количество сохраненных новых чеков
     */
    @Transactional
    public int saveReceipts(List<GetReceiptsTapeResponse.Receipt> receiptsFromMco) {
        int savedCount = 0;

        for (GetReceiptsTapeResponse.Receipt mcoReceipt : receiptsFromMco) {
            try {
                // Парсим Base64 JSON
                String jsonString = decodeBase64Json(mcoReceipt.getJson());
                JsonNode jsonNode = objectMapper.readTree(jsonString);

                // Извлекаем фискальные данные для проверки дубликата
                Long fiscalSign = jsonNode.get("fiscalSign").asLong();
                Long fiscalDocumentNumber = jsonNode.get("fiscalDocumentNumber").asLong();
                String fiscalDriveNumber = jsonNode.get("fiscalDriveNumber").asText();

                // Проверяем, есть ли уже такой чек
                if (receiptRepository.existsByFiscalSignAndFiscalDocumentNumberAndFiscalDriveNumber(
                        fiscalSign, fiscalDocumentNumber, fiscalDriveNumber)) {
                    log.debug("Чек уже существует: fiscalSign={}, fiscalDocumentNumber={}, fiscalDriveNumber={}",
                            fiscalSign, fiscalDocumentNumber, fiscalDriveNumber);
                    continue;
                }

                // Находим или создаем пользователя
                User user = findOrCreateUser(mcoReceipt.getUserIdentifier(), mcoReceipt.getEmail());

                // Создаем Receipt entity
                Receipt receipt = buildReceiptEntity(mcoReceipt, jsonNode, jsonString, user.getId());

                // Сохраняем
                receiptRepository.save(receipt);
                savedCount++;

                log.info("✅ Сохранен чек: fiscalSign={}, user={}, sum={}",
                        fiscalSign, user.getPhoneNumber(), receipt.getTotalSum());

            } catch (Exception e) {
                log.error("❌ Ошибка сохранения чека для пользователя {}: {}",
                        mcoReceipt.getUserIdentifier(), e.getMessage(), e);
            }
        }

        log.info("Сохранено {} новых чеков из {}", savedCount, receiptsFromMco.size());
        return savedCount;
    }

    /**
     * Синхронизация чеков конкретного пользователя
     * Вызывается при заходе пользователя на сайт
     *
     * @param phoneNumber номер телефона пользователя
     * @param receiptsFromMco свежие чеки от MCO API
     * @return количество добавленных новых чеков
     */
    @Transactional
    public int syncUserReceipts(String phoneNumber, List<GetReceiptsTapeResponse.Receipt> receiptsFromMco) {
        log.info("Синхронизация чеков для пользователя: {}", phoneNumber);

        // Фильтруем только чеки этого пользователя
        List<GetReceiptsTapeResponse.Receipt> userReceipts = receiptsFromMco.stream()
                .filter(r -> phoneNumber.equals(r.getUserIdentifier()) || phoneNumber.equals(r.getPhone()))
                .toList();

        if (userReceipts.isEmpty()) {
            log.info("Нет новых чеков для пользователя {}", phoneNumber);
            return 0;
        }

        return saveReceipts(userReceipts);
    }

    /**
     * Найти или создать пользователя
     */
    private User findOrCreateUser(String phoneNumber, String email) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    log.info("Создаем нового пользователя: {}", phoneNumber);
                    User newUser = User.builder()
                            .phoneNumber(phoneNumber)
                            .email(email)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * Построить Receipt entity из данных MCO и распарсенного JSON
     */
    private Receipt buildReceiptEntity(GetReceiptsTapeResponse.Receipt mcoReceipt,
                                       JsonNode jsonNode,
                                       String jsonString,
                                       UUID userId) {
        // Парсим дату чека (Unix timestamp в секундах)
        long dateTimestamp = jsonNode.get("dateTime").asLong();
        LocalDateTime receiptDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(dateTimestamp),
                ZoneId.systemDefault()
        );

        // Парсим receiveDate из строки ISO 8601
        LocalDateTime receiveDate = LocalDateTime.parse(
                mcoReceipt.getReceiveDate().replace("Z", "")
        );

        // Сумма в копейках -> рубли
        BigDecimal totalSum = BigDecimal.valueOf(jsonNode.get("totalSum").asLong())
                .divide(BigDecimal.valueOf(100));

        return Receipt.builder()
                .userId(userId)
                .userIdentifier(mcoReceipt.getUserIdentifier())
                .phone(mcoReceipt.getPhone())
                .email(mcoReceipt.getEmail())
                .fiscalSign(jsonNode.get("fiscalSign").asLong())
                .fiscalDocumentNumber(jsonNode.get("fiscalDocumentNumber").asLong())
                .fiscalDriveNumber(jsonNode.get("fiscalDriveNumber").asText())
                .receiptDateTime(receiptDateTime)
                .receiveDate(receiveDate)
                .totalSum(totalSum)
                .sourceCode(mcoReceipt.getSourceCode())
                .operationType(jsonNode.has("operationType") ? jsonNode.get("operationType").asInt() : null)
                .userInn(jsonNode.has("userInn") ? jsonNode.get("userInn").asText() : null)
                .retailPlace(jsonNode.has("retailPlace") ? jsonNode.get("retailPlace").asText() : null)
                .rawJson(jsonString)
                .build();
    }

    /**
     * Декодировать Base64 JSON из чека
     */
    private String decodeBase64Json(byte[] base64Data) {
        return new String(Base64.getDecoder().decode(base64Data));
    }

    /**
     * Получить все чеки пользователя
     */
    public List<Receipt> getUserReceipts(UUID userId) {
        return receiptRepository.findByUserIdOrderByReceiptDateTimeDesc(userId);
    }

    /**
     * Получить чеки пользователя по номеру телефона
     */
    public List<Receipt> getUserReceiptsByPhone(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + phoneNumber));
        return receiptRepository.findByUserIdOrderByReceiptDateTimeDesc(user.getId());
    }
}