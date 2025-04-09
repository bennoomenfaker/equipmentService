package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.Equipment;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends MongoRepository<Equipment, String> {

    // Trouver un équipement par son code série
    Optional<Equipment> findBySerialCode(String serialCode);

    List<Equipment> findByReceptionFalse();



    Optional<Equipment> findByNom(String nom);

    List<Equipment> findByHospitalIdAndReception(String hospitalId, Boolean reception);

    List<Equipment> findByHospitalId(String hospitalId);
}