package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.CollectionActivityRequestDTO;
import in.zeta.microloan.platform.dto.response.CollectionActivityResponseDTO;
import in.zeta.microloan.platform.dto.response.OverdueLoansResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.model.enums.ContactMethod;
import in.zeta.microloan.platform.repository.collectionactivity.CollectionActivityRepository;
import in.zeta.microloan.platform.repository.overduetracking.OverdueTrackingRepository;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.loan.LoanRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(CollectionService.class);

    private final CollectionActivityRepository activityRepository;
    private final OverdueTrackingRepository overdueRepository;
    private final LoanRepository loanRepository;
    private final BorrowerRepository borrowerRepository;

    public CollectionService(CollectionActivityRepository activityRepository,
                             OverdueTrackingRepository overdueRepository,
                             LoanRepository loanRepository,
                             BorrowerRepository borrowerRepository) {
        this.activityRepository = activityRepository;
        this.overdueRepository = overdueRepository;
        this.loanRepository = loanRepository;
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional
    public CollectionActivityResponseDTO logActivity(CollectionActivityRequestDTO dto) {
        spectraLogger.info("COLLECTION_ACTIVITY_CREATE_ATTEMPT")
                .attr("loanId", dto.getLoanId())
                .attr("activityType", dto.getActivityType())
                .log();

        loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> {
                    spectraLogger.warn("COLLECTION_ACTIVITY_CREATE_LOAN_NOT_FOUND")
                            .attr("loanId", dto.getLoanId()).log();
                    return new ResourceNotFoundException("Loan not found");
                });

        CollectionActivity activity = CollectionActivity.builder()
                .loanId(dto.getLoanId())
                .activityType(dto.getActivityType())
                .contactMethod(ContactMethod.valueOf(dto.getContactMethod()))
                .borrowerResponse(dto.getBorrowerResponse())
                .promiseToPayDate(dto.getPromiseToPayDate())
                .paymentArrangement(dto.getPaymentArrangement())
                .notes(dto.getNotes())
                .assignedTo(dto.getAssignedTo())
                .activityDate(LocalDateTime.now())
                .nextFollowUpDate(dto.getNextFollowUpDate())
                .build();

        Long activityId = activityRepository.create(activity);
        activity.setId(activityId);

        spectraLogger.info("COLLECTION_ACTIVITY_CREATE_SUCCESS")
                .attr("activityId", activityId)
                .attr("loanId", dto.getLoanId())
                .log();

        return mapToResponseDTO(activity);
    }

    public List<OverdueLoansResponseDTO> getAllOverdueLoans() {
        spectraLogger.info("OVERDUE_LOANS_FETCH_ATTEMPT").log();
        List<OverdueTracking> overdueList = overdueRepository.findAll();

        List<OverdueLoansResponseDTO> result = overdueList.stream().map(overdue -> {
            Loan loan = loanRepository.findById(overdue.getLoanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

            Borrower borrower = borrowerRepository.findById(loan.getBorrowerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));

            return OverdueLoansResponseDTO.builder()
                    .loanId(loan.getId())
                    .loanNumber(loan.getLoanNumber())
                    .borrowerId(borrower.getId())
                    .borrowerName(borrower.getName())
                    .borrowerPhone(borrower.getPhone())
                    .overdueSince(overdue.getOverdueSince())
                    .overdueDays(overdue.getOverdueDays())
                    .overdueAmount(overdue.getOverdueAmount())
                    .penaltyAmount(overdue.getPenaltyAmount())
                    .totalDue(overdue.getTotalDue())
                    .collectionStage(overdue.getCollectionStage())
                    .build();
        }).collect(Collectors.toList());

        spectraLogger.info("OVERDUE_LOANS_FETCH_SUCCESS")
                .attr("count", result.size())
                .log();
        return result;
    }

    public List<CollectionActivityResponseDTO> getActivitiesByLoanId(Long loanId) {
        spectraLogger.info("COLLECTION_ACTIVITY_LIST_FETCH_ATTEMPT")
                .attr("loanId", loanId)
                .log();
        List<CollectionActivity> activities = activityRepository.findByLoanId(loanId);
        List<CollectionActivityResponseDTO> result = activities.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        spectraLogger.info("COLLECTION_ACTIVITY_LIST_FETCH_SUCCESS")
                .attr("loanId", loanId)
                .attr("count", result.size())
                .log();
        return result;
    }

    private CollectionActivityResponseDTO mapToResponseDTO(CollectionActivity activity) {
        return CollectionActivityResponseDTO.builder()
                .id(activity.getId())
                .loanId(activity.getLoanId())
                .activityType(activity.getActivityType())
                .contactMethod(activity.getContactMethod())
                .borrowerResponse(activity.getBorrowerResponse())
                .promiseToPayDate(activity.getPromiseToPayDate())
                .notes(activity.getNotes())
                .activityDate(activity.getActivityDate())
                .nextFollowUpDate(activity.getNextFollowUpDate())
                .build();
    }
}