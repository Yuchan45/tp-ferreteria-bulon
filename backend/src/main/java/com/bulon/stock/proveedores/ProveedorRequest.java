package com.bulon.stock.proveedores;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank @Size(max = 150) String razonSocial,
        @Pattern(regexp = "\\d{2}-\\d{8}-\\d", message = "debe tener el formato 30-12345678-9") String cuit,
        @Size(max = 255) String telefono,
        @Email @Size(max = 255) String email
) {
}
