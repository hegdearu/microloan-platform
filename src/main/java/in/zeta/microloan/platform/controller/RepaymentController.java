package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.service.RepaymentService;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/repayments")
public class RepaymentController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(RepaymentController.class);

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping
    public ResponseEntity<RepaymentResponseDTO> recordRepayment(
            @Valid @RequestBody RepaymentRequestDTO dto,
            @RequestParam Long createdBy) {
        spectraLogger.info("REPAYMENT_RECORD_REQUEST")
                .attr("loanId", dto.getLoanId())
                .attr("amount", dto.getAmount())
                .attr("createdBy", createdBy)
                .log();
        RepaymentResponseDTO response = repaymentService.recordRepayment(dto, createdBy);
        spectraLogger.info("REPAYMENT_RECORD_SUCCESS")
                .attr("repaymentId", response.getId())
                .attr("loanId", dto.getLoanId())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<RepaymentResponseDTO>> getRepaymentsByLoan(@PathVariable Long loanId) {
        spectraLogger.info("REPAYMENT_LIST_REQUEST")
                .attr("loanId", loanId)
                .log();
        List<RepaymentResponseDTO> list = repaymentService.getRepaymentsByLoan(loanId);
        spectraLogger.info("REPAYMENT_LIST_SUCCESS")
                .attr("loanId", loanId)
                .attr("count", list.size())
                .log();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/loan/{loanId}/schedule")
    public ResponseEntity<List<RepaymentScheduleResponseDTO>> getRepaymentSchedule(@PathVariable Long loanId) {
        spectraLogger.info("REPAYMENT_SCHEDULE_REQUEST")
                .attr("loanId", loanId)
                .log();
        List<RepaymentScheduleResponseDTO> schedule = repaymentService.getRepaymentSchedule(loanId);
        spectraLogger.info("REPAYMENT_SCHEDULE_SUCCESS")
                .attr("loanId", loanId)
                .attr("count", schedule.size())
                .log();
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/loan/{loanId}/pending")
    public ResponseEntity<List<RepaymentScheduleResponseDTO>> getPendingSchedule(@PathVariable Long loanId) {
        spectraLogger.info("REPAYMENT_PENDING_SCHEDULE_REQUEST")
                .attr("loanId", loanId)
                .log();
        List<RepaymentScheduleResponseDTO> pending = repaymentService.getPendingSchedule(loanId);
        spectraLogger.info("REPAYMENT_PENDING_SCHEDULE_SUCCESS")
                .attr("loanId", loanId)
                .attr("count", pending.size())
                .log();
        return ResponseEntity.ok(pending);
    }
}