package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.ApproveLoanApplicationRequestDTO;
import in.zeta.microloan.platform.dto.LoanApplicationDTO;
import in.zeta.microloan.platform.dto.LoanApplicationResponseDTO;
import in.zeta.microloan.platform.dto.RejectLoanApplicationRequestDTO;
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
    public ResponseEntity<LoanApplicationResponseDTO> createApplication(@Valid @RequestBody LoanApplicationDTO dto) {
        LoanApplicationResponseDTO response = applicationService.createApplication(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanApplicationResponseDTO>> getApplications(@RequestParam Long borrowerId) {
        List<LoanApplicationResponseDTO> applications = applicationService.getApplicationsByBorrower(borrowerId);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponseDTO> getApplication(@PathVariable Long id) {
        LoanApplicationResponseDTO application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<LoanApplicationResponseDTO> approveApplication(@PathVariable Long id, @RequestBody ApproveLoanApplicationRequestDTO request) {

        LoanApplicationResponseDTO response = applicationService.approveApplication(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectApplication(@PathVariable Long id, @RequestBody RejectLoanApplicationRequestDTO request) {
        String rejectionReason = request.getRejectionReason();
        applicationService.rejectApplication(id, rejectionReason);
        return ResponseEntity.ok(null);
    }
}
