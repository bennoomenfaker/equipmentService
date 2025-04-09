package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EquipmentInterServiceTransferEvent {
    private String serialCode;
    private String name;
    private String description;
    private String oldServiceId;
    private String newServiceId;
    private String firstName;
    private String lastName;
    private String email;
    private SupervisorInfo oldSupervisor;
    private SupervisorInfo newSupervisor;
    private List<String> emailsToNotify;



}
