package in.zeta.microloan.platform.provider;

import in.zeta.microloan.platform.dto.response.LoanResponseDTO;
import in.zeta.microloan.platform.service.LoanService;
import in.zeta.oms.sandbox.model.object.ObjectProvider;
import in.zeta.oms.sandbox.model.realm.Realm;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.common.JID;
import olympus.trace.OlympusSpectra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class LoanProvider implements ObjectProvider<LoanResponseDTO> {
    public static final String OBJECT_TYPE = "loan";
    private final LoanService loanService;
    private static final SpectraLogger logger = OlympusSpectra.getLogger(LoanProvider.class);
    private static final String BORROWER_ID = "loanId";

    @Autowired
    public LoanProvider(LoanService loanService) {
        this.loanService = loanService;
    }

    @Override
    public CompletionStage<Optional<LoanResponseDTO>> getObject(JID jid, Realm realm, Long tenantID) {
        return CompletableFuture.supplyAsync(() -> {
            LoanResponseDTO loan = null;
            Long borrowerId = null;

            try {
                borrowerId = Long.valueOf(jid.getNodeId());

                logger.info("Entry: Fetching Loan object")
                        .attr(BORROWER_ID, borrowerId)
                        .log();

                loan = loanService.getLoanById(borrowerId);

                if (loan == null) {
                    logger.error("Loan not found")
                            .attr(BORROWER_ID, borrowerId)
                            .log();
                    loan = getDefaultLoan();
                } else {
                    logger.info("Success: loan object fetched")
                            .attr(BORROWER_ID, borrowerId)
                            .log();
                }

            } catch (Exception ex) {
                logger.error("Error fetching loan object")
                        .attr(BORROWER_ID, borrowerId)
                        .log();
                loan = getDefaultLoan();
            }

            return Optional.of(loan);
        });
    }

    private LoanResponseDTO getDefaultLoan() {
        return  LoanResponseDTO.builder().build();
    }
}