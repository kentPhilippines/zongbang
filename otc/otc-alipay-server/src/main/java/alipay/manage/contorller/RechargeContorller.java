package alipay.manage.contorller;

import alipay.config.annotion.LogMonitor;
import alipay.config.annotion.Submit;
import alipay.manage.api.config.FactoryForStrategy;
import alipay.manage.bean.Product;
import alipay.manage.bean.UserFund;
import alipay.manage.bean.UserInfo;
import alipay.manage.service.*;
import alipay.manage.util.LogUtil;
import alipay.manage.util.SessionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
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
import otc.bean.dealpay.Recharge;
import otc.bean.dealpay.Withdraw;
import otc.result.Result;
import otc.util.encode.HashKit;
import otc.util.number.Number;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Controller
@RequestMapping("/recharge")
public class RechargeContorller {
    Logger log = LoggerFactory.getLogger(RechargeContorller.class);
    @Autowired SessionUtil sessionUtil;
    @Autowired LogUtil logUtil;
    @Autowired ProductService productService;
    @Autowired FactoryForStrategy factoryForStrategy;
	@Autowired
	OrderService orderServiceImpl;
	@Autowired
	UserFundService userFundServiceImpl;
	@Autowired
	UserInfoService userInfoServiceImpl;
	@Autowired
	WithdrawService withdrawServiceImpl;
	@Autowired
	RechargeService rechargeServiceImpl;
	@Autowired
	CorrelationService correlationServiceImpl;
	private static final String MY_RECHARGE = "MyChannelRecharge";
	private static final String MY_WITHDRAW = "MyChannelQrWithdraw";
	private static final String RECHARGENO_NOTFIY = "127.0.0.1:3212/otc/rechaege-notfiy";
	private static final String WIT_NOTFIY = "127.0.0.1:3212/otc/rechaege-notfiy";


	/**
	 * <p>???????????????????????????</p>
	 * <p>?????????????????????????????????</p>
	 *
	 * @return
	 */
	@RequestMapping("/findEnabledPayType")
	@ResponseBody
	public Result findEnabledPayType() {
		List<Product> list = productService.findAllProduct();
		return Result.buildSuccessResult(list);
	}

