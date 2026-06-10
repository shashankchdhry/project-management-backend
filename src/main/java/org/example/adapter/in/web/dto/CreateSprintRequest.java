package org.example.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSprintRequest(
        @NotBlank String projectKey,
        @NotBlank String name,
        String goal) {
}
