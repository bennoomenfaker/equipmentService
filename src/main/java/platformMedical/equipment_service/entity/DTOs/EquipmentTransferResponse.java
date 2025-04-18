package platformMedical.equipment_service.entity.DTOs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class EquipmentTransferResponse {
    private String transferId;
    private String type;

    private String oldHospitalName;
    private String newHospitalName;

    private String oldServiceName;
    private String newServiceName;

    private EquipmentRequest equipment;
    private String description;
    private String initiatedByName;
    private LocalDateTime createdAt;
}

