package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.HouseholdRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.HouseholdResponseDTO;
import in.zeta.microloan.platform.service.HouseholdService;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(HouseholdController.class);

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    public ResponseEntity<HouseholdResponseDTO> createHousehold(@Valid @RequestBody HouseholdRegistrationRequestDTO dto) {
        spectraLogger.info("HOUSEHOLD_CREATE_REQUEST")
                .attr("pincode", dto.getPincode())
                .attr("city", dto.getCity())
                .log();
        HouseholdResponseDTO response = householdService.createHousehold(dto);
        spectraLogger.info("HOUSEHOLD_CREATE_SUCCESS")
                .attr("householdId", response.getId())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HouseholdResponseDTO> getHousehold(@PathVariable Long id) {
        spectraLogger.info("HOUSEHOLD_FETCH_REQUEST").attr("householdId", id).log();
        HouseholdResponseDTO response = householdService.getHouseholdById(id);
        spectraLogger.info("HOUSEHOLD_FETCH_SUCCESS").attr("householdId", id).log();
        return ResponseEntity.ok(response);
    }
}