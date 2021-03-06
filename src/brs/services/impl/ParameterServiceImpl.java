package brs.services.impl;

import static brs.http.JSONResponses.HEIGHT_NOT_AVAILABLE;
import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.JSONResponses.INCORRECT_ALIAS;
import static brs.http.JSONResponses.INCORRECT_AMOUNT;
import static brs.http.JSONResponses.INCORRECT_ASSET;
import static brs.http.JSONResponses.INCORRECT_ENCRYPTED_MESSAGE;
import static brs.http.JSONResponses.INCORRECT_GOODS;
import static brs.http.JSONResponses.INCORRECT_HEIGHT;
import static brs.http.JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS;
import static brs.http.JSONResponses.INCORRECT_PLAIN_MESSAGE;
import static brs.http.JSONResponses.INCORRECT_PUBLIC_KEY;
import static brs.http.JSONResponses.INCORRECT_RECIPIENT;
import static brs.http.JSONResponses.MISSING_ACCOUNT;
import static brs.http.JSONResponses.MISSING_ALIAS_OR_ALIAS_NAME;
import static brs.http.JSONResponses.MISSING_AMOUNT;
import static brs.http.JSONResponses.MISSING_ASSET;
import static brs.http.JSONResponses.MISSING_GOODS;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE_OR_PUBLIC_KEY;
import static brs.http.JSONResponses.UNKNOWN_ACCOUNT;
import static brs.http.JSONResponses.UNKNOWN_ALIAS;
import static brs.http.JSONResponses.UNKNOWN_ASSET;
import static brs.http.JSONResponses.UNKNOWN_GOODS;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;
import static brs.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.ENCRYPTED_MESSAGE_DATA_PARAMETER;
import static brs.http.common.Parameters.ENCRYPTED_MESSAGE_NONCE_PARAMETER;
import static brs.http.common.Parameters.ENCRYPT_TO_SELF_MESSAGE_DATA;
import static brs.http.common.Parameters.ENCRYPT_TO_SELF_MESSAGE_NONCE;
import static brs.http.common.Parameters.GOODS_PARAMETER;
import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER;
import static brs.http.common.Parameters.NUMBER_OF_CONFIRMATIONS_PARAMETER;
import static brs.http.common.Parameters.PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;

