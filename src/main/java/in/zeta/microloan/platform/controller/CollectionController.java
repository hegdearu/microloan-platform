package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.CollectionActivityRequestDTO;
import in.zeta.microloan.platform.dto.response.CollectionActivityResponseDTO;
import in.zeta.microloan.platform.dto.response.OverdueLoansResponseDTO;
import in.zeta.microloan.platform.service.CollectionService;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.LOAN_ID;

@RestController
@RequestMapping("/api/v1/collections")
public class CollectionController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(CollectionController.class);

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping("/activities")
    public ResponseEntity<CollectionActivityResponseDTO> logActivity(
            @Valid @RequestBody CollectionActivityRequestDTO dto) {

        spectraLogger.info("COLLECTION_ACTIVITY_CREATE_REQUEST")
                .attr(LOAN_ID, dto.getLoanId())
                .attr("activityType", dto.getActivityType())
                .log();

        CollectionActivityResponseDTO response = collectionService.logActivity(dto);

        spectraLogger.info("COLLECTION_ACTIVITY_CREATED")
                .attr("activityId", response.getId())
                .attr(LOAN_ID, response.getLoanId())
                .log();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/activities")
    public ResponseEntity<List<CollectionActivityResponseDTO>> getActivities(@RequestParam UUID loanId) {
        spectraLogger.info("COLLECTION_ACTIVITY_LIST_REQUEST")
                .attr(LOAN_ID, loanId)
                .log();

        List<CollectionActivityResponseDTO> activities = collectionService.getActivitiesByLoanId(loanId);

        spectraLogger.info("COLLECTION_ACTIVITY_LIST_RESPONSE")
                .attr(LOAN_ID, loanId)
                .attr("count", activities.size())
                .log();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<OverdueLoansResponseDTO>> getOverdueLoans() {
        spectraLogger.info("OVERDUE_LOANS_LIST_REQUEST").log();
        List<OverdueLoansResponseDTO> overdueLoans = collectionService.getAllOverdueLoans();
        spectraLogger.info("OVERDUE_LOANS_LIST_RESPONSE")
                .attr("count", overdueLoans.size())
                .log();
        return ResponseEntity.ok(overdueLoans);
    }
}