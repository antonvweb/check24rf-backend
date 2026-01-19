package org.example.mcoService.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "PostBindPartnerBatchResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class PostBindPartnerBatchResponse {

    @JsonProperty("RequestId")
    @XmlElement(name = "RequestId", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String requestId;

    @JsonProperty("AcceptedUserIdentifiers")
    @XmlElement(name = "AcceptedUserIdentifiers", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<String> acceptedUserIdentifiers;

    @JsonProperty("RejectedUserIdentifiers")
    @XmlElement(name = "RejectedUserIdentifiers", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<RejectedUser> rejectedUserIdentifiers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RejectedUser {

        @JsonProperty("UserIdentifier")
        @XmlElement(name = "UserIdentifier", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String userIdentifier;

        @JsonProperty("RejectionReasonCode")
        @XmlElement(name = "RejectionReasonCode", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String rejectionReasonCode;

        @JsonProperty("RejectionReasonMessage")
        @XmlElement(name = "RejectionReasonMessage", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String rejectionReasonMessage;
    }
}