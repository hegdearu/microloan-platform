package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanProductResponseDTO;
import in.zeta.microloan.platform.service.LoanProductService;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static in.zeta.microloan.platform.constants.LogConstants.PRODUCT_ID;

@RestController
@RequestMapping("/api/v1/loan-products")
public class LoanProductController {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(LoanProductController.class);

    private final LoanProductService productService;

    public LoanProductController(LoanProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<LoanProductResponseDTO> createProduct(@Valid @RequestBody LoanProductRequestDTO dto) {
        spectraLogger.info("LOAN_PRODUCT_CREATE_REQUEST")
                .attr("name", dto.getName())
                .attr("minAmount", dto.getMinAmount())
                .attr("maxAmount", dto.getMaxAmount())
                .log();
        LoanProductResponseDTO response = productService.createProduct(dto);
        spectraLogger.info("LOAN_PRODUCT_CREATE_SUCCESS")
                .attr(PRODUCT_ID, response.getId())
                .attr("name", response.getName())
                .log();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        spectraLogger.info("LOAN_PRODUCT_LIST_REQUEST")
                .attr("activeOnly", activeOnly)
                .log();
        List<LoanProductResponseDTO> products = activeOnly
                ? productService.getAllActiveProducts()
                : productService.getAllProducts();
        spectraLogger.info("LOAN_PRODUCT_LIST_SUCCESS")
                .attr("count", products.size())
                .log();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanProductResponseDTO> getProductById(@PathVariable UUID id) {
        spectraLogger.info("LOAN_PRODUCT_FETCH_REQUEST").attr(PRODUCT_ID, id).log();
        LoanProductResponseDTO product = productService.getProductById(id);
        spectraLogger.info("LOAN_PRODUCT_FETCH_SUCCESS").attr(PRODUCT_ID, id).log();
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanProductResponseDTO> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody LoanProductRequestDTO dto) {
        spectraLogger.info("LOAN_PRODUCT_UPDATE_REQUEST")
                .attr(PRODUCT_ID, id)
                .attr("name", dto.getName())
                .log();
        LoanProductResponseDTO response = productService.updateProduct(id, dto);
        spectraLogger.info("LOAN_PRODUCT_UPDATE_SUCCESS")
                .attr(PRODUCT_ID, id)
                .log();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        spectraLogger.info("LOAN_PRODUCT_DELETE_REQUEST").attr(PRODUCT_ID, id).log();
        productService.deleteProduct(id);
        spectraLogger.info("LOAN_PRODUCT_DELETE_SUCCESS").attr(PRODUCT_ID, id).log();
        return ResponseEntity.noContent().build();
    }
}