package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.service.RepaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repayments")
public class RepaymentController {

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping
    public ResponseEntity<RepaymentResponseDTO> recordRepayment(
            @Valid @RequestBody RepaymentRequestDTO dto,
            @RequestParam Long createdBy) {

        RepaymentResponseDTO response = repaymentService.recordRepayment(dto, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<RepaymentResponseDTO>> getRepaymentsByLoan(@PathVariable UUID loanId) {
        List<RepaymentResponseDTO> list = repaymentService.getRepaymentsByLoan(loanId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/loan/{loanId}/schedule")
    public ResponseEntity<List<RepaymentScheduleResponseDTO>> getRepaymentSchedule(@PathVariable UUID loanId) {
        List<RepaymentScheduleResponseDTO> schedule = repaymentService.getRepaymentSchedule(loanId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/loan/{loanId}/pending")
    public ResponseEntity<List<RepaymentScheduleResponseDTO>> getPendingSchedule(@PathVariable UUID loanId) {
        List<RepaymentScheduleResponseDTO> pending = repaymentService.getPendingSchedule(loanId);
        return ResponseEntity.ok(pending);
    }
}