package org.example.mcoService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.xml.bind.annotation.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetMessageRequest",
        namespace = "urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetMessageRequest {

    @XmlElement(name = "MessageId", required = true)
    private String messageId;
}