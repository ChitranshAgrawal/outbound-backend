package com.addverb.outbound_service.auth.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank @Size(min = 4, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dot, underscore and hyphen")
        String username,

        @NotBlank @Email @Size(max = 100)
        String email,

        @NotBlank @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/]).{8,}$",
                message = "Password must include upper case, lower case, number and special character")
        String password,

        @NotBlank @Size(max = 60)
        String firstName,

        @NotBlank @Size(max = 60)
        String lastName,

        @NotBlank @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
        String phoneNumber
) {}



