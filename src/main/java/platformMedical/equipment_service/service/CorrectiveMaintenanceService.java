package platformMedical.equipment_service.service;

import platformMedical.equipment_service.entity.CorrectiveMaintenance;
import platformMedical.equipment_service.entity.DTOs.CorrectiveMaintenanceResponseDTO;

import java.util.List;

public interface CorrectiveMaintenanceService {
    List<CorrectiveMaintenanceResponseDTO> getAllCorrectiveMaintenances();
    List<CorrectiveMaintenanceResponseDTO> getCorrectiveMaintenancesByCompany(String userIdCompany);
    CorrectiveMaintenance updateCorrectiveMaintenance(String id, CorrectiveMaintenance updated);
    void deleteCorrectiveMaintenance(String id);
}
