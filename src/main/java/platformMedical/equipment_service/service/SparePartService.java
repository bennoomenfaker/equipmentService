package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.MaintenancePlan;
import platformMedical.equipment_service.entity.SparePart;
import platformMedical.equipment_service.entity.SparePartLot;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.MaintenancePlanRepository;
import platformMedical.equipment_service.repository.SparePartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SparePartService {

    private final SparePartRepository sparePartRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenancePlanService maintenancePlanService;
    private  final EquipmentService equipmentService;
    private final MaintenancePlanRepository maintenancePlanRepository;



    // Créer une nouvelle pièce de rechange avec des lots
    public SparePart createSparePart(SparePart sparePart) {
        // Vérifier si l'équipement associé existe
        Equipment equipment = equipmentRepository.findById(sparePart.getEquipmentId())
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

        // Initialiser la liste des lots s'il est null
        if (sparePart.getLots() == null) {
            sparePart.setLots(new ArrayList<>());
        }

        // Enregistrer la pièce de rechange (sans les plans de maintenance pour l'instant)
        SparePart newSparePart = sparePartRepository.save(sparePart);

        // Vérifier et créer les plans de maintenance s'il n'y en a pas d'existant
        if (sparePart.getMaintenancePlans() != null && !sparePart.getMaintenancePlans().isEmpty()) {
            List<MaintenancePlan> createdPlans = new ArrayList<>();

            for (MaintenancePlan plan : sparePart.getMaintenancePlans()) {
                // Vérifier que chaque plan a bien une date de maintenance (si c'est nécessaire)
                if (plan.getMaintenanceDate() != null) {
                    // Créer un nouveau plan de maintenance
                    MaintenancePlan newPlan = new MaintenancePlan();
                    newPlan.setMaintenanceDate(plan.getMaintenanceDate());
                    newPlan.setDescription(plan.getDescription());
                    newPlan.setEquipmentId(sparePart.getEquipmentId()); // Associer l'équipement
                    newPlan.setSparePartId(newSparePart.getId()); // Associer l'ID de la pièce de rechange nouvellement créée

                    // Sauvegarder le plan de maintenance
                    createdPlans.add(maintenancePlanRepository.save(newPlan));
                } else {
                    // Si la date de maintenance n'est pas définie, tu peux gérer le cas d'erreur ici
                    throw new RuntimeException("Chaque plan de maintenance doit avoir une date.");
                }
            }

            // Une fois tous les plans créés, les associer à la pièce de rechange
            newSparePart.setMaintenancePlans(createdPlans);
            sparePartRepository.save(newSparePart); // Mettre à jour la pièce de rechange avec les plans
        }

        // Ajouter la pièce de rechange à l'équipement
        equipmentService.addSparePart(equipment.getId(), newSparePart);

        return newSparePart;
    }


    // Ajouter un lot à une pièce de rechange existante
    public SparePart addLotToSparePart(String sparePartId, SparePartLot newLot) {
        SparePart sparePart = sparePartRepository.findById(sparePartId)
                .orElseThrow(() -> new RuntimeException("Pièce de rechange non trouvée"));

        if (sparePart.getLots() == null) {
            sparePart.setLots(new ArrayList<>());
        }

        sparePart.getLots().add(newLot);
        return sparePartRepository.save(sparePart);
    }

    // Supprimer un lot spécifique d'une pièce de rechange
    public SparePart removeLotFromSparePart(String sparePartId, SparePartLot lotToRemove) {
        SparePart sparePart = sparePartRepository.findById(sparePartId)
                .orElseThrow(() -> new RuntimeException("Pièce de rechange non trouvée"));

        if (sparePart.getLots() != null) {
            sparePart.getLots().removeIf(lot ->
                    lot.getAcquisitionDate().equals(lotToRemove.getAcquisitionDate()) &&
                            lot.getStartDateWarranty().equals(lotToRemove.getStartDateWarranty()) &&
                            lot.getEndDateWarranty().equals(lotToRemove.getEndDateWarranty()) &&
                            lot.getQuantity() == lotToRemove.getQuantity()
            );
        }

        return sparePartRepository.save(sparePart);
    }

    // Récupérer toutes les pièces de rechange d'un équipement
    public List<SparePart> getSparePartsByEquipmentId(String equipmentId) {
        return sparePartRepository.findByEquipmentId(equipmentId);
    }

    // Récupérer toutes les pièces de rechange d'un hôpital
    public List<SparePart> getSparePartsByHospitalId(String hospitalId) {
        return sparePartRepository.findByHospitalId(hospitalId);
    }

    // Récupérer une pièce de rechange par son ID
    public SparePart getSparePartById(String id) {
        return sparePartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pièce de rechange non trouvée"));
    }

    // Mettre à jour une pièce de rechange
  /*  public SparePart updateSparePart(String id, SparePart updatedSparePart) {
        SparePart sparePart = sparePartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pièce de rechange non trouvée"));

        if (updatedSparePart.getName() != null) {
            sparePart.setName(updatedSparePart.getName());
        }
        if (updatedSparePart.getLifespan() > 0) {
            sparePart.setLifespan(updatedSparePart.getLifespan());
        }
        if (updatedSparePart.getSupplier() != null) {
            sparePart.setSupplier(updatedSparePart.getSupplier());
        }


        // Mise à jour de la liste des lots (remplacement complet)
        if (updatedSparePart.getLots() != null) {
            sparePart.setLots(updatedSparePart.getLots());
        }



        return sparePartRepository.save(sparePart);
    }*/

    // Supprimer une pièce de rechange
    public void deleteSparePart(String equipmentId, String sparePartId) {
        // Récupérer l'équipement par ID
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

        // Vérifier si la pièce de rechange existe bien dans l'équipement
        if (equipment.getSparePartIds().contains(sparePartId)) {
            // Supprimer l'ID de la liste
            equipment.getSparePartIds().remove(sparePartId);
            // Sauvegarder l'équipement mis à jour
            equipmentRepository.save(equipment);
            // Supprimer la pièce de rechange de la base de données si nécessaire
            sparePartRepository.deleteById(sparePartId);
        } else {
            throw new RuntimeException("Pièce de rechange non trouvée dans cet équipement");
        }
    }

    // Méthode pour mettre à jour les plans de maintenance d'une pièce de rechange
    public SparePart updateSparePartMaintenancePlans(String sparePartId, List<MaintenancePlan> updatedPlans) {
        // Appeler la méthode du service de maintenance pour mettre à jour les plans
        return maintenancePlanService.updateMaintenancePlansForSparePart(sparePartId, updatedPlans);
    }

    public SparePart updateSparePart(String sparePartId, SparePart updatedSparePart) {
        // Vérifier si la pièce de rechange existe
        SparePart existingSparePart = sparePartRepository.findById(sparePartId)
                .orElseThrow(() -> new RuntimeException("Pièce de rechange non trouvée"));

        // Mettre à jour les informations générales de la pièce de rechange
        existingSparePart.setName(updatedSparePart.getName());
        existingSparePart.setLifespan(updatedSparePart.getLifespan());
        existingSparePart.setSupplier(updatedSparePart.getSupplier());
        existingSparePart.setServiceId(updatedSparePart.getServiceId());
        existingSparePart.setHospitalId(updatedSparePart.getHospitalId());

        // Gestion des lots : supprimer les anciens et ajouter les nouveaux
        existingSparePart.setLots(updatedSparePart.getLots());

        // Gestion des plans de maintenance
        List<MaintenancePlan> updatedMaintenancePlans = new ArrayList<>();

        if (updatedSparePart.getMaintenancePlans() != null) {
            for (MaintenancePlan plan : updatedSparePart.getMaintenancePlans()) {
                if (plan.getId() == null) {
                    // Cas 1 : Nouveau plan de maintenance (id == null)
                    MaintenancePlan newPlan = new MaintenancePlan();
                    newPlan.setMaintenanceDate(plan.getMaintenanceDate());
                    newPlan.setDescription(plan.getDescription());
                    //newPlan.setEquipmentId(existingSparePart.getEquipmentId()); // Associer à l'équipement
                    newPlan.setSparePartId(sparePartId); // Associer à la pièce de rechange

                    // Sauvegarde du nouveau plan
                    newPlan = maintenancePlanRepository.save(newPlan);
                    updatedMaintenancePlans.add(newPlan);
                } else {
                    // Cas 2 : Mise à jour d'un plan existant (id != null)
                    MaintenancePlan existingPlan = maintenancePlanRepository.findById(plan.getId())
                            .orElseThrow(() -> new RuntimeException("Plan de maintenance non trouvé"));

                    // Mettre à jour les informations du plan
                    existingPlan.setMaintenanceDate(plan.getMaintenanceDate());
                    existingPlan.setDescription(plan.getDescription());
                    existingPlan.setEquipmentId(plan.getEquipmentId());
                    existingPlan.setSparePartId(plan.getSparePartId());

                    // Sauvegarde de la mise à jour
                    maintenancePlanRepository.save(existingPlan);
                    updatedMaintenancePlans.add(existingPlan);
                }
            }
        }

        // Mise à jour des plans de maintenance associés à la pièce de rechange
        existingSparePart.setMaintenancePlans(updatedMaintenancePlans);

        // Sauvegarde de la pièce de rechange mise à jour
        SparePart updatedSparePartEntity = sparePartRepository.save(existingSparePart);

        return updatedSparePartEntity;
    }

}
