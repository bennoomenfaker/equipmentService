package platformMedical.equipment_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.MaintenancePlan;
import platformMedical.equipment_service.entity.SparePart;
import platformMedical.equipment_service.entity.SparePartLot;
import platformMedical.equipment_service.service.SparePartService;

import java.util.List;

@RestController
@RequestMapping("/api/spare-parts")
@AllArgsConstructor
public class SparePartController {

    private final SparePartService sparePartService;

    //  Créer une nouvelle pièce de rechange
    @PostMapping
    public ResponseEntity<SparePart> createSparePart(@RequestBody SparePart sparePart) {
        SparePart createdSparePart = sparePartService.createSparePart(sparePart);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSparePart);
    }

    //  Ajouter un lot à une pièce de rechange existante
    @PostMapping("/{sparePartId}/lots")
    public ResponseEntity<SparePart> addLotToSparePart(
            @PathVariable String sparePartId,
            @RequestBody SparePartLot newLot) {
        SparePart updatedSparePart = sparePartService.addLotToSparePart(sparePartId, newLot);
        return ResponseEntity.ok(updatedSparePart);
    }

    //  Supprimer un lot spécifique d'une pièce de rechange
    @DeleteMapping("/{sparePartId}/lots")
    public ResponseEntity<SparePart> removeLotFromSparePart(
            @PathVariable String sparePartId,
            @RequestBody SparePartLot lotToRemove) {
        SparePart updatedSparePart = sparePartService.removeLotFromSparePart(sparePartId, lotToRemove);
        return ResponseEntity.ok(updatedSparePart);
    }

    //  Récupérer toutes les pièces de rechange d'un équipement
    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<SparePart>> getSparePartsByEquipmentId(@PathVariable String equipmentId) {
        return ResponseEntity.ok(sparePartService.getSparePartsByEquipmentId(equipmentId));
    }

    // Récupérer toutes les pièces de rechange d'un hôpital
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<SparePart>> getSparePartsByHospitalId(@PathVariable String hospitalId) {
        return ResponseEntity.ok(sparePartService.getSparePartsByHospitalId(hospitalId));
    }

    //  Récupérer une pièce de rechange par son ID
    @GetMapping("/{id}")
    public ResponseEntity<SparePart> getSparePartById(@PathVariable String id) {
        return ResponseEntity.ok(sparePartService.getSparePartById(id));
    }

    //  Mettre à jour une pièce de rechange
    @PutMapping("/{id}")
    public ResponseEntity<SparePart> updateSparePart(
            @PathVariable String id,
            @RequestBody SparePart updatedSparePart) {
        SparePart sparePart = sparePartService.updateSparePart(id, updatedSparePart);
        return ResponseEntity.ok(sparePart);
    }

    //  Supprimer une pièce de rechange
    @DeleteMapping("/{equipmentId}/spareParts/{sparePartId}")
    public ResponseEntity<Void> deleteSparePartFromEquipment(
            @PathVariable String equipmentId,
            @PathVariable String sparePartId) {

        sparePartService.deleteSparePart(equipmentId, sparePartId);
        return ResponseEntity.noContent().build();
    }
    // Endpoint pour mettre à jour les plans de maintenance d'une pièce de rechange
    @PutMapping("/{sparePartId}/maintenance-plans")
    public ResponseEntity<SparePart> updateSparePartMaintenancePlans(@PathVariable String sparePartId, @RequestBody List<MaintenancePlan> updatedPlans) {
        SparePart updatedSparePart = sparePartService.updateSparePartMaintenancePlans(sparePartId, updatedPlans);
        return ResponseEntity.ok(updatedSparePart);
    }

}
