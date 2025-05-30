package platformMedical.equipment_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "alerts")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Alert {

    @Id
    private String id;  // L'ID de l'alerte, généré automatiquement par MongoDB

    private String equipmentId;  // L'ID de l'équipement concerné
    private String type;  // Le type de l'alerte, par exemple "predicted_failure"
    private String message;  // Le message de l'alerte expliquant la cause
    private LocalDateTime timestamp;  // La date et l'heure de la création de l'alerte


}
