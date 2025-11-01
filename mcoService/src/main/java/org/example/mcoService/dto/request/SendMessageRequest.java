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
@XmlRootElement(name = "SendMessageRequest",
        namespace = "urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class SendMessageRequest {

    @XmlElement(name = "Message", required = true)
    private MessageWrapper message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MessageWrapper {

        @XmlAnyElement(lax = true)
        private Object content;

    }
}