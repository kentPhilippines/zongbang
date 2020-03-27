package alipay.manage.mapper;

import alipay.manage.bean.Amount;
import alipay.manage.bean.UserFund;
import alipay.manage.bean.UserFundExample;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UserFundMapper {
    int countByExample(UserFundExample example);
    int deleteByExample(UserFundExample example);
    int deleteByPrimaryKey(Integer id);
    int insert(UserFund record);
    int insertSelective(UserFund record);
    List<UserFund> selectByExample(UserFundExample example);
    UserFund selectByPrimaryKey(Integer id);
    int updateByExampleSelective(@Param("record") UserFund record, @Param("example") UserFundExample example);
    int updateByExample(@Param("record") UserFund record, @Param("example") UserFundExample example);
    int updateByPrimaryKeySelective(UserFund record);
    int updateByPrimaryKey(UserFund record);
    
    
    
    @Select("select * from alipay_user_fund where userType = 2 and accountBalance > #{amount}  ")
	List<UserFund> findUserByAmount(BigDecimal amount);

    @Select("select * from alipay_user_fund where userId=#{userId}")
    UserFund findUserFundByUserId(@Param("userId") String userId);

    @Update("update alipay_user_fund set rechargeNumber = rechargeNumber + #{deduct}, freezeBalance = freezeBalance - #{deduct}, " +
            "accountBalance = accountBalance - #{deduct}, version = version + 1 where id = #{id} and version = #{version} ")
    int updateBalanceById(@Param("id") Integer id, @Param("deduct") BigDecimal deduct, @Param("version") Integer version);

    @Insert("insert into alipay_amount (orderId, userId, amountType, accname, orderStatus, amount, actualAmount, dealDescribe) " +
            "values (#{orderId}, #{userId},#{amountType},#{accname},#{orderStatus},#{amount},#{amount},#{dealDescribe} )")
    int insetAmountEntity(Amount amount);
}