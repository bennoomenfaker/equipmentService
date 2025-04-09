package platformMedical.equipment_service.entity.DTOs;

import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentRequest {

    // Champs obligatoires (Ministère de la Santé)
    private String nom;
    private String emdnCode;
    private int lifespan;
    private String riskClass;
    private String hospitalId;
    private String serialNumber; // Généré automatiquement

    // Champs optionnels (remplis par l’hôpital après réception)
    private Double amount;
    private String supplier;
    private Date acquisitionDate;
    private String serviceId;
    private String brand;
    private List<String> sparePartIds = new ArrayList<>();
    private String slaId;
    private Date startDateWarranty; // Date de début de la garantie
    private Date endDateWarranty; // Date de fin de la garantie
    private boolean reception;
    private String status;
}
