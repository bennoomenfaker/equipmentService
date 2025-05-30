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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
            EquipmentMinimalProjection equipment =  equipmentRepository.findMinimalById(maintenance.getEquipmentId());

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





    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Pool de threads pour `CompletableFuture`

    @Override
    public List<CorrectiveMaintenanceResponseDTO> getCorrectiveMaintenancesByCompany(String userIdCompany) {
        List<CorrectiveMaintenance> maintenances = correctiveMaintenanceRepository.findByAssignedTo(userIdCompany);
        log.info("Récupération des maintenances pour la société: {}", userIdCompany);

        return maintenances.parallelStream()
                .map(maintenance -> {
                    CompletableFuture<Optional<EquipmentMinimalProjection>> equipmentFuture = CompletableFuture.supplyAsync(() ->
                                    Optional.ofNullable(equipmentRepository.findMinimalById(maintenance.getEquipmentId())), executorService)
                            .exceptionally(ex -> {
                                log.error("Erreur de récupération de l'équipement: {}", maintenance.getEquipmentId(), ex);
                                return Optional.empty();
                            });

                    CompletableFuture<Optional<Incident>> incidentFuture = CompletableFuture.supplyAsync(() ->
                                    incidentRepository.findById(maintenance.getIncidentId()), executorService)
                            .exceptionally(ex -> {
                                log.error("Erreur de récupération de l'incident: {}", maintenance.getIncidentId(), ex);
                                return Optional.empty();
                            });

                    CompletableFuture<UserDTO> assignedToUserFuture = CompletableFuture.supplyAsync(() -> getUserSafely(maintenance.getAssignedTo()), executorService);
                    CompletableFuture<UserDTO> validatedByUserFuture = incidentFuture.thenApplyAsync(incidentOpt -> getUserSafely(incidentOpt.map(Incident::getValidatedBy).orElse(null)), executorService);
                    CompletableFuture<UserDTO> resolvedByUserFuture = incidentFuture.thenApplyAsync(incidentOpt -> getUserSafely(incidentOpt.map(Incident::getResolvedBy).orElse(null)), executorService);

                    CompletableFuture<HospitalServiceEntity> hospitalServiceFuture = equipmentFuture.thenApplyAsync(equipmentOpt -> {
                        try {
                            return equipmentOpt.map(equipment -> hospitalServiceClient.getServiceById(token, equipment.getServiceId()).getBody()).orElse(null);
                        } catch (Exception e) {
                            log.error("Erreur lors de la récupération du service hospitalier pour serviceId: {}", equipmentOpt.map(EquipmentMinimalProjection::getServiceId).orElse("UNKNOWN"), e);
                            return null;
                        }
                    }, executorService);

                    // Attendre toutes les requêtes
                    CompletableFuture.allOf(equipmentFuture, incidentFuture, assignedToUserFuture, validatedByUserFuture, resolvedByUserFuture, hospitalServiceFuture).join();

                    Optional<EquipmentMinimalProjection> equipmentOpt = equipmentFuture.join();
                    Optional<Incident> incidentOpt = incidentFuture.join();

                    if (equipmentOpt.isEmpty() || incidentOpt.isEmpty()) {
                        log.warn("Équipement ou incident manquant pour la maintenance ID: {}", maintenance.getId());
                        return null;
                    }

                    return new CorrectiveMaintenanceResponseDTO(
                            maintenance.getId(),
                            maintenance.getDescription(),
                            maintenance.getStatus(),
                            maintenance.getPlannedDate(),
                            maintenance.getCompletedDate(),
                            maintenance.getResolutionDetails(),
                            equipmentOpt.get(),
                            incidentOpt.get(),
                            assignedToUserFuture.join(),
                            validatedByUserFuture.join(),
                            resolvedByUserFuture.join(),
                            hospitalServiceFuture.join()
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


    @Override
    public List<CorrectiveMaintenance> getCorrectiveMaintenancesByHospitalId(String hospitalId) {
        List<String> equipmentIds = equipmentRepository.findByHospitalId(hospitalId)
                .stream()
                .map(Equipment::getId)
                .collect(Collectors.toList());

        return correctiveMaintenanceRepository.findByEquipmentIdIn(equipmentIds);
    }


}
