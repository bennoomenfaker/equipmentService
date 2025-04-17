package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlaWithEquipmentDTO {
    private String id;
    private String name;
    private int maxResponseTime;
    private int maxResolutionTime;
    private double penaltyAmount;
    private String hospitalId;
    private String userIdCompany;
    private EquipmentRequest equipment;
}

