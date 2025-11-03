package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.provider.UserProvider;
import in.zeta.microloan.platform.service.BorrowerService;
import in.zeta.springframework.boot.commons.authorization.sandboxAccessControl.SandboxAuthorizedSync;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping("/register")
    @SandboxAuthorizedSync(action = "user.create", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> registerBorrower(
            @Valid @RequestBody BorrowerRegistrationRequestDTO request) {
        BorrowerResponseDTO response = borrowerService.registerBorrower(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{borrowerId}")
    @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerDetails(
            @PathVariable Long borrowerId) {
        BorrowerResponseDTO response = borrowerService.getBorrowerById(borrowerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phone/{phone}")
    @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerByPhone(
            @PathVariable String phone) {
        BorrowerResponseDTO response = borrowerService.getBorrowerByPhone(phone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/household/{householdId}")
    @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<List<BorrowerResponseDTO>> getBorrowersByHousehold(
            @PathVariable Long householdId) {
        List<BorrowerResponseDTO> borrowers = borrowerService.getBorrowersByHousehold(householdId);
        return ResponseEntity.ok(borrowers);
    }

    @GetMapping
    @SandboxAuthorizedSync(action = "user.getAll", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<List<BorrowerResponseDTO>> getAllBorrowers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        List<BorrowerResponseDTO> borrowers = borrowerService.getAllBorrowers(status, page, limit);
        return ResponseEntity.ok(borrowers);
    }

    @PutMapping("/{borrowerId}")
    @SandboxAuthorizedSync(action = "user.update", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> updateBorrowerDetails(
            @PathVariable Long borrowerId,
            @Valid @RequestBody BorrowerUpdateRequestDTO request) {
        BorrowerResponseDTO response = borrowerService.updateBorrower(borrowerId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/verify")
    @SandboxAuthorizedSync(action = "user.verify", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> verifyBorrower(
            @PathVariable Long borrowerId) {
        BorrowerResponseDTO response = borrowerService.verifyBorrower(borrowerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/status")
    @SandboxAuthorizedSync(action = "user.status.update", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> updateBorrowerStatus(
            @PathVariable Long borrowerId,
            @RequestParam String status) {
        BorrowerResponseDTO response = borrowerService.updateBorrowerStatus(borrowerId, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{borrowerId}")
    @SandboxAuthorizedSync(action = "user.delete", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<Void> deleteBorrower(@PathVariable Long borrowerId) {
        borrowerService.deleteBorrower(borrowerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{borrowerId}/credit-summary")
    @SandboxAuthorizedSync(action = "user.get", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerCreditSummaryResponseDTO> getBorrowerCreditSummary(
            @PathVariable Long borrowerId) {
        BorrowerCreditSummaryResponseDTO summary = borrowerService.getBorrowerCreditSummary(borrowerId);
        return ResponseEntity.ok(summary);
    }
}
