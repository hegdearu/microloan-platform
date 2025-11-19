package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.*;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponseDTO> createLoan(
            @Valid @RequestBody LoanIssuanceRequestDTO dto,
            @RequestParam Long createdBy) {

        LoanResponseDTO response = loanService.createLoan(dto, createdBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> getLoanById(@PathVariable UUID id) {
        LoanResponseDTO loan = loanService.getLoanById(id);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<LoanDetailResponseDTO> getLoanDetails(@PathVariable UUID id) {
        LoanDetailResponseDTO details = loanService.getLoanDetails(id);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/borrower/{borrowerId}")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByBorrower(@PathVariable UUID borrowerId) {
        List<LoanResponseDTO> loans = loanService.getLoansByBorrower(borrowerId);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/household/{householdId}")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByHousehold(@PathVariable UUID householdId) {
        List<LoanResponseDTO> loans = loanService.getLoansByHousehold(householdId);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<LoanResponseDTO> loans = loanService.getLoansByStatus(status, page, limit);
        return ResponseEntity.ok(loans);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelLoan(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        loanService.cancelLoan(id, reason);
        return ResponseEntity.ok().build();
    }
}