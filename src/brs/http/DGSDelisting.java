package brs.http;

import static brs.http.JSONResponses.UNKNOWN_GOODS;
import static brs.http.common.Parameters.GOODS_PARAMETER;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.DigitalGoodsStore;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class DGSDelisting extends CreateTransaction {

  private final ParameterService parameterService;

  public DGSDelisting(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain) {
    super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, GOODS_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account account = parameterService.getSenderAccount(req);
    DigitalGoodsStore.Goods goods = parameterService.getGoods(req);
    if (goods.isDelisted() || goods.getSellerId() != account.getId()) {
      return UNKNOWN_GOODS;
    }
    Attachment attachment = new Attachment.DigitalGoodsDelisting(goods.getId());
    return createTransaction(req, account, attachment);
  }

}
