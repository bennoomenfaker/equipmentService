package platformMedical.equipment_service.entity;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SparePartLot {
    private int quantity; // Quantité de ce lot
    private Date startDateWarranty; // Date de début de la garantie
    private Date endDateWarranty; // Date de fin de la garantie
    private Date acquisitionDate; // Date d'acquisition
}
