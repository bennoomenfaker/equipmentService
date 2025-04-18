package platformMedical.equipment_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import platformMedical.equipment_service.entity.DTOs.EquipmentTransferResponse;
import platformMedical.equipment_service.entity.EquipmentTransferHistory;
import platformMedical.equipment_service.service.EquipmentTransferService;

import java.util.List;

@RestController
@RequestMapping("/api/equipment-transfers")
@RequiredArgsConstructor
public class EquipmentTransferController {

    private final EquipmentTransferService equipmentTransferService;

    // Vue globale pour le Ministère de la Santé
    @GetMapping("/all")
    public ResponseEntity<List<EquipmentTransferHistory>> getAllTransfers() {
        return ResponseEntity.ok(equipmentTransferService.getAllTransfers());
    }

    // Transferts faits par un hôpital (inter-hospital)
    @GetMapping("/by-hospital/{hospitalId}")
    public ResponseEntity<List<EquipmentTransferResponse>> getTransfersByHospital(@PathVariable String hospitalId) {
        return ResponseEntity.ok(equipmentTransferService.getTransfersByHospital(hospitalId));
    }

    //  Transferts faits par un service (inter-service)
    @GetMapping("/by-service/{serviceId}")
    public ResponseEntity<List<EquipmentTransferResponse>> getTransfersByService(@PathVariable String serviceId) {
        return ResponseEntity.ok(equipmentTransferService.getTransfersByService(serviceId));
    }
}
