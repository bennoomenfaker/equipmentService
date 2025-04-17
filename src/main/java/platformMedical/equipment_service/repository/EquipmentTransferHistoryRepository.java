package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.EquipmentTransferHistory;

import java.util.List;

@Repository
public interface EquipmentTransferHistoryRepository extends MongoRepository<EquipmentTransferHistory, String> {
    List<EquipmentTransferHistory> findByOldHospitalId(String hospitalId);

    List<EquipmentTransferHistory> findByOldServiceId(String serviceId);
}
