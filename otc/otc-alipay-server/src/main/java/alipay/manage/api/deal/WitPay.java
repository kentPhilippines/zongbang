package alipay.manage.api.deal;

import alipay.config.redis.RedisLockUtil;
import alipay.config.redis.RedisUtil;
import alipay.manage.api.AccountApiService;
import alipay.manage.api.DealAppApi;
import alipay.manage.api.VendorRequestApi;
import alipay.manage.api.config.FactoryForStrategy;
import alipay.manage.api.config.PayOrderService;
import alipay.manage.bean.ChannelFee;
import alipay.manage.bean.UserFund;
import alipay.manage.bean.UserInfo;
import alipay.manage.bean.UserRate;
import alipay.manage.bean.util.WithdrawalBean;
import alipay.manage.mapper.ChannelFeeMapper;
import alipay.manage.service.ExceptionOrderService;
import alipay.manage.service.UserInfoService;
import alipay.manage.service.WithdrawService;
import alipay.manage.util.BankTypeUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import otc.api.alipay.Common;
import otc.bean.dealpay.Withdraw;
import otc.exception.order.OrderException;
import otc.result.Result;
import otc.util.MapUtil;
import otc.util.number.Number;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class WitPay extends PayOrderService {
    @Autowired
    VendorRequestApi vendorRequestApi;
    Logger log = LoggerFactory.getLogger(DealAppApi.class);
    @Autowired
    private FactoryForStrategy factoryForStrategy;
    @Autowired
    private AccountApiService accountApiServiceImpl;
    @Autowired
    private WithdrawService withdrawServiceImpl;
    @Resource
    private ChannelFeeMapper channelFeeDao;
    @Autowired
    private ExceptionOrderService exceptionOrderServiceImpl;
    @Autowired
    private UserInfoService userInfoServiceImpl;
    @Autowired
    private RedisLockUtil redisLockUtil;
    @Autowired
    private RedisUtil redis;

    static final String COMMENT = "???????????????";

    /**
     * ????????????
     *
     * @param request
     * @param amount  true   ????????????   false  ???????????????
     * @return
     */
    public Result wit(HttpServletRequest request, boolean amount) {
        String userId = request.getParameter("userId");
        if (ObjectUtil.isNull(userId)) {
            return Result.buildFailMessage("?????????????????????????????????????????????[application/x-www-form-urlencoded]??????????????????");
        }
        redisLockUtil.redisLock(RedisLockUtil.AMOUNT_USER_KEY + userId);
        String manage = request.getParameter("manage");
        boolean flag = false;
        if (StrUtil.isNotBlank(manage)) {
            flag = true;
        }
        Result withdrawal = vendorRequestApi.withdrawal(request, flag);
        if (!withdrawal.isSuccess()) {
            return withdrawal;
        }
        Object result = withdrawal.getResult();
        WithdrawalBean wit = MapUtil.mapToBean((Map<String, Object>) result, WithdrawalBean.class);
        wit.setIp(VendorRequestApi.getIpAddress(request, wit.getAppid()));
        UserRate userRate = accountApiServiceImpl.findUserRateWitByUserId(wit.getAppid(), wit.getAmount());
        if (amount) {
            UserFund userFund = userInfoServiceImpl.fundUserFundAccounrBalace(userId);
            BigDecimal accountBalance = userFund.getAccountBalance();
            BigDecimal quota = userFund.getQuota();
            accountBalance = accountBalance.add(quota);
            if (accountBalance.compareTo(new BigDecimal(wit.getAmount()).add(userRate.getFee())) == -1) {
                exceptionOrderServiceImpl.addWitEx(userId, wit.getAmount(), "????????????????????????????????????????????????" + "???????????????????????????????????????????????????????????????????????????????????????", HttpUtil.getClientIP(request), wit.getApporderid());
                return Result.buildFailMessage("????????????????????????");
            }
        }
        UserInfo userInfoByUserId = userInfoServiceImpl.findUserInfoByUserId(userRate.getChannelId());
        if (Common.Order.DAPY_OFF.equals(userInfoByUserId.getRemitOrderState())) {
            log.info("??????????????????");
            exceptionOrderServiceImpl.addWitOrder(wit, "??????????????????????????????????????????????????????????????????????????????????????????", HttpUtil.getClientIP(request));
            return Result.buildFailMessage("?????????????????????????????????");
        }
        ChannelFee channelFee = channelFeeDao.findImpl(userRate.getChannelId(), userRate.getPayTypr());//????????????
        if (ObjectUtil.isNull(channelFee)) {
            log.info("????????????????????????????????????????????????");
            exceptionOrderServiceImpl.addWitOrder(wit, "??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????", HttpUtil.getClientIP(request));
            return Result.buildFailMessage("??????????????????????????????????????????");
        }
        String bankcode = BankTypeUtil.getBank(wit.getBankcode());
        if (StrUtil.isBlank(bankcode)) {
            log.info("????????????????????????????????????????????????" + wit.getAppid() + "??????????????????:" + wit.getApporderid() + "???");
            exceptionOrderServiceImpl.addWitOrder(wit, "?????????????????????????????????????????? ??????code????????????????????????????????????????????????????????????code?????????????????????code?????????" + bankcode, HttpUtil.getClientIP(request));
            return Result.buildFailMessage("??????????????????????????? ??????code?????????");
        }
        Object o = redis.get(wit.getApporderid() + userRate.getUserId());
        if (null != o) {
            if (o.toString().equals(wit.getApporderid() + userRate.getUserId())) {
                log.info("?????????????????????????????????" + wit.getApporderid() + "???");
                exceptionOrderServiceImpl.addWitOrder(wit, "????????????????????????????????????????????????????????????????????????????????????????????????????????????", HttpUtil.getClientIP(request));
                return Result.buildFailMessage("?????????????????????");
            }
        }
        Result deal = null;
        try {
            Withdraw bean = createWit(wit, userRate, flag, channelFee);
            if (ObjectUtil.isNull(bean)) {
                return Result.buildFailMessage("????????????????????????");
            }
            deal = Result.buildSuccessMessage("???????????????");
          /*  UserInfo userInfo = accountApiServiceImpl.findautoWit(wit.getAppid());
            //????????????
            if (1 == userInfo.getAutoWit()) {
                //????????????
                deal = super.withdraw(bean);
                if (deal.isSuccess()) {
                    log.info("??????????????????" + channelFee.toString() + "???");
                    deal = factoryForStrategy.getStrategy(channelFee.getImpl()).withdraw(bean);  //   ?????????????????????????????????????????????????????????
                } else {
                    withdrawServiceImpl.updateWitError(bean.getOrderId());
                    return Result.buildFailMessage("?????????????????????????????????????????????????????????");
                }
            } else {
                //????????????
                deal = super.withdraw(bean);
            }
*/
        } catch (Exception e) {
            log.error("[??????????????????????????????]", e);
            //		super.withdrawEr(bean, "??????????????????????????????????????????", HttpUtil.getClientIP(request));
            exceptionOrderServiceImpl.addWitOrder(wit, "??????????????????????????????????????????????????????????????????????????????????????????????????????" + e.getMessage(), HttpUtil.getClientIP(request));
            log.info("???????????????????????????????????????????????????");
            //    withdrawServiceImpl.updateWitError(bean.getOrderId());
            redisLockUtil.unLock(RedisLockUtil.AMOUNT_USER_KEY + userId);
            return Result.buildFailMessage("???????????????????????????");
        } finally {
            //    bean = null;
            o = null;
            bankcode = null;
            channelFee = null;
            wit = null;
            userRate = null;
            result = null;
            redisLockUtil.unLock(RedisLockUtil.AMOUNT_USER_KEY + userId);
        }
        return deal;
    }

    private Withdraw createWit(WithdrawalBean wit, UserRate userRate, Boolean fla, ChannelFee channelFee) {
        log.info("????????????????????? ?????????????????????" + wit.toString() + "???");
        String type = "";
        String bankName = "";
        if (fla) {
            type = Common.Order.Wit.WIT_TYPE_API;
        } else {
            type = Common.Order.Wit.WIT_TYPE_MANAGE;
        }
        Withdraw witb = new Withdraw();
        witb.setUserId(wit.getAppid());
        witb.setAmount(new BigDecimal(wit.getAmount()));
        witb.setFee(userRate.getFee());
        witb.setActualAmount(new BigDecimal(wit.getAmount()));
        witb.setMobile(wit.getMobile());
        witb.setBankNo(wit.getAcctno());
        witb.setAccname(wit.getAcctname());
        bankName = wit.getBankName();
        if (StrUtil.isBlank(bankName)) {
            bankName = BankTypeUtil.getBankName(wit.getBankcode());
        }
        witb.setBankName(bankName);
        witb.setWithdrawType(Common.Order.Wit.WIT_ACC);
        witb.setOrderId(Number.getWitOrder());
        witb.setOrderStatus(Common.Order.DealOrderApp.ORDER_STATUS_DISPOSE.toString());
        witb.setNotify(wit.getNotifyurl());
        witb.setRetain2(wit.getIp());//??????ip
        witb.setAppOrderId(wit.getApporderid());
        witb.setRetain1(type);
        witb.setWitType(userRate.getPayTypr());//????????????
        witb.setApply(wit.getApply());
        witb.setBankcode(wit.getBankcode());
        witb.setWitChannel(channelFee.getChannelId());
        witb.setPushOrder(0);
        UserFund userFund = userInfoServiceImpl.findCurrency(wit.getAppid());//????????????
        //    witb.setStatus(2);//???????????????   ???????????????????????????
        witb.setCurrency(userFund.getCurrency());
        //  witb.setComment(Common.Order.DealOrderApp.COMMENT_WIT.toString());
        boolean flag = false;
        try {
            flag = withdrawServiceImpl.addOrder(witb);
        } catch (Exception e) {
            log.info("?????????????????????????????????" + wit.getApporderid() + "???");
            exceptionOrderServiceImpl.addWitOrder(wit, "????????????????????????????????????????????????????????????????????????????????????????????????????????????", wit.getIp());
            //	throw new OrderException("???????????????", null);
        }
        if (flag) {
            redis.set(witb.getAppOrderId() + witb.getUserId(), witb.getAppOrderId() + witb.getUserId(), 60 * 60);
            return witb;
        }
        return null;
    }

    /**
     * ????????????????????????????????????
     */


    public Result witAutoPush(Withdraw order) {
        Result deal = Result.buildFail();
        try {
            UserInfo userInfo = accountApiServiceImpl.findautoWit(order.getUserId());
            Future<Result> result = ThreadUtil.execAsync(() -> {
                //????????????
                BigDecimal witAmount = order.getAmount();
                UserFund userFund = userInfoServiceImpl.fundUserFundAccounrBalace(order.getUserId());
                BigDecimal accountBalance = userFund.getAccountBalance();
                BigDecimal quota = userFund.getQuota();
                accountBalance = accountBalance.add(quota);
                if (accountBalance.compareTo(witAmount.add(order.getFee())) == -1) {
                    exceptionOrderServiceImpl.addWitEx(order.getUserId(), order.getAmount().toString(), "????????????????????????????????????????????????" + "???????????????????????????????????????????????????????????????????????????????????????", order.getRetain2(), order.getAppOrderId());
                    withdrawErByAmount(order.getOrderId(), "????????????????????????");
                    return Result.buildFailMessage("????????????????????????");
                }
                return Result.buildSuccessResult();
            });

            try {
                Result result1 = result.get();
                if (!result1.isSuccess()) {
                    return result1;
                }
            } catch (Exception ex) {
                push("????????????????????????????????????????????????????????????????????????" + order.getOrderId() + "??????????????????????????????" + printStackTrace(ex.getStackTrace()));
                return Result.buildFailMessage("???????????????????????????????????????????????????");
            }
            if (1 == userInfo.getAutoWit()) {
                deal = super.withdraw(order);
                if (deal.isSuccess()) {
                    ThreadUtil.execute(() -> {
                        ChannelFee channelFee = channelFeeDao.findImpl(order.getWitChannel(), order.getWitType());//????????????
                        Result withdraw = Result.buildFail();
                        try {
                            withdraw = factoryForStrategy.getStrategy(channelFee.getImpl()).withdraw(order);
                        } catch (Exception e) {
                            push("???????????????????????????????????????????????????????????????????????????" + order.getOrderId() + "??????????????????????????????" + printStackTrace(e.getStackTrace()));
                            //  return Result.buildFailMessage("????????????");
                            log.error("????????????", e);
                        }
                    });
                    ThreadUtil.execute(() -> {
                        //???????????????????????? ????????????????????????????????????
                        boolean b = withdrawServiceImpl.updatePush(order.getOrderId());
                        if (b) {
                            log.info("???????????????????????????????????????????????????????????????" + order.getOrderId() + "???");
                        } else {
                            log.info("???????????????????????????????????????????????????????????????" + order.getOrderId() + "???");
                        }
                    });
                } else {
                    throw new OrderException("????????????????????????", null);
                }
            } else {
                deal = super.withdraw(order);
                if (deal.isSuccess()) {
                    withdrawServiceImpl.updateMsg(order.getOrderId(), "???????????????????????????????????????????????????");
                }

            }
        } catch (Exception e) {
            push("???????????????????????????????????????????????????????????? ????????????????????????????????????????????????" + order.getOrderId() + "??????????????????????????????" + printStackTrace(e.getStackTrace()));
            boolean b = withdrawServiceImpl.updatePush(order.getOrderId());
            if (b) {
                log.info("???????????????????????????????????????????????????????????????" + order.getOrderId() + "???");
            } else {
                log.info("???????????????????????????????????????????????????????????????" + order.getOrderId() + "???");
            }
        }
        return deal;
    }

    void push(String msg) {
        ThreadUtil.execute(() -> {
            String url = "http://10.170.0.4:8888/api/send?text=";
            String test = msg + "??????????????????" + DatePattern.NORM_DATETIME_FORMAT.format(new Date());
            test = HttpUtil.encode(test, "UTF-8");
            String id = "&id=";
            String ids = "-414940159";
            id += ids;
            String s = url + test + id;
            HttpUtil.get(s, 1000);
        });
    }

    String printStackTrace(StackTraceElement[] s) {
        String er = "";
        StackTraceElement[] stackTrace = s;
        for (StackTraceElement stack : stackTrace) {
            er = "-------" + stack.getMethodName() + " : " + stack;
        }
        return er;
    }


}
