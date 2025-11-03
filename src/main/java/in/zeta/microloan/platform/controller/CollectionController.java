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
                .attr("loanId", dto.getLoanId())
                .attr("activityType", dto.getActivityType())
                .log();

        CollectionActivityResponseDTO response = collectionService.logActivity(dto);

        spectraLogger.info("COLLECTION_ACTIVITY_CREATED")
                .attr("activityId", response.getId())
                .attr("loanId", response.getLoanId())
                .log();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/activities")
    public ResponseEntity<List<CollectionActivityResponseDTO>> getActivities(@RequestParam Long loanId) {
        spectraLogger.info("COLLECTION_ACTIVITY_LIST_REQUEST")
                .attr("loanId", loanId)
                .log();

        List<CollectionActivityResponseDTO> activities = collectionService.getActivitiesByLoanId(loanId);

        spectraLogger.info("COLLECTION_ACTIVITY_LIST_RESPONSE")
                .attr("loanId", loanId)
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