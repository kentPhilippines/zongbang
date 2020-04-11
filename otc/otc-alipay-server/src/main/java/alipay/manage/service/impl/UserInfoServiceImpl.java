package alipay.manage.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import alipay.config.redis.RedisUtil;
import alipay.manage.bean.UserFund;
import alipay.manage.bean.UserFundExample;
import alipay.manage.bean.UserInfo;
import alipay.manage.bean.UserInfoExample;
import alipay.manage.bean.UserInfoExample.Criteria;
import alipay.manage.bean.UserRate;
import alipay.manage.bean.*;
import alipay.manage.mapper.FileListMapper;
import alipay.manage.mapper.UserFundMapper;
import alipay.manage.mapper.UserInfoMapper;
import alipay.manage.mapper.UserRateMapper;
import alipay.manage.service.MediumService;
import alipay.manage.util.AttributeUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import alipay.manage.service.UserInfoService;
import otc.bean.alipay.FileList;
import otc.bean.alipay.Medium;
import otc.common.RedisConstant;
import otc.result.Result;

@Component
public class UserInfoServiceImpl implements UserInfoService{
	static final String QR_CODE = "QR:CODE";
	@Autowired UserInfoMapper userInfoMapper;
	@Autowired RedisUtil redisUtil;
	@Autowired MediumService mediumService;
	@Autowired FileListMapper fileListMapper;
	@Autowired UserRateMapper userRateDao;
	@Autowired UserFundMapper userFundDao;
	@Autowired AttributeUtil attributeUtil;
    Logger log= LoggerFactory.getLogger(UserInfoServiceImpl.class);

	/**
	 * <p>查询自己的子账户</p>
	 * @param user
	 * @return
	 */
	@Override
	public List<UserInfo> findSunAccount(UserInfo user) {
		UserInfoExample example = new UserInfoExample();
		UserInfoExample.Criteria criteria = example.createCriteria();
		if (StrUtil.isNotBlank(user.getUserId()))
			criteria.andUserIdEqualTo(user.getUserId());
		if (StrUtil.isNotBlank(user.getAgent()))
			criteria.andAgentEqualTo(user.getAgent());
		List<UserInfo> userInfos = userInfoMapper.selectByExample(example);
		return userInfos;
	}

	@Override
	public List<UserInfo> findSumAgentUserByAccount(String userId) {
		return null;
	}

	@Override
	public UserInfo getUser(String username) {
		UserInfo selectByAccountId = userInfoMapper.selectByUserName(username);
		return selectByAccountId;
	}

	@Override
	@Cacheable(cacheNames= {RedisConstant.User.USER} ,  unless="#result == null")
	public List<String> findSunAccountByUserId(String userId) {
		// TODO Auto-generated method stub
		UserInfoExample example = new UserInfoExample();
	    Criteria criteria = example.createCriteria();
		criteria.andAgentEqualTo(userId);
		List<UserInfo> selectByExample = userInfoMapper.selectByExample(example);
		List<String> list = new ArrayList();
		for(UserInfo user : selectByExample)
			list.add(user.getUserId());
		return list;
	}

	@Override
	public boolean updateIsAgent(String accountId) {
		return false;
	}

	@Override
	public Boolean updataStatusEr(String userId) {
		int a = userInfoMapper.updataStatusEr(userId);
		return a > 0 && a < 2;
	}

	@Override
	public UserInfo findUserInfoByUserId(String userId) {
		UserInfo selectByAccountId = userInfoMapper.findUserByUserId(userId);
		return selectByAccountId;
	}
	@Override
	public Boolean updataAmount(UserFund userFund) {
		UserFundExample example = new UserFundExample();
		UserFundExample.Criteria criteria = example.createCriteria();
		criteria.andUserIdEqualTo(userFund.getUserId());
		criteria.andVersionEqualTo(userFund.getVersion());
		userFund.setVersion(null);
		int updateByExampleSelective = userFundDao.updateByExampleSelective(userFund , example );
		return updateByExampleSelective > 0 && updateByExampleSelective   < 2;
	}

	@Override
	public UserRate findUserRateById(Integer feeId) {
		return userRateDao.selectByPrimaryKey(feeId);
	}

	@Override
	public List<String> findSubLevelMembers(String accountId) {
		List<String> childAgentList = userInfoMapper.selectChildAgentListById(accountId);
		String first = CollUtil.getFirst(childAgentList);
		String[] split = first.split(",");
		if(split.length>2)
			childAgentList = Arrays.asList(split);
		return childAgentList;
	}

