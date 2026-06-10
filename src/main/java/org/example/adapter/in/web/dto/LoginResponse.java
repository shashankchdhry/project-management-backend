package org.example.adapter.in.web.dto;

import java.util.UUID;

public record LoginResponse(String token, UUID userId, String displayName) {
}
