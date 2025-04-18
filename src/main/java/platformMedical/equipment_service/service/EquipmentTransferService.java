package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.DTOs.EquipmentRequest;
import platformMedical.equipment_service.entity.DTOs.EquipmentTransferResponse;
import platformMedical.equipment_service.entity.DTOs.HospitalServiceClient;
import platformMedical.equipment_service.entity.DTOs.HospitalServiceEntity;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.EquipmentTransferHistory;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.EquipmentTransferHistoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EquipmentTransferService {

    private final EquipmentTransferHistoryRepository equipmentTransferHistoryRepository;
    private final EquipmentRepository equipmentRepository;
    private final HospitalServiceClient hospitalServiceClient;
    private final String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ";

    public List<EquipmentTransferResponse> getTransfersByHospital(String hospitalId) {
        List<EquipmentTransferHistory> histories = equipmentTransferHistoryRepository.findByOldHospitalId(hospitalId);

        return histories.stream().map(history -> {
            Equipment equipment = equipmentRepository.findById(history.getEquipmentId()).orElse(null);
            String oldHospitalName = null;
            String newHospitalName = null;

            if ("INTER_HOSPITAL".equals(history.getType())) {
                oldHospitalName = getHospitalNameById(history.getOldHospitalId());
                newHospitalName = getHospitalNameById(history.getNewHospitalId());
            }

            return EquipmentTransferResponse.builder()
                    .transferId(history.getId())
                    .type(history.getType())
                    .equipment(mapToEquipmentRequest(equipment))
                    .oldHospitalName(oldHospitalName)
                    .newHospitalName(newHospitalName)
                    .oldServiceName(null)
                    .newServiceName(null)
                    .description(history.getDescription())
                    .initiatedByName(history.getInitiatedByName())
                    .createdAt(history.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<EquipmentTransferResponse> getTransfersByService(String serviceId) {
        List<EquipmentTransferHistory> histories = equipmentTransferHistoryRepository.findByOldServiceId(serviceId);

        return histories.stream().map(history -> {
            Equipment equipment = equipmentRepository.findById(history.getEquipmentId()).orElse(null);

            String oldServiceName = null;
            String newServiceName = null;

            if ("INTER_SERVICE".equals(history.getType())) {
                oldServiceName = getServiceNameById(history.getOldServiceId());
                newServiceName = getServiceNameById(history.getNewServiceId());
            }

            return EquipmentTransferResponse.builder()
                    .transferId(history.getId())
                    .type(history.getType())
                    .equipment(mapToEquipmentRequest(equipment))
                    .oldHospitalName(null)
                    .newHospitalName(null)
                    .oldServiceName(oldServiceName)
                    .newServiceName(newServiceName)
                    .description(history.getDescription())
                    .initiatedByName(history.getInitiatedByName())
                    .createdAt(history.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<EquipmentTransferHistory> getAllTransfers() {
        return equipmentTransferHistoryRepository.findAll();
    }

    private EquipmentRequest mapToEquipmentRequest(Equipment equipment) {
        if (equipment == null) return null;

        return new EquipmentRequest(
                equipment.getNom(),
                equipment.getSerialCode(),
                equipment.getLifespan(),
                equipment.getRiskClass(),
                equipment.getHospitalId(),
                equipment.getSerialCode(),
                equipment.getAmount(),
                equipment.getSupplier(),
                equipment.getAcquisitionDate(),
                equipment.getServiceId(),
                equipment.getBrand().getName(),
                equipment.getSparePartIds(),
                equipment.getSlaId(),
                equipment.getStartDateWarranty(),
                equipment.getEndDateWarranty(),
                equipment.isReception(),
                equipment.getStatus()
        );
    }

    private String getHospitalNameById(String hospitalId) {
        try {
            String response = hospitalServiceClient.getHospitalNameById(token, hospitalId);
            if (response!= null) {
                return response;
            } else {
                return "Nom de l'hôpital inconnu";
            }
        } catch (Exception e) {
            return "Nom de l'hôpital inconnu";
        }
    }

    private String getServiceNameById(String serviceId) {
        try {
            ResponseEntity<HospitalServiceEntity> response = hospitalServiceClient.getServiceById(token, serviceId);
            if (response.getBody() != null) {
                return response.getBody().getName();
            } else {
                return "Nom du service inconnu";
            }
        } catch (Exception e) {
            return "Nom du service inconnu";
        }
    }
}