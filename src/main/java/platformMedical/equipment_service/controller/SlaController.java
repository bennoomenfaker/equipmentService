package platformMedical.equipment_service.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.DTOs.EquipmentPenaltyDTO;
import platformMedical.equipment_service.entity.DTOs.SLADetailsDTO;
import platformMedical.equipment_service.entity.DTOs.SlaComplianceStats;
import platformMedical.equipment_service.entity.DTOs.SlaWithEquipmentDTO;
import platformMedical.equipment_service.entity.SLA;
import platformMedical.equipment_service.service.SlaService;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/slas")
@AllArgsConstructor
public class SlaController {

    private final SlaService slaService;

    // Créer un SLA
    @PostMapping
    public ResponseEntity<SLA> createSla(@RequestBody SLA sla) {
        SLA createdSla = slaService.createSla(sla);
        return new ResponseEntity<>(createdSla, HttpStatus.CREATED);
    }

    // Récupérer un SLA par ID
    @GetMapping("/{slaId}")
    public ResponseEntity<SLA> getSlaById(@PathVariable String slaId) {
        Optional<SLA> sla = slaService.getSlaById(slaId);
        return sla.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Récupérer le SLA d'un équipement
    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<SLA> getSlaByEquipmentId(@PathVariable String equipmentId) {
        Optional<SLA> sla = slaService.getSlaByEquipmentId(equipmentId);
        return sla.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Lister les SLA d'un prestataire de maintenance
    @GetMapping("/provider/{maintenanceProviderId}")
    public ResponseEntity<List<SlaWithEquipmentDTO>> getSlasByProvider(@PathVariable String maintenanceProviderId) {
        List<SlaWithEquipmentDTO> slas = slaService.getSlasWithEquipmentByCompany(maintenanceProviderId);
        return ResponseEntity.ok(slas);
    }

    // Mettre à jour un SLA
    @PutMapping("/{slaId}")
    public ResponseEntity<SLA> updateSla(@PathVariable String slaId, @RequestBody SLA updatedSla) {
        try {
            SLA sla = slaService.updateSla(slaId, updatedSla);
            return ResponseEntity.ok(sla);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Supprimer un SLA
    @DeleteMapping("/{slaId}")
    public ResponseEntity<Void> deleteSla(@PathVariable String slaId) {
        slaService.deleteSla(slaId);
        return ResponseEntity.noContent().build();
    }

    // Vérifier la conformité SLA d'un incident
    @PostMapping("/check-compliance/{incidentId}")
    public ResponseEntity<String> checkSlaCompliance(@PathVariable String incidentId) {
        String response =  slaService.checkSlaCompliance(incidentId);
        return  ResponseEntity.ok(response);
    }

    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<SLADetailsDTO>> getSLAsByHospitalWithEquipment(
            @PathVariable String hospitalId) {

        List<SLADetailsDTO> slaDetails = slaService.getSLAsWithEquipmentByHospital(hospitalId);

        return ResponseEntity.ok(slaDetails);
    }




    /*------------------*/
    // 1. Taux de conformité SLA
    @GetMapping("/compliance/{hospitalId}")
    public ResponseEntity<SlaComplianceStats> getComplianceStats(@PathVariable String hospitalId) {
        SlaComplianceStats stats = slaService.getSlaComplianceStats(hospitalId);
        return ResponseEntity.ok(stats);
    }

    // 2. Total des pénalités par hôpital
    @GetMapping("/penalties/hospital/{hospitalId}")
    public ResponseEntity<Double> getTotalPenaltiesByHospital(@PathVariable String hospitalId) {
        double total = slaService.getTotalPenaltiesByHospital(hospitalId);
        return ResponseEntity.ok(total);
    }

    // 3. Total des pénalités par prestataire (company)
    @GetMapping("/penalties/company/{companyId}")
    public ResponseEntity<Double> getTotalPenaltiesByCompany(@PathVariable String companyId) {
        double total = slaService.getTotalPenaltiesByCompany(companyId);
        return ResponseEntity.ok(total);
    }

    // 4. Top 5 équipements les plus pénalisés
    @GetMapping("/top-penalized-equipment/{hospitalId}")
    public ResponseEntity<List<EquipmentPenaltyDTO>> getTopPenalizedEquipments(@PathVariable String hospitalId) {
        List<EquipmentPenaltyDTO> list = slaService.getTopPenalizedEquipments(hospitalId);
        return ResponseEntity.ok(list);
    }
}
