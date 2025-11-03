package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.service.LoanApplicationService;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.*;

@RestController
@RequestMapping("/api/v1/loan-applications")
public class LoanApplicationController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(LoanApplicationController.class);

    private final LoanApplicationService applicationService;

    public LoanApplicationController(LoanApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponseDTO> createApplication(
            @Valid @RequestBody LoanApplicationRequestDTO dto) {
        spectraLogger.info("LOAN_APPLICATION_CREATE_REQUEST")
                .attr(BORROWER_ID, dto.getBorrowerId())
                .attr("productId", dto.getProductId())
                .attr("requestedAmount", dto.getRequestedAmount())
                .log();

        LoanApplicationResponseDTO response = applicationService.createApplication(dto);

        spectraLogger.info("LOAN_APPLICATION_CREATE_SUCCESS")
                .attr(APPLICATION_ID, response.getId())
                .attr("applicationNumber", response.getApplicationNumber())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanApplicationResponseDTO>> getApplications(
            @RequestParam(required = false) UUID borrowerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        spectraLogger.info("LOAN_APPLICATION_LIST_REQUEST")
                .attr("borrowerIdFilter", borrowerId)
                .attr("statusFilter", status)
                .attr("page", page)
                .attr(LIMIT, limit)
                .log();

        List<LoanApplicationResponseDTO> applications;
        if (borrowerId != null) {
            applications = applicationService.getApplicationsByBorrower(borrowerId);
        } else if (status != null) {
            applications = applicationService.getApplicationsByStatus(status, page, limit);
        } else {
            applications = applicationService.getAllApplications(page, limit);
        }

        spectraLogger.info("LOAN_APPLICATION_LIST_RESPONSE")
                .attr(COUNT, applications.size())
                .log();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponseDTO> getApplication(@PathVariable UUID id) {
        spectraLogger.info("LOAN_APPLICATION_FETCH_REQUEST").attr(APPLICATION_ID, id).log();
        LoanApplicationResponseDTO application = applicationService.getApplicationById(id);
        spectraLogger.info("LOAN_APPLICATION_FETCH_SUCCESS").attr(APPLICATION_ID, id).log();
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LoanApplicationResponseDTO> approveApplication(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        spectraLogger.info("LOAN_APPLICATION_APPROVE_REQUEST")
                .attr(APPLICATION_ID, id)
                .attr("approvedAmountRaw", String.valueOf(request.get(APPROVED_AMOUNT)))
                .log();

        BigDecimal approvedAmount = new BigDecimal(request.get(APPROVED_AMOUNT).toString());

        LoanApplicationResponseDTO response = applicationService.approveApplication(id, approvedAmount);

        spectraLogger.info("LOAN_APPLICATION_APPROVE_SUCCESS")
                .attr(APPLICATION_ID, id)
                .attr(APPROVED_AMOUNT, approvedAmount)
                .log();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        String rejectionReason = request.get("rejectionReason");
        spectraLogger.info("LOAN_APPLICATION_REJECT_REQUEST")
                .attr(APPLICATION_ID, id)
                .attr("reason", rejectionReason)
                .log();

        applicationService.rejectApplication(id, rejectionReason);

        spectraLogger.info("LOAN_APPLICATION_REJECT_SUCCESS")
                .attr(APPLICATION_ID, id)
                .log();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelApplication(@PathVariable UUID id) {
        spectraLogger.info("LOAN_APPLICATION_CANCEL_REQUEST").attr(APPLICATION_ID, id).log();
        applicationService.cancelApplication(id);
        spectraLogger.info("LOAN_APPLICATION_CANCEL_SUCCESS").attr(APPLICATION_ID, id).log();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getPendingApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        spectraLogger.info("LOAN_APPLICATION_PENDING_LIST_REQUEST")
                .attr("page", page)
                .attr(LIMIT, limit)
                .log();
        List<LoanApplicationResponseDTO> applications = applicationService.getPendingApplications(page, limit);
        spectraLogger.info("LOAN_APPLICATION_PENDING_LIST_RESPONSE")
                .attr(COUNT, applications.size())
                .log();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getExpiredApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        spectraLogger.info("LOAN_APPLICATION_EXPIRED_LIST_REQUEST")
                .attr("page", page)
                .attr(LIMIT, limit)
                .log();
        List<LoanApplicationResponseDTO> applications = applicationService.getExpiredApplications(page, limit);
        spectraLogger.info("LOAN_APPLICATION_EXPIRED_LIST_RESPONSE")
                .attr(COUNT, applications.size())
                .log();
        return ResponseEntity.ok(applications);
    }
}