package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.SLA;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.SLARepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SlaService {

    private final SLARepository slaRepository;
    private final EquipmentRepository equipmentRepository;

    //  Créer un SLA
    public SLA createSla(SLA sla) {
        return slaRepository.save(sla);
    }

    //  Récupérer un SLA par ID
    public Optional<SLA> getSlaById(String slaId) {
        return slaRepository.findById(slaId);
    }

    //  Récupérer le SLA d'un équipement
    public Optional<SLA> getSlaByEquipmentId(String equipmentId) {
        return slaRepository.findByEquipmentId(equipmentId);
    }

    //  Lister les SLA d'un prestataire de maintenance
    public List<SLA> getSlasByUserCompany(String userIdCompany) {
        return slaRepository.findByUserIdCompany(userIdCompany);
    }

    // Mise à jour d'un SLA
    public SLA updateSla(String slaId, SLA updatedSla) {
        // Vérifier si le SLA existe
        Optional<SLA> existingSla = slaRepository.findById(slaId);
        if (existingSla.isPresent()) {
            SLA sla = existingSla.get();

            // Mettre à jour les champs du SLA avec les nouvelles données
            sla.setName(updatedSla.getName());  // Par exemple, mettre à jour la description
            sla.setEquipmentId(updatedSla.getEquipmentId());
            sla.setPenaltyAmount(updatedSla.getPenaltyAmount());
            sla.setMaxResponseTime(updatedSla.getMaxResponseTime());
            sla.setMaxResolutionTime(updatedSla.getMaxResolutionTime());
            sla.setUserIdCompany(updatedSla.getUserIdCompany());  // Mettre à jour le prestataire

            // Vous pouvez ajouter d'autres champs à mettre à jour ici

            return slaRepository.save(sla);  // Sauvegarder et retourner le SLA mis à jour
        } else {
            throw new RuntimeException("SLA non trouvé");
        }
    }


    //  Supprimer un SLA
    public void deleteSla(String slaId) {
        slaRepository.deleteById(slaId);
    }




    // Récupérer les SLA associés à un hôpital
    public List<SLA> getSlasByHospital(String hospitalId) {
        return slaRepository.findByHospitalId(hospitalId);
    }
}