    /**
     * ??????????????????
     * @param param
     * @param request
     * @return
     */
    @PostMapping("/generateRechargeOrder")
    @ResponseBody
	@LogMonitor(required = true)//????????????
	@Submit(required = true)
    public Result generateRechargeOrder(Recharge param, HttpServletRequest request) {
        UserInfo user = sessionUtil.getUser(request);
        log.info("??????????????????"+param.toString()+"???");
		if (ObjectUtil.isNull(user)) {
			return Result.buildFailMessage("?????????????????????");
		}
		if (ObjectUtil.isNull(param.getAmount())
				|| StrUtil.isBlank(param.getDepositor())
				|| StrUtil.isBlank(param.getPhone())
		) {
			return Result.buildFailMessage("??????????????????");
		}
		param.setUserId(user.getUserId());
		String clientIP = HttpUtil.getClientIP(request);
		if (StrUtil.isBlank(clientIP)) {
			return Result.buildFailMessage("??????????????????????????? ????????????ip??????????????????????????????");
		}
		param.setRetain1(clientIP);
		String msg = "????????????????????????,????????????????????????????????????" + param.getAmount() + "?????????????????????" + param.getDepositor() +
				"????????????????????????" + user.getUserId() + "?????????????????????" + param.getPhone();
		boolean addLog = logUtil.addLog(request, msg, user.getUserId());
		log.info("??????addLog" + addLog);
		Recharge createRechrage = createRechrage(param);
		if (ObjectUtil.isNull(createRechrage)) {
			return Result.buildFailMessage("????????????????????????");
		}
		Result recharge = Result.buildFail();
		try {
			recharge = factoryForStrategy.getAmountChannel(MY_RECHARGE).recharge(createRechrage);
		} catch (Exception e) {
			return Result.buildFailMessage("??????????????????");
		}
		if (recharge.isSuccess()) {
			Object result = recharge.getResult();
			//LinkedHashMap<Object, Object> map = (LinkedHashMap<Object, Object>) result;
			//	Map<String, Object> objectToMap = otc.util.MapUtil.objectToMap(result);
			//	log.info("???json?????????"+map.toString()+"???");
			return recharge;
        }
        return Result.buildFailMessage("??????????????????");
    }
    Recharge createRechrage(Recharge param){
	   Recharge  order = new Recharge();
	   order.setActualAmount(param.getAmount());
	   order.setOrderId(Number.getRechargeQr());
	   order.setAmount(param.getAmount());
	   order.setRechargeType(param.getRechargeType());
	   order.setFee(new BigDecimal("0"));
	   order.setOrderStatus(Common.Order.DealOrder.ORDER_STATUS_DISPOSE.toString());
	   order.setNotfiy(RECHARGENO_NOTFIY);
	   order.setUserId(param.getUserId());
	   order.setRetain1(param.getRetain1());
	   order.setDepositor(param.getDepositor());
	   order.setPhone(param.getPhone());
		Future<UserInfo> execAsync = ThreadUtil.execAsync(() -> {
			String findAgent = correlationServiceImpl.findAgent(param.getUserId());
			return userInfoServiceImpl.findUserInfoByUserId(findAgent);
		});
		UserInfo userInfo;
		try {
			userInfo = execAsync.get();
		} catch (InterruptedException | ExecutionException e) {
			return null;
		}
		if (ObjectUtil.isNull(userInfo)) {
			return null;
		}
		order.setWeight(userInfo.getQrRechargeList());
		order.setNotfiy(RECHARGENO_NOTFIY);
		order.setBackUrl(userInfo.getDealUrl());//????????????????????????????????????
		boolean flag = orderServiceImpl.addRechargeOrder(order);
		if (flag) {
			return order;
		}
		return null;
	}
   private static final String USER_ID = "USER_ID";
   private static final String ACC_NAME = "ACC_NAME";
   private static final String BANK_NAME = "BANK_NAME";
   private static final String BANK_NO = "BANK_NO";
   private static final String AMOUNT = "AMOUNT";
   private static final String MONEY_PWD = "MONEY_PWD";
	private static final String TYPE = "TYPE";
	private static final String MOBILE = "MOBILE";
    @PostMapping("/startWithdraw")
	@ResponseBody
	@LogMonitor(required = true)//????????????
	@Submit(required = true)
	public Result startWithdraw(HttpServletRequest request,
			String withdrawAmount ,		
			String moneyPwd,
			String bankCard,
			String accountHolder,//?????????
			String bankcardAccount,
			String mobile,
			String type,
			String userId
			) {
		String clientIP = HttpUtil.getClientIP(request);
		if (StrUtil.isBlank(clientIP)) {
			return Result.buildFailMessage("??????????????????????????? ????????????ip??????????????????????????????");
		}
		int hour = DateUtil.hour(new Date(), true);
		if (10 > hour && hour > 22) {
			return Result.buildFailResult("???????????????10???00 ~ 22???00??????????????????????????????");
		}
		UserInfo user = sessionUtil.getUser(request);
		if (ObjectUtil.isNull(user)) {
			return Result.buildFailMessage("?????????????????????");
		}
		log.info("??????????????????????????????" + user.getUserId() + "???");
		Map<String, String> map = new HashMap();
		map.put(ACC_NAME, accountHolder);
		map.put(BANK_NAME, bankCard);
		map.put(BANK_NO, bankcardAccount);
		map.put(AMOUNT, withdrawAmount);
		map.put(USER_ID, user.getUserId());
		map.put(MOBILE, mobile);
		map.put(MONEY_PWD, moneyPwd);
		map.put(TYPE, type);
		Result clickWithdraw = isClickWithdraw(map);
		if (!clickWithdraw.isSuccess()) {
			return clickWithdraw;
		}
		String msg = "????????????????????????,?????????????????????????????????" + accountHolder + "??????????????????" + bankCard +
				"????????????????????????" + user.getUserId() + "?????????????????????" + mobile + "??????????????????" + withdrawAmount + "????????????????????????" + clickWithdraw.isSuccess();
		boolean addLog = logUtil.addLog(request, msg, user.getUserId());
		Withdraw createWit = createWit(map, clientIP);
		if (ObjectUtil.isNull(createWit)) {
			return Result.buildFailResult("????????????????????????????????????????????????");
		}
		//???????????????????????????
		Result withdraw = Result.buildFail();
		try {
			withdraw = factoryForStrategy.getAmountChannel(MY_WITHDRAW).withdraw(createWit);
			if (withdraw.isSuccess()) {
				return withdraw;
			}
		} catch (Exception e) {
			return withdraw;
		}

		return Result.buildSuccessMessage("????????????");
	}
   
