package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import platformMedical.equipment_service.entity.*;
import platformMedical.equipment_service.entity.DTOs.*;
import platformMedical.equipment_service.kafka.KafkaProducerService;
import platformMedical.equipment_service.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@AllArgsConstructor
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final EmdnNomenclatureRepository emdnNomenclatureRepository;
    private final BrandRepository brandRepository;
    private final SparePartRepository sparePartRepository;
    private final MaintenancePlanRepository maintenancePlanRepository;
    private final SLARepository slaRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaProducerService kafkaProducerService;
    private final HospitalServiceClient hospitalServiceClient;
    private final EquipmentTransferHistoryRepository equipmentTransferHistoryRepository;
    private final  SupplierRepository supplierRepository;


    // Générer un code série unique
    private String generateSerialCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    // Création d'un équipement par le Ministère de la Santé
    public MessageResponse createEquipment(EquipmentRequest request) {
        // Vérifier si un équipement avec le même nom existe déjà


        // Vérifier les dates de garantie
        if (request.getStartDateWarranty() != null && request.getEndDateWarranty() != null
                && request.getEndDateWarranty().before(request.getStartDateWarranty())) {
            return new MessageResponse("La date de fin de garantie doit être postérieure à la date de début.");
        }

        // Trouver le code EMDN correspondant
        EmdnNomenclature emdn = findByCodeRecursive(request.getEmdnCode())
                .orElseThrow(() -> new RuntimeException("Code EMDN invalide"));

        // Générer un numéro de série
        String serialNumber = generateSerialCode();
        Optional<Equipment> exist  = equipmentRepository.findBySerialCode(serialNumber);
        if (exist.isPresent()) {
            serialNumber = generateSerialCode();
        }

        // Créer l'équipement
        Equipment equipment = Equipment.builder()
                .nom(request.getNom())
                .emdnCode(emdn)
                .lifespan(request.getLifespan())
                .riskClass(request.getRiskClass())
                .hospitalId(request.getHospitalId())
                .serialCode(serialNumber)
                .reception(false)
                .status("en attente de réception")
                .acquisitionDate(request.getAcquisitionDate())
                .amount(request.getAmount())
                .startDateWarranty(request.getStartDateWarranty())
                .endDateWarranty(request.getEndDateWarranty())
                .fromMinistere(request.isFromMinistere())
                .supplier(null)
                .build();

        Equipment savedEquipment = equipmentRepository.save(equipment);

        // Envoyer les notifications
        sendEquipmentCreationNotifications(savedEquipment);

        return new MessageResponse("Équipement créé avec succès.", savedEquipment.getSerialCode());
    }

    public MessageResponse createEquipment1(EquipmentRequest request) {
        // Vérifier les dates de garantie
        if (request.getStartDateWarranty() != null && request.getEndDateWarranty() != null
                && request.getEndDateWarranty().before(request.getStartDateWarranty())) {
            return new MessageResponse("La date de fin de garantie doit être postérieure à la date de début.");
        }

        // Trouver le code EMDN correspondant (recherche récursive dans la nomenclature)
        EmdnNomenclature emdn = findByCodeRecursive(request.getEmdnCode())
                .orElseThrow(() -> new RuntimeException("Code EMDN invalide"));

        // Générer un numéro de série unique
        String serialNumber;
        do {
            serialNumber = generateSerialCode();
        } while (equipmentRepository.findBySerialCode(serialNumber).isPresent());

        // Gestion de la marque : récupérer la marque existante ou lancer une erreur si inexistante
        Brand brand = null;
        if (request.getBrand() != null && !request.getBrand().trim().isEmpty()) {
            brand = brandRepository.findByNameAndHospitalId(request.getBrand(), request.getHospitalId())
                    .orElseThrow(() -> new RuntimeException("Marque non trouvée pour le nom fourni"));
        }
        // Récupérer le supplier par id envoyé dans la requête
        Supplier supplier = null;
        if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Supplier non trouvé avec l'id : " + request.getSupplierId()));
        }


        // Créer l'équipement avec toutes les données
        Equipment equipment = Equipment.builder()
                .nom(request.getNom())
                .emdnCode(emdn)
                .lifespan(request.getLifespan())
                .riskClass(request.getRiskClass())
                .hospitalId(request.getHospitalId())
                .serialCode(serialNumber)
                .reception(true) // Par défaut true (comme dans le front)
                .status("en service") // Statut initial
                .acquisitionDate(request.getAcquisitionDate())
                .amount(request.getAmount())
                .startDateWarranty(request.getStartDateWarranty())
                .endDateWarranty(request.getEndDateWarranty())
                .fromMinistere(request.isFromMinistere())
                .brand(brand)
                .supplier(supplier)
                .build();

        Equipment savedEquipment = equipmentRepository.save(equipment);

        // Envoyer les notifications à la création
        sendEquipmentCreationNotifications(savedEquipment);

        return new MessageResponse("Équipement créé avec succès.", savedEquipment.getSerialCode());
    }

    private String formatDateToFrench(String rawDate) {
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH); // format ISO de LocalDate
            LocalDate date = LocalDate.parse(rawDate, inputFormatter);
            DateTimeFormatter frenchFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
            return frenchFormatter.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }

    private String formatAmount(String rawAmount) {
        try {
            double amount = Double.parseDouble(rawAmount);
            NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);
            nf.setMinimumFractionDigits(0);
            return nf.format(amount) + " TND";
        } catch (Exception e) {
            return rawAmount;
        }
    }

    // Méthode pour envoyer les notifications après création d'un équipement
    private void sendEquipmentCreationNotifications(Equipment equipment) {
        String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ";

        try {
            // Récupérer l'admin de l'hôpital
            UserDTO hospitalAdmin = userServiceClient.getAdminByHospitalId(token, equipment.getHospitalId());

            // Récupérer les ingénieurs de maintenance de l'hôpital
            List<UserDTO> maintenanceEngineers = userServiceClient.getUsersByHospitalAndRoles(
                    token,
                    equipment.getHospitalId(),
                    List.of("ROLE_MAINTENANCE_ENGINEER")
            );

            // Liste des emails à notifier
            List<String> emailsToNotify = new ArrayList<>();
            if (hospitalAdmin != null) {
                emailsToNotify.add(hospitalAdmin.getEmail());
            }
            maintenanceEngineers.forEach(engineer -> emailsToNotify.add(engineer.getEmail()));

            // Récupérer le nom de l'hôpital
            String hospitalName = hospitalServiceClient.getHospitalNameById(token, equipment.getHospitalId());

            // Créer le contenu de l'email
            String emailSubject = "Nouvel équipement affecté à votre hôpital";
            String emailContent = String.format(
                    "Un nouvel équipement a été affecté à votre hôpital %s:\n\n" +
                            "Détails de l'équipement:\n" +
                            "- Nom: %s\n" +
                            "- Code Série: %s\n" +
                            "- Code EMDN: %s\n" +
                            "- Date d'acquisition: %s\n" +
                            "- Montant d'acquisition: %s\n" +
                            "- Période de garantie: %s à %s\n\n" +
                            "Veuillez ajouter cet équipement à votre inventaire en saisissant le code série valide: %s",
                    hospitalName,
                    equipment.getNom(),
                    equipment.getSerialCode(),
                    equipment.getEmdnCode().getCode(),
                    equipment.getAcquisitionDate() != null ? formatDateToFrench(equipment.getAcquisitionDate().toString()) : "Non spécifiée",
                    equipment.getAmount() != 0 ? formatAmount(String.valueOf(equipment.getAmount())) :  0,
                    equipment.getStartDateWarranty() != null ? formatDateToFrench(equipment.getStartDateWarranty().toString()) : "Non spécifiée",
                    equipment.getEndDateWarranty() != null ? formatDateToFrench(equipment.getEndDateWarranty().toString()) : "Non spécifiée",
                    equipment.getSerialCode()
            );

            // Envoyer la notification
            NotificationEvent notificationEvent = new NotificationEvent(
                    emailSubject,
                    emailContent,
                    emailsToNotify
            );
            kafkaProducerService.sendMessage("notification-events", notificationEvent);

            // Création de l'événement spécifique aux équipements
            EquipmentCreationEvent event = new EquipmentCreationEvent(
                    equipment.getId(),
                    equipment.getNom(),
                    equipment.getSerialCode(),
                    emailSubject,
                    emailsToNotify,
                    equipment.getHospitalId(),
                    hospitalName,
                    equipment.getEmdnCode().getCode(),
                    equipment.getAcquisitionDate() != null ? equipment.getAcquisitionDate().toString() : null,
                    String.valueOf(equipment.getAmount()),
                    equipment.getStartDateWarranty() != null ? equipment.getStartDateWarranty().toString() : null,
                    equipment.getEndDateWarranty() != null ? equipment.getEndDateWarranty().toString() : null
            );


            // Envoi via le topic spécifique
            kafkaProducerService.sendMessage("equipment-service-create-equipment", event);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications pour le nouvel équipement", e);
        }
    }

    public MessageResponse updateEquipmentAfterReception(String serialNumber, EquipmentRequest request) {
        try {
            // Vérifier si l'équipement existe
            Optional<Equipment> existEquipment = equipmentRepository.findBySerialCode(serialNumber);
            if (existEquipment.isEmpty()) {
                return new MessageResponse("Aucun équipement trouvé avec ce numéro de série.");
            }

            Equipment equipment = existEquipment.get();

            // Vérifier si l'équipement a déjà été réceptionné
            if (equipment.isReception()) {
                return new MessageResponse("L'équipement a déjà été réceptionné.");
            }

            // Vérifier si un équipement avec le même nom existe déjà
            Optional<Equipment> existingEquipment = equipmentRepository.findByNom(request.getNom());
            if (existingEquipment.isPresent()) {
                return new MessageResponse("Un équipement avec ce nom existe déjà.");
            }
            // Mettre à jour le Supplier via supplierId (à adapter si dans EquipmentRequest tu as supplierId au lieu de Supplier)
            if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                        .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id " + request.getSupplierId()));
                equipment.setSupplier(supplier);
            } else {
                // Si supplierId absent, tu peux choisir de ne rien changer ou de mettre null selon ton besoin
                // equipment.setSupplier(null);
            }

            // Trouver ou créer la marque
            Optional<Brand> existBrand = brandRepository.findByName(request.getBrand());
            Brand brand =  existBrand.get();
            // Mettre à jour les propriétés de l'équipement
            equipment.setBrand(brand);
            equipment.setReception(true);
            equipment.setStatus("En service");
            equipment.setAcquisitionDate(request.getAcquisitionDate());
            equipment.setAmount(request.getAmount());
            equipment.setEndDateWarranty(request.getEndDateWarranty());
            equipment.setStartDateWarranty(request.getStartDateWarranty());
            equipment.setServiceId(request.getServiceId());
            equipment.setSlaId(request.getSlaId());


            // Sauvegarder l'équipement mis à jour
            Equipment updatedEquipment = equipmentRepository.save(equipment);

            // Retourner un message de succès avec l'ID de l'équipement
            return new MessageResponse("Équipement mis à jour avec succès.", updatedEquipment.getId());
        } catch (Exception e) {
            // En cas d'erreur inattendue, retourner un message d'erreur
            return new MessageResponse("Une erreur s'est produite lors de la mise à jour de l'équipement : " + e.getMessage());
        }
    }
    // Ajouter un plan de maintenance préventive à un équipement
     public Equipment addMaintenancePlan(String equipmentId, MaintenancePlan maintenancePlan) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));
        // Associer le plan de maintenance à l'équipement
         maintenancePlan.setEquipmentId(equipmentId);
         maintenancePlanRepository.save(maintenancePlan);
         // Ajouter le plan de maintenance à la liste de l'équipement
         equipment.getMaintenancePlans().add(maintenancePlan);
         return equipmentRepository.save(equipment);
    }


    // Récupérer tous les équipements d'un hôpital
     public List<Equipment> getEquipmentByHospitalId(String hospitalId) {
        return equipmentRepository.findByHospitalIdAndReception(hospitalId,true);
    }




    // Mettre à jour les informations d'un équipement (ex: warranty, status, etc.)
    public Equipment updateEquipment(String equipmentId, EquipmentRequest equipmentRequest) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

        // Validation des champs obligatoires
        if (equipmentRequest.getEmdnCode() == null || equipmentRequest.getLifespan() <= 0 || equipmentRequest.getRiskClass() == null) {
            throw new IllegalArgumentException("Les champs obligatoires (code EMDN, lifespan, riskClass) doivent être renseignés.");
        }

        // Vérifier unicité du nom
        if (!equipmentRequest.getNom().equals(equipment.getNom())) {
            Optional<Equipment> existingEquipment = equipmentRepository.findByNom(equipmentRequest.getNom());
            if (existingEquipment.isPresent()) {
                throw new RuntimeException("Un équipement avec ce nom existe déjà dans la base de données.");
            }
        }

        // Récupérer l'objet EmdnNomenclature
        EmdnNomenclature emdnCode = findByCodeRecursive(equipmentRequest.getEmdnCode())
                .orElseThrow(() -> new RuntimeException("Code EMDN non trouvé"));

        // Gestion marque (brand)
        if (equipmentRequest.getBrand() != null && !equipmentRequest.getBrand().trim().isEmpty()) {
            Brand brand = brandRepository.findByNameAndHospitalId(
                            equipmentRequest.getBrand(), equipmentRequest.getHospitalId())
                    .orElseThrow(() -> new RuntimeException("Marque non trouvée pour le nom fourni"));
            equipment.setBrand(brand);
        }



        // Mettre à jour le Supplier via supplierId (à adapter si dans EquipmentRequest tu as supplierId au lieu de Supplier)
        if (equipmentRequest.getSupplierId() != null && !equipmentRequest.getSupplierId().isEmpty()) {
            Supplier supplier = supplierRepository.findById(equipmentRequest.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id " + equipmentRequest.getSupplierId()));
            equipment.setSupplier(supplier);
        } else {
            equipment.setSupplier(null);
        }

        // Mise à jour des autres champs
        equipment.setEmdnCode(emdnCode);
        equipment.setNom(equipmentRequest.getNom());
        equipment.setAcquisitionDate(equipmentRequest.getAcquisitionDate());
        equipment.setRiskClass(equipmentRequest.getRiskClass());
        equipment.setAmount(equipmentRequest.getAmount());
        equipment.setLifespan(equipmentRequest.getLifespan());
        equipment.setEndDateWarranty(equipmentRequest.getEndDateWarranty());
        equipment.setStartDateWarranty(equipmentRequest.getStartDateWarranty());
        equipment.setServiceId(equipmentRequest.getServiceId());
        equipment.setHospitalId(equipmentRequest.getHospitalId());
        equipment.setStatus(equipmentRequest.getStatus());
        equipment.setReception(equipmentRequest.isReception());
        equipment.setSlaId(equipmentRequest.getSlaId());
        equipment.setUseCount(equipmentRequest.getUseCount());
        equipment.setUsageDuration(equipmentRequest.getUsageDuration());
        equipment.setLastUsedAt(equipmentRequest.getLastUsedAt());
        equipment.setFromMinistere(equipmentRequest.isFromMinistere());

        return equipmentRepository.save(equipment);
    }


    public Optional<EmdnNomenclature> findByCodeRecursive(String code) {
        List<EmdnNomenclature> allNomenclatures = emdnNomenclatureRepository.findAll();
        for (EmdnNomenclature nomenclature : allNomenclatures) {
            Optional<EmdnNomenclature> found = findByCodeInSubtypes(nomenclature, code);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();


    }
    private Optional<EmdnNomenclature> findByCodeInSubtypes(EmdnNomenclature nomenclature, String code) {
        if (nomenclature.getCode().equals(code)) {
            return Optional.of(nomenclature);
        }
        if (nomenclature.getSubtypes() != null) {
            for (EmdnNomenclature subtype : nomenclature.getSubtypes()) {
                Optional<EmdnNomenclature> found = findByCodeInSubtypes(subtype, code);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }
    public Optional<Equipment> findBySerialNumber(String serialCode) {
        return equipmentRepository.findBySerialCode(serialCode);
    }
    public Equipment assignSlaToEquipment(String equipmentId, String slaId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));
        SLA sla = slaRepository.findById(slaId)
                .orElseThrow(() -> new RuntimeException("SLA non trouvé"));
        equipment.setSlaId(sla.getId()); return equipmentRepository.save(equipment);
    }

    public List<Equipment> getAllNonReceivedEquipment() {
        return equipmentRepository.findByReception(false);
    }

    // Mettre à jour le plan de maintenance pour un équipement spécifique
    public MessageResponse updateMaintenancePlanForEquipment(String equipmentId, List<MaintenancePlan> updatedPlans) {
        try {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

            List<MaintenancePlan> newMaintenancePlans = new ArrayList<>();

            for (MaintenancePlan updatedPlan : updatedPlans) {
                // Vérifier si l'ID est présent dans le payload
                if (updatedPlan.getId() != null && !updatedPlan.getId().isEmpty()) {
                    // Vérifier si ce plan de maintenance existe déjà
                    Optional<MaintenancePlan> existingPlanOpt = maintenancePlanRepository.findById(updatedPlan.getId());

                    if (existingPlanOpt.isPresent()) {
                        // Mettre à jour les détails du plan existant
                        MaintenancePlan existingPlan = existingPlanOpt.get();
                        existingPlan.setMaintenanceDate(updatedPlan.getMaintenanceDate());
                        existingPlan.setDescription(updatedPlan.getDescription());
                        existingPlan.setSparePartId(updatedPlan.getSparePartId());
                        maintenancePlanRepository.save(existingPlan);
                        newMaintenancePlans.add(existingPlan);
                    } else {
                        // L'ID est présent mais le plan n'existe pas (cas rare, mais à gérer)
                        updatedPlan.setEquipmentId(equipmentId);
                        MaintenancePlan newPlan = maintenancePlanRepository.save(updatedPlan);
                        newMaintenancePlans.add(newPlan);
                    }
                } else {
                    // L'ID n'est pas présent, c'est un nouveau plan à créer
                    updatedPlan.setEquipmentId(equipmentId);
                    MaintenancePlan newPlan = maintenancePlanRepository.save(updatedPlan);
                    newMaintenancePlans.add(newPlan);
                }
            }

            // Mettre à jour la liste des plans de maintenance de l'équipement
            equipment.setMaintenancePlans(newMaintenancePlans);
            equipmentRepository.save(equipment);

            return new MessageResponse("Plans de maintenance mis à jour avec succès");
        } catch (Exception e) {
            return new MessageResponse("Erreur lors de la mise à jour des plans de maintenance : " + e.getMessage());
        }
    }

    @Transactional
    public void deleteEquipment(String equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

        // Supprimer les plans de maintenance liés directement à l'équipement (sans sparePartId)
        maintenancePlanRepository.deleteByEquipmentId(equipmentId);

        // Supprimer l'équipement
        equipmentRepository.delete(equipment);
    }



    public Equipment changeEquipmentInterService(String equipmentId, String newServiceId, String description, UserDTO user, String token) {
        // Récupérer l'équipement depuis la base de données
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

        String oldServiceId = equipment.getServiceId();
        equipment.setServiceId(newServiceId);
        equipmentRepository.save(equipment);

        // Récupérer les superviseurs de l'ancien service
        List<UserDTO> oldServiceSupervisors = userServiceClient.getServiceSupervisors(token, oldServiceId);
        UserDTO oldSupervisor = oldServiceSupervisors.isEmpty() ? null : oldServiceSupervisors.get(0);

        // Récupérer les superviseurs du nouveau service
        List<UserDTO> newServiceSupervisors = userServiceClient.getServiceSupervisors(token, newServiceId);
        UserDTO newSupervisor = newServiceSupervisors.isEmpty() ? null : newServiceSupervisors.get(0);

        // Récupérer les noms des services
        String oldServiceName = getServiceNameById(token, oldServiceId);
        String newServiceName = getServiceNameById(token, newServiceId);

        //  Construire les variables pour le mail
        SupervisorInfo oldSupervisorInfo = (oldSupervisor != null) ?
                new SupervisorInfo(oldSupervisor.getFirstName(), oldSupervisor.getLastName(), oldSupervisor.getEmail(), oldServiceId) : null;

        SupervisorInfo newSupervisorInfo = (newSupervisor != null) ?
                new SupervisorInfo(newSupervisor.getFirstName(), newSupervisor.getLastName(), newSupervisor.getEmail(), newServiceId) : null;

        // Envoyer la notification par email
        sendTransferEmail(user, oldSupervisorInfo, newSupervisorInfo, equipment, oldServiceName, newServiceName, description);

        EquipmentTransferHistory history = new EquipmentTransferHistory();
        history.setEquipmentId(equipmentId);
        history.setOldServiceId(oldServiceId);
        history.setNewServiceId(newServiceId);
        history.setType("INTER_SERVICE");
        history.setDescription(description);
        history.setInitiatedByUserId(user.getId());
        history.setInitiatedByName(user.getFirstName() + " " + user.getLastName());
        equipmentTransferHistoryRepository.save(history);

        return equipment;
    }

    private String getServiceNameById(String token, String serviceId) {
        // Appeler le service de gestion des services hospitaliers pour récupérer le nom du service
        try {
            ResponseEntity<HospitalServiceEntity> response = hospitalServiceClient.getServiceById(token, serviceId);

            // Vérifier si le corps de la réponse est présent
            if (response.getBody() != null) {
                return response.getBody().getName();  // Accéder au nom du service
            } else {
                return "Nom du service inconnu";  // Si le corps est vide ou la réponse est incorrecte
            }
        } catch (Exception e) {
            return "Nom du service inconnu"; // Retourner un nom par défaut en cas d'erreur
        }
    }

    private void sendTransferEmail(UserDTO user, SupervisorInfo oldSupervisor, SupervisorInfo newSupervisor, Equipment equipment, String oldServiceName, String newServiceName, String description) {
        // Inclure les emails dans l'événement pour l'envoi de notification
        List<String> emailsToNotify = new ArrayList<>();
        emailsToNotify.add(user.getEmail());  // Ajouter l'email de l'initiateur
        if (oldSupervisor != null) {
            emailsToNotify.add(oldSupervisor.getEmail());  // Ajouter l'email du superviseur actuel
        }
        if (newSupervisor != null) {
            emailsToNotify.add(newSupervisor.getEmail());  // Ajouter l'email du superviseur du nouveau service
        }

        EquipmentInterServiceTransferEvent event = new EquipmentInterServiceTransferEvent(
                equipment.getSerialCode(),
                equipment.getNom(),
                description,
                oldServiceName,  // Ajouter les noms des services dans l'événement
                newServiceName,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                oldSupervisor,
                newSupervisor,
                emailsToNotify  // Ajouter la liste des emails dans l'événement
        );

        // Envoyer l'événement Kafka pour la notification
        kafkaProducerService.sendMessage("equipment-service-transfer-events", event);

        NotificationEvent notificationEvent = new NotificationEvent(
                "Transfert d'équipement",
                "L'équipement " + equipment.getNom() + " a été transféré de " + oldServiceName + " à " + newServiceName + ".",
                emailsToNotify
        );

// Envoyer l'événement à `notification-service`
        kafkaProducerService.sendMessage("notification-events", notificationEvent);

    }







    public Equipment changeEquipmentInterHospital(String equipmentId, String newHospitalId, String description, UserDTO user, String token) {
        System.out.println("**************************************");
        System.out.println(user);

        // Récupérer l'équipement depuis la base de données
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

        // Sauvegarder l'ancien et le nouveau statut de l'équipement
        String oldHospitalId = equipment.getHospitalId();
        equipment.setHospitalId(newHospitalId);
        equipment.setStatus("en attente de réception");
        equipment.setReception(false);
        equipmentRepository.save(equipment);

        // Récupérer l'admin du nouvel hôpital
        UserDTO adminOfNewHospital = userServiceClient.getAdminByHospitalId(token, newHospitalId);
        log.info("Admin du nouvel hôpital : " + adminOfNewHospital);

        // Récupérer les noms des hôpitaux
        String newHospitalName = hospitalServiceClient.getHospitalNameById(token, newHospitalId);
        String oldHospitalName = hospitalServiceClient.getHospitalNameById(token, oldHospitalId);
        log.info("Ancien hôpital : " + oldHospitalName + ", Nouvel hôpital : " + newHospitalName);

        // Récupérer les utilisateurs du service via UserServiceClient avec le token
        List<UserDTO> usersInService = userServiceClient.getUsersByHospitalAndRoles(token, oldHospitalId,
                List.of("ROLE_HOSPITAL_ADMIN", "ROLE_MINISTRY_ADMIN", "ROLE_MAINTENANCE_ENGINEER"));

        log.info("Utilisateurs du service : " + usersInService);

        // Liste des emails à notifier
        List<String> emailsToNotify = new ArrayList<>();
        emailsToNotify.add(user.getEmail()); // L'utilisateur qui effectue la transaction
        if (adminOfNewHospital != null) {
            emailsToNotify.add(adminOfNewHospital.getEmail()); // Admin du nouvel hôpital
        }

        // Ajouter tous les utilisateurs du service
        for (UserDTO serviceUser : usersInService) {
            emailsToNotify.add(serviceUser.getEmail());
        }

        log.info("Emails à notifier : " + emailsToNotify);

        // Création de l'objet de transfert pour Kafka
        // Dans EquipmentService
        EquipmentTransferEvent event = new EquipmentTransferEvent(
                equipment.getSerialCode(),
                equipment.getId(),
                equipment.getNom(),
                description,
                oldHospitalId, oldHospitalName,
                newHospitalId, newHospitalName,
                user.getFirstName(), user.getLastName(), user.getEmail(),
                emailsToNotify
        );

        log.info("Événement Kafka : " + event);

        // Envoyer l'événement Kafka pour notification à mail-service
        kafkaProducerService.sendMessage("equipment-events", event);
        NotificationEvent notificationEvent = new NotificationEvent(
                "Transfert d'équipement inter-hôpital",
                "L'équipement " + equipment.getNom() + " a été transféré de " + oldHospitalName + " à " + newHospitalName + ".",
                emailsToNotify
        );

// Envoyer l'événement à `notification-service`
        kafkaProducerService.sendMessage("notification-events", notificationEvent);


        EquipmentTransferHistory history = new EquipmentTransferHistory();
        history.setEquipmentId(equipmentId);
        history.setOldHospitalId(oldHospitalId);
        history.setNewHospitalId(newHospitalId);
        history.setType("INTER_HOSPITAL");
        history.setDescription(description);
        history.setInitiatedByUserId(user.getId());
        history.setInitiatedByName(user.getFirstName() + " " + user.getLastName());
        equipmentTransferHistoryRepository.save(history);

        return equipment;
    }
     public Optional<Equipment> findEquipmentById(String equipmentId){

        return equipmentRepository.findById(equipmentId);
     }


}
