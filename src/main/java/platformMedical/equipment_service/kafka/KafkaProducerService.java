package platformMedical.equipment_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@AllArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendMessage(String topic, Object payload) {
        try {
            System.out.println(payload);
            // Sérialisation de l'objet payload en JSON
            String payloadJson = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, payloadJson);
            log.info("Message envoyé au topic {}: {}", topic, payloadJson);
        } catch (Exception e) {
            log.error("Erreur lors de la conversion en JSON de l'objet {}: {}", payload.getClass().getName(), e.getMessage());
        }
    }
}

