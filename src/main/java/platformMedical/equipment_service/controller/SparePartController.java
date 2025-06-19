package platformMedical.equipment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.EmdnNomenclature;
import platformMedical.equipment_service.entity.SparePart;
import platformMedical.equipment_service.entity.SparePartLot;
import platformMedical.equipment_service.service.SparePartService;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/api/spare-parts")
@RequiredArgsConstructor
public class SparePartController {

    private final SparePartService sparePartService;

    //  Créer une pièce de rechange
    @PostMapping
    public ResponseEntity<SparePart> createSparePart(@RequestBody SparePart sparePart) {
        return ResponseEntity.ok(sparePartService.createSparePart(sparePart));
    }

    //  Ajouter un lot à une pièce
    @PostMapping("/{sparePartId}/lots")
    public ResponseEntity<SparePart> addLot(
            @PathVariable String sparePartId,
            @RequestBody SparePartLot newLot) {
        return ResponseEntity.ok(sparePartService.addLotToSparePart(sparePartId, newLot));
    }

    //  Supprimer un lot d'une pièce
    @DeleteMapping("/{sparePartId}/lots")
    public ResponseEntity<SparePart> removeLot(
            @PathVariable String sparePartId,
            @RequestBody SparePartLot lotToRemove) {
        return ResponseEntity.ok(sparePartService.removeLotFromSparePart(sparePartId, lotToRemove));
    }

    //  Obtenir toutes les pièces compatibles à un code EMDN (code fourni par le front)
    @GetMapping("/by-emdn-code")
    public ResponseEntity<List<SparePart>> getByEmdnCode(@RequestParam String code) {
        return ResponseEntity.ok(sparePartService.getSparePartsByEmdnCode(code));
    }

    //  Obtenir toutes les pièces pour un hôpital
    @GetMapping("/by-hospital/{hospitalId}")
    public ResponseEntity<List<SparePart>> getByHospital(@PathVariable String hospitalId) {
        return ResponseEntity.ok(sparePartService.getSparePartsByHospitalId(hospitalId));
    }

    //  Obtenir toutes les pièces d’un hôpital + code EMDN
    @GetMapping("/by-hospital-and-emdn")
    public ResponseEntity<List<SparePart>> getByHospitalAndEmdnCode(
            @RequestParam String hospitalId,
            @RequestParam String code) {
        return ResponseEntity.ok(sparePartService.getSparePartsByHospitalAndEmdnCode(hospitalId, code));
    }

    //  Obtenir une pièce par ID
    @GetMapping("/{id}")
    public ResponseEntity<SparePart> getById(@PathVariable String id) {
        return ResponseEntity.ok(sparePartService.getSparePartById(id));
    }

    //  Modifier une pièce de rechange
    @PutMapping("/{id}")
    public ResponseEntity<SparePart> update(
            @PathVariable String id,
            @RequestBody SparePart updatedSparePart) {
        return ResponseEntity.ok(sparePartService.updateSparePart(id, updatedSparePart));
    }

    //  Supprimer une pièce de rechange
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        sparePartService.deleteSparePart(id);
        return ResponseEntity.noContent().build();
    }
}

