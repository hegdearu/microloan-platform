package in.zeta.microloan.platform.service;

import com.google.gson.Gson;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.producer.EventProducer;
import in.zeta.oms.atropos.response.PublishEventResponse;
import in.zeta.oms.atropos.response.PublishStatus;
import in.zeta.spectra.capture.SpectraLogger;
import olympus.trace.OlympusSpectra;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class AtroposEventPublisherService {

    private static final SpectraLogger spectraLogger = OlympusSpectra.getLogger(AtroposEventPublisherService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final EventProducer eventProducer;
    private final Gson gson;

    @Value("${event.topic.loan.issued:microloan-application-issued}")
    private String loanIssuedTopic;
    @Value("${event.topic.loan.repayment:microloan-application-repayment}")
    private String loanRepaymentTopic;
    @Value("${event.topic.loan.overdue:microloan-application-overdue}")
    private String loanOverdueTopic;
    @Value("${event.topic.loan.cancelled:microloan-application-cancelled}")
    private String loanCancelledTopic;
    @Value("${event.topic.loan.closed:microloan-application-closed}")
    private String loanClosedTopic;
    @Value("${event.topic.application.approved:microloan-application-approved}")
    private String applicationApprovedTopic;
    @Value("${event.topic.application.rejected:microloan-application-rejected}")
    private String applicationRejectedTopic;

    public AtroposEventPublisherService(EventProducer eventProducer, Gson gson) {
        this.eventProducer = eventProducer;
        this.gson = gson;
    }

    public void publishLoanIssuedEvent(Loan loan) {
        try {
            String eventData = buildLoanIssuedEventData(loan);
            PublishEventResponse response = eventProducer.publishEvent(eventData, loanIssuedTopic)
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

    private String buildLoanIssuedEventData(Loan loan) {
        return gson.toJson(Map.ofEntries(
                Map.entry("eventType", "LOAN_ISSUED"),
                Map.entry("eventTimestamp", LocalDateTime.now().format(FORMATTER)),
                Map.entry("loanId", loan.getId()),
                Map.entry("loanNumber", loan.getLoanNumber()),
                Map.entry("applicationId", loan.getApplicationId()),
                Map.entry("borrowerId", loan.getBorrowerId()),
                Map.entry("householdId", loan.getHouseholdId()),
                Map.entry("productId", loan.getProductId()),
                Map.entry("principalAmount", loan.getPrincipalAmount()),
                Map.entry("interestRate", loan.getInterestRate()),
                Map.entry("tenureMonths", loan.getTenureMonths()),
                Map.entry("emiAmount", loan.getEmiAmount()),
                Map.entry("totalPayable", loan.getTotalPayable()),
                Map.entry("disbursementDate", loan.getDisbursementDate().toString()),
                Map.entry("firstDueDate", loan.getFirstDueDate().toString()),
                Map.entry("disbursementMethod", loan.getDisbursementMethod().name()),
                Map.entry("status", loan.getStatus().name())
        ));
    }

    public void publishLoanRepaymentEvent(Repayment repayment, Loan loan) {
        try {
            String eventData = buildLoanRepaymentEventData(repayment, loan);
            PublishEventResponse response = eventProducer.publishEvent(eventData, loanRepaymentTopic)
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

    private String buildLoanRepaymentEventData(Repayment repayment, Loan loan) {
        return gson.toJson(Map.ofEntries(
                Map.entry("eventType", "LOAN_REPAYMENT"),
                Map.entry("eventTimestamp", LocalDateTime.now().format(FORMATTER)),
                Map.entry("repaymentId", repayment.getId()),
                Map.entry("receiptNumber", repayment.getReceiptNumber()),
                Map.entry("loanId", repayment.getLoanId()),
                Map.entry("loanNumber", loan.getLoanNumber()),
                Map.entry("borrowerId", repayment.getBorrowerId()),
                Map.entry("amount", repayment.getAmount()),
                Map.entry("principalPaid", repayment.getPrincipalPaid()),
                Map.entry("interestPaid", repayment.getInterestPaid()),
                Map.entry("lateFeePaid", repayment.getLateFeePaid()),
                Map.entry("advancePayment", repayment.getAdvancePayment()),
                Map.entry("paymentDate", repayment.getPaymentDate().toString()),
                Map.entry("paymentMethod", repayment.getPaymentMethod().name()),
                Map.entry("transactionRef", repayment.getTransactionRef()),
                Map.entry("remainingOutstanding", loan.getTotalOutstanding()),
                Map.entry("loanStatus", loan.getStatus().name())
        ));
    }

    public void publishLoanOverdueEvent(Loan loan, OverdueTracking overdueTracking) {
        try {
            String eventData = buildLoanOverdueEventData(loan, overdueTracking);
            PublishEventResponse response = eventProducer.publishEvent(eventData, loanOverdueTopic)
                    .toCompletableFuture().get();
            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_OVERDUE_EVENT_PUBLISH_FAILED")
                        .attr("loanId", loan.getId())
                        .attr("overdueDays", overdueTracking.getOverdueDays())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }
            spectraLogger.info("LOAN_OVERDUE_EVENT_PUBLISHED")
                    .attr("loanId", loan.getId())
                    .attr("overdueDays", overdueTracking.getOverdueDays())
                    .attr("totalDue", overdueTracking.getTotalDue())
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

    private String buildLoanOverdueEventData(Loan loan, OverdueTracking overdueTracking) {
        return gson.toJson(Map.ofEntries(
                Map.entry("eventType", "LOAN_OVERDUE"),
                Map.entry("eventTimestamp", LocalDateTime.now().format(FORMATTER)),
                Map.entry("loanId", loan.getId()),
                Map.entry("loanNumber", loan.getLoanNumber()),
                Map.entry("borrowerId", loan.getBorrowerId()),
                Map.entry("householdId", loan.getHouseholdId()),
                Map.entry("overdueSince", overdueTracking.getOverdueSince().toString()),
                Map.entry("overdueDays", overdueTracking.getOverdueDays()),
                Map.entry("overduePrincipal", overdueTracking.getOverduePrincipal()),
                Map.entry("overdueInterest", overdueTracking.getOverdueInterest()),
                Map.entry("overdueAmount", overdueTracking.getOverdueAmount()),
                Map.entry("penaltyAmount", overdueTracking.getPenaltyAmount()),
                Map.entry("totalDue", overdueTracking.getTotalDue()),
                Map.entry("collectionStage", overdueTracking.getCollectionStage().name()),
                Map.entry("principalAmount", loan.getPrincipalAmount()),
                Map.entry("disbursementDate", loan.getDisbursementDate().toString())
        ));
    }

    public void publishLoanCancelledEvent(Loan loan, String reason) {
        try {
            String eventData = buildLoanCancelledEventData(loan, reason);
            PublishEventResponse response = eventProducer.publishEvent(eventData, loanCancelledTopic)
                    .toCompletableFuture().get();
            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_CANCELLED_EVENT_PUBLISH_FAILED")
                        .attr("loanId", loan.getId())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }
            spectraLogger.info("LOAN_CANCELLED_EVENT_PUBLISHED")
                    .attr("loanId", loan.getId())
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

    private String buildLoanCancelledEventData(Loan loan, String reason) {
        return gson.toJson(Map.ofEntries(
                Map.entry("eventType", "LOAN_CANCELLED"),
                Map.entry("eventTimestamp", LocalDateTime.now().format(FORMATTER)),
                Map.entry("loanId", loan.getId()),
                Map.entry("loanNumber", loan.getLoanNumber()),
                Map.entry("borrowerId", loan.getBorrowerId()),
                Map.entry("principalAmount", loan.getPrincipalAmount()),
                Map.entry("cancellationReason", reason)
        ));
    }

    public void publishLoanClosedEvent(Loan loan) {
        try {
            String eventData = buildLoanClosedEventData(loan);
            PublishEventResponse response = eventProducer.publishEvent(eventData, loanClosedTopic)
                    .toCompletableFuture().get();
            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("LOAN_CLOSED_EVENT_PUBLISH_FAILED")
                        .attr("loanId", loan.getId())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }
            spectraLogger.info("LOAN_CLOSED_EVENT_PUBLISHED")
                    .attr("loanId", loan.getId())
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

    private String buildLoanClosedEventData(Loan loan) {
        return gson.toJson(Map.ofEntries(
                Map.entry("eventType", "LOAN_CLOSED"),
                Map.entry("eventTimestamp", LocalDateTime.now().format(FORMATTER)),
                Map.entry("loanId", loan.getId()),
                Map.entry("loanNumber", loan.getLoanNumber()),
                Map.entry("borrowerId", loan.getBorrowerId()),
                Map.entry("principalAmount", loan.getPrincipalAmount()),
                Map.entry("totalPayable", loan.getTotalPayable()),
                Map.entry("totalPaid", loan.getTotalPaid()),
                Map.entry("disbursementDate", loan.getDisbursementDate().toString())
        ));
    }

    public void publishApplicationApprovedEvent(LoanApplication application) {
        try {
            String eventData = buildApplicationApprovedEventData(application);
            PublishEventResponse response = eventProducer.publishEvent(eventData, applicationApprovedTopic)
                    .toCompletableFuture().get();
            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("APPLICATION_APPROVED_EVENT_PUBLISH_FAILED")
                        .attr("applicationId", application.getId())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }
            spectraLogger.info("APPLICATION_APPROVED_EVENT_PUBLISHED")
                    .attr("applicationId", application.getId())
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

    private String buildApplicationApprovedEventData(LoanApplication application) {
        return gson.toJson(Map.ofEntries(
                Map.entry("eventType", "APPLICATION_APPROVED"),
                Map.entry("eventTimestamp", LocalDateTime.now().format(FORMATTER)),
                Map.entry("applicationId", application.getId()),
                Map.entry("applicationNumber", application.getApplicationNumber()),
                Map.entry("borrowerId", application.getBorrowerId()),
                Map.entry("productId", application.getProductId()),
                Map.entry("requestedAmount", application.getRequestedAmount()),
                Map.entry("approvedAmount", application.getApprovedAmount())
        ));
    }

    public void publishApplicationRejectedEvent(LoanApplication application, String rejectionReason) {
        try {
            String eventData = buildApplicationRejectedEventData(application, rejectionReason);
            PublishEventResponse response = eventProducer.publishEvent(eventData, applicationRejectedTopic)
                    .toCompletableFuture().get();
            if (response.getStatus() == PublishStatus.FAILED) {
                spectraLogger.error("APPLICATION_REJECTED_EVENT_PUBLISH_FAILED")
                        .attr("applicationId", application.getId())
                        .attr("status", response.getStatus())
                        .log();
                return;
            }
            spectraLogger.info("APPLICATION_REJECTED_EVENT_PUBLISHED")
                    .attr("applicationId", application.getId())
                    .attr("rejectionReason", rejectionReason)
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

    private String buildApplicationRejectedEventData(LoanApplication application, String rejectionReason) {
        return gson.toJson(Map.ofEntries(
                Map.entry("eventType", "APPLICATION_REJECTED"),
                Map.entry("eventTimestamp", LocalDateTime.now().format(FORMATTER)),
                Map.entry("applicationId", application.getId()),
                Map.entry("applicationNumber", application.getApplicationNumber()),
                Map.entry("borrowerId", application.getBorrowerId()),
                Map.entry("requestedAmount", application.getRequestedAmount()),
                Map.entry("rejectionReason", rejectionReason)
        ));
    }
}