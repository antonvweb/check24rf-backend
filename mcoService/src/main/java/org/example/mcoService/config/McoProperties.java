package org.example.mcoService.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mco")
public class McoProperties {

    private Api api;
    private Partner partner;

    @Data
    public static class Api {
        private String baseUrl;
        private String token;
        private Integer timeout;

    }

    @Data
    public static class Partner {
        private String inn;
        private String name;
        private String type;

    }
}