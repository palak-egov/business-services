package org.egov.collection.util;

import static java.util.Objects.isNull;
import static org.egov.collection.model.enums.InstrumentTypesEnum.CARD;
import static org.egov.collection.model.enums.InstrumentTypesEnum.CASH;
import static org.egov.collection.model.enums.InstrumentTypesEnum.ONLINE;
import static org.egov.collection.model.enums.ReceiptStatus.APPROVED;
import static org.egov.collection.model.enums.ReceiptStatus.REMITTED;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.egov.collection.model.*;
import org.egov.collection.model.enums.*;
import org.egov.collection.repository.BillingServiceRepository;
import org.egov.collection.repository.BusinessDetailsRepository;
import org.egov.collection.repository.IdGenRepository;
import org.egov.collection.repository.InstrumentRepository;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.BillAccountDetail;
import org.egov.collection.web.contract.BillDetail;
import org.egov.collection.web.contract.BusinessDetailsResponse;
import org.egov.collection.web.contract.Receipt;
import org.egov.collection.web.contract.ReceiptReq;
import org.egov.collection.web.contract.TaxAndPayment;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReceiptEnricher {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private BillingServiceRepository billingRepository;
    private InstrumentRepository instrumentRepository;
    private IdGenRepository idGenRepository;
    private BusinessDetailsRepository businessDetailsRepository;

    @Autowired
    public ReceiptEnricher(BillingServiceRepository billingRepository, InstrumentRepository instrumentRepository,
                           IdGenRepository idGenRepository, BusinessDetailsRepository businessDetailsRepository) {
        this.billingRepository = billingRepository;
        this.instrumentRepository = instrumentRepository;
        this.idGenRepository = idGenRepository;
        this.businessDetailsRepository = businessDetailsRepository;
    }

    /**
     * Fetch instruments from financials for the given receipts
     *
     * @param requestInfo Request Info for the request
     * @param receipts    Receipts to be enriched
     */
    public void enrichReceiptsWithInstruments(RequestInfo requestInfo, List<Receipt> receipts) {
        Set<String> instruments = receipts.stream().map(receipt -> receipt.getInstrument().getId()).collect(Collectors
                .toSet());
        List<Instrument> fetchedInstruments = instrumentRepository.searchInstruments(String.join(",", instruments),
                requestInfo);

        Map<String, Instrument> map = fetchedInstruments.stream().collect(Collectors.toMap(Instrument::getId,
                instrument -> instrument));

        receipts.forEach(receipt -> receipt.setInstrument(map.get(receipt.getInstrument().getId())));
    }

    /**
     * Fetch bill from billing service for the provided bill id
     * Ensure bill exists and amount paid details exist for all bill details
     * Set paid by and amount paid for each bill detail in the new validated bill
     *
     */
   /* public void enrichReceiptPreValidate(ReceiptReq receiptReq) {
    	
        Receipt receipt = receiptReq.getReceipt().get(0);
        Bill billFromRequest = receipt.getBill().get(0);

        if (isNull(receiptReq.getRequestInfo().getUserInfo()) || isNull(receiptReq.getRequestInfo().getUserInfo()
                .getId())) {
            throw new CustomException("USER_INFO_INVALID", "Invalid user info in request info, user id is mandatory");
        }

        List<Bill> validatedBills = billingRepository.fetchBill(receiptReq.getRequestInfo(), receipt.getTenantId(), billFromRequest.getId
                ());

        if (validatedBills.isEmpty() || Objects.isNull(validatedBills.get(0).getBillDetails()) || validatedBills.get(0)
                .getBillDetails().isEmpty()) {
            log.error("Bill ID provided does not exist or is in an invalid state " + billFromRequest.getId());
            throw new CustomException("INVALID_BILL_ID", "Bill ID provided does not exist or is in an invalid state");
        }

        if (validatedBills.get(0).getBillDetails().size() != billFromRequest.getBillDetails().size()) {
            log.error("Mismatch in bill details records provided in request and actual bill. Expected {} billdetails " +
                    "found {} in request", billFromRequest.getBillDetails().size(), validatedBills.get(0)
                    .getBillDetails().size());
            throw new CustomException("INVALID_BILL_DETAILS", "Mismatch in bill detail records provided in request " +
                    "and actual bill");

		}

		Long expiryDate = validatedBills.get(0).getBillDetails().get(0).getExpiryDate();
		if (isNull(expiryDate) || System.currentTimeMillis() >= expiryDate) {
			throw new CustomException("BILL_EXPIRED", "Bill expired or invalid, regenerate bill!");
		}

        Bill validatedBill = validatedBills.get(0);
        validatedBill.setPaidBy(billFromRequest.getPaidBy());
        validatedBill.setPayerName(billFromRequest.getPayerName());
        validatedBill.setMobileNumber(billFromRequest.getMobileNumber());
        validatedBill.setPayerAddress(billFromRequest.getPayerAddress());

        validatedBill.getBillDetails().sort(Comparator.comparing(BillDetail::getId));
        billFromRequest.getBillDetails().sort(Comparator.comparing(BillDetail::getId));

        validateTaxAndPayment(billFromRequest, validatedBill);

        for (int i = 0; i < validatedBill.getBillDetails().size(); i++) {
            validatedBill.getBillDetails().get(i).setAmountPaid(billFromRequest.getBillDetails().get(i).getAmountPaid());

            validatedBill.getBillDetails().get(i).setManualReceiptNumber(billFromRequest.getBillDetails().get(i)
                    .getManualReceiptNumber());

            validatedBill.getBillDetails().get(i).setManualReceiptDate(billFromRequest.getBillDetails().get(i).getManualReceiptDate());

            if (receipt.getInstrument().getInstrumentType().getName().equalsIgnoreCase(ONLINE.name()))
                validatedBill.getBillDetails().get(i).setCollectionType(CollectionType.ONLINE);
            else
                validatedBill.getBillDetails().get(i).setCollectionType(CollectionType.COUNTER);

            if (Objects.isNull(validatedBill.getBillDetails().get(i).getReceiptDate()))
                validatedBill.getBillDetails().get(i).setReceiptDate(new Date().getTime());

            validatedBill.getBillDetails().get(i).setReceiptType(ReceiptType.BILLBASED.toString());

            validatedBill.getBillDetails().get(i).setAdditionalDetails(billFromRequest.getBillDetails().get(i).getAdditionalDetails());

            enrichBillAccountDetails(validatedBill.getBillDetails().get(i), billFromRequest.getBillDetails().get(i));

        }

        AuditDetails auditDetails = AuditDetails.builder().createdBy(receiptReq.getRequestInfo().getUserInfo().getId
                ().toString()).createdDate(System.currentTimeMillis()).lastModifiedBy(receiptReq.getRequestInfo().getUserInfo().getId
                ().toString()).lastModifiedDate(System.currentTimeMillis()).build();
        receipt.setBill(validatedBills);
        receipt.setAuditDetails(auditDetails);
        List<Receipt> receipts = new ArrayList<>();
        receipts.add(receipt);
        receiptReq.setReceipt(receipts);
    }*/









    /**
     * Fetches bill  based on businessservice
     *
     * @param validatedBill
     * @param businessService
     * @return
     */
    public List<Bill> getBillForBusinessService(List<Bill> validatedBill, String businessService) {
        List<Bill> bills = new LinkedList<>();
        validatedBill.forEach(bill -> {
            if(bill.getBusinessService().equalsIgnoreCase(businessService))
                bills.add(bill);
        });
        return bills;
    }




    /**
     * Enrich instrument for financials
     * For each bill detail,
     * - Set status to approved by default for now, no workflow
     * - Set collection type to online or counter
     * - Set receipt date
     * - Generate and set receipt number
     *
     * @param receiptReq Receipt request to be enriched
     */
/*    public void enrichReceiptPostValidate(ReceiptReq receiptReq) {
        Receipt receipt = receiptReq.getReceipt().get(0);
        Bill bill = receipt.getBill().get(0);
        String instrumentType = receipt.getInstrument().getInstrumentType().getName();

        for (BillDetail billDetail : bill.getBillDetails()) {
            billDetail.setId(UUID.randomUUID().toString());

            if (instrumentType.equalsIgnoreCase(ONLINE.name()) || instrumentType.equalsIgnoreCase(CARD.name()))
                billDetail.setStatus(REMITTED.toString());
            else
                billDetail.setStatus(APPROVED.toString());

            String receiptNumber = idGenRepository.generateReceiptNumber(receiptReq.getRequestInfo(), billDetail.getBusinessService(),
                    billDetail.getTenantId());
            billDetail.setReceiptNumber(receiptNumber);

            for (BillAccountDetail billAccountDetail : billDetail.getBillAccountDetails()) {
                billAccountDetail.setId(UUID.randomUUID().toString());
            }
        }
        enrichInstrument(receiptReq);

        List<Receipt> receipts = new ArrayList<>();
        receipts.add(receipt);
        receiptReq.setReceipt(receipts);

    }*/




    /**
     * Enrich the instrument object,
     * - In case of cash / card [append card digits], generate transaction number
     * - In case of online, dd, cheque use given txn number, and date
     *
     * @param receiptReq Receipt request to be enriched
     */
    private void enrichInstrument(ReceiptReq receiptReq) {
        Receipt receipt = receiptReq.getReceipt().get(0);
        String instrumentType = receipt.getInstrument().getInstrumentType().getName();

        Instrument instrument = receipt.getInstrument();
        instrument.setId(UUID.randomUUID().toString());
        instrument.setTransactionType(TransactionType.Debit);
        instrument.setTenantId(receipt.getTenantId());
        instrument.setPayee(receipt.getBill().get(0).getPayerName());

        instrument.setInstrumentDate(instrument.getTransactionDateInput());


        if (instrumentType.equalsIgnoreCase(CASH.name())) {
            String transactionId = idGenRepository.generateTransactionNumber(receiptReq.getRequestInfo(),
                    receipt.getTenantId());
            instrument.setTransactionNumber(transactionId);
        }

        if (instrumentType.equalsIgnoreCase(CASH.name()) || instrumentType.equalsIgnoreCase(CARD.name())) {

            instrument.setTransactionDateInput(new Date().getTime());
            instrument.setTransactionDate(new Date());

        } else {
            instrument.setTransactionDate(new Date(instrument.getTransactionDateInput()));
        }

        if (instrumentType.equalsIgnoreCase(ONLINE.name()) || instrumentType.equalsIgnoreCase(CARD.name()))
            instrument.setInstrumentStatus(InstrumentStatusEnum.DEPOSITED);
        else
            instrument.setInstrumentStatus(InstrumentStatusEnum.NEW);

        receipt.setTransactionId(instrument.getTransactionNumber());

        List<Receipt> receipts = new ArrayList<>();
        receipts.add(receipt);
        receiptReq.setReceipt(receipts);
    }



    /**
     * Enrich the bill account details object
     * - Copy over additional details received part of request to validated bill
     *
     * @param validatedBillDetail   Validated bill detail from billing service
     * @param billDetailFromRequest Bill detail from request
     */
    private void enrichBillAccountDetails(BillDetail validatedBillDetail, BillDetail billDetailFromRequest) {
        if (!Objects.isNull(billDetailFromRequest.getBillAccountDetails()) && billDetailFromRequest
                .getBillAccountDetails().size() == validatedBillDetail.getBillAccountDetails().size()) {

            billDetailFromRequest.getBillAccountDetails().sort(Comparator.comparing(BillAccountDetail::getId));
            validatedBillDetail.getBillAccountDetails().sort(Comparator.comparing(BillAccountDetail::getId));

            for (int i = 0; i < validatedBillDetail.getBillAccountDetails().size(); i++) {
                validatedBillDetail.getBillAccountDetails().get(i).setAdditionalDetails(billDetailFromRequest
                        .getBillAccountDetails().get(i).getAdditionalDetails());
            }
        }
    }

    /**
     * Apportion adds another billAccDetail for Advance tax head when advance amt is paid, this method enriches that object
     * 
     * @param bills
     */
    public void enrichAdvanceTaxHead(List<Bill> bills) {
            bills.forEach(bill -> {
                bill.getBillDetails().forEach(billDetail -> {
                    billDetail.getBillAccountDetails().forEach(billAccountDetail -> {
                        if (StringUtils.isEmpty(billAccountDetail.getId()) && billAccountDetail.getPurpose().equals(Purpose.ADVANCE)) {
                            billAccountDetail.setId(UUID.randomUUID().toString());
                            billAccountDetail.setTenantId(bill.getTenantId());
                            billAccountDetail.setBillDetailId(billDetail.getId());
                        }
                    });
                });
            });
    }





    private void enrichPaymentDetail(PaymentDetail paymentDetail){}



}
