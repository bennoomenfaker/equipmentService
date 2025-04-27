package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class EquipmentCreationEvent {
    private String equipmentId;
    private String equipmentName;
    private String serialCode;
    private String subject;
   // private String message;
    private List<String> recipients;
    private String hospitalId;

    // Ajoute les champs suivants
    private String hospitalName;
    private String emdnCode;
    private String acquisitionDate;
    private String amount;
    private String startDateWarranty;
    private String endDateWarranty;
}

