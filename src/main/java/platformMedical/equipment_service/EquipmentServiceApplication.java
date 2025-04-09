package platformMedical.equipment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import platformMedical.equipment_service.entity.*;
import platformMedical.equipment_service.entity.DTOs.EquipmentRequest;
import platformMedical.equipment_service.entity.DTOs.MessageResponse;
import platformMedical.equipment_service.service.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


import java.util.ArrayList;
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "platformMedical.equipment_service.entity.DTOs") // Enable Feign clients
@EnableScheduling

public class EquipmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(EquipmentService equipmentService, SlaService slaService,
                                   BrandService brandService,
                                   SparePartService sparePartService, MongoTemplate mongoTemplate,
                                   IncidentService incidentService) {
        return args -> {
            System.out.println("Initialisation de la base de données avec les nouvelles données...");

            if (!isDatabaseEmpty(mongoTemplate)) {
                System.out.println("Base de données déjà initialisée.");
                return;
            }

            System.out.println("Initialisation des données dans la base...");

            // *** Création des marques ***
            Brand brand1 = brandService.createBrand(
                    Brand.builder()
                            .name("BD (Becton, Dickinson and Company)")
                            .hospitalId("7a34da16-6bd3-4cc6-8aa6-c1d512c2bf4e")
                            .build()
            );

            Brand brand2 = brandService.createBrand(
                    Brand.builder()
                            .name("Beckman Coulter")
                            .hospitalId("d09282f5-e925-479e-be2f-058ba1c0ab2a")
                            .build()
            );

            // *** Création des équipements ***
            MessageResponse equipment1 = equipmentService.createEquipment(
                    EquipmentRequest.builder()
                            .nom("Scanner IRM")
                            .emdnCode("Z119011")
                            .lifespan(10)
                            .riskClass("Classe IIa")
                            .hospitalId("7a34da16-6bd3-4cc6-8aa6-c1d512c2bf4e")
                            .serviceId("d09282f5-e925-479e-be2f-058ba1c0ab2a")
                            .brand("Beckman Coulter")
                            .amount(120000.0)
                            .supplier("Fournisseur ABC")
                            .acquisitionDate(new Date())
                            .startDateWarranty(new Date())
                            .endDateWarranty(new Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000))
                            .build()
            );

            MessageResponse equipment2 = equipmentService.createEquipment(
                    EquipmentRequest.builder()
                            .nom("Prothèse 3D")
                            .emdnCode("Z110102")
                            .lifespan(5)
                            .riskClass("Classe I")
                            .hospitalId("d09282f5-e925-479e-be2f-058ba1c0ab2a")
                            .serviceId("67bd9d2ee3dbea56f771281f")
                            .brand("BD (Becton, Dickinson and Company)")
                            .amount(25000.0)
                            .supplier("Fournisseur XYZ")
                            .acquisitionDate(new Date())
                            .startDateWarranty(new Date())
                            .endDateWarranty(new Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000))
                            .build()
            );

// *** Création et association de pièces de rechange ***
            SparePart sparePart1 = sparePartService.createSparePart(
                    SparePart.builder()
                            .name("Tube à rayons X")
                            .lifespan(5)
                            .supplier("Fournisseur C")
                            .hospitalId("7a34da16-6bd3-4cc6-8aa6-c1d512c2bf4e")
                            .serviceId("d09282f5-e925-479e-be2f-058ba1c0ab2a")
                            .equipmentId(equipment1.getData()) // Correction
                            .maintenancePlans(new ArrayList<>()) // Ajouter des plans de maintenance vides si nécessaire
                            .lots(Arrays.asList( // Ajouter un ou plusieurs lots
                                    SparePartLot.builder()
                                            .quantity(10) // Quantité du lot
                                            .startDateWarranty(new Date()) // Date de début de la garantie
                                            .endDateWarranty(new Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000)) // Date de fin de la garantie (ex: 2 ans)
                                            .acquisitionDate(new Date()) // Date d'acquisition
                                            .build()
                            ))
                            .build()
            );

            SparePart sparePart2 = sparePartService.createSparePart(
                    SparePart.builder()
                            .name("Batterie")
                            .lifespan(3)
                            .supplier("Fournisseur D")
                            .hospitalId("d09282f5-e925-479e-be2f-058ba1c0ab2a")
                            .serviceId("67bd9d2ee3dbea56f771281f")
                            .equipmentId(equipment2.getData()) // Correction
                            .maintenancePlans(new ArrayList<>()) // Ajouter des plans de maintenance vides si nécessaire
                            .lots(Arrays.asList( // Ajouter un ou plusieurs lots
                                    SparePartLot.builder()
                                            .quantity(5) // Quantité du lot
                                            .startDateWarranty(new Date()) // Date de début de la garantie
                                            .endDateWarranty(new Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000)) // Date de fin de la garantie (ex: 2 ans)
                                            .acquisitionDate(new Date()) // Date d'acquisition
                                            .build()
                            ))
                            .build()
            );

// Associer les pièces de rechange aux équipements
            equipmentService.addSparePart(equipment1.getData(), sparePart1);
            equipmentService.addSparePart(equipment2.getData(), sparePart2);


            // *** Création des SLA associés aux équipements ***
            SLA sla1 = slaService.createSla(
                    SLA.builder()
                            .name("SLA Scanner IRM")
                            .equipmentId(equipment1.getData())
                            .maxResponseTime(4)
                            .maxResolutionTime(48)
                            .penaltyAmount(500.0)
                            .userIdCompany("67bcd293911fbd57e8e1e613")
                            .build()
            );

            SLA sla2 = slaService.createSla(
                    SLA.builder()
                            .name("SLA Prothèse 3D")
                            .equipmentId(equipment2.getData())
                            .maxResponseTime(2)
                            .maxResolutionTime(24)
                            .penaltyAmount(300.0)
                            .userIdCompany("67b9bd6c1e3d2048a96b9e3c")
                            .build()
            );

            // *** Création des plans de maintenance ***
            equipmentService.addMaintenancePlan(equipment1.getData(),
                    MaintenancePlan.builder()
                            .maintenanceDate(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
                            .description("Maintenance préventive annuelle pour Scanner IRM")
                            .build()
            );

            equipmentService.addMaintenancePlan(equipment2.getData(),
                    MaintenancePlan.builder()
                            .maintenanceDate(new Date(System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000))
                            .description("Vérification des composants pour Prothèse 3D")
                            .build()
            );

            // *** Signalement des incidents ***
            incidentService.reportIncident(
                    equipment1.getData(),
                    "67bcd5d9911fbd57e8e1e615",
                    "Problème de refroidissement détecté"
            );

            incidentService.reportIncident(
                    equipment2.getData(),
                    "67b9bd6b1e3d2048a96b9e3a",
                    "Dysfonctionnement du moteur"
            );

            System.out.println("Base de données initialisée avec succès !");
        };
    }

    private boolean isDatabaseEmpty(MongoTemplate mongoTemplate) {
        List<String> collections = mongoTemplate.getDb().listCollectionNames().into(new ArrayList<>());
        return collections.isEmpty();
    }
}
