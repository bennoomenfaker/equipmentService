package platformMedical.equipment_service.entity.DTOs;

import lombok.Builder;
import lombok.Data;
import platformMedical.equipment_service.entity.SLA;

@Data
@Builder
public class SLADetailsDTO {
    private SLA sla;
    private String equipmentNom;
    private String serialCode;
}
