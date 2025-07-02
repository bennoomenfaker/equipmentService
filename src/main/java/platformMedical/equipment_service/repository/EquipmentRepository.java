package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.DTOs.EquipmentMinimalProjection;
import platformMedical.equipment_service.entity.Equipment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EquipmentRepository extends MongoRepository<Equipment, String> {


    List<Equipment> findByReception(boolean reception);



    Optional<Equipment> findByNom(String nom);

    List<Equipment> findByHospitalIdAndReception(String hospitalId, Boolean reception);

    List<Equipment> findByHospitalId(String hospitalId);




    @Query(value = "{ '_id': ?0 }", fields = "{ 'id': 1, 'nom': 1, 'serialCode': 1, 'status': 1, 'hospitalId': 1, 'serviceId': 1 }")
    EquipmentMinimalProjection findMinimalById(String id);

    Optional<Equipment> findBySerialCode(String serialNumber);

    List<Equipment> findByIdIn(Set<String> equipmentIds);
}