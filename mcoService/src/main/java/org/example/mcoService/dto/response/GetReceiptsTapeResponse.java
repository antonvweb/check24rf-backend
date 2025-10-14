package org.example.mcoService.dto.response;

import lombok.Data;
import jakarta.xml.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@XmlRootElement(name = "PostPlatformRegistrationRequest",
        namespace = "urn://x-artefacts-gnivc-ru/ais3/smz/SmzIntegrationService/v0.1")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetReceiptsTapeResponse {

    @XmlElement(name = "NextMarker")
    private String nextMarker;

    @XmlElement(name = "TotalExpectedRemainingPolls")
    private Long totalExpectedRemainingPolls;

    @XmlElementWrapper(name = "Receipts")
    @XmlElement(name = "Receipt")
    private List<Receipt> receipts;

    public List<Receipt> getReceipts() {
        return receipts;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Receipt {

        @XmlElement(name = "UserIdentifier")
        private String userIdentifier;

        @XmlElement(name = "Phone")
        private String phone;

        @XmlElement(name = "Email")
        private String email;

        @XmlElement(name = "Json")
        private byte[] json; // Base64 encoded JSON

        @XmlElement(name = "ReceiveDate")
        private LocalDateTime receiveDate;

        @XmlElement(name = "SourceCode")
        private String sourceCode; // KKT_RECEIPT, SCAN_MPPCH, SCAN_LKDR, SCAN_PARTNER
    }
}