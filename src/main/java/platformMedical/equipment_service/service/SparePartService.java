package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.EmdnNomenclature;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.SparePart;
import platformMedical.equipment_service.entity.SparePartLot;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.SparePartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SparePartService {

    private final SparePartRepository sparePartRepository;

    // Créer une nouvelle pièce de rechange avec des lots
    public SparePart createSparePart(SparePart sparePart) {
        if (sparePart.getLots() == null) {
            sparePart.setLots(new ArrayList<>());
        }
        return sparePartRepository.save(sparePart);
    }

    // Ajouter un lot à une pièce de rechange
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

    // ✅ Récupérer les pièces de rechange compatibles avec un code EMDN (fourni par le front)
    public List<SparePart> getSparePartsByEmdnCode(String emdnCode) {
        return sparePartRepository.findByEmdnCode(emdnCode);
    }

    // Récupérer toutes les pièces de rechange d’un hôpital
    public List<SparePart> getSparePartsByHospitalId(String hospitalId) {
        return sparePartRepository.findByHospitalId(hospitalId);
    }

    // Récupérer les pièces de rechange d’un hôpital ET d’un code EMDN
    public List<SparePart> getSparePartsByHospitalAndEmdnCode(String hospitalId, String emdnCode) {
        return sparePartRepository.findByHospitalIdAndEmdnCode(hospitalId, emdnCode);
    }

    // Récupérer une pièce de rechange par son ID
    public SparePart getSparePartById(String id) {
        return sparePartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pièce de rechange non trouvée"));
    }

    // Mettre à jour une pièce de rechange
    public SparePart updateSparePart(String sparePartId, SparePart updatedSparePart) {
        SparePart existingSparePart = sparePartRepository.findById(sparePartId)
                .orElseThrow(() -> new RuntimeException("Pièce de rechange non trouvée"));

        existingSparePart.setName(updatedSparePart.getName());
        existingSparePart.setLifespan(updatedSparePart.getLifespan());
        existingSparePart.setSupplier(updatedSparePart.getSupplier());
        existingSparePart.setServiceId(updatedSparePart.getServiceId());
        existingSparePart.setHospitalId(updatedSparePart.getHospitalId());
        existingSparePart.setEmdnCode(updatedSparePart.getEmdnCode());
        existingSparePart.setEmdnNom(updatedSparePart.getEmdnNom());
        existingSparePart.setLots(updatedSparePart.getLots());

        return sparePartRepository.save(existingSparePart);
    }

    // Supprimer une pièce de rechange
    public void deleteSparePart(String sparePartId) {
        if (!sparePartRepository.existsById(sparePartId)) {
            throw new RuntimeException("Pièce de rechange non trouvée");
        }
        sparePartRepository.deleteById(sparePartId);
    }
}

