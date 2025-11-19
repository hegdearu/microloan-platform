package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.CollectionActivityRequestDTO;
import in.zeta.microloan.platform.dto.response.CollectionActivityResponseDTO;
import in.zeta.microloan.platform.dto.response.OverdueLoansResponseDTO;
import in.zeta.microloan.platform.service.CollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.LOAN_ID;

@RestController
@RequestMapping("/api/v1/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping("/activities")
    public ResponseEntity<CollectionActivityResponseDTO> logActivity(
            @Valid @RequestBody CollectionActivityRequestDTO dto) {
        CollectionActivityResponseDTO response = collectionService.logActivity(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/activities")
    public ResponseEntity<List<CollectionActivityResponseDTO>> getActivities(@RequestParam UUID loanId) {
        List<CollectionActivityResponseDTO> activities = collectionService.getActivitiesByLoanId(loanId);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<OverdueLoansResponseDTO>> getOverdueLoans() {
        List<OverdueLoansResponseDTO> overdueLoans = collectionService.getAllOverdueLoans();
        return ResponseEntity.ok(overdueLoans);
    }
}