import brs.Account;
import brs.Alias;
import brs.Asset;
import brs.Burst;
import brs.BurstException;
import brs.Constants;
import brs.DigitalGoodsStore;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.http.ParameterException;
import brs.http.common.Parameters;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetService;
import brs.services.ParameterService;
import brs.util.Convert;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ParameterServiceImpl implements ParameterService {

  private final AccountService accountService;
  private final AliasService aliasService;
  private final AssetService assetService;

  public ParameterServiceImpl(AccountService accountService, AliasService aliasService, AssetService assetService) {
    this.accountService = accountService;
    this.aliasService = aliasService;
    this.assetService = assetService;
  }

  @Override
  public Account getAccount(HttpServletRequest req) throws BurstException {
    String accountValue = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
    if (accountValue == null) {
      throw new ParameterException(MISSING_ACCOUNT);
    }
    try {
      Account account = accountService.getAccount(Convert.parseAccountId(accountValue));
      if (account == null) {
        throw new ParameterException(UNKNOWN_ACCOUNT);
      }
      return account;
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_ACCOUNT);
    }
  }

  @Override
  public List<Account> getAccounts(HttpServletRequest req) throws ParameterException {
    String[] accountValues = req.getParameterValues(ACCOUNT_PARAMETER);
    if (accountValues == null || accountValues.length == 0) {
      throw new ParameterException(MISSING_ACCOUNT);
    }
    List<Account> result = new ArrayList<>();
    for (String accountValue : accountValues) {
      if (accountValue == null || accountValue.isEmpty()) {
        continue;
      }
      try {
        Account account = accountService.getAccount(Convert.parseAccountId(accountValue));
        if (account == null) {
          throw new ParameterException(UNKNOWN_ACCOUNT);
        }
        result.add(account);
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ACCOUNT);
      }
    }
    return result;
  }

  @Override
  public Account getSenderAccount(HttpServletRequest req) throws ParameterException {
    Account account;
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    String publicKeyString = Convert.emptyToNull(req.getParameter(PUBLIC_KEY_PARAMETER));
    if (secretPhrase != null) {
      account = accountService.getAccount(Crypto.getPublicKey(secretPhrase));
    } else if (publicKeyString != null) {
      try {
        account = accountService.getAccount(Convert.parseHexString(publicKeyString));
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_PUBLIC_KEY);
      }
    } else {
      throw new ParameterException(MISSING_SECRET_PHRASE_OR_PUBLIC_KEY);
    }
    if (account == null) {
      throw new ParameterException(UNKNOWN_ACCOUNT);
    }
    return account;
  }

  @Override
  public Alias getAlias(HttpServletRequest req) throws ParameterException {
    long aliasId;
    try {
      aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(ALIAS_PARAMETER)));
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_ALIAS);
    }
    String aliasName = Convert.emptyToNull(req.getParameter(ALIAS_NAME_PARAMETER));
    Alias alias;
    if (aliasId != 0) {
      alias = aliasService.getAlias(aliasId);
    } else if (aliasName != null) {
      alias = aliasService.getAlias(aliasName);
    } else {
      throw new ParameterException(MISSING_ALIAS_OR_ALIAS_NAME);
    }
    if (alias == null) {
      throw new ParameterException(UNKNOWN_ALIAS);
    }
    return alias;
  }

  @Override
  public long getAmountNQT(HttpServletRequest req) throws ParameterException {
    String amountValueNQT = Convert.emptyToNull(req.getParameter(AMOUNT_NQT_PARAMETER));
    if (amountValueNQT == null) {
      throw new ParameterException(MISSING_AMOUNT);
    }
    long amountNQT;
    try {
      amountNQT = Long.parseLong(amountValueNQT);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_AMOUNT);
    }
    if (amountNQT <= 0 || amountNQT >= Constants.MAX_BALANCE_NQT) {
      throw new ParameterException(INCORRECT_AMOUNT);
    }
    return amountNQT;
  }

  @Override
  public Asset getAsset(HttpServletRequest req) throws ParameterException {
    String assetValue = Convert.emptyToNull(req.getParameter(ASSET_PARAMETER));
    if (assetValue == null) {
      throw new ParameterException(MISSING_ASSET);
    }
    Asset asset;
    try {
      long assetId = Convert.parseUnsignedLong(assetValue);
      asset = assetService.getAsset(assetId);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_ASSET);
    }
    if (asset == null) {
      throw new ParameterException(UNKNOWN_ASSET);
    }
    return asset;
  }

  @Override
  public DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException {
    String goodsValue = Convert.emptyToNull(req.getParameter(GOODS_PARAMETER));
    if (goodsValue == null) {
      throw new ParameterException(MISSING_GOODS);
    }
    DigitalGoodsStore.Goods goods;
    try {
      long goodsId = Convert.parseUnsignedLong(goodsValue);
      goods = DigitalGoodsStore.getGoods(goodsId);
      if (goods == null) {
        throw new ParameterException(UNKNOWN_GOODS);
      }
      return goods;
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_GOODS);
    }
  }

  @Override
  public EncryptedData getEncryptedMessage(HttpServletRequest req, Account recipientAccount) throws ParameterException {
    String data = Convert.emptyToNull(req.getParameter(ENCRYPTED_MESSAGE_DATA_PARAMETER));
    String nonce = Convert.emptyToNull(req.getParameter(ENCRYPTED_MESSAGE_NONCE_PARAMETER));
    if (data != null && nonce != null) {
      try {
        return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
      }
    }
    String plainMessage = Convert.emptyToNull(req.getParameter(MESSAGE_TO_ENCRYPT_PARAMETER));
    if (plainMessage == null) {
      return null;
    }
    if (recipientAccount == null) {
      throw new ParameterException(INCORRECT_RECIPIENT);
    }
    String secretPhrase = getSecretPhrase(req);
    boolean isText = !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER));
    try {
      byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
      return recipientAccount.encryptTo(plainMessageBytes, secretPhrase);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
    }
  }

  @Override
  public EncryptedData getEncryptToSelfMessage(HttpServletRequest req) throws ParameterException {
    String data = Convert.emptyToNull(req.getParameter(ENCRYPT_TO_SELF_MESSAGE_DATA));
    String nonce = Convert.emptyToNull(req.getParameter(ENCRYPT_TO_SELF_MESSAGE_NONCE));
    if (data != null && nonce != null) {
      try {
        return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ENCRYPTED_MESSAGE);
      }
    }
    String plainMessage = Convert.emptyToNull(req.getParameter(MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER));
    if (plainMessage == null) {
      return null;
    }
    String secretPhrase = getSecretPhrase(req);
    Account senderAccount = accountService.getAccount(Crypto.getPublicKey(secretPhrase));
    boolean isText = !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER));
    try {
      byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
      return senderAccount.encryptTo(plainMessageBytes, secretPhrase);
    } catch (RuntimeException e) {
      throw new ParameterException(INCORRECT_PLAIN_MESSAGE);
    }
  }

  @Override
  public String getSecretPhrase(HttpServletRequest req) throws ParameterException {
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    if (secretPhrase == null) {
      throw new ParameterException(MISSING_SECRET_PHRASE);
    }
    return secretPhrase;
  }

  @Override
  public int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException {
    String numberOfConfirmationsValue = Convert.emptyToNull(req.getParameter(NUMBER_OF_CONFIRMATIONS_PARAMETER));
    if (numberOfConfirmationsValue != null) {
      try {
        int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
        if (numberOfConfirmations <= Burst.getBlockchain().getHeight()) {
          return numberOfConfirmations;
        }
        throw new ParameterException(INCORRECT_NUMBER_OF_CONFIRMATIONS);
      } catch (NumberFormatException e) {
        throw new ParameterException(INCORRECT_NUMBER_OF_CONFIRMATIONS);
      }
    }
    return 0;
  }

  @Override
  public int getHeight(HttpServletRequest req) throws ParameterException {
    String heightValue = Convert.emptyToNull(req.getParameter(HEIGHT_PARAMETER));
    if (heightValue != null) {
      try {
        int height = Integer.parseInt(heightValue);
        if (height < 0 || height > Burst.getBlockchain().getHeight()) {
          throw new ParameterException(INCORRECT_HEIGHT);
        }
        if (height < Burst.getBlockchainProcessor().getMinRollbackHeight()) {
          throw new ParameterException(HEIGHT_NOT_AVAILABLE);
        }
        return height;
      } catch (NumberFormatException e) {
        throw new ParameterException(INCORRECT_HEIGHT);
      }
    }
    return -1;
  }

}
