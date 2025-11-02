package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.BorrowerCreditSummaryDTO;
import in.zeta.microloan.platform.dto.BorrowerRegistrationDTO;
import in.zeta.microloan.platform.dto.BorrowerResponseDTO;
import in.zeta.microloan.platform.dto.BorrowerUpdateDTO;
import in.zeta.microloan.platform.service.BorrowerService;
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
    public ResponseEntity<BorrowerResponseDTO> registerBorrower(
            @Valid @RequestBody BorrowerRegistrationDTO request) {
        BorrowerResponseDTO response = borrowerService.registerBorrower(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{borrowerId}")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerDetails(
            @PathVariable Long borrowerId) {
        BorrowerResponseDTO response = borrowerService.getBorrowerById(borrowerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerByPhone(
            @PathVariable String phone) {
        BorrowerResponseDTO response = borrowerService.getBorrowerByPhone(phone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/household/{householdId}")
    public ResponseEntity<List<BorrowerResponseDTO>> getBorrowersByHousehold(
            @PathVariable Long householdId) {
        List<BorrowerResponseDTO> borrowers = borrowerService.getBorrowersByHousehold(householdId);
        return ResponseEntity.ok(borrowers);
    }

    @GetMapping
    public ResponseEntity<List<BorrowerResponseDTO>> getAllBorrowers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        List<BorrowerResponseDTO> borrowers = borrowerService.getAllBorrowers(status, page, limit);
        return ResponseEntity.ok(borrowers);
    }

    @PutMapping("/{borrowerId}")
    public ResponseEntity<BorrowerResponseDTO> updateBorrowerDetails(
            @PathVariable Long borrowerId,
            @Valid @RequestBody BorrowerUpdateDTO request) {
        BorrowerResponseDTO response = borrowerService.updateBorrower(borrowerId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/verify")
    public ResponseEntity<BorrowerResponseDTO> verifyBorrower(
            @PathVariable Long borrowerId) {
        BorrowerResponseDTO response = borrowerService.verifyBorrower(borrowerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/status")
    public ResponseEntity<BorrowerResponseDTO> updateBorrowerStatus(
            @PathVariable Long borrowerId,
            @RequestParam String status) {
        BorrowerResponseDTO response = borrowerService.updateBorrowerStatus(borrowerId, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{borrowerId}")
    public ResponseEntity<Void> deleteBorrower(@PathVariable Long borrowerId) {
        borrowerService.deleteBorrower(borrowerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{borrowerId}/credit-summary")
    public ResponseEntity<BorrowerCreditSummaryDTO> getBorrowerCreditSummary(
            @PathVariable Long borrowerId) {
        BorrowerCreditSummaryDTO summary = borrowerService.getBorrowerCreditSummary(borrowerId);
        return ResponseEntity.ok(summary);
    }
}
