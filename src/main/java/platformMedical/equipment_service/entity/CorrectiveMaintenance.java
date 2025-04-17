package platformMedical.equipment_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;
@Document(collection = "corrective_maintenances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectiveMaintenance {

    @Id
    private String id;

    private String equipmentId;
    private String incidentId; // Pour traçabilité
    private String assignedTo; // ID utilisateur (société de maintenance)
    private String description;
    private LocalDateTime plannedDate;    // Date prévue d'intervention
    private LocalDateTime completedDate;  // Date réelle de fin
    private String status;       // "Planifiée", "En cours", "Terminée"
    private  String resolutionDetails;
}
