package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.SLA;

import java.util.List;
import java.util.Optional;


@Repository
public interface SLARepository  extends MongoRepository<SLA, String> {
    Optional<SLA> findByEquipmentId(String equipmentId);

    List<SLA> findByHospitalId(String hospitalId);

    List<SLA> findByUserIdCompany(String userIdCompany);
}
