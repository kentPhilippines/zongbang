package alipay.manage.mapper;

import alipay.manage.bean.UserFund;
import alipay.manage.bean.UserInfo;
import alipay.manage.bean.UserInfoExample;
import alipay.manage.bean.UserRate;
import org.apache.ibatis.annotations.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Mapper
public interface UserInfoMapper {
    static final String USER = "USERINFO:INFO";
    static final String USER_LONG = "USER_LONG:USERINFO:INFO";

    int countByExample(UserInfoExample example);

    int deleteByExample(UserInfoExample example);

    int deleteByPrimaryKey(@Param("id") Integer id, @Param("userId") String userId);

    int insert(UserInfo record);

    int insertSelective(UserInfo record);

    List<UserInfo> selectByExample(UserInfoExample example);

    UserInfo selectByPrimaryKey(@Param("id") Integer id, @Param("userId") String userId);

    @CacheEvict(value = USER, allEntries = true)
    int updateByExampleSelective(@Param("record") UserInfo record, @Param("example") UserInfoExample example);

    @CacheEvict(value = USER, allEntries = true)
    int updateByExample(@Param("record") UserInfo record, @Param("example") UserInfoExample example);

    @CacheEvict(value = USER, allEntries = true)
    int updateByPrimaryKeySelective(UserInfo record);

    @CacheEvict(value = USER, allEntries = true)
    int updateByPrimaryKey(UserInfo record);

    
    
    
    
    
    
    
    
    
    
    @Select("select * from alipay_user_info where userId = #{userId}")
    UserInfo selectByUserId(@Param("userId")String userId);

    UserInfo selectByUserName(String username);

    @Select("select * from alipay_user_info where userId = #{userId} or userName = #{userName}")
    UserInfo findUserId(@Param("userId") String userId, @Param("userName") String userName);

    @Select("select queryChildAgents(#{accountId})")
    List<String> selectChildAgentListById(@Param("accountId") String accountId);

    /**
     * <p>???????????????id???????????? ???????????????????????????</p>
     *
     * @param userId
     * @return
     */
    @Select("select id, userId, userName, password, payPasword, salt, userType, switchs, userNode," +
            "    agent, isAgent, credit, receiveOrderState, remitOrderState," +
            "    createTime, submitTime, status, privateKey, publicKey, minAmount, maxAmount," +
            "    qrRechargeList,dealUrl,queueList,witip,startTime,endTime,timesTotal,totalAmount," +
            "autoWit , enterWitOpen , interFace  from alipay_user_info where userId = #{userId}")
    UserInfo findUserByUserId(@Param("userId") String userId);

    @Select("select * from alipay_user_rate where userId = #{userId} and payTypr = #{passCode} and switchs = 1")
    List<UserRate> selectUserRateByUserId(@Param("userId") String userId, @Param("passCode") String passCode);

    List<UserInfo> getLoginAccountInfo(String userId);

    @Select("select  id, userId, userName, cashBalance , freezeBalance, accountBalance  " +
            " from alipay_user_fund where userId = #{userId}")
    UserFund selectUsrFundByUserId(@Param("userId") String userId);

    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set ${paramKey} = #{paramValue} where userId = #{userId} ")
    int updateMerchantStatusByUserId(@Param("userId") String userId, @Param("paramKey") String paramKey, @Param("paramValue") String paramValue);

    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set receiveOrderState = #{status}, remitOrderState = #{status} where userId = #{userId}")
    void stopAllStatusByUserId(@Param("userId") String userId, @Param("status") Integer status);

    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_rate set switchs = #{status} where userId = #{userId} and userType = 1 ")
    void closeMerchantRateChannel(@Param("userId") String userId, @Param("status") Integer status);

    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set switchs = 0 where userId = #{userId}")
	int updataStatusEr(@Param("userId")String userId);
    /**
     * ??????????????????
     * @param user
     * @return
     */
    @CacheEvict(value = USER, allEntries = true)
    int updateproxyByUser(UserInfo user);

    /**
     * <p>??????????????????</p>
     * @param userId
     * @param newPassword
     * @return
     */
    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set password = #{newPassword} where userId = #{userId}")
	int updataPassword(@Param("userId")String userId, @Param("newPassword")String newPassword);

    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set payPasword = #{newPassword} where userId = #{userId}")
    int updataPayPassword(@Param("userId") String userId, @Param("newPassword") String newPayPassword);


    @Update("update alipay_user_fund set  todayDealAmount = 0 ,todayProfit = 0,todayOrderCount = 0 , todayAgentProfit = 0  , todayWitAmount = 0 ,todayOtherWitAmount = 0")
    void updateUserTime();

    @Insert("insert into  alipay_user_fund_bak (userId, userName, cashBalance, rechargeNumber, freezeBalance, accountBalance,  " +
            "    sumDealAmount, sumRechargeAmount, sumProfit, sumAgentProfit, sumOrderCount, todayDealAmount,  " +
            "    todayProfit, todayOrderCount, todayAgentProfit, userType, agent, isAgent   ,todayWitAmount ,todayOtherWitAmount)  select userId, userName, cashBalance, rechargeNumber, freezeBalance, accountBalance,  " +
            "    sumDealAmount, sumRechargeAmount, sumProfit, sumAgentProfit, sumOrderCount, todayDealAmount,  " +
            "    todayProfit, todayOrderCount, todayAgentProfit, userType, agent, isAgent ,todayWitAmount,todayOtherWitAmount   FROM alipay_user_fund")
    void bak();


