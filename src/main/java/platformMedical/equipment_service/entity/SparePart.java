package platformMedical.equipment_service.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.util.Date;
import java.util.List;
@Document(collection = "spare_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SparePart {
    @Id
    private String id;
    private String name; // Nom de la pièce de rechange
    private int lifespan; // Durée de vie en années
    private String supplier; // Fournisseur
    private String serviceId; // ID du service
    private String hospitalId; // ID de l'hôpital
    private String equipmentId; // ID de l'équipement associé
    private List<MaintenancePlan> maintenancePlans; // Liste des plans de maintenance préventive
    private List<SparePartLot> lots; // Liste des lots de cette pièce
}
