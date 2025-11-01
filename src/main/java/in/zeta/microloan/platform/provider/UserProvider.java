package in.zeta.microloan.platform.provider;

import in.zeta.microloan.platform.dto.BorrowerResponseDTO;
import in.zeta.microloan.platform.service.BorrowerService;
import in.zeta.oms.sandbox.model.object.ObjectProvider;
import in.zeta.oms.sandbox.model.realm.Realm;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.common.JID;
import olympus.trace.OlympusSpectra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class UserProvider implements ObjectProvider<BorrowerResponseDTO> {
    public static final String OBJECT_TYPE = "borrower";
    private final BorrowerService borrowerService;
    private static final SpectraLogger logger = OlympusSpectra.getLogger(UserProvider.class);

    private static final String BORROWER_ID = "borrowerId";
    private static final String ERROR = "error";

    @Autowired
    public UserProvider(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @Override
    public CompletionStage<Optional<BorrowerResponseDTO>> getObject(JID jid, Realm realm, Long tenantID) {
        return CompletableFuture.supplyAsync(() -> {
            BorrowerResponseDTO user = null;
            Long userId = null;

            try {
                userId = Long.valueOf(jid.getNodeId());

                logger.info("Entry: Fetching user object")
                        .attr(BORROWER_ID, userId)
                        .log();

                user = borrowerService.getBorrowerById(userId);

                if (user == null) {
                    logger.error("User not found")
                            .attr(BORROWER_ID, userId)
                            .log();
                    user = getDefaultUser();
                } else {
                    logger.info("Success: User object fetched")
                            .attr(BORROWER_ID, userId)
                            .log();
                }

            } catch (Exception ex) {
                logger.error("Error fetching user object")
                        .attr(BORROWER_ID, userId)
                        .log();
                user = getDefaultUser();
            }

            return Optional.of(user);
        });
    }

    private BorrowerResponseDTO getDefaultUser() {
        return  BorrowerResponseDTO.builder().build();
    }
}