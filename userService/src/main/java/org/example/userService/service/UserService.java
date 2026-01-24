package org.example.userService.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.entity.User;
import org.example.common.repository.UserRepository;
import org.example.common.security.JwtUtil;
import org.example.userService.dto.*;
import org.example.userService.dto.UserListResponse.PaginationInfo;
import org.example.userService.dto.UserListResponse.UserSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    
    @Value("${mco.service.url:http://localhost:8085}")
    private String mcoServiceUrl;

    /**
     * Получить полную информацию о пользователе по токену (включая чеки и статистику)
     */
    public UserDetailResponse getUserDetailByToken(String token) {
        UUID userId = extractUserIdFromToken(token);
        return getUserDetail(userId);
    }

    /**
     * Получить полную информацию о пользователе по ID
     */
    public UserDetailResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
        
        return UserDetailResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .phoneNumberAlt(user.getPhoneNumberAlt())
                .email(user.getEmail())
                .emailAlt(user.getEmailAlt())
                .telegramChatId(user.getTelegramChatId())
                .createdAt(user.getCreatedAt())
                .isActive(user.isActive())
                .isPartnerConnected(user.isPartnerConnected())
                .build();
    }

    /**
     * Получить список всех пользователей с пагинацией
     */
    public UserListResponse getAllUsers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> usersPage = userRepository.findAll(pageable);
        
        List<UserSummary> userSummaries = usersPage.getContent().stream()
                .map(user -> {
                    long receiptsCount = getReceiptsCount(user.getId());
                    return UserSummary.builder()
                            .id(user.getId())
                            .phoneNumber(user.getPhoneNumber())
                            .email(user.getEmail())
                            .createdAt(user.getCreatedAt())
                            .isActive(user.isActive())
                            .receiptsCount(receiptsCount)
                            .build();
                })
                .collect(Collectors.toList());
        
        PaginationInfo paginationInfo = PaginationInfo.builder()
                .currentPage(usersPage.getNumber())
                .pageSize(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .hasNext(usersPage.hasNext())
                .hasPrevious(usersPage.hasPrevious())
                .build();
        
        return UserListResponse.builder()
                .users(userSummaries)
                .pagination(paginationInfo)
                .build();
    }

    /**
     * Поиск пользователей по номеру телефона или email
     */
    public List<UserSummary> searchUsers(String query) {
        List<User> users = userRepository.findAll().stream()
                .filter(user -> 
                    (user.getPhoneNumber() != null && user.getPhoneNumber().contains(query)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(query.toLowerCase())) ||
                    (user.getPhoneNumberAlt() != null && user.getPhoneNumberAlt().contains(query)) ||
                    (user.getEmailAlt() != null && user.getEmailAlt().toLowerCase().contains(query.toLowerCase()))
                )
                .toList();
        
        return users.stream()
                .map(user -> UserSummary.builder()
                        .id(user.getId())
                        .phoneNumber(user.getPhoneNumber())
                        .email(user.getEmail())
                        .createdAt(user.getCreatedAt())
                        .isActive(user.isActive())
                        .receiptsCount(getReceiptsCount(user.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Создать нового пользователя
     */
    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        // Проверяем, существует ли пользователь с таким номером
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с номером " + request.getPhoneNumber() + " уже существует");
        }
        
        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .phoneNumberAlt(request.getPhoneNumberAlt())
                .email(request.getEmail())
                .emailAlt(request.getEmailAlt())
                .telegramChatId(request.getTelegramChatId())
                .createdAt(LocalDateTime.now())
                .isActive(request.isActive())
                .build();
        
        user = userRepository.save(user);
        log.info("✅ Создан новый пользователь: {}", user.getPhoneNumber());
        
        return getUserDetail(user.getId());
    }

    /**
     * Обновить информацию о пользователе
     */
    @Transactional
    public UserDetailResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
        
        if (request.getPhoneNumberAlt() != null) {
            user.setPhoneNumberAlt(request.getPhoneNumberAlt());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getEmailAlt() != null) {
            user.setEmailAlt(request.getEmailAlt());
        }
        if (request.getTelegramChatId() != null) {
            user.setTelegramChatId(request.getTelegramChatId());
        }
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }
        
        userRepository.save(user);
        log.info("✅ Обновлена информация о пользователе: {}", userId);
        
        return getUserDetail(userId);
    }

    /**
     * Удалить пользователя (мягкое удаление - деактивация)
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
        
        user.setActive(false);
        userRepository.save(user);
        log.info("⚠️ Пользователь деактивирован: {}", userId);
    }

    /**
     * Удалить пользователя полностью (жесткое удаление)
     */
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Пользователь не найден: " + userId);
        }
        
        userRepository.deleteById(userId);
        log.info("🗑️ Пользователь удален: {}", userId);
    }

    /**
     * Активировать пользователя
     */
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
        
        user.setActive(true);
        userRepository.save(user);
        log.info("✅ Пользователь активирован: {}", userId);
    }

    /**
     * Получить чеки пользователя с пагинацией
     */
    public UserReceiptsResponse getUserReceipts(UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
        
        List<ReceiptData> allReceipts = fetchUserReceipts(userId);
        
        // Пагинация в памяти (можно улучшить, делая запрос с пагинацией в mcoService)
        int start = page * size;
        int end = Math.min(start + size, allReceipts.size());
        
        List<UserReceiptsResponse.ReceiptDetail> receipts = allReceipts.stream()
                .skip(start)
                .limit(size)
                .map(this::mapToReceiptDetail)
                .collect(Collectors.toList());
        
        // Расчет статистики
        BigDecimal totalAmount = allReceipts.stream()
                .map(r -> r.totalSum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageAmount = allReceipts.isEmpty() 
                ? BigDecimal.ZERO 
                : totalAmount.divide(BigDecimal.valueOf(allReceipts.size()), 2, RoundingMode.HALF_UP);
        
        LocalDateTime oldestDate = allReceipts.stream()
                .map(r -> r.receiptDateTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        
        LocalDateTime newestDate = allReceipts.stream()
                .map(r -> r.receiptDateTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        UserReceiptsResponse.ReceiptsSummary summary = UserReceiptsResponse.ReceiptsSummary.builder()
                .totalCount(allReceipts.size())
                .totalAmount(totalAmount)
                .averageAmount(averageAmount)
                .oldestReceiptDate(oldestDate)
                .newestReceiptDate(newestDate)
                .build();
        
        int totalPages = (int) Math.ceil((double) allReceipts.size() / size);
        
        UserReceiptsResponse.PaginationInfo pagination = UserReceiptsResponse.PaginationInfo.builder()
                .currentPage(page)
                .pageSize(size)
                .totalElements(allReceipts.size())
                .totalPages(totalPages)
                .hasNext(end < allReceipts.size())
                .hasPrevious(page > 0)
                .build();
        
        return UserReceiptsResponse.builder()
                .userId(userId)
                .phoneNumber(user.getPhoneNumber())
                .receipts(receipts)
                .pagination(pagination)
                .summary(summary)
                .build();
    }

    // ============ Вспомогательные методы ============

    /**
     * Извлечь userId из JWT токена
     */
    private UUID extractUserIdFromToken(String token) {
        String userIdStr = jwtUtil.getUserId(token)
                .orElseThrow(() -> new IllegalArgumentException("Некорректный токен"));
        return UUID.fromString(userIdStr);
    }

    /**
     * Получить чеки пользователя из mcoService
     */
    private List<ReceiptData> fetchUserReceipts(UUID userId) {
        try {
            String url = mcoServiceUrl + "/api/mco/receipts/user/" + userId;
            ReceiptData[] receipts = restTemplate.getForObject(url, ReceiptData[].class);
            return receipts != null ? Arrays.asList(receipts) : Collections.emptyList();
        } catch (Exception e) {
            log.warn("⚠️ Не удалось получить чеки для пользователя {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Получить количество чеков пользователя
     */
    private long getReceiptsCount(UUID userId) {
        try {
            String url = mcoServiceUrl + "/api/mco/receipts/user/" + userId + "/count";
            Long count = restTemplate.getForObject(url, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private UserReceiptsResponse.ReceiptDetail mapToReceiptDetail(ReceiptData data) {
        return UserReceiptsResponse.ReceiptDetail.builder()
                .id(data.id)
                .fiscalDriveNumber(data.fiscalDriveNumber)
                .fiscalDocumentNumber(data.fiscalDocumentNumber)
                .fiscalSign(data.fiscalSign)
                .receiptDateTime(data.receiptDateTime)
                .receiveDate(data.receiveDate)
                .totalSum(data.totalSum)
                .sourceCode(data.sourceCode)
                .retailPlace(data.retailPlace)
                .userInn(data.userInn)
                .operationType(data.operationType)
                .build();
    }

    /**
     * Внутренний класс для маппинга данных чека
     */
    @lombok.Data
    private static class ReceiptData {
        private UUID id;
        private String fiscalDriveNumber;
        private Long fiscalDocumentNumber;
        private Long fiscalSign;
        private LocalDateTime receiptDateTime;
        private LocalDateTime receiveDate;
        private BigDecimal totalSum;
        private String sourceCode;
        private String retailPlace;
        private String userInn;
        private Integer operationType;
    }

    // Старые методы для обратной совместимости
    
    public boolean getUserIsActive(String token) {
        UUID userId = extractUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
        return user.isActive();
    }

    public UserResponse getUser(String token) {
        UUID userId = extractUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
        return fromUser(user);
    }

    public UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.phoneNumber = user.getPhoneNumber();
        response.phoneNumberAlt = user.getPhoneNumberAlt();
        response.email = user.getEmail();
        response.emailAlt = user.getEmailAlt();
        response.telegramChatId = user.getTelegramChatId();
        response.createdAt = user.getCreatedAt().toString();
        response.isActive = user.isActive();
        return response;
    }

    public boolean changeAltData(String type, String data, String token) {
        UUID userId = extractUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        switch (type) {
            case "phone":
                user.setPhoneNumberAlt(data);
                userRepository.save(user);
                return true;
            case "email":
                user.setEmailAlt(data);
                userRepository.save(user);
                return true;
            default:
                return false;
        }
    }
}
