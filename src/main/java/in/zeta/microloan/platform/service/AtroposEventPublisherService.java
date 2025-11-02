package in.zeta.microloan.platform.service;

import com.google.gson.Gson;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.producer.EventProducer;
import in.zeta.oms.atropos.response.PublishEventResponse;
import in.zeta.oms.atropos.response.PublishStatus;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.pubsub.model.TopicScope;
import olympus.trace.OlympusSpectra;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AtroposEventPublisherService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(AtroposEventPublisherService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final EventProducer eventProducer;
    private final Gson gson;

    @Value("${event.topic.loan.issued}")
    private String loanIssuedTopic;

    @Value("${event.topic.loan.repayment}")
    private String loanRepaymentTopic;

    @Value("${event.topic.loan.overdue}")
    private String loanOverdueTopic;

    @Value("${event.topic.loan.cancelled}")
    private String loanCancelledTopic;

    @Value("${event.topic.loan.closed}")
    private String loanClosedTopic;

    @Value("${event.topic.application.approved}")
    private String applicationApprovedTopic;

    @Value("${event.topic.application.rejected}")
    private String applicationRejectedTopic;

    public AtroposEventPublisherService(EventProducer eventProducer, Gson gson) {
        this.eventProducer = eventProducer;
        this.gson = gson;
    }

    // ============================================
    // LOAN ISSUED EVENT
    // ============================================
    public void publishLoanIssuedEvent(Loan loan) {
        try {
            Map<String, Object> eventData = buildLoanIssuedEventData(loan);
            PublishEventResponse response = eventProducer
                    .publishEvent(String.valueOf(loan.getId()), loanIssuedTopic, eventData, TopicScope.SYSTEM)
                    .toCompletableFuture().get();

            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_ISSUED_EVENT_PUBLISH_FAILED")
                        .attr("loanId", loan.getId())
                        .attr("loanNumber", loan.getLoanNumber())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }

            spectraLogger.info("LOAN_ISSUED_EVENT_PUBLISHED")
                    .attr("loanId", loan.getId())
                    .attr("loanNumber", loan.getLoanNumber())
                    .attr("borrowerId", loan.getBorrowerId())
                    .attr("principalAmount", loan.getPrincipalAmount())
                    .log();

        } catch (InterruptedException ie) {
            spectraLogger.error("LOAN_ISSUED_EVENT_PUBLISH_INTERRUPTED", ie)
                    .attr("loanId", loan.getId())
                    .log();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            spectraLogger.error("LOAN_ISSUED_EVENT_PUBLISH_ERROR", e)
                    .attr("loanId", loan.getId())
                    .attr("errorMessage", e.getMessage())
                    .log();
        }
    }

    private Map<String, Object> buildLoanIssuedEventData(Loan loan) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "LOAN_ISSUED");
        eventData.put("eventTimestamp", LocalDateTime.now().format(FORMATTER));
        eventData.put("loanId", loan.getId());
        eventData.put("loanNumber", loan.getLoanNumber());
        eventData.put("applicationId", loan.getApplicationId());
        eventData.put("borrowerId", loan.getBorrowerId());
        eventData.put("householdId", loan.getHouseholdId());
        eventData.put("productId", loan.getProductId());
        eventData.put("principalAmount", loan.getPrincipalAmount());
        eventData.put("interestRate", loan.getInterestRate());
        eventData.put("tenureMonths", loan.getTenureMonths());
        eventData.put("emiAmount", loan.getEmiAmount());
        eventData.put("totalPayable", loan.getTotalPayable());
        eventData.put("disbursementDate", loan.getDisbursementDate().toString());
        eventData.put("firstDueDate", loan.getFirstDueDate().toString());
        eventData.put("disbursementMethod", loan.getDisbursementMethod().name());
        eventData.put("status", loan.getStatus().name());
        return eventData;
    }

    // ============================================
    // LOAN REPAYMENT EVENT
    // ============================================
    public void publishLoanRepaymentEvent(Repayment repayment, Loan loan) {
        try {
            Map<String, Object> eventData = buildLoanRepaymentEventData(repayment, loan);
            PublishEventResponse response = eventProducer
                    .publishEvent(String.valueOf(repayment.getId()), loanRepaymentTopic, eventData, TopicScope.SYSTEM)
                    .toCompletableFuture().get();

            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_REPAYMENT_EVENT_PUBLISH_FAILED")
                        .attr("repaymentId", repayment.getId())
                        .attr("receiptNumber", repayment.getReceiptNumber())
                        .attr("loanId", loan.getId())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }

            spectraLogger.info("LOAN_REPAYMENT_EVENT_PUBLISHED")
                    .attr("repaymentId", repayment.getId())
                    .attr("receiptNumber", repayment.getReceiptNumber())
                    .attr("loanId", loan.getId())
                    .attr("amount", repayment.getAmount())
                    .attr("paymentMethod", repayment.getPaymentMethod())
                    .log();

        } catch (InterruptedException ie) {
            spectraLogger.error("LOAN_REPAYMENT_EVENT_PUBLISH_INTERRUPTED", ie)
                    .attr("repaymentId", repayment.getId())
                    .log();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            spectraLogger.error("LOAN_REPAYMENT_EVENT_PUBLISH_ERROR", e)
                    .attr("repaymentId", repayment.getId())
                    .attr("errorMessage", e.getMessage())
                    .log();
        }
    }

    private Map<String, Object> buildLoanRepaymentEventData(Repayment repayment, Loan loan) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "LOAN_REPAYMENT");
        eventData.put("eventTimestamp", LocalDateTime.now().format(FORMATTER));
        eventData.put("repaymentId", repayment.getId());
        eventData.put("receiptNumber", repayment.getReceiptNumber());
        eventData.put("loanId", repayment.getLoanId());
        eventData.put("loanNumber", loan.getLoanNumber());
        eventData.put("borrowerId", repayment.getBorrowerId());
        eventData.put("amount", repayment.getAmount());
        eventData.put("principalPaid", repayment.getPrincipalPaid());
        eventData.put("interestPaid", repayment.getInterestPaid());
        eventData.put("lateFeePaid", repayment.getLateFeePaid());
        eventData.put("advancePayment", repayment.getAdvancePayment());
        eventData.put("paymentDate", repayment.getPaymentDate().toString());
        eventData.put("paymentMethod", repayment.getPaymentMethod().name());
        eventData.put("transactionRef", repayment.getTransactionRef());
        eventData.put("remainingOutstanding", loan.getTotalOutstanding());
        eventData.put("loanStatus", loan.getStatus().name());
        return eventData;
    }

    // ============================================
    // LOAN OVERDUE EVENT
    // ============================================
    public void publishLoanOverdueEvent(Loan loan, OverdueTracking overdueTracking) {
        try {
            Map<String, Object> eventData = buildLoanOverdueEventData(loan, overdueTracking);
            PublishEventResponse response = eventProducer
                    .publishEvent(String.valueOf(overdueTracking.getId()), loanOverdueTopic, eventData, TopicScope.SYSTEM)
                    .toCompletableFuture().get();

            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_OVERDUE_EVENT_PUBLISH_FAILED")
                        .attr("loanId", loan.getId())
                        .attr("loanNumber", loan.getLoanNumber())
                        .attr("overdueDays", overdueTracking.getOverdueDays())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }

            spectraLogger.info("LOAN_OVERDUE_EVENT_PUBLISHED")
                    .attr("loanId", loan.getId())
                    .attr("loanNumber", loan.getLoanNumber())
                    .attr("borrowerId", loan.getBorrowerId())
                    .attr("overdueDays", overdueTracking.getOverdueDays())
                    .attr("overdueAmount", overdueTracking.getOverdueAmount())
                    .attr("collectionStage", overdueTracking.getCollectionStage())
                    .log();

        } catch (InterruptedException ie) {
            spectraLogger.error("LOAN_OVERDUE_EVENT_PUBLISH_INTERRUPTED", ie)
                    .attr("loanId", loan.getId())
                    .log();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            spectraLogger.error("LOAN_OVERDUE_EVENT_PUBLISH_ERROR", e)
                    .attr("loanId", loan.getId())
                    .attr("errorMessage", e.getMessage())
                    .log();
        }
    }

    private Map<String, Object> buildLoanOverdueEventData(Loan loan, OverdueTracking overdueTracking) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "LOAN_OVERDUE");
        eventData.put("eventTimestamp", LocalDateTime.now().format(FORMATTER));
        eventData.put("loanId", loan.getId());
        eventData.put("loanNumber", loan.getLoanNumber());
        eventData.put("borrowerId", loan.getBorrowerId());
        eventData.put("householdId", loan.getHouseholdId());
        eventData.put("overdueSince", overdueTracking.getOverdueSince().toString());
        eventData.put("overdueDays", overdueTracking.getOverdueDays());
        eventData.put("overduePrincipal", overdueTracking.getOverduePrincipal());
        eventData.put("overdueInterest", overdueTracking.getOverdueInterest());
        eventData.put("overdueAmount", overdueTracking.getOverdueAmount());
        eventData.put("penaltyAmount", overdueTracking.getPenaltyAmount());
        eventData.put("totalDue", overdueTracking.getTotalDue());
        eventData.put("collectionStage", overdueTracking.getCollectionStage().name());
        eventData.put("principalAmount", loan.getPrincipalAmount());
        eventData.put("disbursementDate", loan.getDisbursementDate().toString());
        return eventData;
    }

    // ============================================
    // LOAN CANCELLED EVENT
    // ============================================
    public void publishLoanCancelledEvent(Loan loan, String reason) {
        try {
            Map<String, Object> eventData = buildLoanCancelledEventData(loan, reason);
            PublishEventResponse response = eventProducer
                    .publishEvent(String.valueOf(loan.getId()), loanCancelledTopic, eventData, TopicScope.SYSTEM)
                    .toCompletableFuture().get();

            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_CANCELLED_EVENT_PUBLISH_FAILED")
                        .attr("loanId", loan.getId())
                        .attr("loanNumber", loan.getLoanNumber())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }

            spectraLogger.info("LOAN_CANCELLED_EVENT_PUBLISHED")
                    .attr("loanId", loan.getId())
                    .attr("loanNumber", loan.getLoanNumber())
                    .attr("reason", reason)
                    .log();

        } catch (InterruptedException ie) {
            spectraLogger.error("LOAN_CANCELLED_EVENT_PUBLISH_INTERRUPTED", ie)
                    .attr("loanId", loan.getId())
                    .log();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            spectraLogger.error("LOAN_CANCELLED_EVENT_PUBLISH_ERROR", e)
                    .attr("loanId", loan.getId())
                    .attr("errorMessage", e.getMessage())
                    .log();
        }
    }

    private Map<String, Object> buildLoanCancelledEventData(Loan loan, String reason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "LOAN_CANCELLED");
        eventData.put("eventTimestamp", LocalDateTime.now().format(FORMATTER));
        eventData.put("loanId", loan.getId());
        eventData.put("loanNumber", loan.getLoanNumber());
        eventData.put("borrowerId", loan.getBorrowerId());
        eventData.put("principalAmount", loan.getPrincipalAmount());
        eventData.put("cancellationReason", reason);
        return eventData;
    }

    // ============================================
    // LOAN CLOSED EVENT
    // ============================================
    public void publishLoanClosedEvent(Loan loan) {
        try {
            Map<String, Object> eventData = buildLoanClosedEventData(loan);
            PublishEventResponse response = eventProducer
                    .publishEvent(String.valueOf(loan.getId()), loanClosedTopic, eventData, TopicScope.SYSTEM)
                    .toCompletableFuture().get();

            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_CLOSED_EVENT_PUBLISH_FAILED")
                        .attr("loanId", loan.getId())
                        .attr("loanNumber", loan.getLoanNumber())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }

            spectraLogger.info("LOAN_CLOSED_EVENT_PUBLISHED")
                    .attr("loanId", loan.getId())
                    .attr("loanNumber", loan.getLoanNumber())
                    .attr("totalPaid", loan.getTotalPaid())
                    .log();

        } catch (InterruptedException ie) {
            spectraLogger.error("LOAN_CLOSED_EVENT_PUBLISH_INTERRUPTED", ie)
                    .attr("loanId", loan.getId())
                    .log();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            spectraLogger.error("LOAN_CLOSED_EVENT_PUBLISH_ERROR", e)
                    .attr("loanId", loan.getId())
                    .attr("errorMessage", e.getMessage())
                    .log();
        }
    }

    private Map<String, Object> buildLoanClosedEventData(Loan loan) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "LOAN_CLOSED");
        eventData.put("eventTimestamp", LocalDateTime.now().format(FORMATTER));
        eventData.put("loanId", loan.getId());
        eventData.put("loanNumber", loan.getLoanNumber());
        eventData.put("borrowerId", loan.getBorrowerId());
        eventData.put("principalAmount", loan.getPrincipalAmount());
        eventData.put("totalPayable", loan.getTotalPayable());
        eventData.put("totalPaid", loan.getTotalPaid());
        eventData.put("disbursementDate", loan.getDisbursementDate().toString());
        return eventData;
    }

    // ============================================
    // APPLICATION APPROVED EVENT
    // ============================================
    public void publishApplicationApprovedEvent(LoanApplication application, Long approvedBy) {
        try {
            Map<String, Object> eventData = buildApplicationApprovedEventData(application, approvedBy);
            PublishEventResponse response = eventProducer
                    .publishEvent(String.valueOf(application.getId()), applicationApprovedTopic, eventData, TopicScope.SYSTEM)
                    .toCompletableFuture().get();

            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("APPLICATION_APPROVED_EVENT_PUBLISH_FAILED")
                        .attr("applicationId", application.getId())
                        .attr("applicationNumber", application.getApplicationNumber())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }

            spectraLogger.info("APPLICATION_APPROVED_EVENT_PUBLISHED")
                    .attr("applicationId", application.getId())
                    .attr("applicationNumber", application.getApplicationNumber())
                    .attr("borrowerId", application.getBorrowerId())
                    .attr("approvedAmount", application.getApprovedAmount())
                    .log();

        } catch (InterruptedException ie) {
            spectraLogger.error("APPLICATION_APPROVED_EVENT_PUBLISH_INTERRUPTED", ie)
                    .attr("applicationId", application.getId())
                    .log();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            spectraLogger.error("APPLICATION_APPROVED_EVENT_PUBLISH_ERROR", e)
                    .attr("applicationId", application.getId())
                    .attr("errorMessage", e.getMessage())
                    .log();
        }
    }

    private Map<String, Object> buildApplicationApprovedEventData(LoanApplication application, Long approvedBy) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "APPLICATION_APPROVED");
        eventData.put("eventTimestamp", LocalDateTime.now().format(FORMATTER));
        eventData.put("applicationId", application.getId());
        eventData.put("applicationNumber", application.getApplicationNumber());
        eventData.put("borrowerId", application.getBorrowerId());
        eventData.put("productId", application.getProductId());
        eventData.put("requestedAmount", application.getRequestedAmount());
        eventData.put("approvedAmount", application.getApprovedAmount());
        eventData.put("approvedBy", approvedBy);
        return eventData;
    }

    // ============================================
    // APPLICATION REJECTED EVENT
    // ============================================
    public void publishApplicationRejectedEvent(LoanApplication application, String rejectionReason) {
        try {
            Map<String, Object> eventData = buildApplicationRejectedEventData(application, rejectionReason);
            PublishEventResponse response = eventProducer
                    .publishEvent(String.valueOf(application.getId()), applicationRejectedTopic, eventData, TopicScope.SYSTEM)
                    .toCompletableFuture().get();

            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("APPLICATION_REJECTED_EVENT_PUBLISH_FAILED")
                        .attr("applicationId", application.getId())
                        .attr("applicationNumber", application.getApplicationNumber())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }

            spectraLogger.info("APPLICATION_REJECTED_EVENT_PUBLISHED")
                    .attr("applicationId", application.getId())
                    .attr("applicationNumber", application.getApplicationNumber())
                    .attr("borrowerId", application.getBorrowerId())
                    .log();

        } catch (InterruptedException ie) {
            spectraLogger.error("APPLICATION_REJECTED_EVENT_PUBLISH_INTERRUPTED", ie)
                    .attr("applicationId", application.getId())
                    .log();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            spectraLogger.error("APPLICATION_REJECTED_EVENT_PUBLISH_ERROR", e)
                    .attr("applicationId", application.getId())
                    .attr("errorMessage", e.getMessage())
                    .log();
        }
    }

    private Map<String, Object> buildApplicationRejectedEventData(LoanApplication application, String rejectionReason) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventType", "APPLICATION_REJECTED");
        eventData.put("eventTimestamp", LocalDateTime.now().format(FORMATTER));
        eventData.put("applicationId", application.getId());
        eventData.put("applicationNumber", application.getApplicationNumber());
        eventData.put("borrowerId", application.getBorrowerId());
        eventData.put("requestedAmount", application.getRequestedAmount());
        eventData.put("rejectionReason", rejectionReason);
        return eventData;
    }
}