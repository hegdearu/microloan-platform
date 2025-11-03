package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanProductResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.exception.ValidationException;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanProductService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(LoanProductService.class);

    private final LoanProductRepository productRepository;

    public LoanProductService(LoanProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public LoanProductResponseDTO createProduct(LoanProductRequestDTO dto) {
        spectraLogger.info("LOAN_PRODUCT_CREATE_ATTEMPT")
                .attr("name", dto.getName())
                .attr("minAmount", dto.getMinAmount())
                .attr("maxAmount", dto.getMaxAmount())
                .log();

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

        spectraLogger.info("LOAN_PRODUCT_CREATE_SUCCESS")
                .attr("productId", productId)
                .attr("name", product.getName())
                .log();
        return mapToResponseDTO(product);
    }

    public List<LoanProductResponseDTO> getAllActiveProducts() {
        List<LoanProduct> products = productRepository.findAllActive();
        List<LoanProductResponseDTO> result = products.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        spectraLogger.info("LOAN_PRODUCT_ACTIVE_LIST_SUCCESS")
                .attr("count", result.size())
                .log();
        return result;
    }

    public List<LoanProductResponseDTO> getAllProducts() {
        List<LoanProduct> products = productRepository.findAll();
        List<LoanProductResponseDTO> result = products.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        spectraLogger.info("LOAN_PRODUCT_LIST_SUCCESS")
                .attr("count", result.size())
                .log();
        return result;
    }

    public LoanProductResponseDTO getProductById(Long id) {
        spectraLogger.info("LOAN_PRODUCT_FETCH_BY_ID_ATTEMPT").attr("productId", id).log();
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_PRODUCT_FETCH_BY_ID_NOT_FOUND").attr("productId", id).log();
                    return new ResourceNotFoundException("Loan product not found");
                });
        spectraLogger.info("LOAN_PRODUCT_FETCH_BY_ID_SUCCESS").attr("productId", id).log();
        return mapToResponseDTO(product);
    }

    @Transactional
    public LoanProductResponseDTO updateProduct(Long id, LoanProductRequestDTO dto) {
        spectraLogger.info("LOAN_PRODUCT_UPDATE_ATTEMPT")
                .attr("productId", id)
                .attr("name", dto.getName())
                .log();

        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_PRODUCT_UPDATE_NOT_FOUND").attr("productId", id).log();
                    return new ResourceNotFoundException("Loan product not found");
                });

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

        spectraLogger.info("LOAN_PRODUCT_UPDATE_SUCCESS").attr("productId", id).log();
        return mapToResponseDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        spectraLogger.info("LOAN_PRODUCT_DELETE_ATTEMPT").attr("productId", id).log();

        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> {
                    spectraLogger.warn("LOAN_PRODUCT_DELETE_NOT_FOUND").attr("productId", id).log();
                    return new ResourceNotFoundException("Loan product not found");
                });

        productRepository.delete(id);

        spectraLogger.info("LOAN_PRODUCT_DELETE_SUCCESS").attr("productId", id).log();
    }

    private void validateProductData(LoanProductRequestDTO dto) {
        if (dto.getMinAmount().compareTo(dto.getMaxAmount()) > 0) {
            spectraLogger.warn("LOAN_PRODUCT_VALIDATE_MIN_GT_MAX")
                    .attr("minAmount", dto.getMinAmount())
                    .attr("maxAmount", dto.getMaxAmount())
                    .log();
            throw new ValidationException("Minimum amount cannot be greater than maximum amount");
        }

        if (dto.getInterestRate().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            spectraLogger.warn("LOAN_PRODUCT_VALIDATE_INTEREST_NON_POSITIVE")
                    .attr("interestRate", dto.getInterestRate())
                    .log();
            throw new ValidationException("Interest rate must be positive");
        }

        if (dto.getTenureMonths() <= 0) {
            spectraLogger.warn("LOAN_PRODUCT_VALIDATE_TENURE_NON_POSITIVE")
                    .attr("tenureMonths", dto.getTenureMonths())
                    .log();
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