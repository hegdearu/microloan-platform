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
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.HOUSEHOLD_ID;

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
                .attr(HOUSEHOLD_ID, response.getId())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<HouseholdResponseDTO> verifyHousehold(@PathVariable UUID id) {
        spectraLogger.info("HOUSEHOLD_VERIFY_REQUEST").attr(HOUSEHOLD_ID, id).log();
        HouseholdResponseDTO response = householdService.verifyHousehold(id);
        spectraLogger.info("HOUSEHOLD_VERIFY_SUCCESS").attr(HOUSEHOLD_ID, id).log();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HouseholdResponseDTO> getHousehold(@PathVariable UUID id) {
        spectraLogger.info("HOUSEHOLD_FETCH_REQUEST").attr(HOUSEHOLD_ID, id).log();
        HouseholdResponseDTO response = householdService.getHouseholdById(id);
        spectraLogger.info("HOUSEHOLD_FETCH_SUCCESS").attr(HOUSEHOLD_ID, id).log();
        return ResponseEntity.ok(response);
    }
}