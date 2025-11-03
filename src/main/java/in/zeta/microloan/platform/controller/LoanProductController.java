package in.zeta.microloan.platform.controller;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanProductResponseDTO;
import in.zeta.microloan.platform.service.LoanProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loan-products")
public class LoanProductController {

    private final LoanProductService productService;

    public LoanProductController(LoanProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<LoanProductResponseDTO> createProduct(
            @Valid @RequestBody LoanProductRequestDTO dto) {
        LoanProductResponseDTO response = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<LoanProductResponseDTO> products = activeOnly
                ? productService.getAllActiveProducts()
                : productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanProductResponseDTO> getProductById(@PathVariable Long id) {
        LoanProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody LoanProductRequestDTO dto) {
        LoanProductResponseDTO response = productService.updateProduct(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
