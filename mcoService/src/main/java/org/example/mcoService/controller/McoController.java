package org.example.mcoService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.service.McoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/mco")
@RequiredArgsConstructor
public class McoController {

    @Autowired
    private McoService mcoService;

    /**
     * Регистрация партнера
     * POST /api/mco/register?logoPath=/path/to/logo.jpg
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerPartner(
            @RequestParam String logoPath) {

        String partnerId = mcoService.initializePartner(logoPath);
        return ResponseEntity.ok("Партнер зарегистрирован: " + partnerId);
    }

    /**
     * Подключение пользователя
     * POST /api/mco/bind-user?phone=79991234567
     */
    @PostMapping("/bind-user")
    public ResponseEntity<String> bindUser(@RequestParam String phone) {
        mcoService.connectUser(phone);
        return ResponseEntity.ok("Заявка на подключение отправлена");
    }

    /**
     * Синхронизация чеков
     * GET /api/mco/sync-receipts
     */
    @GetMapping("/sync-receipts")
    public ResponseEntity<String> syncReceipts() {
        mcoService.syncReceipts();
        return ResponseEntity.ok("Синхронизация запущена");
    }
}