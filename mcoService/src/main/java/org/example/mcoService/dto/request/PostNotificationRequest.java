package org.example.mcoService.dto.request;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostNotificationRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostNotificationRequest {

    /**
     * Уникальный идентификатор для дедубликации
     * Если отправить 2 раза с одним RequestId - пользователь получит только 1 уведомление
     */
    @XmlElement(name = "RequestId", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String requestId;

    /**
     * Телефон пользователя (79998887766)
     */
    @XmlElement(name = "UserIdentifier", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String userIdentifier;

    /**
     * Заголовок уведомления (до 255 символов)
     */
    @XmlElement(name = "NotificationTitle", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String notificationTitle;

    /**
     * Текст уведомления в Markdown формате (до 2048 символов)
     */
    @XmlElement(name = "NotificationMessage", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String notificationMessage;

    /**
     * Короткое сообщение для PUSH-уведомлений (до 255 символов)
     */
    @XmlElement(name = "ShortMessage", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String shortMessage;

    /**
     * Категория уведомления: GENERAL или CASHBACK
     */
    @XmlElement(name = "NotificationCategory", required = true,
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String notificationCategory;

    /**
     * Внутренний ID купона/промокода (необязательно)
     */
    @XmlElement(name = "ExternalItemId",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String externalItemId;

    /**
     * Ссылка на страницу активации купона/промокода (необязательно)
     * Только HTTPS!
     */
    @XmlElement(name = "ExternalItemUrl",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String externalItemUrl;
}