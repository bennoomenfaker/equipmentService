package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class HospitalServiceEntity {
    private String id;
    private String name;
    private String description;
    private String hospitalId;
    private boolean activated;
    private List<String> userIds;


}