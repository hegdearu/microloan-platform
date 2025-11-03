package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.service.HouseholdService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    public ResponseEntity<HouseholdResponseDTO> createHousehold(
            @Valid @RequestBody HouseholdRegistrationRequestDTO dto) {
        HouseholdResponseDTO response = householdService.createHousehold(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HouseholdResponseDTO> getHousehold(@PathVariable Long id) {
        HouseholdResponseDTO response = householdService.getHouseholdById(id);
        return ResponseEntity.ok(response);
    }
}
