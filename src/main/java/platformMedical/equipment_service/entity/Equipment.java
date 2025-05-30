package platformMedical.equipment_service.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "equipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    private String id;
    private String nom;
    private String serialCode; // Code série généré
    private EmdnNomenclature emdnCode; // Code EMDN lié
    private Date acquisitionDate;
    private Supplier supplier;
    private String riskClass; // Classe de risque (ex: "Classe I", "Classe IIa", etc.)
    private double amount; // Montant
    private int lifespan; // Durée de vie en années
    private String status; // Statut (en cours, hors service, etc.)
    private int useCount; // Nombre d'utilisations (pour certains équipements)
    private double usageDuration; // Durée totale d'utilisation en heures
    private LocalDateTime lastUsedAt; // Date de dernière utilisation
    private String serviceId; // ID du service
    private String hospitalId; // ID de l'hôpital
    private Brand brand; // Marque
    private List<String> sparePartIds = new ArrayList<>();
    private List<MaintenancePlan> maintenancePlans =  new ArrayList<>(); // Liste des plans de maintenance préventive
    private boolean reception = false; // Par défaut, non réceptionné
    private String slaId;
    private Date startDateWarranty; // Date de début de la garantie
    private Date endDateWarranty; // Date de fin de la garantie
    private boolean fromMinistere; // true = ministère, false = fournisseur externe


}