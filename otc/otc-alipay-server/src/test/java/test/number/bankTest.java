package test.number;

import alipay.manage.bean.UserFund;
import alipay.manage.util.bankcardUtil.BankTypeUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class bankTest {

    public static void main(String[] args) {

        List<UserFund> userFundList = new ArrayList<>();
        UserFund fund1 = new UserFund();
        UserFund fund2 = new UserFund();
        UserFund fund3 = new UserFund();
        UserFund fund4 = new UserFund();

        fund1.setTodayDealAmount(new BigDecimal(5000));//当日入款金额
        fund1.setTodayOtherWitAmount(new BigDecimal(4000));//当日出款金额
        fund1.setUserId("1");
        userFundList.add(fund1);
        fund2.setTodayDealAmount(new BigDecimal(6000));//当日入款金额
        fund2.setTodayOtherWitAmount(new BigDecimal(4000));//当日出款金额
        fund2.setUserId("2");
        userFundList.add(fund2);
        fund3.setTodayDealAmount(new BigDecimal(7000));//当日入款金额
        fund3.setTodayOtherWitAmount(new BigDecimal(4000));//当日出款金额
        fund3.setUserId("3");
        userFundList.add(fund3);
        fund4.setTodayDealAmount(new BigDecimal(8000));//当日入款金额
        fund4.setTodayOtherWitAmount(new BigDecimal(4000));//当日出款金额
        fund4.setUserId("4");
        userFundList.add(fund4);
        //入款金额 - 出款金额   差额最小  且根据入款金额排序
        Collections.sort(userFundList, new Comparator<UserFund>() {
            @Override
            public int compare(UserFund o1, UserFund o2) {
                return o1.getTodayDealAmount().subtract(o1.getTodayOtherWitAmount()).subtract(new BigDecimal(500)).compareTo(o2.getTodayDealAmount().subtract(o2.getTodayOtherWitAmount()).subtract(new BigDecimal(500))) * -1;
            }
        });


        for (UserFund fund : userFundList) {
            System.out.println(fund.toString());
        }

    }


}



