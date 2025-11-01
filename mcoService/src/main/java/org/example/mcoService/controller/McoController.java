package org.example.mcoService.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mcoService.client.McoApiClient;
import org.example.mcoService.dto.response.SendMessageResponse;
import org.example.mcoService.service.McoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/mco")
@RequiredArgsConstructor
public class McoController {

    @Autowired
    private McoService mcoService;
    @Autowired
    private McoApiClient mcoApiClient;

    @PostMapping("/register")
    public ResponseEntity<String> registerPartner(@RequestParam String logoPath) {
        String messageId = mcoService.initializePartner(logoPath);
        return ResponseEntity.ok("Партнер зарегистрирован, MessageId: " + messageId);
    }

    @PostMapping("/bind-user")
    public ResponseEntity<String> bindUser(@RequestParam String phone) {
        mcoService.connectUser(phone);
        return ResponseEntity.ok("Заявка на подключение отправлена");
    }

    @PostMapping("/bind-user-test")
    public ResponseEntity<String> bindUserTest() {
        String phone = "79054459061"; // Ваш номер
        String requestId = UUID.randomUUID().toString();

        SendMessageResponse response = mcoApiClient.bindUser(phone, requestId);

        return ResponseEntity.ok(
                "Заявка на подключение отправлена. MessageId: " + response.getMessageId() +
                        "\nRequestId: " + requestId +
                        "\nТеперь нужно одобрить заявку в ЛК МЧО: https://dr.stm-labs.ru/partners"
        );
    }

    @GetMapping("/sync-receipts")
    public ResponseEntity<String> syncReceipts() {
        mcoService.syncReceipts();
        return ResponseEntity.ok("Синхронизация запущена");
    }
}