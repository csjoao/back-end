package com.hyperativa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {
    @NotBlank(message = "Card number is required")
    @Pattern(
        regexp = "^[0-9]{13,19}$",
        message = "Card number must contain only digits and have between 13 and 19 characters"
    )
    private String cardNumber;
}
