package alipay.manage.contorller;

import alipay.config.annotion.LogMonitor;
import alipay.config.annotion.Submit;
import alipay.manage.api.channel.amount.AmountChannel;
import alipay.manage.api.config.FactoryForStrategy;
import alipay.manage.bean.Product;
import alipay.manage.bean.Recharge;
import alipay.manage.bean.UserFund;
import alipay.manage.bean.UserInfo;
import alipay.manage.bean.Withdraw;
import alipay.manage.service.OrderService;
import alipay.manage.service.ProductService;
import alipay.manage.service.UserFundService;
import alipay.manage.service.UserInfoService;
import alipay.manage.service.WithdrawService;
import alipay.manage.util.LogUtil;
import alipay.manage.util.SessionUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import otc.api.alipay.Common;
import otc.result.Result;
import otc.util.encode.HashKit;

import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/recharge")
public class RechargeContorller {
    Logger log = LoggerFactory.getLogger(RechargeContorller.class);
    @Autowired SessionUtil sessionUtil;
    @Autowired LogUtil logUtil;
    @Autowired ProductService productService;
    @Autowired FactoryForStrategy factoryForStrategy;
    @Autowired OrderService orderServiceImpl;
    @Autowired UserFundService userFundServiceImpl;
    @Autowired UserInfoService userInfoServiceImpl;
    @Autowired WithdrawService withdrawServiceImpl;
    private static final String MY_RECHARGE = "MyChannelRecharge";
    private static final String MY_WITHDRAW = "MyChannelWithdraw";
    private static final String RECHARGENO_NOTFIY = "127.0.0.1:3212/otc/rechaege-notfiy";
    private static final String WIT_NOTFIY = "127.0.0.1:3212/otc/rechaege-notfiy";
    /**
     * <p>获取可用的充值渠道</p>
     * <p>这里的渠道就是自营产品</p>
     * @return
     */
    @RequestMapping("/findEnabledPayType")
    @ResponseBody
    public Result findEnabledPayType() {
       List<Product> list = productService.findAllProduct();
        return Result.buildSuccessResult("发送成功",list);
    }

    /**
     * 充值生成订单
     * @param param
     * @param request
     * @return
     */
    @PostMapping("/generateRechargeOrder")
    @ResponseBody
    public Result generateRechargeOrder(Recharge param, HttpServletRequest request) {
        UserInfo user = sessionUtil.getUser(request);
        if(ObjectUtil.isNull(user))
            return Result.buildFailMessage("当前用户未登陆");
        if(ObjectUtil.isNull(param.getAmount() )
        		|| StrUtil.isBlank(param.getDepositor()) 
        		|| StrUtil.isBlank(param.getPhone())
        		|| StrUtil.isBlank(param.getSync_url())
        		)
        		return Result.buildFailMessage("关键信息为空");
        param.setUserId(user.getUserId());
        String clientIP = HttpUtil.getClientIP(request);
		if(StrUtil.isBlank(clientIP))
			return Result.buildFailMessage("当前使用代理服务器 或是操作ip识别出错，不允许操作");
		param.setRetain1(clientIP);
        String msg = "码商发起充值操作,当前充值参数：充值金额："+param.getAmount()+"，充值人姓名："+ param.getDepositor()+
                "，关联码商账号："+user.getUserId()+"，充值手机号："+ param.getPhone();
        boolean addLog = logUtil.addLog(request, msg, user.getUserId());
        log.info("获取addLog"+addLog);
        Result createRechrage = createRechrage(param);
        if(!createRechrage.isSuccess())
        	return Result.buildFailMessage("充值订单生成失败");
		Result recharge = null;
		try {
			recharge = factoryForStrategy.getAmountChannel(MY_RECHARGE).recharge(param);
		} catch (Exception e) {
			 return Result.buildFailMessage("暂无充值渠道");
		}
        if(recharge.isSuccess()) 
        	return Result.buildSuccessResult(recharge.getResult());
        return Result.buildFailMessage("暂无充值渠道");
    }
   Result createRechrage(Recharge param){
	   Recharge  order = new Recharge();
	   order.setActualAmount(param.getAmount());
	   order.setAmount(param.getAmount());
	   order.setRechargeType(param.getRechargeType());
	   order.setFee(new BigDecimal("0"));
	   order.setOrderStatus(Common.Order.DealOrder.ORDER_STATUS_DISPOSE.toString());
	   order.setNotfiy(RECHARGENO_NOTFIY);
	   order.setUserId(param.getUserId());
	   order.setRetain1(param.getRetain1());
	   boolean flag =  orderServiceImpl.addRechargeOrder(order);
	   if(flag)
		   return Result.buildSuccess();
	   return Result.buildFail();
    }
   @PostMapping("/startWithdraw")
   @ResponseBody
   public Result startWithdraw(Withdraw  param, HttpServletRequest request) {
       UserInfo user = sessionUtil.getUser(request);
       if(ObjectUtil.isNull(user))
           return Result.buildFailMessage("当前用户未登陆");
       return Result.buildFail();
   }
   private static final String USER_ID = "USER_ID";
   private static final String ACC_NAME = "ACC_NAME";
   private static final String BANK_NAME = "BANK_NAME";
   private static final String BANK_NO = "BANK_NO";
   private static final String AMOUNT = "AMOUNT";
   private static final String MONEY_PWD = "MONEY_PWD";
   private static final String MOBILE = "MOBILE";
    @PostMapping("/startWithdraw")
	@ResponseBody
	@LogMonitor(required = true)//登录放开
	@Submit(required = true)
	public Result startWithdraw(HttpServletRequest request,
			String withdrawAmount ,		
			String moneyPwd,
			String bankCard,
			String accountHolder,//开户名
			String bankCardAccount,
			String mobile,
			String type,
			String userId
			) {
    	String clientIP = HttpUtil.getClientIP(request);
    	if(StrUtil.isBlank(clientIP))
			return Result.buildFailMessage("当前使用代理服务器 或是操作ip识别出错，不允许操作");
		int hour = DateUtil.hour(new Date(),true);
		if(10 > hour && hour > 22)
			return Result.buildFailResult("提现时间为10：00 ~ 22：00，请在规定时间内提现");
		 UserInfo user = sessionUtil.getUser(request);
	        if(ObjectUtil.isNull(user))
	            return Result.buildFailMessage("当前用户未登陆");
		log.info("【用户提现，提现人："+user.getUserId()+"】");
		Map<String, String> map = new HashMap();
		map.put(ACC_NAME, accountHolder);
		map.put(BANK_NAME, bankCard);
		map.put(BANK_NO, bankCardAccount);
		map.put(AMOUNT, withdrawAmount);
		map.put(USER_ID, user.getUserId());
		map.put(MOBILE,mobile);
		map.put(MONEY_PWD,moneyPwd);
		Result clickWithdraw = isClickWithdraw(map);
		String msg = "码商发起提现操作,当前提现参数：开户名："+accountHolder+"，银行名称："+bankCard+
				"，关联码商账号："+ user.getUserId()+"，提现类型：1为充值点数2为分润"+type+"，提现手机号："+ mobile+"，提现金额："+withdrawAmount+"，提现验证密码："+clickWithdraw.isSuccess();
		boolean addLog = logUtil.addLog(request, msg,user.getUserId());
		Withdraw createWit = createWit(map,clientIP);
		if(ObjectUtil.isNull(createWit))
			return Result.buildFailResult("生成提现订单失败，请联系客服人员");
		Result withdraw = Result.buildFail();
		try {
			 withdraw = factoryForStrategy.getStrategy(MY_WITHDRAW).withdraw(createWit);
			if(withdraw.isSuccess())
				return withdraw;
		} catch (Exception e) {
			return withdraw;
		}
		return Result.buildFail();
	}
   
