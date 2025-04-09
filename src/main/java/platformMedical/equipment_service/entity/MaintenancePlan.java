package platformMedical.equipment_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "maintenance_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenancePlan {

    @Id
    private String id;
    private Date maintenanceDate; // Date de maintenance préventive
    private String description; // Description de la maintenance
    private String equipmentId; // ID de l'équipement associé
    private String sparePartId; // ID de la pièce de rechange associée (optionnel)
}
