package alipay.manage.mapper;

import alipay.manage.bean.MediumExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import otc.bean.alipay.Medium;

import java.math.BigDecimal;
import java.util.List;
@Mapper
public interface MediumMapper {
    static final String MEDIUMBANK = "MEDIUM:BANK";
    int countByExample(MediumExample example);

    int deleteByExample(MediumExample example);

    int deleteByPrimaryKey(@Param("id") Integer id, @Param("mediumNumber") String mediumNumber, @Param("mediumId") String mediumId);

    int insert(Medium record);

    int insertSelective(Medium record);

    List<Medium> selectByExampleWithBLOBs(MediumExample example);

    List<Medium> selectByExample(MediumExample example);

    Medium selectByPrimaryKey(@Param("id") Integer id);

    int updateByExampleSelective(@Param("record") Medium record, @Param("example") MediumExample example);

    int updateByExampleWithBLOBs(@Param("record") Medium record, @Param("example") MediumExample example);

    int updateByExample(@Param("record") Medium record, @Param("example") MediumExample example);

    int updateByPrimaryKeySelective(Medium record);

    int updateByPrimaryKeyWithBLOBs(Medium record);

    int updateByPrimaryKey(Medium record);


    List<Medium> findIsMyMediumPage(String accountId);

    @Select("select  *  from alipay_medium where mediumId = #{mediumId}")
    Medium findMediumBy(@Param("mediumId") String mediumId);

    @Select("select  *  from alipay_medium where code = #{mediumType} and attr = #{code} and isDeal = 2 and status = 1")
    List<Medium> findMediumByType(@Param("mediumType") String mediumType, @Param("code") String code);

    @Select("select  *  from alipay_medium where code = #{mediumType}  and isDeal = 2 and status = 1")
    List<Medium> findMediumByType1(@Param("mediumType") String mediumType);


    /**
     * ???????????????????????????????????????
     *
     * @param amount
     * @return
     */
    @Select("select * from alipay_medium where status = 1  and  isDeal = '2'   and black = 1  and error  = 0  and  #{amount}  + mountSystem  < mountLimit   ")
    List<Medium> findBankByAmount(@Param("amount") BigDecimal amount);
    /**
     * ???????????????????????????????????????
     *
     * @param amount
     * @return
     */
    @Select("select * from alipay_medium where status = 1  and  isDeal = '2'   and black = 1 and error  = 0  and  #{amount}  <= witAmount    ")
    List<Medium> findBankByAmountWit(@Param("amount") BigDecimal amount);


    /**
     * ???????????????????????????????????????
     *
     * @param bankInfo
     * @param code
     * @return
     */
    List<Medium> findBankByAmountAndAttr(@Param("code") List<String> code);


    /**
     * ????????????????????????
     *  @param bankAccount
     * @param dealAmount
     * @param version
     */
    @Update("update alipay_medium set `version` =  `version` + 1 ,   mountSystem = mountSystem - #{dealAmount} where mediumNumber = #{bankAccount}  and `version`  = #{version} ")
    int subMountNow(@Param("bankAccount") String bankAccount, @Param("dealAmount") BigDecimal dealAmount,@Param("version")  Integer version);

    @Update("update alipay_medium set `version` =  `version` + 1 ,    mountSystem = mountSystem + #{dealAmount} , witAmount = witAmount + #{dealAmount}   where mediumNumber = #{bankAccount}  and `version`  = #{version}")
    int addMountNow(@Param("bankAccount") String bankAccount, @Param("dealAmount") BigDecimal dealAmount, @Param("version") Integer version);

    @Select("select * from alipay_medium where   mediumNumber = #{cardInfo}  and  qrcodeId = #{userId}")
    Medium findMediumByBankAndId(@Param("cardInfo") String cardInfo, @Param("userId") String userId);

    @Select("select * from alipay_medium where   mediumNumber = #{bankInfo} ")
    Medium findBank(@Param("bankInfo") String bankInfo);


    /**
     * ?????????????????????????????????
     *
     * @return
     */
    @Select("select * from alipay_medium where  isDeal = 2 and status = 1 ")
    List<Medium> findBankOpen();
    @Update("update alipay_medium set mountSystem = mountSystem - #{amount} where id = #{id} ")
    void deleteMed(@Param("id")Integer id, @Param("amount")BigDecimal amount);


    @Update("update alipay_medium set `version` =  `version` + 1 , witAmount = witAmount + #{amount} where mediumNumber = #{bankId} and `version`  = #{version} ")
    int addMountNowWit(@Param("bankId") String bankId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
    @Update("update alipay_medium set `version` =  `version` + 1 , witAmount = witAmount - #{amount} where mediumNumber = #{bankId} and `version`  = #{version} ")
    int subMountNowWit(@Param("bankId") String bankId, @Param("amount") BigDecimal amount, @Param("version")  Integer version);
}