package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.BorrowerRegistrationResponse;
import in.zeta.microloan.platform.dto.BorrowerRegistrationDTO;
import in.zeta.microloan.platform.dto.BorrowerResponseDTO;
import in.zeta.microloan.platform.provider.UserProvider;
import in.zeta.microloan.platform.service.BorrowerService;
import in.zeta.springframework.boot.commons.authorization.sandboxAccessControl.SandboxAuthorizedSync;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {

    private final BorrowerService borrowerService;

    public BorrowerController(BorrowerService borrowerService) {
        this.borrowerService = borrowerService;
    }

    @PostMapping("/register")
    @SandboxAuthorizedSync(action = "borrower.view_profile", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<BorrowerResponseDTO> registerBorrower(@RequestBody BorrowerRegistrationDTO request) {
        BorrowerResponseDTO response = borrowerService.registerBorrower(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{borrowerId}")
    public ResponseEntity<BorrowerResponseDTO> getBorrowerDetails(@PathVariable Long borrowerId) {
        BorrowerResponseDTO response = borrowerService.getBorrowerById(borrowerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{borrowerId}")
    public ResponseEntity<BorrowerRegistrationResponse> updateBorrowerDetails(@PathVariable String borrowerId, BorrowerRegistrationDTO request) {
        // Implementation goes here
        return ResponseEntity.ok(new BorrowerRegistrationResponse());
    }

    @DeleteMapping("/{borrowerId}")
    @SandboxAuthorizedSync(action = "borrower.delete_profile", object = "$$borrowers$$@" + UserProvider.OBJECT_TYPE + ".cipher.app", tenantID = "1001034")
    public ResponseEntity<Void> deleteBorrower(@PathVariable String borrowerId) {
        // Implementation goes here
        return ResponseEntity.noContent().build();
    }
}
