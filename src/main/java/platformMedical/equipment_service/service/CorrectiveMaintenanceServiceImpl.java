package platformMedical.equipment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.CorrectiveMaintenance;
import platformMedical.equipment_service.entity.DTOs.*;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.Incident;
import platformMedical.equipment_service.repository.CorrectiveMaintenanceRepository;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.IncidentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorrectiveMaintenanceServiceImpl implements CorrectiveMaintenanceService {

    private final CorrectiveMaintenanceRepository correctiveMaintenanceRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserServiceClient userServiceClient;
    private final HospitalServiceClient hospitalServiceClient;
    private final IncidentRepository incidentRepository;

    private final String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ";

    private UserDTO getUserSafely(String userId) {
        try {
            return userId != null ? userServiceClient.getUserById(token, userId) : null;
        } catch (Exception e) {
            // Log l'erreur si besoin
            return null;
        }
    }


    @Override
    public List<CorrectiveMaintenanceResponseDTO> getAllCorrectiveMaintenances() {
        List<CorrectiveMaintenance> maintenances = correctiveMaintenanceRepository.findAll();

        return maintenances.stream().map(maintenance -> {
            Equipment equipment = equipmentRepository.findById(maintenance.getEquipmentId()).orElse(null);

            Incident incident = null;
            HospitalServiceEntity hospitalServiceEntity = null;

            try {
                if (equipment != null && equipment.getServiceId() != null) {
                    hospitalServiceEntity = hospitalServiceClient.getServiceById(token, equipment.getServiceId()).getBody();
                }
            } catch (Exception e) {
                // Gérer ou logguer l'erreur
            }

            try {
                if (maintenance.getIncidentId() != null) {
                    incident = incidentRepository.findById(maintenance.getIncidentId()).orElse(null);
                }
            } catch (Exception e) {
                // Gérer ou logguer l'erreur
            }

            return new CorrectiveMaintenanceResponseDTO(
                    maintenance.getId(),
                    maintenance.getDescription(),
                    maintenance.getStatus(),
                    maintenance.getPlannedDate(),
                    maintenance.getCompletedDate(),
                    maintenance.getResolutionDetails(),
                    equipment,
                    incident,
                    getUserSafely(maintenance.getAssignedTo()),
                    getUserSafely(incident != null ? incident.getValidatedBy() : null),
                    getUserSafely(incident != null ? incident.getResolvedBy() : null),
                    hospitalServiceEntity

            );
        }).collect(Collectors.toList());
    }

    @Override
    public List<CorrectiveMaintenanceResponseDTO> getCorrectiveMaintenancesByCompany(String userIdCompany) {
        List<CorrectiveMaintenance> maintenances = correctiveMaintenanceRepository.findByAssignedTo(userIdCompany);
        log.info(userIdCompany);
        return maintenances.stream()
                .map(maintenance -> {
                    Optional<Equipment> equipmentOpt = equipmentRepository.findById(maintenance.getEquipmentId());
                    Optional<Incident> incidentOpt = incidentRepository.findById(maintenance.getIncidentId());

                    if (equipmentOpt.isEmpty() || incidentOpt.isEmpty()) {
                        // Log l'erreur si nécessaire
                        log.warn("Missing equipment or incident for maintenance ID: {}", maintenance.getId());
                        return null;
                    }

                    Equipment equipment = equipmentOpt.get();
                    Incident incident = incidentOpt.get();

                    HospitalServiceEntity hospitalServiceEntity = null;
                    try {
                        hospitalServiceEntity = hospitalServiceClient
                                .getServiceById(token, equipment.getServiceId())
                                .getBody();
                    } catch (Exception e) {
                        log.error("Failed to fetch hospital service for serviceId: {}", equipment.getServiceId(), e);
                    }

                    return new CorrectiveMaintenanceResponseDTO(
                            maintenance.getId(),
                            maintenance.getDescription(),
                            maintenance.getStatus(),
                            maintenance.getPlannedDate(),
                            maintenance.getCompletedDate(),
                            maintenance.getResolutionDetails(),
                            equipment,
                            incident,
                            getUserSafely(maintenance.getAssignedTo()),
                            getUserSafely(incident.getValidatedBy()),
                            getUserSafely(incident.getResolvedBy()),
                            hospitalServiceEntity
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @Override
    public CorrectiveMaintenance updateCorrectiveMaintenance(String id, CorrectiveMaintenance updated) {
        CorrectiveMaintenance existing = correctiveMaintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance corrective non trouvée"));

        existing.setDescription(updated.getDescription());
        existing.setResolutionDetails(updated.getResolutionDetails());
        existing.setPlannedDate(existing.getPlannedDate());
        // Vérification que completedDate n'est pas null avant de le mettre à jour
        if (updated.getCompletedDate() != null) { // Utiliser != pour comparer avec null en Java
            existing.setCompletedDate(updated.getCompletedDate());
        }
        existing.setStatus(updated.getStatus());

        // Si statut == "Terminé", on met à jour l'équipement et l'incident
        if ("Terminé".equalsIgnoreCase(updated.getStatus())) {
            existing.setCompletedDate(LocalDateTime.now());

            // 1. Mettre à jour l'équipement
            Equipment equipment = equipmentRepository.findById(existing.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Équipement non trouvé"));
            equipment.setStatus("en service");
            equipmentRepository.save(equipment);

            // 2. Mettre à jour l'incident lié
            if (existing.getIncidentId() != null) {
                Incident incident = incidentRepository.findById(existing.getIncidentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));

                incident.setStatus("Résolu");
                incident.setResolvedBy(existing.getAssignedTo());
                incident.setResolvedAt(LocalDateTime.now());

                // ResolutionDetails peut venir d’un champ dans l'objet `updated` ou en paramètre séparé
                if (updated.getIncidentId() != null) {
                    incident.setResolutionDetails(updated.getResolutionDetails()); // ou autre champ si ajouté
                }

                incidentRepository.save(incident);
            }
        }

        return correctiveMaintenanceRepository.save(existing);
    }


    @Override
    public void deleteCorrectiveMaintenance(String id) {
        if (!correctiveMaintenanceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Maintenance corrective non trouvée");
        }
        correctiveMaintenanceRepository.deleteById(id);
    }
}
