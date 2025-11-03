package in.zeta.microloan.platform.service.validator;

import in.zeta.microloan.platform.dto.request.BorrowerRegistrationRequestDTO;
import in.zeta.microloan.platform.dto.request.BorrowerUpdateRequestDTO;
import in.zeta.microloan.platform.exception.BusinessRuleException;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.Borrower;
import in.zeta.microloan.platform.model.enums.UserStatus;
import in.zeta.microloan.platform.repository.borrower.BorrowerRepository;
import in.zeta.microloan.platform.repository.household.HouseholdRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class BorrowerValidator {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(BorrowerValidator.class);

    private final BorrowerRepository borrowerRepository;
    private final HouseholdRepository householdRepository;

    @Value("${app.min-age-requirement:18}")
    private int minAgeRequirement;

    public BorrowerValidator(BorrowerRepository borrowerRepository,
                             HouseholdRepository householdRepository) {
        this.borrowerRepository = borrowerRepository;
        this.householdRepository = householdRepository;
    }

    public void validateRegistration(BorrowerRegistrationRequestDTO dto) {
        int age = Period.between(dto.getDob(), LocalDate.now()).getYears();
        if (age < minAgeRequirement) {
            spectraLogger.warn("BORROWER_REGISTER_AGE_VALIDATION_FAILED")
                    .attr("age", age)
                    .attr("minAge", minAgeRequirement)
                    .log();
            throw new ValidationException("Borrower must be at least " + minAgeRequirement + " years old");
        }
        borrowerRepository.findByPhone(dto.getPhone()).ifPresent(b -> {
            spectraLogger.warn("BORROWER_REGISTER_PHONE_ALREADY_EXISTS")
                    .attr("phone", dto.getPhone())
                    .log();
            throw new ValidationException("Phone number already registered");
        });
        if (dto.getHouseholdId() != null) {
            householdRepository.findById(dto.getHouseholdId()).orElseThrow(() -> {
                spectraLogger.warn("BORROWER_REGISTER_HOUSEHOLD_NOT_FOUND")
                        .attr("householdId", dto.getHouseholdId())
                        .log();
                return new ResourceNotFoundException("Household not found");
            });
        }
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            validateEmail(dto.getEmail(), "BORROWER_REGISTER_EMAIL_INVALID");
        }
    }

    public void validateUpdate(BorrowerUpdateRequestDTO dto) {
        if (dto.getEmail() != null) {
            validateEmail(dto.getEmail(), "BORROWER_UPDATE_EMAIL_INVALID");
        }
    }

    public void validateVerification(Borrower borrower) {
        if (borrower.getIsVerified()) {
            spectraLogger.warn("BORROWER_VERIFY_ALREADY_VERIFIED").attr("borrowerId", borrower.getId()).log();
            throw new BusinessRuleException("Borrower is already verified");
        }
    }

    public UserStatus validateStatusChange(Borrower borrower, String statusStr) {
        UserStatus status;
        try {
            status = UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            spectraLogger.warn("BORROWER_STATUS_UPDATE_INVALID_STATUS")
                    .attr("borrowerId", borrower.getId())
                    .attr("status", statusStr)
                    .log();
            throw new ValidationException("Invalid status: " + statusStr);
        }
        if (status == UserStatus.SUSPENDED || status == UserStatus.INACTIVE) {
            int activeLoans = borrowerRepository.countActiveLoansByBorrower(borrower.getId());
            if (activeLoans > 0) {
                spectraLogger.warn("BORROWER_STATUS_UPDATE_ACTIVE_LOANS_BLOCKED")
                        .attr("borrowerId", borrower.getId())
                        .attr("activeLoans", activeLoans)
                        .log();
                throw new BusinessRuleException("Cannot change status. Borrower has " + activeLoans + " active loan(s)");
            }
        }
        return status;
    }

    public void validateDeletion(Borrower borrower) {
        int activeLoans = borrowerRepository.countActiveLoansByBorrower(borrower.getId());
        if (activeLoans > 0) {
            spectraLogger.warn("BORROWER_DELETE_ACTIVE_LOANS_BLOCKED")
                    .attr("borrowerId", borrower.getId())
                    .attr("activeLoans", activeLoans)
                    .log();
            throw new BusinessRuleException("Cannot delete borrower with active loans. Please close all loans first.");
        }
    }

    public void ensureBorrowerExists(Borrower borrower, Long id, String eventCode) {
        if (borrower == null) {
            spectraLogger.warn(eventCode).attr("borrowerId", id).log();
            throw new ResourceNotFoundException("Borrower not found");
        }
    }

    private void validateEmail(String email, String eventCode) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            spectraLogger.warn(eventCode).attr("email", email).log();
            throw new ValidationException("Invalid email format");
        }
    }
}