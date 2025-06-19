package platformMedical.equipment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import platformMedical.equipment_service.entity.Brand;
import platformMedical.equipment_service.entity.DTOs.EquipmentRequest;
import platformMedical.equipment_service.entity.DTOs.MessageResponse;
import platformMedical.equipment_service.entity.EmdnNomenclature;
import platformMedical.equipment_service.entity.Supplier;
import platformMedical.equipment_service.repository.BrandRepository;
import platformMedical.equipment_service.repository.EmdnNomenclatureRepository;
import platformMedical.equipment_service.repository.SupplierRepository;
import platformMedical.equipment_service.service.EquipmentService;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EquipmentServiceApplicationTests {

	@Autowired
	private EquipmentService equipmentService;

	@Autowired
	private EmdnNomenclatureRepository emdnRepository;

	@Autowired
	private BrandRepository brandRepository;

	@Autowired
	private SupplierRepository supplierRepository;

	@Test
	void testCreateEquipment1_Success() {
		// Arrange : créer les dépendances nécessaires
		EmdnNomenclature emdn = emdnRepository.save(new EmdnNomenclature(null, "TEST-EMDN", "Appareil test", null));
		Brand brand = brandRepository.save(Brand.builder().name("TEST-BRAND").hospitalId("HOSP123").build());
		Supplier supplier = supplierRepository.save(Supplier.builder().name("Test Supplier").build());

		EquipmentRequest request = new EquipmentRequest();
		request.setNom("Équipement Testé");
		request.setEmdnCode(emdn.getCode());
		request.setLifespan(10);
		request.setRiskClass("IIb");
		request.setHospitalId("HOSP123");
		request.setAcquisitionDate(new Date());
		request.setAmount(15000.0);
		request.setStartDateWarranty(new Date());
		request.setEndDateWarranty(new Date(System.currentTimeMillis() + 100000000)); // Date future
		request.setFromMinistere(true);
		request.setBrand(brand.getName());
		request.setSupplierId(supplier.getId());

		// Act
		MessageResponse response = equipmentService.createEquipment1(request);

		// Assert
		assertNotNull(response);
		assertTrue(response.getMessage().contains("Équipement créé avec succès"));
		assertNotNull(response.getData()); // le code série généré
		System.out.println("✅ Équipement créé avec le code : " + response.getData());
	}
}
