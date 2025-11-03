package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.response.LoanDetailResponseDTO;
import in.zeta.microloan.platform.dto.request.LoanIssuanceRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.service.LoanService;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.*;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(LoanController.class);

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponseDTO> createLoan(
            @Valid @RequestBody LoanIssuanceRequestDTO dto,
            @RequestParam Long createdBy) {

        spectraLogger.info("LOAN_CREATE_REQUEST")
                .attr(BORROWER_ID, dto.getBorrowerId())
                .attr("productId", dto.getProductId())
                .attr("principalAmount", dto.getPrincipalAmount())
                .attr("createdBy", createdBy)
                .log();

        LoanResponseDTO response = loanService.createLoan(dto, createdBy);

        spectraLogger.info("LOAN_CREATE_SUCCESS")
                .attr(LOAN_ID, response.getId())
                .attr("loanNumber", response.getLoanNumber())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> getLoanById(@PathVariable UUID id) {
        spectraLogger.info("LOAN_FETCH_REQUEST").attr(LOAN_ID, id).log();
        LoanResponseDTO loan = loanService.getLoanById(id);
        spectraLogger.info("LOAN_FETCH_SUCCESS").attr(LOAN_ID, id).log();
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<LoanDetailResponseDTO> getLoanDetails(@PathVariable UUID id) {
        spectraLogger.info("LOAN_DETAILS_REQUEST").attr(LOAN_ID, id).log();
        LoanDetailResponseDTO details = loanService.getLoanDetails(id);
        spectraLogger.info("LOAN_DETAILS_SUCCESS").attr(LOAN_ID, id).log();
        return ResponseEntity.ok(details);
    }

    @GetMapping("/borrower/{borrowerId}")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByBorrower(@PathVariable UUID borrowerId) {
        spectraLogger.info("LOANS_BY_BORROWER_REQUEST").attr(BORROWER_ID, borrowerId).log();
        List<LoanResponseDTO> loans = loanService.getLoansByBorrower(borrowerId);
        spectraLogger.info("LOANS_BY_BORROWER_SUCCESS")
                .attr(BORROWER_ID, borrowerId)
                .attr(COUNT, loans.size())
                .log();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/household/{householdId}")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByHousehold(@PathVariable UUID householdId) {
        spectraLogger.info("LOANS_BY_HOUSEHOLD_REQUEST").attr("householdId", householdId).log();
        List<LoanResponseDTO> loans = loanService.getLoansByHousehold(householdId);
        spectraLogger.info("LOANS_BY_HOUSEHOLD_SUCCESS")
                .attr("householdId", householdId)
                .attr(COUNT, loans.size())
                .log();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoanResponseDTO>> getLoansByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        spectraLogger.info("LOANS_BY_STATUS_REQUEST")
                .attr("status", status)
                .attr("page", page)
                .attr("limit", limit)
                .log();

        List<LoanResponseDTO> loans = loanService.getLoansByStatus(status, page, limit);

        spectraLogger.info("LOANS_BY_STATUS_SUCCESS")
                .attr("status", status)
                .attr(COUNT, loans.size())
                .log();
        return ResponseEntity.ok(loans);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelLoan(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        spectraLogger.info("LOAN_CANCEL_REQUEST")
                .attr(LOAN_ID, id)
                .attr("reason", reason)
                .log();
        loanService.cancelLoan(id, reason);
        spectraLogger.info("LOAN_CANCEL_SUCCESS").attr(LOAN_ID, id).log();
        return ResponseEntity.ok().build();
    }
}