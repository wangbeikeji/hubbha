package com.wangbei.service;

import com.wangbei.dao.AccountDao;
import com.wangbei.dao.TradeDao;
import com.wangbei.entity.Account;
import com.wangbei.entity.Trade;
import com.wangbei.exception.ServiceException;
import com.wangbei.util.RandomUtil;
import com.wangbei.util.enums.PaymentTypeEnum;
import com.wangbei.util.enums.TradeStatusEnum;
import com.wangbei.util.enums.TradeTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Created by yuyidi on 2017/7/17.
 * @desc
 */
@Service
public class TradeService {

    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    @Autowired
    private TradeDao tradeDao;
    @Autowired
    private AccountDao accountDao;

    /**
     * 生成交易流水号
     *
     * @return
     */
    public static String generateTradeNo() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(sdf.format(new Date()));
        strBuilder.append(RandomUtil.generateRandomCode(6));
        return strBuilder.toString();
    }

    @Transactional
    public Trade trade(Integer user, TradeTypeEnum type, Integer meritValue) {
        Account account = accountDao.findByUser(user);
        if (account == null) {
            throw new ServiceException(ServiceException.USER_ACCOUNT_NOT_FOUND_EXCEPTION);
        }
        //账户信息正常，则构造交易记录数据
        Trade trade = new Trade();
        trade.setUserId(user);
        trade.setType(type);
        trade.setMeritValue(meritValue);
        trade.setTradeNo(generateTradeNo());
        trade.setStatus(TradeStatusEnum.COMPLETED);
        boolean isIncrease = false;
        //判断交易类型是增加功德还是减功德  除充值|放生|功德箱|签到为增加功德，其他为减功德
        if (type == TradeTypeEnum.CHECKIN || type == TradeTypeEnum.FREELIFE || type == TradeTypeEnum.MERIT || type ==
                TradeTypeEnum.CHARGE) {
            //增加功德，并设置交易状态为待支付
            isIncrease = true;
            trade.setStatus(TradeStatusEnum.PENDING);
        }
        Integer finalMeritValue = isIncrease ? (account.getMeritValue() + meritValue)
                : (account.getMeritValue() - meritValue);
        if (finalMeritValue < 0) {
            //用户功德值不够，则抛出异常
            throw new ServiceException(ServiceException.MERIT_POOL);
        }
        account.setMeritValue(finalMeritValue);
        Account resultAccount = accountDao.update(account);
        if (resultAccount != null) {
            // step 4 : 更新账户功德数（充值、放生、功德箱不更新，等确认充值成功再更新）
            Trade result = tradeDao.create(trade);
            return result;
        }
        throw new ServiceException(ServiceException.TRADE_ERROR_EXCEPTION);
    }

    /**
     * 创建支付交易
     *
     * @param user        用户id
     * @param type        交易类型
     * @param paymentType 支付类型
     * @param meritValue  功德数
     * @param totalFee    总金额
     * @return 交易
     */
    @Transactional
    public Trade paymentTrade(String tradeNo, String thirdTradeNo, Integer user, TradeTypeEnum type,
                              PaymentTypeEnum paymentType, Integer meritValue, Double totalFee) {
        if (!(type == TradeTypeEnum.CHARGE || type == TradeTypeEnum.FREELIFE || type == TradeTypeEnum.MERIT)) {
            throw new ServiceException(ServiceException.CHARGETYPE_NOTMATCH_EXCEPTION);
        }
        // 保存交易记录
        Trade trade = new Trade();
        trade.setUserId(user);
        trade.setType(type);
        trade.setMeritValue(meritValue);
        trade.setTradeNo(tradeNo);
        trade.setStatus(TradeStatusEnum.PENDING);
//        trade.setTotalFee(totalFee);
//        trade.setPaymentType(paymentType);
//        trade.setThirdTradeNo(thirdTradeNo);
        return tradeDao.create(trade);
    }

    /**
     * 完成支付交易
     *
     * @param tradeNo 交易流水号
     * @return 交易
     */
    @Transactional
    public Trade completePaymentTrade(String tradeNo) {
        Trade trade = tradeDao.retrieveByTradeNo(tradeNo);
        if (trade == null) {
            throw new ServiceException(ServiceException.TRADENO_NOTEXIST_EXCEPTION);
        }
        if (!(trade.getStatus() == TradeStatusEnum.COMPLETED)) {
            trade.setStatus(TradeStatusEnum.COMPLETED);
            Account account = accountDao.findByUser(trade.getUserId());
            if (account == null) {
                account = new Account();
                account.setUserId(trade.getUserId());
                account.setMeritValue(trade.getMeritValue());
                accountDao.create(account);
            } else {
                account.setMeritValue(account.getMeritValue() + trade.getMeritValue());
                accountDao.update(account);
            }
        }
        return trade;
    }

    @Transactional
    public Trade wxTrade(Integer user, TradeTypeEnum type, Integer meritValue) {
        // step 1 : 获取用户账号，不存在则创建用户账号
        Account account = accountDao.findByUser(user);
        if (account == null) {
            account = new Account();
            account.setUserId(user);
            account.setMeritValue(0);
            accountDao.create(account);
        }
        // step 2 : 判断增加功德还是扣除功德
        boolean isIncrease = false;
        if (type == TradeTypeEnum.CHARGE || type == TradeTypeEnum.CHECKIN || type == TradeTypeEnum.FREELIFE
                || type == TradeTypeEnum.MERIT) {
            isIncrease = true;
        }
        // step 3 : 得到最终功德数
        Integer finalMeritValue = isIncrease ? (account.getMeritValue() + meritValue)
                : (account.getMeritValue() - meritValue);
        if (finalMeritValue < 0) {
            throw new ServiceException(ServiceException.MERIT_POOL);
        }
        // step 4 : 更新账户功德数（充值、放生、功德箱不更新，等确认充值成功再更新）
        if (type != TradeTypeEnum.CHARGE && type != TradeTypeEnum.FREELIFE && type != TradeTypeEnum.MERIT) {
            account.setMeritValue(finalMeritValue);
            accountDao.update(account);
        }
        // step 5 : 保存交易记录（交易状态默认为已完成，充值、放生、功德箱为待完成）
        Trade trade = new Trade();
        trade.setUserId(user);
        trade.setType(type);
        trade.setMeritValue(meritValue);
        trade.setTradeNo(generateTradeNo());
        if (type != TradeTypeEnum.CHARGE && type != TradeTypeEnum.FREELIFE && type != TradeTypeEnum.MERIT) {
            trade.setStatus(TradeStatusEnum.COMPLETED);
        } else {
            trade.setStatus(TradeStatusEnum.PENDING);
        }
        return tradeDao.create(trade);
    }

    public Trade getTradeInfo(Integer tradeId) {
        return tradeDao.retrieveById(tradeId);
    }

}
