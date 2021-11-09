package alipay.manage.service.impl;

import alipay.manage.bean.DealOrder;
import alipay.manage.bean.UserFund;
import alipay.manage.mapper.DealOrderMapper;
import alipay.manage.mapper.UserFundMapper;
import alipay.manage.service.UserFundService;
import org.springframework.stereotype.Component;
import otc.bean.alipay.Medium;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UserFundServiceImpl implements UserFundService {
	@Resource
	UserFundMapper userFundDao;

	@Resource
	DealOrderMapper orderMapper;
	@Override
	public UserFund showTodayReceiveOrderSituation(String userId) {
		return userFundDao.findUserFundByUserId(userId);
	}

	@Override
	public UserFund findUserInfoByUserId(String userId) {
		return userFundDao.findUserFundByUserId(userId);
	}

	@Override
	public List<UserFund> findBankUserId(BigDecimal amount) {
		List<UserFund>  fundList  = new ArrayList<>();
		List<UserFund> bankUser = userFundDao.findBankUserId(amount);
		List<DealOrder> orderList = orderMapper.findWaitWitUser();
		ConcurrentHashMap<String, DealOrder> qrCollect = orderList.stream().collect(Collectors.toConcurrentMap(DealOrder::getOrderQrUser, Function.identity(), (o1, o2) -> o1, ConcurrentHashMap::new));
		List<String> user = new ArrayList<>();
		for(UserFund fund : bankUser){
			DealOrder dealOrder = qrCollect.get(fund.getUserId());
			BigDecimal dealAmount = dealOrder.getDealAmount();//正在处理金额
			BigDecimal deposit = fund.getDeposit();
			BigDecimal divide = deposit.subtract(dealAmount);
			if(amount.doubleValue()<divide.doubleValue()){
				fundList.add(fund);
			}
		}
		return fundList;
	}

}
