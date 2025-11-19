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
import in.zeta.microloan.platform.service.mappers.CollectionMapper;
import in.zeta.microloan.platform.service.validator.CollectionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.LOAN_ID;
import static in.zeta.microloan.platform.exception.Error.BORROWER_NOT_FOUND;
import static in.zeta.microloan.platform.exception.Error.LOAN_NOT_FOUND;

@Service
public class CollectionService {

    private final CollectionActivityRepository activityRepository;
    private final OverdueTrackingRepository overdueRepository;
    private final LoanRepository loanRepository;
    private final BorrowerRepository borrowerRepository;
    private final CollectionValidator validator;
    private final CollectionMapper mapper;

    public CollectionService(CollectionActivityRepository activityRepository,
                             OverdueTrackingRepository overdueRepository,
                             LoanRepository loanRepository,
                             BorrowerRepository borrowerRepository,
                             CollectionValidator validator,
                             CollectionMapper mapper) {
        this.activityRepository = activityRepository;
        this.overdueRepository = overdueRepository;
        this.loanRepository = loanRepository;
        this.borrowerRepository = borrowerRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    public CollectionActivityResponseDTO logActivity(CollectionActivityRequestDTO dto) {

        validator.validateActivity(dto);

        loanRepository.findById(dto.getLoanId())
                .orElseThrow(() -> {
                    return new ResourceNotFoundException(LOAN_NOT_FOUND);
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

        UUID activityId = activityRepository.create(activity);
        activity.setId(activityId);

        return mapper.toActivityResponse(activity);
    }

    public List<OverdueLoansResponseDTO> getAllOverdueLoans() {
        List<OverdueTracking> overdueList = overdueRepository.findAll();

        List<OverdueLoansResponseDTO> result = overdueList.stream().map(overdue -> {
            Loan loan = loanRepository.findById(overdue.getLoanId())
                    .orElseThrow(() -> new ResourceNotFoundException(LOAN_NOT_FOUND));

            Borrower borrower = borrowerRepository.findById(loan.getBorrowerId())
                    .orElseThrow(() -> new ResourceNotFoundException(BORROWER_NOT_FOUND));

            return mapper.toOverdueResponse(overdue, loan, borrower);
        }).toList();

        return result;
    }

    public List<CollectionActivityResponseDTO> getActivitiesByLoanId(UUID loanId) {

        List<CollectionActivity> activities = activityRepository.findByLoanId(loanId);
        List<CollectionActivityResponseDTO> result = activities.stream()
                .map(mapper::toActivityResponse)
                .toList();

        return result;
    }
}