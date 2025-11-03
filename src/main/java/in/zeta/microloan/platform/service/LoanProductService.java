package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanProductResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanProductService {

    private final LoanProductRepository productRepository;

    public LoanProductService(LoanProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public LoanProductResponseDTO createProduct(LoanProductRequestDTO dto) {
        validateProductData(dto);

        LoanProduct product = LoanProduct.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .minAmount(dto.getMinAmount())
                .maxAmount(dto.getMaxAmount())
                .interestRate(dto.getInterestRate())
                .processingFeeType(dto.getProcessingFeeType())
                .processingFeeValue(dto.getProcessingFeeValue())
                .tenureMonths(dto.getTenureMonths())
                .gracePeriodDays(dto.getGracePeriodDays())
                .lateFeePercent(dto.getLateFeePercent())
                .maxLateFeePercent(dto.getMaxLateFeePercent())
                .prepaymentChargesType(dto.getPrepaymentChargesType())
                .prepaymentChargesValue(dto.getPrepaymentChargesValue())
                .status(LoanProductStatus.ACTIVE)
                .build();

        Long productId = productRepository.create(product);
        product.setId(productId);

        return mapToResponseDTO(product);
    }

    public List<LoanProductResponseDTO> getAllActiveProducts() {
        List<LoanProduct> products = productRepository.findAllActive();
        return products.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<LoanProductResponseDTO> getAllProducts() {
        List<LoanProduct> products = productRepository.findAll();
        return products.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public LoanProductResponseDTO getProductById(Long id) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));
        return mapToResponseDTO(product);
    }

    @Transactional
    public LoanProductResponseDTO updateProduct(Long id, LoanProductRequestDTO dto) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        validateProductData(dto);

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setMinAmount(dto.getMinAmount());
        product.setMaxAmount(dto.getMaxAmount());
        product.setInterestRate(dto.getInterestRate());
        product.setProcessingFeeType(dto.getProcessingFeeType());
        product.setProcessingFeeValue(dto.getProcessingFeeValue());
        product.setTenureMonths(dto.getTenureMonths());
        product.setGracePeriodDays(dto.getGracePeriodDays());
        product.setLateFeePercent(dto.getLateFeePercent());
        product.setMaxLateFeePercent(dto.getMaxLateFeePercent());
        product.setPrepaymentChargesType(dto.getPrepaymentChargesType());
        product.setPrepaymentChargesValue(dto.getPrepaymentChargesValue());

        productRepository.update(product);

        return mapToResponseDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found"));

        productRepository.delete(id);

    }

    private void validateProductData(LoanProductRequestDTO dto) {
        if (dto.getMinAmount().compareTo(dto.getMaxAmount()) > 0) {
            throw new ValidationException("Minimum amount cannot be greater than maximum amount");
        }

        if (dto.getInterestRate().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Interest rate must be positive");
        }

        if (dto.getTenureMonths() <= 0) {
            throw new ValidationException("Tenure must be positive");
        }
    }

    private LoanProductResponseDTO mapToResponseDTO(LoanProduct product) {
        return LoanProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .interestRate(product.getInterestRate())
                .processingFeeType(product.getProcessingFeeType())
                .processingFeeValue(product.getProcessingFeeValue())
                .tenureMonths(product.getTenureMonths())
                .gracePeriodDays(product.getGracePeriodDays())
                .lateFeePercent(product.getLateFeePercent())
                .maxLateFeePercent(product.getMaxLateFeePercent())
                .prepaymentChargesType(product.getPrepaymentChargesType())
                .prepaymentChargesValue(product.getPrepaymentChargesValue())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
