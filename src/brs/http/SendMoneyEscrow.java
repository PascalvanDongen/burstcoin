package brs.http;

import static brs.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static brs.http.common.Parameters.ESCROW_DEADLINE_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static brs.http.common.Parameters.REQUIRED_SIGNERS_PARAMETER;
import static brs.http.common.Parameters.SIGNERS_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.*;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

public final class SendMoneyEscrow extends CreateTransaction {
	
  private final ParameterService parameterService;
	
  SendMoneyEscrow(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain) {
    super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, RECIPIENT_PARAMETER, AMOUNT_NQT_PARAMETER, ESCROW_DEADLINE_PARAMETER);
    this.parameterService = parameterService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account sender = parameterService.getSenderAccount(req);
    Long recipient = ParameterParser.getRecipientId(req);
    Long amountNQT = parameterService.getAmountNQT(req);
    String signerString = Convert.emptyToNull(req.getParameter(SIGNERS_PARAMETER));
		
    Long requiredSigners;
    try {
      requiredSigners = Convert.parseLong(req.getParameter(REQUIRED_SIGNERS_PARAMETER));
      if(requiredSigners < 1 || requiredSigners > 10) {
        JSONObject response = new JSONObject();
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid number of requiredSigners");
        return response;
      }
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid requiredSigners parameter");
      return response;
    }
		
    if(signerString == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Signers not specified");
      return response;
    }
		
    String signersArray[] = signerString.split(";", 10);
		
    if(signersArray.length < 1 || signersArray.length > 10 || signersArray.length < requiredSigners) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid number of signers");
      return response;
    }
		
    ArrayList<Long> signers = new ArrayList<>();
		
    try {
      for(String signer : signersArray) {
        Long id = Convert.parseAccountId(signer);
        if(id == null) {
          throw new Exception("");
        }
				
        signers.add(id);
      }
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid signers parameter");
      return response;
    }
		
    Long totalAmountNQT = Convert.safeAdd(amountNQT, signers.size() * Constants.ONE_NXT);
    if(sender.getBalanceNQT() < totalAmountNQT) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 6);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Insufficient funds");
      return response;
    }
		
    Long deadline;
    try {
      deadline = Convert.parseLong(req.getParameter("escrowDeadline"));
      if(deadline < 1 || deadline > 7776000) {
        JSONObject response = new JSONObject();
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Escrow deadline must be 1 - 7776000");
        return response;
      }
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid escrowDeadline parameter");
      return response;
    }
		
    Escrow.DecisionType deadlineAction = Escrow.stringToDecision(req.getParameter("deadlineAction"));
    if(deadlineAction == null || deadlineAction == Escrow.DecisionType.UNDECIDED) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid deadlineAction parameter");
      return response;
    }
		
    Attachment.AdvancedPaymentEscrowCreation attachment = new Attachment.AdvancedPaymentEscrowCreation(amountNQT, deadline.intValue(), deadlineAction, requiredSigners.intValue(), signers);
		
    return createTransaction(req, sender, recipient, 0, attachment);
  }
}
