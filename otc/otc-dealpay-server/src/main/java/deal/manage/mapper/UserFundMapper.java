package deal.manage.mapper;

import deal.manage.bean.UserFund;
import deal.manage.bean.UserFundExample;
import deal.manage.bean.UserInfo;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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

    @Select("select * from dealpay_user_fund where userId = #{userId}")
	UserFund findUserFund(@Param("userId")String userId);

	List<UserFund> findSunAccount(UserInfo user);

	@Update("update dealpay_user_fund set isAgent = 1 where userId = #{userId}")
	int updateIsAgent(@Param("userId")String userId);

	@Select("select accountBalance from dealpay_user_fund where userId = #{userId}")
	UserFund findUserFundMount(@Param("userId") String userId);
}