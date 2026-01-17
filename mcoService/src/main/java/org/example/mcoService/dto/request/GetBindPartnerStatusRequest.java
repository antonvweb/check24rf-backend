package org.example.mcoService.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Запрос на получение статуса заявки на подключение Покупателя к Партнёру
 * Протокол 3.2.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetBindPartnerStatusRequest", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"requestIds"})
public class GetBindPartnerStatusRequest {

    /**
     * Список идентификаторов заявок (PostBindPartnerRequest)
     * Не более 50 идентификаторов
     *
     * ВАЖНО: НЕ использовать @XmlElementWrapper!
     * Элемент RequestIds должен повторяться для каждого значения:
     * <RequestIds>value1</RequestIds>
     * <RequestIds>value2</RequestIds>
     */
    @JsonProperty("RequestIds")
    @XmlElement(name = "RequestIds", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<String> requestIds;
}