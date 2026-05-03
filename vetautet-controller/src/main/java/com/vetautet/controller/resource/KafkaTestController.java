package com.vetautet.controller.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class KafkaTestController {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping("/test-kafka")
    public String testKafka(@RequestParam(name = "message", defaultValue = "12345") String message) {
        try {
            // Thử convert message thành con số Long để gửi cho Consumer
            Long orderId = Long.parseLong(message);
            kafkaTemplate.send("order-created", orderId);
            System.out.println(">>> [KAFKA TEST] Sent Order ID: " + orderId);
            return "Đã gửi Order ID [" + orderId + "] vào Kafka. Hãy kiểm tra Consumer log!";
        } catch (NumberFormatException e) {
            return "Lỗi: Vui lòng nhập message là một con số (Ví dụ: 1001) để đúng kiểu Long!";
        }
    }
}
