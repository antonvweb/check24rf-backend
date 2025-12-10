package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;

@Data
@XmlRootElement(name = "SendMessageResponse",
        namespace = "urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class SendMessageResponse {

    @XmlElement(name = "MessageId")  // Добавлен namespace
    private String messageId;
}