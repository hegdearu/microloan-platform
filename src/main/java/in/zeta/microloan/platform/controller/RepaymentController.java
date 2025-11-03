package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.RepaymentRequestDTO;
import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.dto.response.RepaymentScheduleResponseDTO;
import in.zeta.microloan.platform.provider.RepaymentProvider;
import in.zeta.microloan.platform.service.RepaymentService;
import in.zeta.spectra.capture.SpectraLogger;
import in.zeta.springframework.boot.commons.authorization.sandboxAccessControl.SandboxAuthorizedSync;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.COUNT;
import static in.zeta.microloan.platform.constants.LogConstants.LOAN_ID;

@RestController
@RequestMapping("/api/v1/repayments")
public class RepaymentController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(RepaymentController.class);

    private final RepaymentService repaymentService;

    public RepaymentController(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @PostMapping
    @SandboxAuthorizedSync(action = "repayment.create", object = "$$repayments$$@" + RepaymentProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<RepaymentResponseDTO> recordRepayment(
            @Valid @RequestBody RepaymentRequestDTO dto,
            @RequestParam Long createdBy) {
        spectraLogger.info("REPAYMENT_RECORD_REQUEST")
                .attr(LOAN_ID, dto.getLoanId())
                .attr("amount", dto.getAmount())
                .attr("createdBy", createdBy)
                .log();
        RepaymentResponseDTO response = repaymentService.recordRepayment(dto, createdBy);
        spectraLogger.info("REPAYMENT_RECORD_SUCCESS")
                .attr("repaymentId", response.getId())
                .attr(LOAN_ID, dto.getLoanId())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/loan/{loanId}")
    @SandboxAuthorizedSync(action = "repayment.get", object = "$$repayments$$@" + RepaymentProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<List<RepaymentResponseDTO>> getRepaymentsByLoan(@PathVariable UUID loanId) {
        spectraLogger.info("REPAYMENT_LIST_REQUEST")
                .attr(LOAN_ID, loanId)
                .log();
        List<RepaymentResponseDTO> list = repaymentService.getRepaymentsByLoan(loanId);
        spectraLogger.info("REPAYMENT_LIST_SUCCESS")
                .attr(LOAN_ID, loanId)
                .attr(COUNT, list.size())
                .log();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/loan/{loanId}/schedule")
    @SandboxAuthorizedSync(action = "repayment.get", object = "$$repayments$$@" + RepaymentProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<List<RepaymentScheduleResponseDTO>> getRepaymentSchedule(@PathVariable UUID loanId) {
        spectraLogger.info("REPAYMENT_SCHEDULE_REQUEST")
                .attr(LOAN_ID, loanId)
                .log();
        List<RepaymentScheduleResponseDTO> schedule = repaymentService.getRepaymentSchedule(loanId);
        spectraLogger.info("REPAYMENT_SCHEDULE_SUCCESS")
                .attr(LOAN_ID, loanId)
                .attr(COUNT, schedule.size())
                .log();
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/loan/{loanId}/pending")
    @SandboxAuthorizedSync(action = "repayment.get", object = "$$repayments$$@" + RepaymentProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<List<RepaymentScheduleResponseDTO>> getPendingSchedule(@PathVariable UUID loanId) {
        spectraLogger.info("REPAYMENT_PENDING_SCHEDULE_REQUEST")
                .attr(LOAN_ID, loanId)
                .log();
        List<RepaymentScheduleResponseDTO> pending = repaymentService.getPendingSchedule(loanId);
        spectraLogger.info("REPAYMENT_PENDING_SCHEDULE_SUCCESS")
                .attr(LOAN_ID, loanId)
                .attr(COUNT, pending.size())
                .log();
        return ResponseEntity.ok(pending);
    }
}