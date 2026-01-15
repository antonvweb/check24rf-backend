package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;

@Data
@XmlRootElement(name = "GetMessageResponse",
        namespace = "urn://x-artefacts-gnivc-ru/inplat/servin/OpenApiAsyncMessageConsumerService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetMessageResponse {

    @XmlElement(name = "Message")
    private MessageContent message;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MessageContent {

        @XmlAnyElement(lax = true)
        private Object content;
    }

    @XmlElement(name = "ProcessingStatus")
    private String processingStatus; // COMPLETED, PROCESSING, FAILED
}