package in.zeta.microloan.platform.provider;

import in.zeta.microloan.platform.dto.response.RepaymentResponseDTO;
import in.zeta.microloan.platform.service.RepaymentService;
import in.zeta.oms.sandbox.model.object.ObjectProvider;
import in.zeta.oms.sandbox.model.realm.Realm;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.common.JID;
import olympus.trace.OlympusSpectra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class RepaymentProvider implements ObjectProvider<RepaymentResponseDTO> {
    public static final String OBJECT_TYPE = "repayment";
    private final RepaymentService repaymentService;
    private static final SpectraLogger logger = OlympusSpectra.getLogger(RepaymentProvider.class);

    private static final String LOAN_ID = "borrowerId";
    private static final String ERROR = "error";

    @Autowired
    public RepaymentProvider(RepaymentService repaymentService) {
        this.repaymentService = repaymentService;
    }

    @Override
    public CompletionStage<Optional<RepaymentResponseDTO>> getObject(JID jid, Realm realm, Long tenantID) {
        return CompletableFuture.supplyAsync(() -> {
            RepaymentResponseDTO repaymentResponse = null;
            Long loanId = null;

            try {
                loanId = Long.getLong(jid.getNodeId());

                logger.info("Entry: Fetching repayment object")
                        .attr(LOAN_ID, loanId)
                        .log();

                List<RepaymentResponseDTO> repayments = repaymentService.getRepaymentsByLoan(loanId);

                if (repayments == null) {
                    logger.error("Repayments not found")
                            .attr(LOAN_ID, loanId)
                            .log();
                    repaymentResponse = getDefaultRepayment();
                } else {
                    logger.info("Success: Repayment object fetched")
                            .attr(LOAN_ID, loanId)
                            .log();
                    repaymentResponse = repayments.get(0);
                }

            } catch (Exception ex) {
                logger.error("Error fetching repayment object")
                        .attr(LOAN_ID, loanId)
                        .log();
                repaymentResponse = getDefaultRepayment();
            }

            return Optional.of(repaymentResponse);
        });
    }

    private RepaymentResponseDTO getDefaultRepayment() {
        return  RepaymentResponseDTO.builder().build();
    }
}