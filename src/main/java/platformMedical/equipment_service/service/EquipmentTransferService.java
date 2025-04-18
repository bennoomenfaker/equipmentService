package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.EquipmentTransferHistory;
import platformMedical.equipment_service.repository.EquipmentTransferHistoryRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class EquipmentTransferService {

    private final EquipmentTransferHistoryRepository equipmentTransferHistoryRepository;


    public List<EquipmentTransferHistory> getTransfersByHospital(String hospitalId) {
        return equipmentTransferHistoryRepository.findByOldHospitalId(hospitalId);
    }

    public List<EquipmentTransferHistory> getTransfersByService(String serviceId) {
        return equipmentTransferHistoryRepository.findByOldServiceId(serviceId);
    }

    public List<EquipmentTransferHistory> getAllTransfers() {
        return equipmentTransferHistoryRepository.findAll();
    }
}

    private String getServiceNameById(String serviceId) {
        try {
            ResponseEntity<HospitalServiceEntity> response = hospitalServiceClient.getServiceById(token, serviceId);
            if (response.getBody() != null) {
                return response.getBody().getName();
            } else {
                return "Nom du service inconnu";
            }
        } catch (Exception e) {
            return "Nom du service inconnu";
        }
    }
}