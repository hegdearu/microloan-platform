package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanApplicationRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.service.LoanApplicationService;
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
            @RequestParam(required = false) Long borrowerId,
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
    public ResponseEntity<LoanApplicationResponseDTO> getApplication(@PathVariable Long id) {
        LoanApplicationResponseDTO application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LoanApplicationResponseDTO> approveApplication(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        Long approvedBy = Long.valueOf(request.get("approvedBy").toString());
        BigDecimal approvedAmount = new BigDecimal(request.get("approvedAmount").toString());

        LoanApplicationResponseDTO response = applicationService.approveApplication(id, approvedBy, approvedAmount);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String rejectionReason = request.get("rejectionReason");
        applicationService.rejectApplication(id, rejectionReason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<LoanApplicationResponseDTO> moveToVerification(@PathVariable Long id) {
        LoanApplicationResponseDTO response = applicationService.moveToVerification(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelApplication(@PathVariable Long id) {
        applicationService.cancelApplication(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getPendingApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<LoanApplicationResponseDTO> applications =
                applicationService.getPendingApplications(page, limit);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getExpiredApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        List<LoanApplicationResponseDTO> applications =
                applicationService.getExpiredApplications(page, limit);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/borrower/{borrowerId}/latest")
    public ResponseEntity<LoanApplicationResponseDTO> getLatestApplication(
            @PathVariable Long borrowerId) {

        LoanApplicationResponseDTO application =
                applicationService.getLatestApplicationByBorrower(borrowerId);
        return ResponseEntity.ok(application);
    }
}
