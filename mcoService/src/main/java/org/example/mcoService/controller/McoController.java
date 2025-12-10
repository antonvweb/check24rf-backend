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

    @PostMapping("/register")
    public ResponseEntity<String> registerPartner(@RequestParam String logoPath) {
        String partnerId = mcoService.initializePartner(logoPath);
        return ResponseEntity.ok("Партнер зарегистрирован с ID: " + partnerId +
                "\n\nПроверьте: https://dr.stm-labs.ru/partners");
    }

    @PostMapping("/bind-user")
    public ResponseEntity<String> bindUser(@RequestParam String phone) {
        mcoService.connectUser(phone);
        return ResponseEntity.ok("Заявка на подключение отправлена");
    }

    @PostMapping("/bind-user-test")
    public ResponseEntity<String> bindUserTest() {
        String phone = "79054459061";

        String messageId = mcoService.connectUser(phone);

        return ResponseEntity.ok(
                "Заявка на подключение отправлена. MessageId: " + messageId +
                        "\n\nТеперь нужно одобрить заявку в ЛК МЧО:" +
                        "\nhttps://dr.stm-labs.ru/partners\n"
        );
    }

    @GetMapping("/sync-receipts")
    public ResponseEntity<String> syncReceipts() {
        mcoService.syncReceipts();
        return ResponseEntity.ok("Синхронизация запущена");
    }

    @GetMapping("/check-binding-result")
    public ResponseEntity<String> checkBindingResult(@RequestParam String messageId) {
        String result = mcoService.checkBindingResult(messageId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check-binding-status")
    public ResponseEntity<String> checkBindingStatus(@RequestParam String requestId) {
        String result = mcoService.checkBindingStatus(requestId);
        return ResponseEntity.ok(result);
    }

    // Комбинированная проверка
    @GetMapping("/full-check")
    public ResponseEntity<String> fullCheck(
            @RequestParam String messageId,
            @RequestParam String requestId
    ) {
        StringBuilder result = new StringBuilder();

        result.append("=== ПРОВЕРКА ПО MESSAGE ID ===\n");
        result.append(mcoService.checkBindingResult(messageId));
        result.append("\n\n=== ПРОВЕРКА ПО REQUEST ID ===\n");
        result.append(mcoService.checkBindingStatus(requestId));

        return ResponseEntity.ok(result.toString());
    }
}