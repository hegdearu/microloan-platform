package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.provider.UserProvider;
import in.zeta.microloan.platform.service.BorrowerService;
import in.zeta.spectra.capture.SpectraLogger;
import in.zeta.springframework.boot.commons.authorization.sandboxAccessControl.SandboxAuthorizedSync;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(BorrowerController.class);

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping("/register")
//  @SandboxAuthorizedSync(action = "user.create", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> registerBorrower(
            @Valid @RequestBody BorrowerRegistrationRequestDTO request) {
        spectraLogger.info("BORROWER_REGISTER_REQUEST")
                .attr("name", request.getName())
                .attr("phone", request.getPhone())
                .log();
        BorrowerResponseDTO response = borrowerService.registerBorrower(request);
        spectraLogger.info("BORROWER_REGISTER_SUCCESS")
                .attr("borrowerId", response.getId())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{borrowerId}")
//        @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerDetails(@PathVariable Long borrowerId) {
        spectraLogger.info("BORROWER_FETCH_REQUEST")
                .attr("borrowerId", borrowerId)
                .log();
        BorrowerResponseDTO response = borrowerService.getBorrowerById(borrowerId);
        spectraLogger.info("BORROWER_FETCH_SUCCESS")
                .attr("borrowerId", borrowerId)
                .log();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phone/{phone}")
    //    @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerByPhone(@PathVariable String phone) {
        spectraLogger.info("BORROWER_FETCH_BY_PHONE_REQUEST")
                .attr("phone", phone)
                .log();
        BorrowerResponseDTO response = borrowerService.getBorrowerByPhone(phone);
        spectraLogger.info("BORROWER_FETCH_BY_PHONE_SUCCESS")
                .attr("borrowerId", response.getId())
                .log();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/household/{householdId}")
    //    @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<List<BorrowerResponseDTO>> getBorrowersByHousehold(@PathVariable Long householdId) {
        spectraLogger.info("BORROWER_LIST_BY_HOUSEHOLD_REQUEST")
                .attr("householdId", householdId)
                .log();
        List<BorrowerResponseDTO> borrowers = borrowerService.getBorrowersByHousehold(householdId);
        spectraLogger.info("BORROWER_LIST_BY_HOUSEHOLD_SUCCESS")
                .attr("householdId", householdId)
                .attr("count", borrowers.size())
                .log();
        return ResponseEntity.ok(borrowers);
    }

    @GetMapping
//    @SandboxAuthorizedSync(action = "user.getAll", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<List<BorrowerResponseDTO>> getAllBorrowers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        spectraLogger.info("BORROWER_LIST_REQUEST")
                .attr("statusFilter", status)
                .attr("page", page)
                .attr("limit", limit)
                .log();
        List<BorrowerResponseDTO> borrowers = borrowerService.getAllBorrowers(status, page, limit);
        spectraLogger.info("BORROWER_LIST_SUCCESS")
                .attr("count", borrowers.size())
                .log();
        return ResponseEntity.ok(borrowers);
    }

    @PutMapping("/{borrowerId}")
//    @SandboxAuthorizedSync(action = "user.update", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> updateBorrowerDetails(
            @PathVariable Long borrowerId,
            @Valid @RequestBody BorrowerUpdateRequestDTO request) {
        spectraLogger.info("BORROWER_UPDATE_REQUEST")
                .attr("borrowerId", borrowerId)
                .log();
        BorrowerResponseDTO response = borrowerService.updateBorrower(borrowerId, request);
        spectraLogger.info("BORROWER_UPDATE_SUCCESS")
                .attr("borrowerId", response.getId())
                .log();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/verify")
    //    @SandboxAuthorizedSync(action = "user.verify", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> verifyBorrower(@PathVariable Long borrowerId) {
        spectraLogger.info("BORROWER_VERIFY_REQUEST")
                .attr("borrowerId", borrowerId)
                .log();
        BorrowerResponseDTO response = borrowerService.verifyBorrower(borrowerId);
        spectraLogger.info("BORROWER_VERIFY_SUCCESS")
                .attr("borrowerId", borrowerId)
                .log();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/status")
//    @SandboxAuthorizedSync(action = "user.status.update", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> updateBorrowerStatus(
            @PathVariable Long borrowerId,
            @RequestParam String status) {
        spectraLogger.info("BORROWER_STATUS_UPDATE_REQUEST")
                .attr("borrowerId", borrowerId)
                .attr("newStatus", status)
                .log();
        BorrowerResponseDTO response = borrowerService.updateBorrowerStatus(borrowerId, status);
        spectraLogger.info("BORROWER_STATUS_UPDATE_SUCCESS")
                .attr("borrowerId", borrowerId)
                .attr("updatedStatus", response.getStatus())
                .log();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{borrowerId}")
//    @SandboxAuthorizedSync(action = "user.delete", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<Void> deleteBorrower(@PathVariable Long borrowerId) {
        spectraLogger.info("BORROWER_DELETE_REQUEST")
                .attr("borrowerId", borrowerId)
                .log();
        borrowerService.deleteBorrower(borrowerId);
        spectraLogger.info("BORROWER_DELETE_SUCCESS")
                .attr("borrowerId", borrowerId)
                .log();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{borrowerId}/credit-summary")
    //    @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerCreditSummaryResponseDTO> getBorrowerCreditSummary(@PathVariable Long borrowerId) {
        spectraLogger.info("BORROWER_CREDIT_SUMMARY_REQUEST")
                .attr("borrowerId", borrowerId)
                .log();
        BorrowerCreditSummaryResponseDTO summary = borrowerService.getBorrowerCreditSummary(borrowerId);
        spectraLogger.info("BORROWER_CREDIT_SUMMARY_SUCCESS")
                .attr("borrowerId", borrowerId)
                .log();
        return ResponseEntity.ok(summary);
    }
}