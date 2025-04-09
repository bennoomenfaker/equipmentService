package platformMedical.equipment_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "service_level_agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SLA {

    @Id
    private String id;
    private String equipmentId; // ID de l'équipement associé
    private String hospitalId; // ID de l'hôpital propriétaire de l'équipement
    private String name;
    private int maxResponseTime; // Temps maximal de réponse en heures
    private int maxResolutionTime; // Temps maximal de résolution en heures
    private double penaltyAmount; // Montant de la pénalité en cas de non-respect
    private String userIdCompany; // ID du prestataire de maintenance associé
}

