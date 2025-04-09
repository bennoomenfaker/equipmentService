package platformMedical.equipment_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {
    @Id
    private String id; // ID généré par MongoDB
    private String name; // Nom de la marque
    private String hospitalId; // ID de l'hôpital associé
}