package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "GetReceiptsTapeResponse",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetReceiptsTapeResponse {

    @XmlElement(name = "NextMarker",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private String nextMarker;

    @XmlElement(name = "TotalExpectedRemainingPolls",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private Long totalExpectedRemainingPolls;

    @XmlElement(name = "Receipts",
            namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<Receipt> receipts;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Receipt {

        @XmlElement(name = "UserIdentifier",
                namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String userIdentifier;

        @XmlElement(name = "Phone",
                namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String phone;

        @XmlElement(name = "Email",
                namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String email;

        @XmlElement(name = "Json",
                namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private byte[] json; // Base64 encoded JSON

        @XmlElement(name = "ReceiveDate",
                namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String receiveDate;

        @XmlElement(name = "SourceCode",
                namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
        private String sourceCode; // KKT_RECEIPT, SCAN_MPPCH, SCAN_LKDR, SCAN_PARTNER
    }
}