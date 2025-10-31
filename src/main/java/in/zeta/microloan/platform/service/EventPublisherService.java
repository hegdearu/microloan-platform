package in.zeta.microloan.platform.service;

import in.zeta.microloan.platform.model.Loan;
import in.zeta.microloan.platform.model.LoanProduct;
import in.zeta.microloan.platform.model.Repayment;
//import in.zeta.oms.atropos.client.AtroposPublisherClient;
import in.zeta.oms.atropos.model.PublishMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);
//    private final AtroposPublisherClient atroposPublisherClient;
//    private final PublishMode publishMode;

//    public EventPublisherService(AtroposPublisherClient atroposPublisherClient, @Value("${atropos.publish.mode}") String publishModeString) {
////        this.atroposPublisherClient = atroposPublisherClient;
////        this.publishMode = PublishMode.valueOf(publishModeString.toUpperCase());
//    }


    public void publishLoanDisbursedEvent(Loan loan) {
        logger.info("Publishing loan disbursed event for loan: {}", loan.getLoanNumber());
        // Implementation for publishing to message queue (Kafka, RabbitMQ, etc.)
        // For now, just logging
    }

    public void publishLoanCancelledEvent(Loan loan, String reason) {
        logger.info("Publishing loan cancelled event for loan: {} with reason: {}",
                loan.getLoanNumber(), reason);
        // Implementation for publishing to message queue
    }

    public void publishRepaymentReceivedEvent(Repayment repayment, Loan loan) {
        logger.info("Publishing repayment received event for loan: {} amount: {}",
                loan.getLoanNumber(), repayment.getAmount());
        // Implementation for publishing to message queue
    }

    public void publishProductCreatedEvent(LoanProduct product) {
        logger.info("Publishing product created event for product: {}", product.getName());

//        atroposPublisherClient.publish(,publishMode);

    }

    public void publishProductUpdatedEvent(LoanProduct product) {
        logger.info("Publishing product updated event for product: {}", product.getName());
        // Implementation for publishing to message queue
    }

    public void publishProductDeletedEvent(LoanProduct product) {
        logger.info("Publishing product deleted event for product: {}", product.getName());
        // Implementation for publishing to message queue
    }
}