	/**
	 * <p>验证钱是否够，密码是否正确</p>
	 * @param map
	 * @return
	 */
   Result isClickWithdraw(Map<String, String> map) {
	   UserFund userFund = userFundServiceImpl.findUserInfoByUserId(map.get(USER_ID).toString());
	   UserInfo userInfo = userInfoServiceImpl.findUserInfoByUserId(userFund.getUserId());
	   String money_pwd = map.get(MONEY_PWD).toString();
	   Result password = HashKit.encodePassword(userInfo.getUserId(), money_pwd, userInfo.getSalt());
	   if(!money_pwd.equals(password.getResult().toString())) 
		   return Result.buildFailResult("资金密码错误；");
	   BigDecimal balance = userFund.getAccountBalance();
	   BigDecimal amount = new BigDecimal(map.get(AMOUNT).toString());
		if(balance.compareTo(amount)== -1)
			return Result.buildFailResult("当前金额不足，请重新");
		return Result.buildSuccessResult();
	}
   
   Withdraw createWit(Map<String, String> map,String ip) {
	   BigDecimal fee = new BigDecimal("2");
	   Withdraw wit = new Withdraw();
	   wit.setAccname(map.get(ACC_NAME).toString());
	   wit.setAmount(new BigDecimal(map.get(AMOUNT).toString()));
	   wit.setBankName(map.get(BANK_NAME).toString());
	   wit.setBankNo(map.get(BANK_NO).toString());
	   wit.setFee(fee);//码商代付手续费
	   wit.setMobile(map.get(MOBILE).toString());
	   wit.setNotify(WIT_NOTFIY);
	   wit.setOrderStatus(Common.Order.Wit.ORDER_STATUS_YU);
	   wit.setUserId(map.get(USER_ID).toString());
	   wit.setWithdrawType(Common.Order.Wit.WIT_QR);
	   wit.setActualAmount(new BigDecimal(map.get(AMOUNT).toString()).subtract(fee));
	   wit.setRetain2(ip);
	   wit.setRetain1(Common.Order.Wit.WIT_TYPE_CLI);
	   boolean addOrder = withdrawServiceImpl.addOrder(wit);
	   if(addOrder)
		   return wit;
	return null;
   }
    
}
