package alipay.config.task;

import alipay.config.redis.RedisUtil;
import alipay.manage.api.channel.deal.jiabao.RSAUtil;
import alipay.manage.service.OrderService;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import otc.util.RSAUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BankOpen {
    private static final Log log = LogFactory.get();
    public static List<String> BANK_LIST = new ArrayList();



    @Autowired private OrderService orderServiceImpl;

    public static final String HEARTBEAT = "HEARTBEAT_";
    @Autowired
    private RedisUtil redis;

    static {
      //  BANK_LIST.add("623059138002823992"); //fang777
      //  BANK_LIST.add("6230361215006774818");  //fang777
    }

    void open() {
        for (String bank : BANK_LIST) {
            String s = RSAUtils.md5(bank);
            ThreadUtil.execute(() -> {
                boolean set = redis.set(HEARTBEAT + bank, HEARTBEAT, 15);//设置心跳过期时间1分钟
                log.info("心跳检测值：" + HEARTBEAT + bank + "结果：" + set);

            });
        }
    }

    public void updateBnakAmount() {
    //  Map<String,String> map =   orderServiceImpl.findBankAmount();










    }
}


class Heart {
    private String MD5;

    public String getMD5() {
        return MD5;
    }

    public void setMD5(String MD5) {
        this.MD5 = MD5;
    }
}