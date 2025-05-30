package platformMedical.equipment_service.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.Alert;

import java.util.List;

@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {



    // Méthode pour trouver des alertes par ID d'équipement
    List<Alert> findByEquipmentId(String equipmentId);

    List<Alert> findByEquipmentIdIn(List<String> equipmentIds);
}