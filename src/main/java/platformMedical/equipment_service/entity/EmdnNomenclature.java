package platformMedical.equipment_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "emdn_nomenclature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmdnNomenclature {

    @Id
    private String id; // ID généré par MongoDB

    private String code; // Code EMDN (ex: "Z110101")
    private String nom;  // Description (ex: "ACCÉLÉRATEURS LINÉAIRES")

    private List<EmdnNomenclature> subtypes; // Liste des sous-catégories
}
