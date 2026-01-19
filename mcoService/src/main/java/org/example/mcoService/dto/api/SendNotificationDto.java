package org.example.mcoService.dto.api;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationDto {

    private String phoneNumber;
    private String title;
    private String message;
    private String shortMessage;
    private String category; // GENERAL или CASHBACK
    private String externalItemId;
    private String externalItemUrl;
}