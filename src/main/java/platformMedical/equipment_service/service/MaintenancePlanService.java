package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.DTOs.*;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.MaintenanceFrequency;
import platformMedical.equipment_service.entity.MaintenancePlan;
import platformMedical.equipment_service.entity.SparePart;
import platformMedical.equipment_service.kafka.KafkaProducerService;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.MaintenancePlanRepository;
import platformMedical.equipment_service.repository.SparePartRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static platformMedical.equipment_service.entity.MaintenanceFrequency.MENSUELLE;


@Service
@AllArgsConstructor
public class MaintenancePlanService {
    private final MaintenancePlanRepository maintenancePlanRepository;
    private final EquipmentRepository equipmentRepository;
    private final SparePartRepository sparePartRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaProducerService kafkaProducerService;
    private final HospitalServiceClient hospitalServiceClient;
    private static final Logger log = LoggerFactory.getLogger(MaintenancePlanService.class);





    // Récupérer un plan de maintenance par ID
    public MaintenancePlan getMaintenancePlanByIdAndEquipmentId(String equipmentId, String maintenancePlanId) {
        // Trouver l'équipement
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

        // Chercher le plan de maintenance associé à l'équipement
        MaintenancePlan plan = maintenancePlanRepository.findById(maintenancePlanId)
                .orElseThrow(() -> new RuntimeException("Plan de maintenance non trouvé"));

        // Vérifier que le plan appartient bien à l'équipement (si applicable)
        if (!equipment.getMaintenancePlans().contains(plan)) {
            throw new RuntimeException("Le plan de maintenance n'appartient pas à cet équipement");
        }

        return plan;
    }


// Mettre à jour un plan de maintenance
    public MessageResponse updateMaintenancePlan(String equipmentId, String maintenancePlanId, MaintenancePlan newPlanDetails) {
        try {
            // Vérifier si l'équipement existe
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

            // Vérifier si le plan existe
            MaintenancePlan existingPlan = maintenancePlanRepository.findById(maintenancePlanId)
                    .orElseThrow(() -> new RuntimeException("Plan de maintenance non trouvé"));

            // Mettre à jour les champs du plan
            existingPlan.setMaintenanceDate(newPlanDetails.getMaintenanceDate());
            existingPlan.setDescription(newPlanDetails.getDescription());
            existingPlan.setFrequency(newPlanDetails.getFrequency());

            // Sauvegarder le plan modifié
            maintenancePlanRepository.save(existingPlan);

            // Mettre à jour la liste dans l'équipement
            equipment.getMaintenancePlans().removeIf(plan -> plan.getId().equals(maintenancePlanId));
            equipment.getMaintenancePlans().add(existingPlan);
            equipmentRepository.save(equipment);

            return new MessageResponse("Plan de maintenance mis à jour avec succès", existingPlan.getId());
        } catch (Exception e) {
            return new MessageResponse("Erreur lors de la mise à jour du plan de maintenance: " + e.getMessage());
        }
    }

    //  Supprimer un SLA
    public void deleteMaintenancePlanId(String mainteannacePlanId) {
        maintenancePlanRepository.deleteById(mainteannacePlanId);
    }

    public LocalDate calculateNextMaintenanceDate(LocalDate currentDate, MaintenanceFrequency frequency) {
        return switch (frequency) {
            case MENSUELLE -> currentDate.plusMonths(1);
            case TRIMESTRIELLE -> currentDate.plusMonths(3);
            case SEMESTRIELLE -> currentDate.plusMonths(6);
            case ANNUELLE -> currentDate.plusYears(1);
            default -> throw new IllegalArgumentException("Fréquence non supportée : " + frequency);
        };
    }


