package alipay.manage.api.channel.notfiy;

import alipay.manage.api.config.NotfiyChannel;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import otc.common.PayApiConstant;
import otc.result.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RequestMapping(PayApiConstant.Notfiy.NOTFIY_API_WAI)
@RestController
public class ShanFuPayNotfiy extends NotfiyChannel {
    private static final Log log = LogFactory.get();

    public static String createParam(Map<String, String> map) {
        try {
            if (map == null || map.isEmpty()) {
                return null;
            }
            Object[] key = map.keySet().toArray();
            Arrays.sort(key);
            StringBuffer res = new StringBuffer(128);
            for (int i = 0; i < key.length; i++) {
                if (ObjectUtil.isNotNull(map.get(key[i]))) {
                    res.append(key[i] + "=" + map.get(key[i]) + "&");
                }
            }
            String rStr = res.substring(0, res.length() - 1);
            return rStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String md5(String a) {
        String c = "";
        MessageDigest md5;
        String result = "";
        try {
            md5 = MessageDigest.getInstance("md5");
            md5.update(a.getBytes("utf-8"));
            byte[] temp;
            temp = md5.digest(c.getBytes("utf-8"));
            for (int i = 0; i < temp.length; i++) {
                result += Integer.toHexString((0x000000ff & temp[i]) | 0xffffff00).substring(6);
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
        }
        return result;
    }

    /**
     * memberid			????????????			???
     * orderid				?????????			???
     * amount				????????????			???
     * transaction_id		???????????????			???
     * datetime			????????????			???
     * returncode			????????????			???	???00??? ?????????
     * attach				????????????			???	????????????????????????
     * sign				??????	???			??????????????????????????????
     */
    @PostMapping("/shanfu-notfiy")
    public String notify(HttpServletRequest request, HttpServletResponse res) {
        String clientIP = HttpUtil.getClientIP(request);
        log.info("???????????????ip??????" + clientIP + "???");
        if (!"34.92.76.25".equals(clientIP)) {
            log.info("???????????????ip??????" + clientIP + "?????????IP????????????" + "34.92.76.25" + "???");
            log.info("???????????????ip????????????");
            return "ip errer";
        }

        String memberid = request.getParameter("memberid");
        String orderid = request.getParameter("orderid");
	    String amount=request.getParameter("amount");
	    String datetime=request.getParameter("datetime");
	    String returncode=request.getParameter("returncode");
	    String transaction_id = request.getParameter("transaction_id");
	    Map<String , String> map  = new HashMap();
	    map.put("memberid", memberid);
	    map.put("orderid", orderid);
	    map.put("amount", amount);
	    map.put("datetime", datetime);
	    map.put("returncode", returncode);
	    map.put("transaction_id", transaction_id);
	    String createParam = createParam(map);
        log.info("??????????????????????????????" + createParam + "???");
        String keyValue = "hjisna4yigfbaux4c2rth0frwco8md3j";
        String pay_md5sign = md5(createParam + "&key=" + keyValue).toUpperCase();
        String sign = request.getParameter("sign");
        if (sign.equals(pay_md5sign)) {
            log.info("??????????????????");
        } else {
            log.info("??????????????????????????????????????????" + pay_md5sign + "???????????????????????????" + sign + "???");
            return "sign is error";
        }
        if ("00".equals(returncode)) {
            Result dealpayNotfiy = dealpayNotfiy(orderid, clientIP, "????????????????????????");
            if (dealpayNotfiy.isSuccess()) {
                return "OK";
            }
        }
        return "NO";
    }
}
