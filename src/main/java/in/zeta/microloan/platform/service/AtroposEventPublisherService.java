package in.zeta.microloan.platform.service;

import com.google.gson.Gson;
import in.zeta.microloan.platform.model.*;
import in.zeta.microloan.platform.producer.EventProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static in.zeta.microloan.platform.constants.LogConstants.*;

@Service
public class AtroposEventPublisherService {

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

    public void publishLoanIssuedEvent(Loan loan){

    };

    private String buildLoanIssuedEventData(Loan loan) {
        return gson.toJson(Map.ofEntries(
                Map.entry(EVENT_TYPE, "LOAN_ISSUED"),
                Map.entry(EVENT_TIME_STAMP, LocalDateTime.now().format(FORMATTER)),
                Map.entry(LOAN_ID, loan.getId()),
                Map.entry(LOAN_NUMBER, loan.getLoanNumber()),
                Map.entry(APPLICATION_ID, loan.getApplicationId()),
                Map.entry(BORROWER_ID, loan.getBorrowerId()),
                Map.entry("householdId", loan.getHouseholdId()),
                Map.entry("productId", loan.getProductId()),
                Map.entry(PRINCIPAL_AMOUNT, loan.getPrincipalAmount()),
                Map.entry("interestRate", loan.getInterestRate()),
                Map.entry("tenureMonths", loan.getTenureMonths()),
                Map.entry("emiAmount", loan.getEmiAmount()),
                Map.entry("totalPayable", loan.getTotalPayable()),
                Map.entry(DISBURSEMENT_DATE, loan.getDisbursementDate().toString()),
                Map.entry("firstDueDate", loan.getFirstDueDate().toString()),
                Map.entry("disbursementMethod", loan.getDisbursementMethod().name()),
                Map.entry(STATUS, loan.getStatus().name())
        ));
    }

    public void publishLoanRepaymentEvent(Repayment repayment, Loan loan){

    }

    private String buildLoanRepaymentEventData(Repayment repayment, Loan loan) {
        return gson.toJson(Map.ofEntries(
                Map.entry(EVENT_TYPE, "LOAN_REPAYMENT"),
                Map.entry(EVENT_TIME_STAMP, LocalDateTime.now().format(FORMATTER)),
                Map.entry(REPAYMENT_ID, repayment.getId()),
                Map.entry(RECEIPT_NUMBER, repayment.getReceiptNumber()),
                Map.entry(LOAN_ID, repayment.getLoanId()),
                Map.entry(LOAN_NUMBER, loan.getLoanNumber()),
                Map.entry(BORROWER_ID, repayment.getBorrowerId()),
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

    public void publishLoanOverdueEvent(Loan loan, OverdueTracking overdueTracking){

    }

    private String buildLoanOverdueEventData(Loan loan, OverdueTracking overdueTracking) {
        return gson.toJson(Map.ofEntries(
                Map.entry(EVENT_TYPE, "LOAN_OVERDUE"),
                Map.entry(EVENT_TIME_STAMP, LocalDateTime.now().format(FORMATTER)),
                Map.entry(LOAN_ID, loan.getId()),
                Map.entry(LOAN_NUMBER, loan.getLoanNumber()),
                Map.entry(BORROWER_ID, loan.getBorrowerId()),
                Map.entry("householdId", loan.getHouseholdId()),
                Map.entry("overdueSince", overdueTracking.getOverdueSince().toString()),
                Map.entry(OVERDUE_DAYS, overdueTracking.getOverdueDays()),
                Map.entry("overduePrincipal", overdueTracking.getOverduePrincipal()),
                Map.entry("overdueInterest", overdueTracking.getOverdueInterest()),
                Map.entry("overdueAmount", overdueTracking.getOverdueAmount()),
                Map.entry("penaltyAmount", overdueTracking.getPenaltyAmount()),
                Map.entry("totalDue", overdueTracking.getTotalDue()),
                Map.entry("collectionStage", overdueTracking.getCollectionStage().name()),
                Map.entry(PRINCIPAL_AMOUNT, loan.getPrincipalAmount()),
                Map.entry(DISBURSEMENT_DATE, loan.getDisbursementDate().toString())
        ));
    }

    public void publishLoanCancelledEvent(Loan loan, String reason){

    }

    private String buildLoanCancelledEventData(Loan loan, String reason) {
        return gson.toJson(Map.ofEntries(
                Map.entry(EVENT_TYPE, "LOAN_CANCELLED"),
                Map.entry(EVENT_TIME_STAMP, LocalDateTime.now().format(FORMATTER)),
                Map.entry(LOAN_ID, loan.getId()),
                Map.entry(LOAN_NUMBER, loan.getLoanNumber()),
                Map.entry(BORROWER_ID, loan.getBorrowerId()),
                Map.entry(PRINCIPAL_AMOUNT, loan.getPrincipalAmount()),
                Map.entry("cancellationReason", reason)
        ));
    }

    public void publishLoanClosedEvent(Loan loan){

    }

    private String buildLoanClosedEventData(Loan loan) {
        return gson.toJson(Map.ofEntries(
                Map.entry(EVENT_TYPE, "LOAN_CLOSED"),
                Map.entry(EVENT_TIME_STAMP, LocalDateTime.now().format(FORMATTER)),
                Map.entry(LOAN_ID, loan.getId()),
                Map.entry(LOAN_NUMBER, loan.getLoanNumber()),
                Map.entry(BORROWER_ID, loan.getBorrowerId()),
                Map.entry(PRINCIPAL_AMOUNT, loan.getPrincipalAmount()),
                Map.entry("totalPayable", loan.getTotalPayable()),
                Map.entry("totalPaid", loan.getTotalPaid()),
                Map.entry(DISBURSEMENT_DATE, loan.getDisbursementDate().toString())
        ));
    }

    public void publishApplicationApprovedEvent(LoanApplication application){

    }

    private String buildApplicationApprovedEventData(LoanApplication application) {
        return gson.toJson(Map.ofEntries(
                Map.entry(EVENT_TYPE, "APPLICATION_APPROVED"),
                Map.entry(EVENT_TIME_STAMP, LocalDateTime.now().format(FORMATTER)),
                Map.entry(APPLICATION_ID, application.getId()),
                Map.entry("applicationNumber", application.getApplicationNumber()),
                Map.entry(BORROWER_ID, application.getBorrowerId()),
                Map.entry("productId", application.getProductId()),
                Map.entry("requestedAmount", application.getRequestedAmount()),
                Map.entry("approvedAmount", application.getApprovedAmount())
        ));
    }

    public void publishApplicationRejectedEvent(LoanApplication application, String rejectionReason){

    }

    private String buildApplicationRejectedEventData(LoanApplication application, String rejectionReason) {
        return gson.toJson(Map.ofEntries(
                Map.entry(EVENT_TYPE, "APPLICATION_REJECTED"),
                Map.entry(EVENT_TIME_STAMP, LocalDateTime.now().format(FORMATTER)),
                Map.entry(APPLICATION_ID, application.getId()),
                Map.entry("applicationNumber", application.getApplicationNumber()),
                Map.entry(BORROWER_ID, application.getBorrowerId()),
                Map.entry("requestedAmount", application.getRequestedAmount()),
                Map.entry("rejectionReason", rejectionReason)
        ));
    }
}