package org.mifos.connector.bulk.utils;

import org.mifos.connector.bulk.schema.Transaction;
import org.mifos.connector.bulk.schema.TransactionResult;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.gsma.dto.*;
import org.mifos.connector.common.mojaloop.dto.MoneyData;
import org.mifos.connector.common.mojaloop.dto.Party;
import org.mifos.connector.common.mojaloop.dto.PartyIdInfo;
import org.mifos.connector.common.mojaloop.type.IdentifierType;

import java.io.*;

public class Utils {

    public static String getTenantSpecificWorkflowId(String originalWorkflowName, String tenantName) {
        return originalWorkflowName.replace("{dfspid}", tenantName);
    }

    public static String getBulkConnectorBpmnName(String originalWorkflowName,
                                                  String paymentMode, String tenantName) {
        return originalWorkflowName.replace("{MODE}", paymentMode.toLowerCase()).replace("{dfspid}", tenantName);
    }

    public static String mergeCsvFile(String file1, String file2) {
        try {
            // create a writer for permFile
            BufferedWriter out = new BufferedWriter(new FileWriter(file1, true));
            // create a reader for tmpFile
            BufferedReader in = new BufferedReader(new FileReader(file2));
            String str;
            boolean isFirstLine = true;
            while ((str = in.readLine()) != null) {
                if (isFirstLine) {
                    // used for skipping header writing
                    isFirstLine = false;
                    continue;
                }
                out.write(str+"\n");
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file1;
    }

    public static String getAwsFileUrl(String baseUrl, String filename) {
        return String.format("%s/%s", baseUrl, filename);
    }

    /**
     * takes initial timer in the ISO 8601 durations format
     * for more info check
     * https://docs.camunda.io/docs/0.26/reference/bpmn-workflows/timer-events/#time-duration
     *
     * @param initialTimer initial timer in the ISO 8601 durations format, ex: PT45S
     * @return next timer value in the ISO 8601 durations format
     */
    public static String getNextTimer(String initialTimer){
        String stringSecondsValue = initialTimer.split("T")[1].split("S")[0];
        int initialSeconds = Integer.parseInt(stringSecondsValue);

        int currentPower = (int) ( Math.log(initialSeconds) / Math.log(2) );
        int next = (int) Math.pow(2, ++currentPower);

        return String.format("PT%sS", next);
    }

    public static String getZeebeTimerValue(int timer) {
        return String.format("PT%sS", timer);
    }

    public static TransactionResult mapToResultDTO(Transaction transaction) {
        TransactionResult transactionResult = new TransactionResult();
        transactionResult.setId(transaction.getId());
        transactionResult.setRequestId(transaction.getRequestId());
        transactionResult.setPaymentMode(transaction.getPaymentMode());
        transactionResult.setPayerIdentifierType(transaction.getPayerIdentifierType());
        transactionResult.setPayerIdentifier(transaction.getPayerIdentifier());
        transactionResult.setAmount(transaction.getAmount());
        transactionResult.setCurrency(transaction.getCurrency());
        transactionResult.setNote(transaction.getNote());
        transactionResult.setPayeeIdentifierType(transaction.getPayeeIdentifierType());
        if (transaction.getAccountNumber() != null) {
            transactionResult.setPayeeIdentifier(transaction.getAccountNumber());
        } else {
            transactionResult.setPayeeIdentifier(transaction.getPayeeIdentifier());
        }
        transactionResult.setProgramShortCode(transaction.getProgramShortCode());
        transactionResult.setCycle(transactionResult.getCycle());
        return transactionResult;
    }

    public static GSMATransaction convertTxnToGSMA(Transaction transaction) {
        GSMATransaction gsmaTransaction = new GSMATransaction();
        gsmaTransaction.setAmount(transaction.getAmount());
        gsmaTransaction.setCurrency(transaction.getCurrency());
        GsmaParty payer = new GsmaParty();
        //logger.info("Payer {} {}", transaction.getPayerIdentifier(),payer[0].);
        payer.setKey("msisdn");
        payer.setValue(transaction.getPayerIdentifier());
        GsmaParty payee = new GsmaParty();
        payee.setKey("msisdn");
        payee.setValue(transaction.getPayeeIdentifier());
        GsmaParty[] debitParty = new GsmaParty[1];
        GsmaParty[] creditParty = new GsmaParty[1];
        debitParty[0] = payer;
        creditParty[0] = payee;
        gsmaTransaction.setDebitParty(debitParty);
        gsmaTransaction.setCreditParty(creditParty);
        gsmaTransaction.setRequestingOrganisationTransactionReference("string");
        gsmaTransaction.setSubType("string");
        gsmaTransaction.setDescriptionText("string");
        Fee fees = new Fee();
        fees.setFeeType(transaction.getAmount());
        fees.setFeeCurrency(transaction.getCurrency());
        fees.setFeeType("string");
        Fee[] fee = new Fee[1];
        fee[0] = fees;
        gsmaTransaction.setFees(fee);
        gsmaTransaction.setGeoCode("37.423825,-122.082900");
        InternationalTransferInformation internationalTransferInformation =
                new InternationalTransferInformation();
        internationalTransferInformation.setQuotationReference("string");
        internationalTransferInformation.setQuoteId("string");
        internationalTransferInformation.setDeliveryMethod("directtoaccount");
        internationalTransferInformation.setOriginCountry("USA");
        internationalTransferInformation.setReceivingCountry("USA");
        internationalTransferInformation.setRelationshipSender("string");
        internationalTransferInformation.setRemittancePurpose("string");
        gsmaTransaction.setInternationalTransferInformation(internationalTransferInformation);
        gsmaTransaction.setOneTimeCode("string");
        IdDocument idDocument = new IdDocument();
        idDocument.setIdType("passport");
        idDocument.setIdNumber("string");
        idDocument.setIssuerCountry("USA");
        idDocument.setExpiryDate("2022-09-28T12:51:19.260+00:00");
        idDocument.setIssueDate("2022-09-28T12:51:19.260+00:00");
        idDocument.setIssuer("string");
        idDocument.setIssuerPlace("string");
        IdDocument[] idDocuments = new IdDocument[1];
        idDocuments[0] = idDocument;
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setAddressLine1("string");
        postalAddress.setAddressLine2("string");
        postalAddress.setAddressLine3("string");
        postalAddress.setCity("string");
        postalAddress.setCountry("USA");
        postalAddress.setPostalCode("string");
        postalAddress.setStateProvince("string");
        SubjectName subjectName = new SubjectName();
        subjectName.setFirstName("string");
        subjectName.setLastName("string");
        subjectName.setMiddleName("string");
        subjectName.setTitle("string");
        subjectName.setNativeName("string");
        Kyc recieverKyc = new Kyc();
        recieverKyc.setBirthCountry("USA");
        recieverKyc.setDateOfBirth("2000-11-20");
        recieverKyc.setContactPhone("string");
        recieverKyc.setEmailAddress("string");
        recieverKyc.setEmployerName("string");
        recieverKyc.setGender('m');
        recieverKyc.setIdDocument(idDocuments);
        recieverKyc.setNationality("USA");
        recieverKyc.setOccupation("string");
        recieverKyc.setPostalAddress(postalAddress);
        recieverKyc.setSubjectName(subjectName);
        Kyc senderKyc = new Kyc();
        senderKyc.setBirthCountry("USA");
        senderKyc.setDateOfBirth("2000-11-20");
        senderKyc.setContactPhone("string");
        senderKyc.setEmailAddress("string");
        senderKyc.setEmployerName("string");
        senderKyc.setGender('m');
        senderKyc.setIdDocument(idDocuments);
        senderKyc.setNationality("USA");
        senderKyc.setOccupation("string");
        senderKyc.setPostalAddress(postalAddress);
        senderKyc.setSubjectName(subjectName);
        gsmaTransaction.setReceiverKyc(recieverKyc);
        gsmaTransaction.setSenderKyc(senderKyc);
        gsmaTransaction.setServicingIdentity("string");
        gsmaTransaction.setRequestDate("2022-09-28T12:51:19.260+00:00");


        return gsmaTransaction;
    }

    public static TransactionChannelRequestDTO convertTxnToInboundTransferPayload(Transaction transaction) {
        TransactionChannelRequestDTO requestDTO = new TransactionChannelRequestDTO();

        requestDTO.setAmount(new MoneyData(){{
            setCurrency(transaction.getCurrency());
            setAmount(transaction.getAmount());
        }});

        IdentifierType identifierType;
        try {
            identifierType = IdentifierType.valueOf(transaction.getPaymentMode().toUpperCase());
        } catch (Exception e) {
            identifierType = IdentifierType.MSISDN;
        }

        // PAYER SETUP
        Party payerParty = new Party(new PartyIdInfo(identifierType, transaction.getPayerIdentifier()));

        // PAYEE SETUP
        Party payeeParty = new Party(new PartyIdInfo(identifierType, transaction.getPayeeIdentifier()));

        requestDTO.setPayer(payerParty);
        requestDTO.setPayee(payeeParty);
        requestDTO.setNote(transaction.getNote());

        return requestDTO;
    }

}
