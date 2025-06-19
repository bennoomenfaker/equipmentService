package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.DTOs.*;
import platformMedical.equipment_service.entity.Equipment;
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
// Mettre à jour un plan de maintenance
    public MessageResponse updateMaintenancePlan(String equipmentId, String maintenancePlanId, MaintenancePlan newPlanDetails) {
        try {
            // Trouver l'équipement auquel appartient le plan de maintenance
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

            // Vérifier si le plan de maintenance existe
            MaintenancePlan existingPlan = maintenancePlanRepository.findById(maintenancePlanId)
                    .orElseThrow(() -> new RuntimeException("Plan de maintenance non trouvé"));

            // Mettre à jour les détails du plan de maintenance
            existingPlan.setMaintenanceDate(newPlanDetails.getMaintenanceDate());
            existingPlan.setDescription(newPlanDetails.getDescription());
            existingPlan.setSparePartId(newPlanDetails.getSparePartId());

            // Sauvegarder les modifications du plan de maintenance
            maintenancePlanRepository.save(existingPlan);

            // Mettre à jour l'équipement pour refléter la modification
            equipment.getMaintenancePlans().removeIf(plan -> plan.getId().equals(maintenancePlanId));
            equipment.getMaintenancePlans().add(existingPlan);

            // Sauvegarder l'équipement mis à jour
            equipmentRepository.save(equipment);

            // Retourner un message de succès
            return new MessageResponse("Plan de maintenance mis à jour avec succès", existingPlan.getId());
        } catch (Exception e) {
            return new MessageResponse("Erreur lors de la mise à jour du plan de maintenance: " + e.getMessage());
        }
    }

    //  Supprimer un SLA
    public void deleteMaintenancePlanId(String mainteannacePlanId) {
        maintenancePlanRepository.deleteById(mainteannacePlanId);
    }



    // Suivi des maintenances pour un équipement (exécuté automatiquement tous les jours à 8h)
    @Scheduled(cron = "0 0 8 * * ?")
    public void trackMaintenanceForEquipment() {
        LocalDate today = LocalDate.now();
        log.info("Exécution de trackMaintenanceForEquipment à 8h00");

        List<Equipment> allEquipments = equipmentRepository.findAll();

        for (Equipment equipment : allEquipments) {
            List<MaintenancePlan> maintenancePlans = equipment.getMaintenancePlans();
            for (MaintenancePlan plan : maintenancePlans) {
                if (plan.getMaintenanceDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().equals(today)) {
                    equipment.setStatus("en maintenance");
                    equipmentRepository.save(equipment);

                    // Envoyer une notification (ajustez le token si nécessaire)
                    sendMaintenanceNotification(equipment, plan, "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ");
                }
            }
        }
    }

    private void sendMaintenanceNotification(Object entity, MaintenancePlan plan, String token) {
        String entityName;
        String serialCode;
        String hospitalId;
        String entityType;

        if (entity instanceof Equipment) {
            entityName = ((Equipment) entity).getNom();
            serialCode = ((Equipment) entity).getSerialCode();
            hospitalId = ((Equipment) entity).getHospitalId();
            entityType = "équipement";
        } else if (entity instanceof SparePart) {
            entityName = ((SparePart) entity).getName();
            serialCode = ((SparePart) entity).getId();  // ID de la pièce de rechange
            hospitalId = ((SparePart) entity).getHospitalId();
            entityType = "pièce de rechange";
        } else {
            throw new IllegalArgumentException("Type d'entité non pris en charge pour la notification.");
        }

        // Récupérer le nom de l'hôpital
        String hospitalName = hospitalServiceClient.getHospitalNameById(token, hospitalId);

        // Récupérer les utilisateurs concernés
        List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles(token, hospitalId,
                List.of("ROLE_HOSPITAL_ADMIN", "ROLE_MAINTENANCE_ENGINEER"));

        List<String> emails = users.stream().map(UserDTO::getEmail).collect(Collectors.toList());

        String message = "La maintenance Préventive de " + entityType + " [" + serialCode + " - " + entityName + "] à " +
                hospitalName + " est prévue aujourd'hui.";

        // Envoyer l'événement Kafka
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
        System.out.println("*************");
        System.out.println("all plans" + allPlans);
        log.info("res {}", allPlans);

        log.info(allPlans.toString());
        log.info("x"+allPlans);

        // Filtrer les plans par hospitalId
        return allPlans.stream()
                .filter(plan -> {
                    if (plan.getEquipmentId() != null) {
                        Equipment equipment = equipmentRepository.findById(plan.getEquipmentId()).orElse(null);
                        return equipment != null && hospitalId.equals(equipment.getHospitalId());
                    } else if (plan.getSparePartId() != null) {
                        SparePart sparePart = sparePartRepository.findById(plan.getSparePartId()).orElse(null);
                        return sparePart != null && hospitalId.equals(sparePart.getHospitalId());
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