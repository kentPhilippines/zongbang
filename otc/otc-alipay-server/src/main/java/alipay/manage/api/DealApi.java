package alipay.manage.api;

import alipay.manage.api.config.NotfiyChannel;
import alipay.manage.bean.*;
import alipay.manage.service.*;
import alipay.manage.util.LogUtil;
import alipay.manage.util.NotifyUtil;
import alipay.manage.util.QrUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import otc.api.alipay.Common;
import otc.bean.alipay.FileList;
import otc.bean.alipay.Medium;
import otc.common.SystemConstants;
import otc.result.Result;
import otc.util.RSAUtils;
import otc.util.number.Number;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
@Controller
@RequestMapping("/pay")
public class DealApi extends NotfiyChannel {
	private static final String ORDER = "orderid";
	@Autowired OrderAppService orderAppServiceImpl;
	@Autowired OrderService orderServiceImpl;
	@Autowired UserInfoService userInfoServiceImpl;
	@Autowired QrUtil  qrUtil;
	@Autowired LogUtil logUtil;
	@Autowired NotifyUtil notifyUtil;
	@Autowired FileListService fileListServiceImpl;
	@Autowired MediumService mediumServiceImpl;
	@Autowired CorrelationService correlationServiceImpl;
	static Lock lock = new ReentrantLock();
	private static final String tinyurl =  "http://tinyurl.com/api-create.php";
	private static final Log log = LogFactory.get();
	@RequestMapping("/alipayScan/{param:.+}")
	public String alipayScan(@PathVariable String param,HttpServletRequest request) {
		log.info("??????????????????????????????????????????????????????" + param + "???");
		Map<String, Object> stringObjectMap = RSAUtils.retMapDecode(param, SystemConstants.INNER_PLATFORM_PRIVATE_KEY);
		if (CollUtil.isEmpty(stringObjectMap)) {
			log.info("????????????????????????");
		}
		String orderId = stringObjectMap.get(ORDER).toString();
		log.info("????????????????????????????????????" + orderId + "???");
		DealOrder order = orderServiceImpl.findAssOrder(orderId);
		if (ObjectUtil.isNotNull(order)) {
			return "toFixationPay";
		}
		DealOrderApp orderApp = orderAppServiceImpl.findOrderByOrderId(orderId);
		boolean flag = addOrder(orderApp, request);
		if (!flag) {
			log.info("??????????????????????????????????????????????????????");
			ThreadUtil.execute(() -> {
				orderAppServiceImpl.updateOrderEr(orderId, "?????????????????????");
			});
			return "payEr";
		}
		return "toFixationPay";
	}
	private boolean addOrder(DealOrderApp orderApp, HttpServletRequest request) {
		if (!orderApp.getOrderStatus().toString().equals(Common.Order.DealOrder.ORDER_STATUS_DISPOSE.toString())) {
			return false;
		}
		DealOrder order = new DealOrder();
		String orderAccount = orderApp.getOrderAccount();//???????????????
		UserInfo accountInfo = userInfoServiceImpl.findUserInfoByUserId(orderAccount);//??????????????????????????? ??????????????????
		String[] split = {};
		if (StrUtil.isNotBlank(accountInfo.getQueueList())) {
			split = accountInfo.getQueueList().split(",");//????????????????????????
		}
		order.setAssociatedId(orderApp.getOrderId());
		order.setDealDescribe("??????????????????");
		order.setActualAmount(orderApp.getOrderAmount());
		order.setDealAmount(orderApp.getOrderAmount());
		order.setDealFee(new BigDecimal("0"));
		order.setExternalOrderId(orderApp.getAppOrderId());
		order.setGenerationIp(HttpUtil.getClientIP(request));
		order.setOrderAccount(orderApp.getOrderAccount());
		order.setNotify(orderApp.getNotify());
		FileList findQr = null;

		try {
			findQr = qrUtil.findQr(orderApp.getOrderId(), orderApp.getOrderAmount(), Arrays.asList(split), true);
		} catch (ParseException e) {
			log.info("????????????????????????");
		}
		if (ObjectUtil.isNull(findQr)) {
			return false;
		}
		order.setOrderQrUser(findQr.getFileholder());
		order.setOrderQr(findQr.getFileId());
		order.setOrderStatus(Common.Order.DealOrder.ORDER_STATUS_DISPOSE.toString());
		order.setOrderType(Common.Order.ORDER_TYPE_DEAL.toString());
		UserRate rate = userInfoServiceImpl.findUserRate(findQr.getFileholder(), Common.Deal.PRODUCT_ALIPAY_SCAN);
		order.setOrderId(Number.getOrderQr());
		order.setFeeId(rate.getId());
		order.setRetain1(rate.getPayTypr());
		boolean addOrder = orderServiceImpl.addOrder(order);
		if (addOrder) {
			corr(order.getOrderId());
		}
		return addOrder;
	}
	/**
	 * <p>??????????????????</p>
	 */
	void corr(String orderId){
		ThreadUtil.execute(()->{
			DealOrder order = orderServiceImpl.findOrderByOrderId(orderId);
			FileList findQrByNo = fileListServiceImpl.findQrByNo(order.getOrderQr());
			Medium medium = mediumServiceImpl.findMediumById(findQrByNo.getConcealId());
			CorrelationData corr = new CorrelationData();
			corr.setAmount(order.getDealAmount());
			corr.setMediumId(medium.getId());
			corr.setOrderId(order.getOrderId());
			corr.setQrId(findQrByNo.getId().toString());
			corr.setOrderStatus(Integer.valueOf(order.getOrderStatus()));
			corr.setUserId(order.getOrderQrUser());
			corr.setAppId(order.getOrderAccount());
			boolean addCorrelationDate = correlationServiceImpl.addCorrelationDate(corr);
			if (addCorrelationDate) {
				log.info("???????????????" + order.getOrderId() + "??????????????????????????????");
			} else {
				log.info("???????????????" + order.getOrderId() + "??????????????????????????????");
			}
		});
	}
	
	
	@GetMapping("/getOrderGatheringCode")
	@ResponseBody
	public Result findOrder(String orderNo) {
		log.info("????????????????????????"+orderNo+"???");
		String[] split = orderNo.split("/");
		List<String> asList = Arrays.asList(split);
		String last = CollUtil.getLast(asList);
		log.info("?????????????????????"+last+"???");
		Map<String, Object> stringObjectMap = RSAUtils.retMapDecode(last, SystemConstants.INNER_PLATFORM_PRIVATE_KEY);
		DealOrder order2 = orderServiceImpl.findAssOrder(stringObjectMap.get(ORDER).toString());
		return Result.buildSuccessResult(order2);
	}
	
	
	@GetMapping("/getOrderGatheringCode1")
	@ResponseBody
	public Result findOrder1(String orderNo) {
		log.info("????????????????????????"+orderNo+"???");
		DealOrder order2 = orderServiceImpl.findOrderByOrderId(orderNo);
		return Result.buildSuccessResult(order2);
	}
	
	@GetMapping("/testWit")
	@ResponseBody
	public Result testWit(String orderNo) {
		log.info("????????????????????????"+orderNo+"???");
		return witNotfy(orderNo,"123.2.2.2");
	}


	
}