    // Suivi des maintenances pour un équipement (exécuté automatiquement tous les jours à 8h)
    @Scheduled(cron = "0 0 8 * * ?")
    public void trackMaintenanceForEquipment() {
        LocalDate today = LocalDate.now();
        LocalDate twoDaysAfter = today.plusDays(2);
        log.info("Exécution de trackMaintenanceForEquipment à 8h00");

        List<Equipment> allEquipments = equipmentRepository.findAll();

        for (Equipment equipment : allEquipments) {
            List<MaintenancePlan> maintenancePlans = equipment.getMaintenancePlans();
            for (MaintenancePlan plan : maintenancePlans) {
                LocalDate maintenanceDate = plan.getMaintenanceDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();

                if (maintenanceDate.equals(twoDaysAfter)) {
                    // Notification à J-2
                    sendMaintenanceNotification(equipment, plan, "J-2", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ");
                } else if (maintenanceDate.equals(today)) {
                    // Notification le jour même + mise à jour date
                    equipment.setStatus("en maintenance");
                    equipmentRepository.save(equipment);

                    sendMaintenanceNotification(equipment, plan, "AUJOURD'HUI", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ");

                    // Mettre à jour la prochaine date de maintenance
                    LocalDate nextDate = calculateNextMaintenanceDate(maintenanceDate, plan.getFrequency());
                    plan.setMaintenanceDate(Date.from(nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    maintenancePlanRepository.save(plan); // Update en base
                }
            }
        }
    }

    private void sendMaintenanceNotification(Object entity, MaintenancePlan plan, String phase, String token) {
        String entityName;
        String serialCode;
        String hospitalId;
        String entityType;

        if (entity instanceof Equipment) {
            entityName = ((Equipment) entity).getNom();
            serialCode = ((Equipment) entity).getSerialCode();
            hospitalId = ((Equipment) entity).getHospitalId();
            entityType = "équipement";
        } else {
            throw new IllegalArgumentException("Type d'entité non pris en charge pour la notification.");
        }

        String hospitalName = hospitalServiceClient.getHospitalNameById(token, hospitalId);

        List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles(token, hospitalId,
                List.of("ROLE_HOSPITAL_ADMIN", "ROLE_MAINTENANCE_ENGINEER"));
        List<String> emails = users.stream().map(UserDTO::getEmail).collect(Collectors.toList());

        String message;
        if ("J-2".equals(phase)) {
            message = "La maintenance préventive de " + entityType + " [" + serialCode + " - " + entityName +
                    "] à " + hospitalName + " est prévue dans 2 jours.";
        } else {
            message = "La maintenance préventive de " + entityType + " [" + serialCode + " - " + entityName +
                    "] à " + hospitalName + " est prévue aujourd'hui.";
        }

        NotificationEvent notificationEvent = new NotificationEvent(
                "Maintenance Préventive",
                message,
                emails
        );
        kafkaProducerService.sendMessage("notification-events-mail", notificationEvent);
    }



    // Récupérer tous les plans de maintenance (équipements et pièces de rechange) par hôpital
    public List<MaintenancePlan> getAllMaintenancePlansByHospital(String hospitalId, String token) {
        // Récupérer tous les plans de maintenance
        List<MaintenancePlan> allPlans = maintenancePlanRepository.findAll();


        log.info(allPlans.toString());
        log.info("x"+allPlans);

        // Filtrer les plans par hospitalId
        return allPlans.stream()
                .filter(plan -> {
                    if (plan.getEquipmentId() != null) {
                        Equipment equipment = equipmentRepository.findById(plan.getEquipmentId()).orElse(null);
                        return equipment != null && hospitalId.equals(equipment.getHospitalId());
                    }
                    return false; // Exclure les plans sans equipmentId ni sparePartId
                })
                .collect(Collectors.toList());
    }

    public MessageResponse createMaintenancePlan(String equipmentId, MaintenancePlan maintenancePlan) {
        try {
            // Vérifier si l'équipement existe
            Equipment equipment = equipmentRepository.findBySerialCode(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

            // Associer le plan à l'équipement
            maintenancePlan.setEquipmentId(equipmentId);

            // Enregistrer le plan de maintenance
            MaintenancePlan savedPlan = maintenancePlanRepository.save(maintenancePlan);

            // Ajouter le plan à la liste de l’équipement
            if (equipment.getMaintenancePlans() == null) {
                equipment.setMaintenancePlans(new ArrayList<>());
            }
            equipment.getMaintenancePlans().add(savedPlan);

            // Sauvegarder l’équipement mis à jour
            equipmentRepository.save(equipment);

            return new MessageResponse("Plan de maintenance créé avec succès", savedPlan.getId());
        } catch (Exception e) {
            return new MessageResponse("Erreur lors de la création du plan de maintenance: " + e.getMessage());
        }
    }


}