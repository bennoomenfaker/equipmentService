package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.MaintenancePlan;

import java.util.Date;
import java.util.List;

@Repository
public interface MaintenancePlanRepository extends MongoRepository<MaintenancePlan, String> {

    // Trouver tous les plans de maintenance pour un équipement spécifique
    List<MaintenancePlan> findByEquipmentId(String equipmentId);

    // Trouver tous les plans de maintenance pour une pièce de rechange spécifique
    List<MaintenancePlan> findBySparePartId(String sparePartId);

    // Trouver tous les plans de maintenance par date de maintenance
    List<MaintenancePlan> findByMaintenanceDate(Date maintenanceDate);

    void deleteByEquipmentId(String equipmentId);

    void deleteBySparePartId(String sparePartId);
}
