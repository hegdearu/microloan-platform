package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.repository.loanapplication.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class LoanApplicationExpiryJobTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @InjectMocks
    private LoanApplicationExpiryJob loanApplicationExpiryJob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testMarkExpiredApplications_Success() {
        // Arrange
        when(applicationRepository.expirePendingApplications()).thenReturn(5);

        // Act
        loanApplicationExpiryJob.markExpiredApplications();

        // Assert
        verify(applicationRepository, times(1)).expirePendingApplications();
    }

    @Test
    void testMarkExpiredApplications_NoExpiredApplications() {
        // Arrange
        when(applicationRepository.expirePendingApplications()).thenReturn(0);

        // Act
        loanApplicationExpiryJob.markExpiredApplications();

        // Assert
        verify(applicationRepository, times(1)).expirePendingApplications();
    }

    @Test
    void testMarkExpiredApplications_Exception() {
        // Arrange
        when(applicationRepository.expirePendingApplications())
                .thenThrow(new RuntimeException("Database error"));

        // Act
        loanApplicationExpiryJob.markExpiredApplications();

        // Assert
        verify(applicationRepository, times(1)).expirePendingApplications();
        // Job should handle exception gracefully and not throw
    }
}