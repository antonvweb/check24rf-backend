package org.example.authService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ с CSRF токеном
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsrfTokenResponse {
    private String token;
    private String headerName;
}
