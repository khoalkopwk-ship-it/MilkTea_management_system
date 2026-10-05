package vn.edu.ute.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.edu.ute.enums.order.OrderStatus;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendOrderStatus(
            Long orderId,
            OrderStatus status
    ) {

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("orderId", orderId);
        data.put("status", status.name());

        String destination =
                "/topic/orders/" + orderId;

        messagingTemplate.convertAndSend(
                destination,
                (Object) data
        );
    }
}