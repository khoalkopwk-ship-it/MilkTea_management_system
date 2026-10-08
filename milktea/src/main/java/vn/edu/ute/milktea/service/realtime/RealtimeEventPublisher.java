package vn.edu.ute.milktea.service.realtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealtimeEnvelope {
        private String eventId;
        private String type;
        private Instant occurredAt;
        private String resourceId;
        private String sessionId;
        private String version;
        private Object payload;
    }

    public void publishAfterCommit(String destination, String type, String resourceId, String sessionId, String version, Object payload) {
        RealtimeEnvelope event = RealtimeEnvelope.builder()
                .eventId(UUID.randomUUID().toString())
                .type(type)
                .occurredAt(Instant.now())
                .resourceId(resourceId != null ? resourceId : "")
                .sessionId(sessionId)
                .version(version != null ? version : "1")
                .payload(payload)
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        messagingTemplate.convertAndSend(destination, event);
                        log.info("Đã phát sự kiện realtime AFTER_COMMIT tới [{}]: type={}, resourceId={}", destination, type, resourceId);
                    } catch (Exception e) {
                        log.warn("Không thể phát sự kiện realtime tới [{}]: {}", destination, e.getMessage());
                    }
                }
            });
        } else {
            try {
                messagingTemplate.convertAndSend(destination, event);
                log.info("Đã phát sự kiện realtime trực tiếp tới [{}]: type={}, resourceId={}", destination, type, resourceId);
            } catch (Exception e) {
                log.warn("Không thể phát sự kiện realtime tới [{}]: {}", destination, e.getMessage());
            }
        }
    }
}
