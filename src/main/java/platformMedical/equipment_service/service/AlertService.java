package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.Alert;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.repository.AlertRepository;
import platformMedical.equipment_service.repository.EquipmentRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AlertService {
    private  final AlertRepository alertRepository;
    private  final EquipmentRepository equipmentRepository;

    // Récupérer des alertes par equipmentId
    public List<Alert> getAlertsByEquipmentId(String equipmentId) {
        return alertRepository.findByEquipmentId(equipmentId);
    }

    public Optional<Equipment> findEquipment(String equipmentId){
        return  equipmentRepository.findById(equipmentId);
    }
    public List<Alert> getAlertsByHospitalId(String hospitalId) {
        // Étape 1: Récupérer les équipements liés à l'hôpital
        List<Equipment> equipments = equipmentRepository.findByHospitalId(hospitalId);

        // Étape 2: Extraire les IDs des équipements
        List<String> equipmentIds = equipments.stream()
                .map(Equipment::getId)
                .toList();

        // Étape 3: Récupérer toutes les alertes liées à ces équipements
        return alertRepository.findByEquipmentIdIn(equipmentIds);
    }

}
