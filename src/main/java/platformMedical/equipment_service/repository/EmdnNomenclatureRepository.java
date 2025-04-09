package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.EmdnNomenclature;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmdnNomenclatureRepository extends MongoRepository<EmdnNomenclature, String> {

    Optional<EmdnNomenclature> findByCode(String code); // Recherche par code EMDN


}
