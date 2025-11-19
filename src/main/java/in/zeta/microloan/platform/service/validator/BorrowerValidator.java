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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.BORROWER_ID;
import static in.zeta.microloan.platform.constants.LogConstants.HOUSEHOLD_ID;
import static in.zeta.microloan.platform.exception.Error.HOUSEHOLD_NOT_FOUND;
import static in.zeta.microloan.platform.exception.Error.HOUSEHOLD_NOT_VERIFIED;

@Component
public class BorrowerValidator {

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
            throw new ValidationException("Borrower must be at least " + minAgeRequirement + " years old");
        }

        borrowerRepository.findByPhone(dto.getPhone()).ifPresent(b -> {
            throw new ValidationException("Phone number already registered");
        });

        if (dto.getHouseholdId() != null) {
            var householdOpt = householdRepository.findById(dto.getHouseholdId());
            if (householdOpt.isEmpty()) {
                throw new ResourceNotFoundException(HOUSEHOLD_NOT_FOUND);
            }

            var household = householdOpt.get();

            if (!Boolean.TRUE.equals(household.getIsVerified())) {
                throw new ResourceNotFoundException(HOUSEHOLD_NOT_VERIFIED);
            }

            // Validate household member count
            int currentMemberCount = borrowerRepository.findByHouseholdId(dto.getHouseholdId()).size();
            if (currentMemberCount >= household.getTotalMembers()) {
                throw new BusinessRuleException(
                        "Cannot add more members. Household already has " + currentMemberCount +
                                " member(s), which matches the maximum allowed (" + household.getTotalMembers() + ")");
            }
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
            throw new BusinessRuleException("Borrower is already verified");
        }
    }

    public UserStatus validateStatusChange(UUID borrowerId, String statusStr) {
        UserStatus status;
        try {
            status = UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + statusStr);
        }

        if (status == UserStatus.SUSPENDED || status == UserStatus.INACTIVE) {
            int activeLoans = borrowerRepository.countActiveLoansByBorrower(borrowerId);
            if (activeLoans > 0) {
                throw new BusinessRuleException(
                        "Cannot change status. Borrower has " + activeLoans + " active loan(s)");
            }
        }
        return status;
    }

    public void validateDeletion(UUID borrowerId) {
        int activeLoans = borrowerRepository.countActiveLoansByBorrower(borrowerId);
        if (activeLoans > 0) {
            throw new BusinessRuleException(
                    "Cannot delete borrower with active loans. Please close all loans first.");
        }
    }

    private void validateEmail(String email, String eventCode) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new ValidationException("Invalid email format");
        }
    }
}