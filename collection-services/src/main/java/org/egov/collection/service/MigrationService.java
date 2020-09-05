package org.egov.collection.service;

import static org.egov.collection.model.enums.InstrumentTypesEnum.CARD;
import static org.egov.collection.model.enums.InstrumentTypesEnum.ONLINE;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.egov.collection.config.ApplicationProperties;
import org.egov.collection.model.AuditDetails;
import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentDetail;
import org.egov.collection.model.PaymentResponse;
import org.egov.collection.model.RequestInfoWrapper;
import org.egov.collection.model.enums.PaymentModeEnum;
import org.egov.collection.model.enums.PaymentStatusEnum;
import org.egov.collection.model.v1.AuditDetails_v1;
import org.egov.collection.model.v1.BillAccountDetail_v1;
import org.egov.collection.model.v1.BillDetail_v1;
import org.egov.collection.model.v1.Bill_v1;
import org.egov.collection.model.v1.ReceiptSearchCriteria_v1;
import org.egov.collection.model.v1.Receipt_v1;
import org.egov.collection.producer.CollectionProducer;
import org.egov.collection.repository.ServiceRequestRepository;
import org.egov.collection.service.v1.CollectionService_v1;
import org.egov.collection.web.contract.Bill;
import org.egov.collection.web.contract.Bill.StatusEnum;
import org.egov.collection.web.contract.BillAccountDetail;
import org.egov.collection.web.contract.BillDetail;
import org.egov.collection.web.contract.BillResponse;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MigrationService {


    private ApplicationProperties properties;

    private ServiceRequestRepository serviceRequestRepository;

    private CollectionProducer producer;

    @Autowired
    private CollectionService_v1 collectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    public MigrationService(ApplicationProperties properties, ServiceRequestRepository serviceRequestRepository,CollectionProducer producer) {
        this.properties = properties;
        this.serviceRequestRepository = serviceRequestRepository;
        this.producer = producer;
    }

    public static final String TENANT_QUERY = "select distinct tenantid from egcl_receiptheader_v1 order by tenantid;";

    public void migrate(RequestInfo requestInfo, Integer offsetFromApi,  Integer batchSize, String tenantId) throws JsonProcessingException {

		Set<String> receiptSearch = Stream.of( "PT/101/2019-20/000693", "PT/101/2019-20/000694", "PT/101/2019-20/000695", "PT/101/2019-20/000696", "PT/101/2019-20/000697", "PT/101/2019-20/000698", "PT/101/2019-20/000699", "PT/101/2019-20/000702", "PT/101/2019-20/000701", "PT/101/2019-20/000700", "PT/101/2019-20/000703", "PT/101/2019-20/000707", "PT/101/2019-20/000704", "PT/101/2019-20/000705", "PT/101/2019-20/000706", "PT/101/2019-20/000708", "PT/101/2019-20/000709", "PT/101/2019-20/000710", "PT/101/2019-20/000711", "PT/101/2019-20/000712", "PT/101/2019-20/000713", "PT/101/2019-20/000714", "PT/101/2019-20/000715", "PT/101/2019-20/000716", "PT/101/2019-20/000717", "PT/101/2019-20/000718", "PT/101/2019-20/000719", "PT/101/2019-20/000720", "PT/101/2019-20/000721", "PT/101/2019-20/000722", "PT/101/2019-20/000723", "PT/101/2019-20/000725", "PT/101/2019-20/000724", "PT/101/2019-20/000726", "PT/101/2019-20/000727", "PT/101/2019-20/000728", "PT/101/2019-20/000729", "PT/101/2019-20/000731", "PT/101/2019-20/000730", "PT/101/2019-20/000732", "PT/101/2019-20/000733", "PT/101/2019-20/000734", "PT/101/2019-20/000735", "PT/101/2019-20/000736", "PT/101/2019-20/000737", "PT/101/2019-20/000738", "PT/101/2019-20/000739", "PT/101/2019-20/000740", "PT/101/2019-20/000741", "PT/101/2019-20/000742", "PT/101/2019-20/000743", "PT/101/2019-20/000744", "PT/101/2019-20/000745", "PT/101/2019-20/000746", "PT/101/2019-20/000749", "PT/101/2019-20/000747", "PT/101/2019-20/000748", "PT/101/2019-20/000750", "PT/101/2019-20/000751", "PT/101/2019-20/000752", "PT/101/2019-20/000753", "PT/101/2019-20/000754", "PT/101/2019-20/000755", "PT/101/2019-20/000756", "PT/101/2019-20/000757", "PT/101/2019-20/000758", "PT/101/2019-20/000759", "PT/101/2019-20/000760", "PT/101/2019-20/000761", "PT/101/2019-20/000765", "PT/101/2019-20/000762", "PT/101/2019-20/000763", "PT/101/2019-20/000764", "PT/101/2019-20/000768", "PT/101/2019-20/000766", "PT/101/2019-20/000767", "PT/101/2019-20/000769", "PT/101/2019-20/000770", "PT/101/2019-20/000771", "PT/101/2019-20/000772", "PT/101/2019-20/000775", "PT/101/2019-20/000773", "PT/101/2019-20/000774", "PT/101/2019-20/000776", "PT/101/2019-20/000777", "PT/101/2019-20/000778", "PT/101/2019-20/000779", "PT/101/2019-20/000780", "PT/101/2019-20/000781", "PT/101/2019-20/000782", "PT/101/2019-20/000783", "PT/101/2019-20/000784", "PT/101/2019-20/000785", "PT/101/2019-20/000786", "PT/101/2019-20/000787", "PT/101/2019-20/000788", "PT/101/2019-20/000789", "PT/101/2019-20/000790", "PT/101/2019-20/000791", "PT/101/2019-20/000792").collect(Collectors.toCollection(HashSet::new));

//        List<String> tenantIdList =jdbcTemplate.queryForList(TENANT_QUERY,String.class);
		List<String> tenantIdList = new ArrayList<String>();
		tenantIdList.add(tenantId);
        for(String tenantIdEntry:tenantIdList){
        
        	Integer offset = offsetFromApi;
        	
			if (tenantId != null && !tenantIdEntry.equalsIgnoreCase(tenantId)) {
				continue;
			} else {
				tenantId = null;
				offsetFromApi = 0;
			}
        	
                while(true){
                    long startTime = System.currentTimeMillis();
                    ReceiptSearchCriteria_v1 criteria_v1 = ReceiptSearchCriteria_v1.builder()
                            .offset(offset).limit(batchSize).tenantId(tenantIdEntry).receiptNumbers(receiptSearch).build();
                    List<Receipt_v1> receipts = collectionService.fetchReceipts(criteria_v1);
                    if(CollectionUtils.isEmpty(receipts))
                        break;
                    migrateReceipt(requestInfo, receipts);
                    
                    log.info("Total receipts migrated: " + offset + "for tenantId : " + tenantIdEntry);
                    offset += batchSize;
                    
                    long endtime = System.currentTimeMillis();
                    long elapsetime = endtime - startTime;
                    System.out.println("\n\nBatch Elapsed Time--->"+elapsetime+"\n\n");
                }
        }

    }

    public void migrateReceipt(RequestInfo requestInfo, List<Receipt_v1> receipts){
    	
        List<Payment> paymentList = new ArrayList<Payment>();
        
		for (Receipt_v1 receipt : receipts) {

			Bill newBill = convertBillToNew(receipt.getBill().get(0), receipt.getAuditDetails());

			Payment payment = transformToPayment(requestInfo, receipt, newBill);
			paymentList.add(payment);
		}
        
        PaymentResponse paymentResponse = new PaymentResponse(new ResponseInfo(), paymentList);
        producer.producer(properties.getCollectionMigrationTopicName(), properties
                .getCollectionMigrationTopicKey(), paymentResponse);
    }

	private Bill convertBillToNew(Bill_v1 bill_v1, AuditDetails_v1 oldAuditDetails) {
		
		BigDecimal AmountPaid = bill_v1.getBillDetails().stream().map(detail -> detail.getAmountPaid()).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalAmount = bill_v1.getBillDetails().stream().map(detail -> detail.getTotalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
		StatusEnum status = StatusEnum.fromValue(bill_v1.getBillDetails().get(0).getStatus()); 
		status =  status != null ? status : StatusEnum.EXPIRED;
		
		AuditDetails auditDetails = AuditDetails.builder()
				.lastModifiedTime(oldAuditDetails.getLastModifiedDate())
				.lastModifiedBy(oldAuditDetails.getLastModifiedBy())
				.createdTime(oldAuditDetails.getCreatedDate())
				.createdBy(oldAuditDetails.getCreatedBy())
				.build();
		
		JsonNode jsonNode = null;
		
		try {
			if (null != bill_v1.getAdditionalDetails())
				jsonNode = mapper.readTree(bill_v1.getAdditionalDetails().toString());
		} catch (IOException e) {

		}

		List<BillDetail> billdetails = getNewBillDetails(bill_v1.getBillDetails(), auditDetails, bill_v1.getId()); 
		
		return Bill.builder()
				.reasonForCancellation(bill_v1.getBillDetails().get(0).getCancellationRemarks())
				.businessService(bill_v1.getBillDetails().get(0).getBusinessService())
				.consumerCode(bill_v1.getBillDetails().get(0).getConsumerCode())
				.billNumber(bill_v1.getBillDetails().get(0).getBillNumber())
				.billDate(bill_v1.getBillDetails().get(0).getBillDate())
				.payerAddress(bill_v1.getPayerAddress())
				.mobileNumber(bill_v1.getMobileNumber())
				.auditDetails(auditDetails)
				.payerEmail(bill_v1.getPayerEmail())
				.payerName(bill_v1.getPayerName())
				.tenantId(bill_v1.getTenantId())
				.payerId(bill_v1.getPayerId())
				.paidBy(bill_v1.getPaidBy())
				.additionalDetails(jsonNode)
				.totalAmount(totalAmount)
				.billDetails(billdetails)
				.amountPaid(AmountPaid)
				.id(bill_v1.getId())
				.status(status)	
				.build();
		
		
	}

	private List<BillDetail> getNewBillDetails(List<BillDetail_v1> billDetails, AuditDetails auditdetails, String billId) {

		List<BillDetail> newDetails = new ArrayList<>();
		
		for (BillDetail_v1 oldDetail : billDetails) {
			
			List<BillAccountDetail> accDetails = getNewAccDetails(oldDetail.getBillAccountDetails(), auditdetails);
			Long expiryDate = oldDetail.getExpiryDate() != null ? oldDetail.getExpiryDate() : 0l;
			String demandId = oldDetail.getDemandId() != null ? oldDetail.getDemandId() : "";
			String dId = oldDetail.getId() != null ? oldDetail.getId() : UUID.randomUUID().toString();
			
			BillDetail detail = BillDetail.builder()
				.manualReceiptNumber(oldDetail.getManualReceiptNumber())
				.cancellationRemarks(oldDetail.getCancellationRemarks())
				.manualReceiptDate(oldDetail.getManualReceiptDate())
				.billDescription(oldDetail.getBillDescription())
				.collectionType(oldDetail.getCollectionType())
				.displayMessage(oldDetail.getDisplayMessage())
				.voucherHeader(oldDetail.getVoucherHeader())
				.amountPaid(oldDetail.getAmountPaid())
				.fromPeriod(oldDetail.getFromPeriod())
				.amount(oldDetail.getTotalAmount())
				.boundary(oldDetail.getBoundary())
				.demandId(demandId)
				.toPeriod(oldDetail.getToPeriod())
				.tenantId(oldDetail.getTenantId())
				.channel(oldDetail.getChannel())
				.billAccountDetails(accDetails)
				.auditDetails(auditdetails)
				.expiryDate(expiryDate)
				.billId(billId)
				.id(dId)
				.build();

			newDetails.add(detail);
		}
		
		return newDetails;
	}

	private List<BillAccountDetail> getNewAccDetails(List<BillAccountDetail_v1> billAccountDetails, AuditDetails auditdetails) {

		 List<BillAccountDetail> newAccDetails = new ArrayList<>();
	
		for (BillAccountDetail_v1 oldAccDetail : billAccountDetails) {
			
			String DDId = oldAccDetail.getDemandDetailId() != null ? oldAccDetail.getDemandDetailId() : "";
			String bADID = oldAccDetail.getId() != null ? oldAccDetail.getId() : UUID.randomUUID().toString();
			String taxHeadCode = oldAccDetail.getTaxHeadCode() != null ? oldAccDetail.getTaxHeadCode() : "ADVANCE_ADJUSTMENT";
			
			BillAccountDetail accDetail = BillAccountDetail.builder()
					.adjustedAmount(oldAccDetail.getAdjustedAmount())
					.isActualDemand(oldAccDetail.getIsActualDemand())
					.billDetailId(oldAccDetail.getBillDetail())
					.tenantId(oldAccDetail.getTenantId())
					.purpose(oldAccDetail.getPurpose())
					.amount(oldAccDetail.getAmount())
					.order(oldAccDetail.getOrder())
					.auditDetails(auditdetails)
					.taxHeadCode(taxHeadCode)
					.demandDetailId(DDId)
					.id(bADID)
					.build();
			
			newAccDetails.add(accDetail);
		}
		
		return newAccDetails;
	}

	private Payment transformToPayment(RequestInfo requestInfo, Receipt_v1 receipt, Bill newBill) {

		if (null == newBill.getBillNumber()) {
			newBill.setBillNumber("NA");
		}

		return getPayment(requestInfo, receipt, newBill);
	}
    

    private Payment getPayment(RequestInfo requestInfo, Receipt_v1 receipt, Bill newBill){

        Payment payment = new Payment();

        BigDecimal totalAmount = newBill.getTotalAmount();
        BigDecimal totalAmountPaid = receipt.getInstrument().getAmount();
        newBill.setAmountPaid(totalAmountPaid);
        
        for(BillDetail billDetail : newBill.getBillDetails()) {
        	billDetail.setAmountPaid(totalAmountPaid);
        }
        
        payment.setId(UUID.randomUUID().toString());
        payment.setTenantId(receipt.getTenantId());
        payment.setTotalDue(totalAmount.subtract(totalAmountPaid));
        payment.setTotalAmountPaid(totalAmountPaid);
        payment.setTransactionNumber(receipt.getInstrument().getTransactionNumber());
        payment.setTransactionDate(receipt.getReceiptDate());
        payment.setPaymentMode(PaymentModeEnum.fromValue(receipt.getInstrument().getInstrumentType().getName()));
        payment.setInstrumentDate(receipt.getInstrument().getInstrumentDate());
        payment.setInstrumentNumber(receipt.getInstrument().getInstrumentNumber());
        payment.setInstrumentStatus(receipt.getInstrument().getInstrumentStatus());
        payment.setIfscCode(receipt.getInstrument().getIfscCode());
        payment.setPaidBy(receipt.getBill().get(0).getPaidBy());
        payment.setPayerName(receipt.getBill().get(0).getPayerName());
        payment.setPayerAddress(receipt.getBill().get(0).getPayerAddress());
        payment.setPayerEmail(receipt.getBill().get(0).getPayerEmail());
        payment.setPayerId(receipt.getBill().get(0).getPayerId());
        
        if(receipt.getBill().get(0).getMobileNumber() == null){
            payment.setMobileNumber("NA");
        }else{
            payment.setMobileNumber(receipt.getBill().get(0).getMobileNumber());
        }
        
        if ((payment.getPaymentMode().toString()).equalsIgnoreCase(ONLINE.name()) ||
                payment.getPaymentMode().toString().equalsIgnoreCase(CARD.name()))
            payment.setPaymentStatus(PaymentStatusEnum.DEPOSITED);
        else
            payment.setPaymentStatus(PaymentStatusEnum.NEW);


        AuditDetails auditDetails = getAuditDetail(receipt.getAuditDetails());
        payment.setAuditDetails(auditDetails);
        payment.setAdditionalDetails((JsonNode)receipt.getBill().get(0).getAdditionalDetails());

        PaymentDetail paymentDetail = getPaymentDetail(receipt, auditDetails, requestInfo);
    	
        paymentDetail.setBill(newBill);
        paymentDetail.setPaymentId(payment.getId());
    	paymentDetail.setBillId(newBill.getId());
        paymentDetail.setTotalDue(totalAmount.subtract(totalAmountPaid));
        paymentDetail.setTotalAmountPaid(totalAmountPaid);
        payment.setPaymentDetails(Arrays.asList(paymentDetail));

        return payment;

    }

    private PaymentDetail getPaymentDetail(Receipt_v1 receipt, AuditDetails auditDetails, RequestInfo requestInfo){
        
        PaymentDetail paymentDetail = new PaymentDetail();

        paymentDetail.setId(UUID.randomUUID().toString());
        paymentDetail.setTenantId(receipt.getTenantId());
        paymentDetail.setReceiptNumber(receipt.getReceiptNumber());
        paymentDetail.setManualReceiptNumber(receipt.getBill().get(0).getBillDetails().get(0).getManualReceiptNumber());
        paymentDetail.setManualReceiptDate(receipt.getBill().get(0).getBillDetails().get(0).getManualReceiptDate());
        paymentDetail.setReceiptDate(receipt.getReceiptDate());
        paymentDetail.setReceiptType(receipt.getBill().get(0).getBillDetails().get(0).getReceiptType());
        paymentDetail.setBusinessService(receipt.getBill().get(0).getBillDetails().get(0).getBusinessService());

        paymentDetail.setAuditDetails(auditDetails);
        paymentDetail.setAdditionalDetails((JsonNode)receipt.getBill().get(0).getAdditionalDetails());

        return paymentDetail;

    }

    private AuditDetails getAuditDetail(AuditDetails_v1 oldAuditDetails){
        AuditDetails newAuditDetails = new AuditDetails();
        newAuditDetails.setCreatedBy(oldAuditDetails.getCreatedBy());
        newAuditDetails.setCreatedTime(oldAuditDetails.getCreatedDate());
        newAuditDetails.setLastModifiedBy(oldAuditDetails.getLastModifiedBy());
        newAuditDetails.setLastModifiedTime(oldAuditDetails.getLastModifiedDate());
        return newAuditDetails;
    }

    
    private Map<String, Bill> getBillsForReceipts(List<Receipt_v1> receipts, RequestInfo requestInfo){
    	
		Map<String, Bill> BillIdMap = new HashMap<>();
		String tenantId = receipts.get(0).getTenantId();
    	
    	Map<String, Set<String>> businesssAndBillIdsMap = receipts.stream()
				.flatMap(receipt -> receipt.getBill().stream().flatMap(bill -> bill.getBillDetails().stream()))
				.collect(Collectors.groupingBy(BillDetail_v1::getBusinessService,
						Collectors.mapping(BillDetail_v1::getBillNumber, Collectors.toSet())));
    	
		for (Entry<String, Set<String>> entry : businesssAndBillIdsMap.entrySet()) {

			List<Bill> fetchedBills = getBillFromV2(entry.getValue(), entry.getKey(), tenantId, requestInfo);
			if(!CollectionUtils.isEmpty(fetchedBills))
				BillIdMap.putAll(fetchedBills.stream().collect(Collectors.toMap(Bill::getId, Function.identity())));
		}
		
    	return BillIdMap;
    	
    }
    
    private List<Bill> getBillFromV2(Set<String> billids, String businessService, String tenantId, RequestInfo requestInfo){
    	
//            String billDetailId = bill.getBillDetails().get(0).getBillNumber();
//            //String billId = getBillIdFromBillDetail(billDetailId);
//            String billId = billDetailId;
//            String tenantId = bill.getBillDetails().get(0).getTenantId();
//            String service = bill.getBillDetails().get(0).getBusinessService();
//            String status = bill.getBillDetails().get(0).getStatus();

            StringBuilder url = getBillSearchURI(tenantId,billids,businessService);

            RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();

            Object response = serviceRequestRepository.fetchResult(url,requestInfoWrapper);
            ObjectMapper mapper = new ObjectMapper();
            try{
                BillResponse billResponse = mapper.convertValue(response, BillResponse.class);
                if(billResponse.getBill().size() > 0){

                        billResponse.getBill().forEach(newBill -> {
                        	
                        	 if(null == newBill.getStatus())
                                 newBill.setStatus(Bill.StatusEnum.EXPIRED);
                        });
                }
                return billResponse.getBill();
                
            }catch(Exception e) {
                log.error("bill fetch failed : ",e);
                return null;
            }
    }


	private StringBuilder getBillSearchURI(String tenantId, Set<String> billIds, String service) {
		
		StringBuilder builder = new StringBuilder(properties.getBillingServiceHostName());
		builder.append(properties.getSearchBill()).append("?");
		builder.append("tenantId=").append(tenantId);
		builder.append("&service=").append(service);
		builder.append("&billId=").append(billIds.toString().replace("[","").replace("]",""));

		return builder;

	}

}