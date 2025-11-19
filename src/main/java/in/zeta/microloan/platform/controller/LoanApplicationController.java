package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.service.LoanApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.*;

@RestController
@RequestMapping("/api/v1/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService applicationService;

    public LoanApplicationController(LoanApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponseDTO> createApplication(
            @Valid @RequestBody LoanApplicationRequestDTO dto) {
        LoanApplicationResponseDTO response = applicationService.createApplication(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanApplicationResponseDTO>> getApplications(
            @RequestParam(required = false) UUID borrowerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<LoanApplicationResponseDTO> applications;
        if (borrowerId != null) {
            applications = applicationService.getApplicationsByBorrower(borrowerId);
        } else if (status != null) {
            applications = applicationService.getApplicationsByStatus(status, page, limit);
        } else {
            applications = applicationService.getAllApplications(page, limit);
        }
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponseDTO> getApplication(@PathVariable UUID id) {
        LoanApplicationResponseDTO application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LoanApplicationResponseDTO> approveApplication(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        BigDecimal approvedAmount = new BigDecimal(request.get(APPROVED_AMOUNT).toString());
        LoanApplicationResponseDTO response = applicationService.approveApplication(id, approvedAmount);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        String rejectionReason = request.get("rejectionReason");
        applicationService.rejectApplication(id, rejectionReason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelApplication(@PathVariable UUID id) {
        applicationService.cancelApplication(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getPendingApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<LoanApplicationResponseDTO> applications = applicationService.getPendingApplications(page, limit);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getExpiredApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<LoanApplicationResponseDTO> applications = applicationService.getExpiredApplications(page, limit);
        return ResponseEntity.ok(applications);
    }
}