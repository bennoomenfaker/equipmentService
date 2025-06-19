package platformMedical.equipment_service.entity.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import platformMedical.equipment_service.entity.Supplier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentRequest {

    // Champs obligatoires (Ministère de la Santé)
    private String nom;
    private String emdnCode;
    private int lifespan;
    private String riskClass;
    private String hospitalId;
    private String serialNumber; // Généré automatiquement

    // Champs optionnels (remplis par l’hôpital après réception)
    private Double amount;
    private String supplierId;
    private Date acquisitionDate;
    private String serviceId;
    private String brand;
    private String slaId;
    private Date startDateWarranty; // Date de début de la garantie
    private Date endDateWarranty; // Date de fin de la garantie
    private boolean reception;
    private String status;
    private int useCount; // Nombre d'utilisations (pour certains équipements)
    private double usageDuration; // Durée totale d'utilisation en heures
    private LocalDateTime lastUsedAt; // Date de dernière utilisation
    private boolean fromMinistere; // true = ministère, false = fournisseur externe



    public EquipmentRequest(String nom,
                            int lifespan,
                            String riskClass,
                            String hospitalId,
                            String serialNumber,
                            Double amount,
                            String supplierId,
                            Date acquisitionDate,
                            String serviceId,
                            String slaId,
                            Date startDateWarranty,
                            Date endDateWarranty,
                            boolean reception,
                            String status,
                            int useCount,
                            double usageDuration,
                            LocalDateTime lastUsedAt) {
        this.nom = nom;
        this.emdnCode = emdnCode;
        this.lifespan = lifespan;
        this.riskClass = riskClass;
        this.hospitalId = hospitalId;
        this.serialNumber = serialNumber;
        this.amount = amount != null ? amount : 0.0; // Valeur par défaut
        this.supplierId = supplierId;
        this.acquisitionDate = acquisitionDate;
        this.serviceId = serviceId;
        this.slaId = slaId;
        this.startDateWarranty = startDateWarranty;
        this.endDateWarranty = endDateWarranty;
        this.reception = reception;
        this.status = status;
        this.useCount = useCount;
        this.usageDuration = usageDuration;
        this.lastUsedAt = lastUsedAt;
    }

    public EquipmentRequest(String nom,
                            int lifespan,
                            String riskClass,
                            String hospitalId,
                            String serialNumber,
                            Double amount,
                            String supplierId,
                            Date acquisitionDate,
                            String serviceId,
                            String slaId,
                            Date startDateWarranty,
                            Date endDateWarranty,
                            boolean reception,
                            String status,
                            int useCount,
                            double usageDuration,
                            LocalDateTime lastUsedAt,
                            boolean fromMinistere) {
        this.nom = nom;
        this.lifespan = lifespan;
        this.riskClass = riskClass;
        this.hospitalId = hospitalId;
        this.serialNumber = serialNumber;
        this.amount = amount != null ? amount : 0.0;
        this.supplierId = supplierId;
        this.acquisitionDate = acquisitionDate;
        this.serviceId = serviceId;
        this.slaId = slaId;
        this.startDateWarranty = startDateWarranty;
        this.endDateWarranty = endDateWarranty;
        this.reception = reception;
        this.status = status;
        this.useCount = useCount;
        this.usageDuration = usageDuration;
        this.lastUsedAt = lastUsedAt;
        this.fromMinistere = fromMinistere;
    }



}
