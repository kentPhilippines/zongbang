package alipay.config.task;

import alipay.manage.api.channel.deal.jiabao.RSAUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.stereotype.Component;
import otc.util.RSAUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class BankOpen {
    private static final Log log = LogFactory.get();
    public static List<String> BANK_LIST = new ArrayList();

    static {
      /*  BANK_LIST.add("6217001850015770473"); //fang777
        BANK_LIST.add("622908163031245319");  //fang777
        BANK_LIST.add("6217856200019845051");  //fang777
        BANK_LIST.add("6228480078139485672"); //fang777

        BANK_LIST.add("6227002556390509919"); //fang777


        BANK_LIST.add("6228481729215267272"); //WF8888
        BANK_LIST.add("6221804520003024925"); //WF8888
        BANK_LIST.add("6212253803005445297"); //WF8888




        BANK_LIST.add("6217975200000223017"); //lfbbss
        BANK_LIST.add("6224121212530803"); //lfbbss
        BANK_LIST.add("6217932125081307"); //lfbbss
        BANK_LIST.add("6217995200285745557"); //lfbbss
        BANK_LIST.add("6230580000330781241"); //lfbbss
        BANK_LIST.add("6230580000261425842"); //lfbbss



*/


    }

    void open() {
        for (String bank : BANK_LIST) {
            String s = RSAUtils.md5(bank);
            ThreadUtil.execute(() -> {
                Heart heart = new Heart();
                heart.setMD5(s);
                JSONObject jsonObject = JSONUtil.parseObj(heart);
                log.info("当前放开银行卡为：" + bank + "，放开参数为：" + s);
                String body = HttpRequest.post("http://hftfc888.com:8080/http/heartbeat")
                        .body(jsonObject)
                        .execute().body();
                log.info("响应参数为：" + body);
                heart = null;
            });
        }
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