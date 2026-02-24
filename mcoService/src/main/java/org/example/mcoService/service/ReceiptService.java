package org.example.mcoService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.dto.api.ReceiptDto;
import org.example.mcoService.dto.api.SaveReceiptsResult;
import org.example.mcoService.dto.response.GetReceiptsTapeResponse;
import org.example.mcoService.entity.Receipt;
import org.example.common.entity.User;
import org.example.mcoService.repository.ReceiptRepository;
import org.example.common.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SaveReceiptsResult saveReceipts(List<GetReceiptsTapeResponse.Receipt> receiptsFromMco) {
        int savedCount = 0;
        BigDecimal totalSum = BigDecimal.ZERO;

        for (GetReceiptsTapeResponse.Receipt mcoReceipt : receiptsFromMco) {
            try {
                String jsonString = bytesToString(mcoReceipt.getJson());
                JsonNode jsonNode = objectMapper.readTree(jsonString);

                Long fiscalSign = jsonNode.get("fiscalSign").asLong();
                Long fiscalDocumentNumber = jsonNode.get("fiscalDocumentNumber").asLong();
                String fiscalDriveNumber = jsonNode.get("fiscalDriveNumber").asText();

                if (receiptRepository.existsByFiscalSignAndFiscalDocumentNumberAndFiscalDriveNumber(
                        fiscalSign, fiscalDocumentNumber, fiscalDriveNumber)) {
                    log.debug("Чек уже существует: fiscalSign={}, fiscalDocumentNumber={}, fiscalDriveNumber={}",
                            fiscalSign, fiscalDocumentNumber, fiscalDriveNumber);
                    continue;
                }

                User user = findOrCreateUser(mcoReceipt.getUserIdentifier(), mcoReceipt.getEmail());

                Receipt receipt = buildReceiptEntity(mcoReceipt, jsonNode, jsonString, user.getId());

                receiptRepository.save(receipt);
                savedCount++;
                totalSum = totalSum.add(receipt.getTotalSum());

                log.info("Сохранен чек: fiscalSign={}, user={}, sum={}",
                        fiscalSign, user.getPhoneNumber(), receipt.getTotalSum());

            } catch (Exception e) {
                log.error("Ошибка сохранения чека для пользователя {}: {}",
                        mcoReceipt.getUserIdentifier(), e.getMessage(), e);
            }
        }

        log.info("Сохранено {} новых чеков из {} на сумму {}", savedCount, receiptsFromMco.size(), totalSum);
        return new SaveReceiptsResult(savedCount, totalSum);
    }

    @Transactional
    public SaveReceiptsResult syncUserReceipts(String phoneNumber, List<GetReceiptsTapeResponse.Receipt> receiptsFromMco) {
        log.info("Синхронизация чеков для пользователя: {}", phoneNumber);

        List<GetReceiptsTapeResponse.Receipt> userReceipts = receiptsFromMco.stream()
                .filter(r -> phoneNumber.equals(r.getUserIdentifier()) || phoneNumber.equals(r.getPhone()))
                .toList();

        if (userReceipts.isEmpty()) {
            log.info("Нет новых чеков для пользователя {}", phoneNumber);
            return SaveReceiptsResult.empty();
        }

        return saveReceipts(userReceipts);
    }

    private User findOrCreateUser(String phoneNumber, String email) {
        return userRepository.findByPhoneNumberNormalized(phoneNumber)
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

    private Receipt buildReceiptEntity(GetReceiptsTapeResponse.Receipt mcoReceipt,
                                       JsonNode jsonNode,
                                       String jsonString,
                                       UUID userId) {
        long dateTimestamp = jsonNode.get("dateTime").asLong();
        LocalDateTime receiptDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(dateTimestamp),
                ZoneId.systemDefault()
        );

        LocalDateTime receiveDate = LocalDateTime.parse(
                mcoReceipt.getReceiveDate().replace("Z", "")
        );

        BigDecimal totalSum = BigDecimal.valueOf(jsonNode.get("totalSum").asLong())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

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

    private String bytesToString(byte[] jsonBytes) {
        return new String(jsonBytes, StandardCharsets.UTF_8);
    }

    public Page<ReceiptDto> getUserReceiptsByPhone(String phoneNumber, Pageable pageable) {
        User user = userRepository.findByPhoneNumberNormalized(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + phoneNumber));

        Page<Receipt> page = receiptRepository.findByUserIdOrderByReceiptDateTimeDesc(user.getId(), pageable);

        return page.map(this::toDto);
    }

    private ReceiptDto toDto(Receipt receipt) {
        return new ReceiptDto(
                receipt.getPhone(),
                receipt.getEmail(),
                receipt.getFiscalSign(),
                receipt.getFiscalDocumentNumber(),
                receipt.getFiscalDriveNumber(),
                receipt.getReceiptDateTime(),
                receipt.getReceiveDate(),
                receipt.getTotalSum(),
                receipt.getSourceCode(),
                receipt.getOperationType(),
                receipt.getUserInn(),
                receipt.getRetailPlace(),
                receipt.getRawJson()
        );
    }
}