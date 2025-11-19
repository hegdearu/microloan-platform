package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.dto.request.LoanProductRequestDTO;
import in.zeta.microloan.platform.dto.response.LoanProductResponseDTO;
import in.zeta.microloan.platform.exception.ResourceNotFoundException;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.enums.LoanProductStatus;
import in.zeta.microloan.platform.repository.loanproduct.LoanProductRepository;
import in.zeta.microloan.platform.service.mappers.LoanProductMapper;
import in.zeta.microloan.platform.service.validator.LoanProductValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.zeta.microloan.platform.exception.Error.LOAN_PRODUCT_NOT_FOUND;

@Service
public class LoanProductService {
    private final LoanProductRepository productRepository;
    private final LoanProductValidator validator;
    private final LoanProductMapper mapper;

    public LoanProductService(LoanProductRepository productRepository,
                              LoanProductValidator validator,
                              LoanProductMapper mapper) {
        this.productRepository = productRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    public LoanProductResponseDTO createProduct(LoanProductRequestDTO dto) {

        validator.validate(dto);

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

        LoanProduct savedProduct = productRepository.create(product);

        return mapper.toResponse(savedProduct);
    }

    public List<LoanProductResponseDTO> getAllActiveProducts() {
        return productRepository.findAllActive()
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public List<LoanProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public LoanProductResponseDTO getProductById(UUID id) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND));
        return mapper.toResponse(product);
    }

    @Transactional
    public LoanProductResponseDTO updateProduct(UUID id, LoanProductRequestDTO dto) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND));
        validator.validate(dto);
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
        return mapper.toResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LOAN_PRODUCT_NOT_FOUND));
        productRepository.delete(id);
    }
}