    /**
     * ????????????????????????
     *
     * @param userNode
     * @return
     */
    @Cacheable(cacheNames = {USER_LONG}, unless = "#result == null")
    @Select("select * from alipay_user_info where userNode = #{userNode}")
    UserInfo findChannelAppId(@Param("userNode") String userNode);

    /**
     * ??????url??????????????????
     *
     * @param orderAccount
     * @return
     */
    @Cacheable(cacheNames = {USER_LONG}, unless = "#result == null")
    @Select("select id, userId, userName, userNode ,  " +
            "   dealUrl " +
            " from alipay_user_info where userId = #{userId}")
    UserInfo findDealUrl(@Param("userId") String userId);

    /**
     * ???????????????????????????  ????????????????????????
     *
     * @return
     */
    @Select("select id, userId, userName,   userType, switchs, userNode," +
            "    agent, isAgent ,queueList " +
            "  from alipay_user_info where userId = #{userId}")
    UserInfo findUserAgent(@Param("userId") String userId);

    /**
     * ???????????????????????????
     *
     * @return
     */
    @Cacheable(cacheNames = {USER_LONG}, unless = "#result == null")
    @Select("select id, userId, userName, password, payPasword," +
            "    privateKey, publicKey" +
            " from alipay_user_info where userId = #{userId}")
    UserInfo findPassword(@Param("userId") String userId);


    @Select("select * from alipay_user_info where  userType = 1")
    List<UserInfo> findAll();

    @Update("update alipay_user_info set publicKey = #{publickey} , privateKey = #{privactkey} , payPasword = #{dealKey} where userId = #{userId}")
    int updateDealKey(@Param("userId") String userId, @Param("publickey") String publickey, @Param("privactkey") String privactkey, @Param("dealKey") String dealKey);


    /**
     * ??????????????????????????????????????????
     *
     * @param userId
     * @return
     */
    @Cacheable(cacheNames = {USER_LONG}, unless = "#result == null")
    @Select("select id, userId, userName, password, payPasword, salt ," +
            "      privateKey, publicKey  " +
            "   from alipay_user_info where userId = #{userId}")
    UserInfo findPrivateKey(@Param("userId") String userId);


    @Select("select id, userId, userName,   switchs,   " +
            "     receiveOrderState, remitOrderState, " +
            "     minAmount, maxAmount," +
            "    qrRechargeList,dealUrl,queueList,witip, timesTotal,totalAmount," +
            "  enterWitOpen , witip  ,  interFace  from alipay_user_info where userId = #{userId}")
    UserInfo findClick(@Param("userId") String userId);


    @Cacheable(cacheNames = {USER}, unless = "#result == null")
    @Select("select id, userId, userName,   userType, userNode," +
            "    agent, isAgent " +
            "  from alipay_user_info where userId = #{userId}")
    UserInfo findUserByOrder(@Param("userId") String userId);


    @Select("select id, userId, userName, switchs, userNode  " +
            "  from alipay_user_info where userId = #{userId}")
    UserInfo getSwitchs(@Param("userId") String userId);


    /**
     * ???????????????????????????????????????
     *
     * @param userId
     * @return
     */
    @Cacheable(cacheNames = {USER_LONG}, unless = "#result == null")
    @Select("select id, userId, userName, password, payPasword," +
            "    privateKey, publicKey  , witip   , dealUrl ,userNode   " +
            " from alipay_user_info where userId = #{userId}")
    UserInfo findNotifyChannel(@Param("userId") String userId);

    @Select("select userId ,   autoWit   " +
            " from alipay_user_info where userId = #{userId}")
    UserInfo findautoWit(@Param("userId") String userId);

    @Select("select queryChildAgents(#{userId})")
    String queryChildAgents(String userId);

    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set receiveOrderState = 1 where userId = #{userId}")
    int updataReceiveOrderStateNO(@Param("userId") String userId);

    /**
     * <p>??????????????????</p>
     *
     * @param userId
     * @return
     */
    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set receiveOrderState = 2 where userId = #{userId}")
    int updataReceiveOrderStateOFF(@Param("userId") String userId);

    /**
     * ??????????????????
     *
     * @param userId
     * @return
     */
    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set remitOrderState = 1 where userId = #{userId}")
    int updataRemitOrderStateNO(@Param("userId") String userId);

    /**
     * ??????????????????
     *
     * @param userId
     * @return
     */
    @CacheEvict(value = USER, allEntries = true)
    @Update("update alipay_user_info set remitOrderState = 2 where userId = #{userId}")
    int updataRemitOrderStateOFF(@Param("userId") String userId);


    @Select("select * from alipay_user_info where agent is null and isAgent = 1 and userType = 2 ")
    List<UserInfo> findAgentQr();
}