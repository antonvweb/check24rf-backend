package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;
import java.time.LocalDateTime;

@Data
@XmlRootElement(name = "PostBindPartnerResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/smz/SmzIntegrationService/v0.1")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostBindPartnerResponse {

    @XmlElement(name = "requestId", required = true)
    private String requestId;  // Эхо из запроса, для идентификации заявки

    @XmlElement(name = "MessageId", required = true)
    private String messageId;  // Идентификатор для опроса статуса через GetBindPartnerEventRequest

    @XmlElement(name = "BindDate")
    private LocalDateTime bindDate;  // Опционально: дата отправки заявки (если API возвращает)

    // Если есть ошибки, это будет в отдельном элементе DrPlatformError (не в этом классе)
    // Пример: @XmlElement(name = "Error") private DrPlatformError error;
}