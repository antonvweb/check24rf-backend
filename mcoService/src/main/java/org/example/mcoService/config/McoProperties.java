package org.example.mcoService.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mco")
public class McoProperties {

    private Api api;
    private Partner partner;

    public Api getApi() {
        return api;
    }

    public Partner getPartner() {
        return partner;
    }

    @Data
    public static class Api {
        private String baseUrl;
        private String token;
        private Integer timeout;

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getToken() {
            return token;
        }

        public Integer getTimeout() {
            return timeout;
        }
    }

    @Data
    public static class Partner {
        private String inn;
        private String name;
        private String type;

        public String getInn() {
            return inn;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }
}