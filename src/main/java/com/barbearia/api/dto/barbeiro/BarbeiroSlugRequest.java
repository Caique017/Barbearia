package com.barbearia.api.dto.barbeiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BarbeiroSlugRequest(
        @NotBlank
        @Size(max = 110)
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                message = "Use apenas letras minúsculas, números e hífen entre palavras.")
        String slug
) {
}
