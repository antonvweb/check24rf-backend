package org.example.mcoService.dto.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnbindUserResponse {

    private String phoneNumber;
    private String status;
    private LocalDateTime unboundAt;
    private String message;
}