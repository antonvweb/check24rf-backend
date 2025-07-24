package org.example.authService.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {
    public void sendSms(String phone, String message) {
        System.out.printf("📲 Sms for number phone %s: %s%n", phone, message);
    }
}
