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
                .attr("borrowerId", dto.getBorrowerId())
                .attr("productId", dto.getProductId())
                .attr("requestedAmount", dto.getRequestedAmount())
                .log();

        LoanApplicationResponseDTO response = applicationService.createApplication(dto);

        spectraLogger.info("LOAN_APPLICATION_CREATE_SUCCESS")
                .attr("applicationId", response.getId())
                .attr("applicationNumber", response.getApplicationNumber())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanApplicationResponseDTO>> getApplications(
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        spectraLogger.info("LOAN_APPLICATION_LIST_REQUEST")
                .attr("borrowerIdFilter", borrowerId)
                .attr("statusFilter", status)
                .attr("page", page)
                .attr("limit", limit)
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
                .attr("count", applications.size())
                .log();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponseDTO> getApplication(@PathVariable Long id) {
        spectraLogger.info("LOAN_APPLICATION_FETCH_REQUEST").attr("applicationId", id).log();
        LoanApplicationResponseDTO application = applicationService.getApplicationById(id);
        spectraLogger.info("LOAN_APPLICATION_FETCH_SUCCESS").attr("applicationId", id).log();
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LoanApplicationResponseDTO> approveApplication(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        spectraLogger.info("LOAN_APPLICATION_APPROVE_REQUEST")
                .attr("applicationId", id)
                .attr("approvedByRaw", String.valueOf(request.get("approvedBy")))
                .attr("approvedAmountRaw", String.valueOf(request.get("approvedAmount")))
                .log();

        Long approvedBy = Long.valueOf(request.get("approvedBy").toString());
        BigDecimal approvedAmount = new BigDecimal(request.get("approvedAmount").toString());

        LoanApplicationResponseDTO response = applicationService.approveApplication(id, approvedBy, approvedAmount);

        spectraLogger.info("LOAN_APPLICATION_APPROVE_SUCCESS")
                .attr("applicationId", id)
                .attr("approvedAmount", approvedAmount)
                .attr("approvedBy", approvedBy)
                .log();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String rejectionReason = request.get("rejectionReason");
        spectraLogger.info("LOAN_APPLICATION_REJECT_REQUEST")
                .attr("applicationId", id)
                .attr("reason", rejectionReason)
                .log();

        applicationService.rejectApplication(id, rejectionReason);

        spectraLogger.info("LOAN_APPLICATION_REJECT_SUCCESS")
                .attr("applicationId", id)
                .log();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<LoanApplicationResponseDTO> moveToVerification(@PathVariable Long id) {
        spectraLogger.info("LOAN_APPLICATION_VERIFY_REQUEST").attr("applicationId", id).log();
        LoanApplicationResponseDTO response = applicationService.moveToVerification(id);
        spectraLogger.info("LOAN_APPLICATION_VERIFY_SUCCESS").attr("applicationId", id).log();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelApplication(@PathVariable Long id) {
        spectraLogger.info("LOAN_APPLICATION_CANCEL_REQUEST").attr("applicationId", id).log();
        applicationService.cancelApplication(id);
        spectraLogger.info("LOAN_APPLICATION_CANCEL_SUCCESS").attr("applicationId", id).log();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getPendingApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        spectraLogger.info("LOAN_APPLICATION_PENDING_LIST_REQUEST")
                .attr("page", page)
                .attr("limit", limit)
                .log();
        List<LoanApplicationResponseDTO> applications = applicationService.getPendingApplications(page, limit);
        spectraLogger.info("LOAN_APPLICATION_PENDING_LIST_RESPONSE")
                .attr("count", applications.size())
                .log();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getExpiredApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        spectraLogger.info("LOAN_APPLICATION_EXPIRED_LIST_REQUEST")
                .attr("page", page)
                .attr("limit", limit)
                .log();
        List<LoanApplicationResponseDTO> applications = applicationService.getExpiredApplications(page, limit);
        spectraLogger.info("LOAN_APPLICATION_EXPIRED_LIST_RESPONSE")
                .attr("count", applications.size())
                .log();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/borrower/{borrowerId}/latest")
    public ResponseEntity<LoanApplicationResponseDTO> getLatestApplication(@PathVariable Long borrowerId) {
        spectraLogger.info("LOAN_APPLICATION_LATEST_BY_BORROWER_REQUEST")
                .attr("borrowerId", borrowerId)
                .log();
        LoanApplicationResponseDTO application = applicationService.getLatestApplicationByBorrower(borrowerId);
        spectraLogger.info("LOAN_APPLICATION_LATEST_BY_BORROWER_SUCCESS")
                .attr("borrowerId", borrowerId)
                .attr("applicationId", application.getId())
                .log();
        return ResponseEntity.ok(application);
    }
}