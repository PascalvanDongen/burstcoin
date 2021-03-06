package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;

import brs.Account;
import brs.Block;
import brs.Burst;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountBlockIds extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAccountBlockIds(ParameterService parameterService) {
    super(new APITag[] {APITag.ACCOUNTS}, ACCOUNT_PARAMETER, TIMESTAMP_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account account = parameterService.getAccount(req);

    int timestamp = ParameterParser.getTimestamp(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray blockIds = new JSONArray();
    try (BurstIterator<? extends Block> iterator = Burst.getBlockchain().getBlocks(account, timestamp, firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Block block = iterator.next();
        blockIds.add(block.getStringId());
      }
    }

    JSONObject response = new JSONObject();
    response.put("blockIds", blockIds);

    return response;
  }

}
