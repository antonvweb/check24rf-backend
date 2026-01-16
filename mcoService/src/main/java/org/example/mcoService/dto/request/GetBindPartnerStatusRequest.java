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
@XmlRootElement(name = "GetBindPartnerStatusRequest", namespace = "http://kvv.su/dr/types")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"requestIds"})
public class GetBindPartnerStatusRequest {

    /**
     * Список идентификаторов заявок (PostBindPartnerRequest)
     * Не более 50 идентификаторов
     */
    @JsonProperty("RequestIds")
    @XmlElementWrapper(name = "RequestIds", namespace = "http://kvv.su/dr/types")
    @XmlElement(name = "string", namespace = "http://kvv.su/dr/types")
    private List<String> requestIds;
}
