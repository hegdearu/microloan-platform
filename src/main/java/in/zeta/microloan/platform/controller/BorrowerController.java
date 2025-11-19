package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.response.BorrowerCreditSummaryResponseDTO;
import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.response.BorrowerResponseDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.service.BorrowerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping("/register")
    public ResponseEntity<BorrowerResponseDTO> registerBorrower(
            @Valid @RequestBody BorrowerRegistrationRequestDTO request) {
        BorrowerResponseDTO response = borrowerService.registerBorrower(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{borrowerId}")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerDetails(@PathVariable UUID borrowerId) {
        BorrowerResponseDTO response = borrowerService.getBorrowerById(borrowerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerByPhone(@PathVariable String phone) {
        BorrowerResponseDTO response = borrowerService.getBorrowerByPhone(phone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/household/{householdId}")
    public ResponseEntity<List<BorrowerResponseDTO>> getBorrowersByHousehold(@PathVariable UUID householdId) {
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
            @PathVariable UUID borrowerId,
            @Valid @RequestBody BorrowerUpdateRequestDTO request) {
        BorrowerResponseDTO response = borrowerService.updateBorrower(borrowerId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/verify")
    public ResponseEntity<BorrowerResponseDTO> verifyBorrower(@PathVariable UUID borrowerId) {
        BorrowerResponseDTO response = borrowerService.verifyBorrower(borrowerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}/status")
    public ResponseEntity<BorrowerResponseDTO> updateBorrowerStatus(
            @PathVariable UUID borrowerId,
            @RequestParam String status) {
        BorrowerResponseDTO response = borrowerService.updateBorrowerStatus(borrowerId, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{borrowerId}/credit-summary")
    public ResponseEntity<BorrowerCreditSummaryResponseDTO> getBorrowerCreditSummary(@PathVariable UUID borrowerId) {
        BorrowerCreditSummaryResponseDTO summary = borrowerService.getBorrowerCreditSummary(borrowerId);
        return ResponseEntity.ok(summary);
    }
}