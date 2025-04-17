package platformMedical.equipment_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;


@Document(collection = "equipmentTransfertHistory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentTransferHistory {
    @Id
    private String id;

    private String equipmentId;
    private String oldHospitalId;
    private String newHospitalId;
    private String oldServiceId;
    private String newServiceId;
    private String type; // "INTER_SERVICE" ou "INTER_HOSPITAL"
    private String description;

    private String initiatedByUserId;
    private String initiatedByName;

    private LocalDateTime createdAt = LocalDateTime.now();
}
