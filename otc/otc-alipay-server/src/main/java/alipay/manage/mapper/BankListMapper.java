package alipay.manage.mapper;

import alipay.manage.bean.BankList;
import alipay.manage.bean.BankListExample;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Mapper
public interface BankListMapper {
    static final String HUIFU_BANK_INFO_CARD = "HUIFU:BANK:INFO:CARD";

    int countByExample(BankListExample example);

    int deleteByExample(BankListExample example);

    int deleteByPrimaryKey(@Param("id") Integer id, @Param("bankcardId") String bankcardId);

    int insert(BankList record);

    int insertSelective(BankList record);

    List<BankList> selectByExampleWithBLOBs(BankListExample example);

    List<BankList> selectByExample(BankListExample example);

    BankList selectByPrimaryKey(@Param("id") Integer id, @Param("bankcardId") String bankcardId);

    int updateByExampleSelective(@Param("record") BankList record, @Param("example") BankListExample example);

    int updateByExampleWithBLOBs(@Param("record") BankList record, @Param("example") BankListExample example);

    int updateByExample(@Param("record") BankList record, @Param("example") BankListExample example);

    int updateByPrimaryKeySelective(BankList record);

    int updateByPrimaryKeyWithBLOBs(BankList record);

    int updateByPrimaryKey(BankList record);

    @Cacheable(cacheNames = {HUIFU_BANK_INFO_CARD}, unless = "#result == null")
    @Select("select * from alipay_bank_list where bankcardAccount = #{bankNo}")
    BankList selectBankCardByBankNo(@Param("bankNo") String bankNo);

    @Cacheable(cacheNames = {HUIFU_BANK_INFO_CARD}, unless = "#result == null")
    @Select("select * from alipay_bank_list where account = #{account}")
    List<BankList> findBankByAccount(@Param("account") String account);

    @CacheEvict(value = HUIFU_BANK_INFO_CARD, allEntries = true)
    @Delete("delete from alipay_bank_list where account = #{account} ")
    boolean cleanBankToLast(@Param("account") String account);
}