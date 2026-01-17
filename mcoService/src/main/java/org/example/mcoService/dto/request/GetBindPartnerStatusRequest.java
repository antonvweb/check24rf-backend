package org.example.mcoService.dto.request;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Запрос на получение статуса заявок на подключение пользователей
 * Протокол 3.2.2
 * ВАЖНО: MCO API ожидает повторяющиеся элементы <RequestIds>, а не wrapper!
 * Правильная структура:
 * <GetBindPartnerStatusRequest>
 *   <RequestIds>id1</RequestIds>
 *   <RequestIds>id2</RequestIds>
 * </GetBindPartnerStatusRequest>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "GetBindPartnerStatusRequest", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetBindPartnerStatusRequest {

    /**
     * Список идентификаторов заявок (до 50 штук)
     * КРИТИЧНО: Используем только @XmlElement БЕЗ @XmlElementWrapper!
     * Это создает повторяющиеся элементы <RequestIds>, что требует MCO API.
     */
    @XmlElement(name = "RequestIds", namespace = "urn://x-artefacts-gnivc-ru/ais3/DR/DrPartnersIntegrationService/types/1.0")
    private List<String> requestIds;
}