package platformMedical.equipment_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.Brand;
import platformMedical.equipment_service.service.BrandService;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@AllArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    public Brand createBrand(@RequestBody Brand brand) {
        return brandService.createBrand(brand);
    }

    @GetMapping("/hospital/{hospitalId}")
    public List<Brand> getBrandsByHospitalId(@PathVariable String hospitalId) {
        return brandService.getBrandsByHospitalId(hospitalId);
    }

    @GetMapping("/{id}")
    public Brand getBrandById(@PathVariable String id) {
        return brandService.getBrandById(id);
    }

    @PutMapping("/{id}")
    public Brand updateBrand(@PathVariable String id, @RequestBody Brand updatedBrand) {
        return brandService.updateBrand(id, updatedBrand);
    }

    @DeleteMapping("/{id}")
    public void deleteBrand(@PathVariable String id) {
        brandService.deleteBrand(id);
    }
}