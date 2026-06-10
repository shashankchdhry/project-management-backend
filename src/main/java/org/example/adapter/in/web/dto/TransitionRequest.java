package org.example.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/** @param to the target status name, e.g. {@code "In Progress"} */
public record TransitionRequest(@NotBlank String to) {
}
