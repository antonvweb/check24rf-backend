package org.example.userService.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userService.dto.*;
import org.example.userService.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API контроллер для управления пользователями
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ============ CRUD операции ============

    /**
     * Создать нового пользователя
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserDetailResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        
        try {
            log.info("📝 Создание нового пользователя: {}", request.getPhoneNumber());
            UserDetailResponse user = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Пользователь успешно создан", user));
        } catch (IllegalArgumentException e) {
            log.error("❌ Ошибка создания пользователя: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Непредвиденная ошибка при создании пользователя", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Получить информацию о пользователе по ID
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(
            @PathVariable UUID userId) {
        
        try {
            log.info("📖 Запрос информации о пользователе: {}", userId);
            UserDetailResponse user = userService.getUserDetail(userId);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            log.error("❌ Ошибка получения пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Пользователь не найден"));
        }
    }

    /**
     * Получить информацию о текущем пользователе (по токену)
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Отсутствует токен авторизации"));
            }
            
            String token = authHeader.replace("Bearer ", "");
            UserDetailResponse user = userService.getUserDetailByToken(token);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            log.error("❌ Ошибка получения информации о текущем пользователе: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Некорректный токен авторизации"));
        }
    }

    /**
     * Получить список всех пользователей с пагинацией
     * GET /api/users?page=0&size=20&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserListResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            log.info("📋 Запрос списка пользователей: page={}, size={}", page, size);
            UserListResponse users = userService.getAllUsers(page, size, sortBy, sortDir);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("❌ Ошибка получения списка пользователей: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка получения списка пользователей"));
        }
    }

    /**
     * Поиск пользователей по номеру телефона или email
     * GET /api/users/search?query=79054455906
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserListResponse.UserSummary>>> searchUsers(
            @RequestParam String query) {
        
        try {
            log.info("🔍 Поиск пользователей: {}", query);
            List<UserListResponse.UserSummary> users = userService.searchUsers(query);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("❌ Ошибка поиска пользователей: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Ошибка поиска пользователей"));
        }
    }

    /**
     * Обновить информацию о пользователе
     * PUT /api/users/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
           @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        
        try {
            log.info("✏️ Обновление информации о пользователе: {}", userId);
            UserDetailResponse user = userService.updateUser(userId, request);
            return ResponseEntity.ok(ApiResponse.success("Информация о пользователе обновлена", user));
        } catch (Exception e) {
            log.error("❌ Ошибка обновления пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Пользователь не найден"));
        }
    }

    /**
     * Деактивировать пользователя (мягкое удаление)
     * PATCH /api/users/{userId}/deactivate
     */
    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
           @PathVariable UUID userId) {
        
        try {
            log.info("⚠️ Деактивация пользователя: {}", userId);
            userService.deactivateUser(userId);
            return ResponseEntity.ok(ApiResponse.success("Пользователь деактивирован", null));
        } catch (Exception e) {
            log.error("❌ Ошибка деактивации пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Пользователь не найден"));
        }
    }

    /**
     * Активировать пользователя
     * PATCH /api/users/{userId}/activate
     */
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @PathVariable UUID userId) {
        
        try {
            log.info("✅ Активация пользователя: {}", userId);
            userService.activateUser(userId);
            return ResponseEntity.ok(ApiResponse.success("Пользователь активирован", null));
        } catch (Exception e) {
            log.error("❌ Ошибка активации пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Пользователь не найден"));
        }
    }

    /**
     * Удалить пользователя полностью (жесткое удаление)
     * DELETE /api/users/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId) {
        
        try {
            log.info("🗑️ Удаление пользователя: {}", userId);
            userService.deleteUser(userId);
            return ResponseEntity.ok(ApiResponse.success("Пользователь удален", null));
        } catch (Exception e) {
            log.error("❌ Ошибка удаления пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Пользователь не найден"));
        }
    }

    // ============ Работа с чеками ============

    /**
     * Получить чеки пользователя
     * GET /api/users/{userId}/receipts?page=0&size=20
     */
    @GetMapping("/{userId}/receipts")
    public ResponseEntity<ApiResponse<UserReceiptsResponse>> getUserReceipts(
            @PathVariable UUID userId,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            log.info("🧾 Запрос чеков пользователя: {}, page={}, size={}", userId, page, size);
            UserReceiptsResponse receipts = userService.getUserReceipts(userId, page, size);
            return ResponseEntity.ok(ApiResponse.success(receipts));
        } catch (Exception e) {
            log.error("❌ Ошибка получения чеков пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Пользователь не найден"));
        }
    }

    // ============ Старые методы для обратной совместимости ============

    /**
     * Проверить активность пользователя (старый метод)
     * GET /api/users/is-active
     */
    @GetMapping("/is-active")
    public ResponseEntity<Map<String, Boolean>> getUserIsActive(
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.replace("Bearer ", "");
        boolean isActive = userService.getUserIsActive(token);
        return ResponseEntity.ok(Map.of("isActive", isActive));
    }

    /**
     * Получить базовую информацию о пользователе (старый метод)
     * GET /api/users/user
     */
    @GetMapping("/user")
    public ResponseEntity<UserResponse> getUser(
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.replace("Bearer ", "");
        UserResponse user = userService.getUser(token);
        return ResponseEntity.ok(user);
    }

    /**
     * Изменить альтернативные данные (старый метод)
     * POST /api/users/change-data
     */
    @PostMapping("/change-data")
    public ResponseEntity<ChangeAltDataResponse> changeData(
            @Valid @RequestBody ChangeAltDataRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ChangeAltDataResponse(false, "Отсутствует токен авторизации"));
            }

            String token = authHeader.replace("Bearer ", "");
            boolean isChanged = userService.changeAltData(request.getType(), request.getData(), token);

            if (isChanged) {
                return ResponseEntity.ok(new ChangeAltDataResponse(
                        true,
                        "Данные успешно изменены",
                        request.getType(),
                        request.getData()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ChangeAltDataResponse(false, "Не удалось изменить данные"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ChangeAltDataResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChangeAltDataResponse(false, "Внутренняя ошибка сервера"));
        }
    }
}
