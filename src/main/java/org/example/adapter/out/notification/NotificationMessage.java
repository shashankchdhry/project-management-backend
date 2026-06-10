package org.example.adapter.out.notification;

import java.util.UUID;

public record NotificationMessage(UUID recipientId, String type, UUID issueId, String payloadJson) {
}