	/**
	 * <p>???????????????????????????????????????</p>
	 * @param map
	 * @return
	 */
   Result isClickWithdraw(Map<String, String> map) {
	   UserFund userFund = userFundServiceImpl.findUserInfoByUserId(map.get(USER_ID).toString());
	   UserInfo userInfo = userInfoServiceImpl.findUserInfoByUserId(userFund.getUserId());
	   String money_pwd = map.get(MONEY_PWD).toString();
	   Result password = HashKit.encodePassword(userInfo.getUserId(), money_pwd, userInfo.getSalt());
	   if (!userInfo.getPayPasword().equals(password.getResult().toString())) {
		   return Result.buildFailResult("?????????????????????");
	   }
	   if ("1".equals(map.get(TYPE))) {
		   BigDecimal balance = userFund.getAccountBalance().subtract(userFund.getSumProfit());
		   BigDecimal amount = new BigDecimal(map.get(AMOUNT).toString());
		   if (balance.compareTo(amount.add(new BigDecimal("2"))) == -1) {
			   return Result.buildFailResult("??????????????????????????????");
		   }
	   } else {
		   BigDecimal balance = userFund.getSumProfit();
		   BigDecimal amount = new BigDecimal(map.get(AMOUNT).toString());
		   if (balance.compareTo(amount.add(new BigDecimal("2"))) == -1) {
			   return Result.buildFailResult("??????????????????????????????");
		   }
		   BigDecimal balance1 = userFund.getAccountBalance();
		   BigDecimal amount1 = new BigDecimal(map.get(AMOUNT).toString());
		   if (balance1.compareTo(amount1.add(new BigDecimal("2"))) == -1) {
			   return Result.buildFailResult("??????????????????????????????");
		   }
	   }
	   return Result.buildSuccessResult();
   }
   
   Withdraw createWit(Map<String, String> map,String ip) {
	   BigDecimal fee = new BigDecimal("2");
	   Withdraw wit = new Withdraw();
	   wit.setOrderId(Number.getWitOrderQr());
	   wit.setAccname(map.get(ACC_NAME).toString());
	   wit.setAmount(new BigDecimal(map.get(AMOUNT).toString()));
	   wit.setBankName(map.get(BANK_NAME).toString());
	   wit.setBankNo(map.get(BANK_NO).toString());
	   wit.setFee(fee);//?????????????????????
	   wit.setMobile(map.get(MOBILE).toString());
	   wit.setNotify(WIT_NOTFIY);
	   wit.setOrderStatus(Common.Order.Wit.ORDER_STATUS_YU);
	   wit.setUserId(map.get(USER_ID).toString());
	   wit.setWithdrawType(Common.Order.Wit.WIT_QR);
	   wit.setActualAmount(new BigDecimal(map.get(AMOUNT).toString()));
	   wit.setRetain2(ip);
	   wit.setRetain1(map.get(TYPE));
	   Future<UserInfo> execAsync = ThreadUtil.execAsync(() -> {
		   String findAgent = correlationServiceImpl.findAgent(map.get(USER_ID).toString());
		   return userInfoServiceImpl.findUserInfoByUserId(findAgent);
	   });
	   UserInfo userInfo;
	   try {
		   userInfo = execAsync.get();
	   } catch (InterruptedException | ExecutionException e) {
		   return null;
	   }
	   if (ObjectUtil.isNull(userInfo)) {
		   return null;
	   }
	   wit.setWeight(userInfo.getQrRechargeList());
	   boolean addOrder = withdrawServiceImpl.addOrder(wit);
	   if (addOrder) {
		   return wit;
	   }
	   return null;
   }
}