	@Override
	public Result addQrByMedium(String qrcodeId, String mediumId, String amount,String userId, String flag) {
		Medium medium = mediumService.findMediumById(mediumId);
		if(ObjectUtil.isNull(medium))
			return Result.buildFailResult("无此收款媒介");
		FileList qrcode = new FileList();
		qrcode.setFileId(qrcodeId);
		qrcode.setConcealId(mediumId);
		qrcode.setCode(medium.getCode()+"_qr");
		qrcode.setStatus(1);
		if("false".equals(flag)){
			qrcode.setFixationAmount(new BigDecimal(9999.0000));
		}else{
			qrcode.setFixationAmount(new BigDecimal(StrUtil.isBlank(amount)?"0":amount));
		}
		qrcode.setIsFixation(StrUtil.isBlank(amount)?"1":"2");
		qrcode.setFileholder(userId);
		/*	如果不填充这三个值就要改动去吗逻辑和回调参数逻辑
			qrcode.setQrcodeNumber(medium.getMediumNumber());
			qrcode.setQrcodePhone(medium.getMediumPhone());
			qrcode.setRetain1(medium.getMediumHolder());
		*/
		qrcode.setIsDeal("2");
		int insertSelective = fileListMapper.insertSelective(qrcode);
		if(insertSelective > 0 && insertSelective < 2)
			return Result.buildSuccessResult();
		return Result.buildFail();
	}

	@Override
	public UserFund findUserFundByAccount(String userId) {
		UserFund userFund=userFundDao.findUserFundByUserId(userId);
		return userFund;
	}
	/**
	 * 修改登录密码
	 * @param user
	 * @return
	 */

	@Override
	public boolean updataAccountPassword(UserInfo user) {
		UserInfoExample example = new UserInfoExample();
		UserInfoExample.Criteria createCriteria = example.createCriteria();
		createCriteria.andUserNameEqualTo(user.getUserName());
		int updateByExample = userInfoMapper.updateByExampleSelective(user,example);
		return updateByExample > 0 && updateByExample < 2;
	}


	@Cacheable(cacheNames= {RedisConstant.User.USER} ,  unless="#result == null")
	@Override
	public List<UserInfo> getLoginAccountInfo(String userName) {
		UserInfoExample example = new UserInfoExample();
		UserInfoExample.Criteria criteria = example.createCriteria();
		if(StrUtil.isNotBlank(userName))
			criteria.andUserNameEqualTo(userName);
		List<UserInfo> selectByExample = userInfoMapper.selectByExample(example);
		return selectByExample;
	}

	@Override
	public List<UserFund> findUserByAmount(BigDecimal amount) {
		return userFundDao.findUserByAmount(amount);
	}

	@Override
	public UserRate findUserRate(String userId, String productAlipayScan) {
		UserRate rate = userRateDao.findUserRate(userId,productAlipayScan);
		return rate;
	}
	@Override
	public UserInfo getQrCodeUser(UserInfo qruser) {
		UserInfo selectByExample = userInfoMapper.selectByUserId(qruser.getUserId());
		return selectByExample;
	}

	@Override
	public boolean addQrcodeUserInfo(UserInfo entity) {
		int insertSelective = userInfoMapper.insertSelective(entity);
		if (insertSelective>0 && insertSelective<2)
			attributeUtil.deleteRedis();
		return insertSelective>0 && insertSelective<2;
	}

	@Override
	public boolean addQrcodeUser(UserInfo user) {
		int selective = userInfoMapper.insertSelective(user);
		if (selective>0 && selective<2)
			attributeUtil.deleteRedis();
		return selective>0 && selective<2;
	}

	@Override
	public boolean updateproxyByUser(UserInfo user) {
		System.out.println("Id--->"+user.getId());
		System.out.println("userId--->"+user.getUserId());
		System.out.println("isAgant--->"+user.getAgent());
		System.out.println("isAgant--->"+user.getIsAgent());
		int results=userInfoMapper.updateproxyByUser(user);
		System.out.println("results--->" + results);
		return results>0 && results<2?true:false;
	}

    @Override
    public int updateBalanceById(Integer id, BigDecimal deduct, Integer version) {
		return userFundDao.updateBalanceById(id, deduct, version);
    }

	@Override
	public int insertAmountEntitys(Amount amount) {
		return userFundDao.insetAmountEntity(amount);
	}
}
