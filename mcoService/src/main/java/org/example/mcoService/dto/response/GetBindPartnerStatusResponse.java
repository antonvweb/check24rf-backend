package org.example.mcoService.dto.response;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@XmlRootElement(name = "GetBindPartnerStatusResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetBindPartnerStatusResponse {

    @XmlElement(name = "Statuses")
    private List<Status> statuses;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Status {
        @XmlElement(name = "RequestId")
        private String requestId;

        @XmlElement(name = "Result")
        private String result;

        @XmlElement(name = "PermissionGroups")
        private List<String> permissionGroups;

        @XmlElement(name = "ResponseTime")
        private LocalDateTime responseTime;

        @XmlElement(name = "UserIdentifier")
        private String userIdentifier;
    }
}