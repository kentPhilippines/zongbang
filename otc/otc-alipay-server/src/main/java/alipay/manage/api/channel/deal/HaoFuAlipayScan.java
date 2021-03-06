package alipay.manage.api.channel.deal;

import alipay.manage.api.channel.util.haofu.HaoFuUtil;
import alipay.manage.api.config.PayOrderService;
import alipay.manage.bean.DealOrderApp;
import alipay.manage.bean.UserInfo;
import alipay.manage.bean.util.ResultDeal;
import alipay.manage.service.UserInfoService;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import otc.common.PayApiConstant;
import otc.result.Result;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component("HaoFuAlipayScan")
public class HaoFuAlipayScan extends PayOrderService {
    private static final Log log = LogFactory.get();
    @Autowired
    private UserInfoService userInfoServiceImpl;

    public static String createParam(Map<String, Object> map) {
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

    @Override
    public Result deal(DealOrderApp dealOrderApp, String payType) {
        log.info("???????????????????????????????????????");
        String channelId = payType;//?????????????????????
        String create = create(dealOrderApp, channelId);
        if (StrUtil.isNotBlank(create)) {
            log.info("???????????????????????????????????????????????????????????????");
            UserInfo userInfo = userInfoServiceImpl.findUserInfoByUserId(dealOrderApp.getOrderAccount());
            if (StrUtil.isBlank(userInfo.getDealUrl())) {
				orderEr(dealOrderApp, "??????????????????url?????????");
				return Result.buildFailMessage("?????????????????????????????????????????????url");
			}
			log.info("???????????????ip??????" + userInfo.getDealUrl() + "???");
			String url = createOrder(userInfo.getDealUrl() + PayApiConstant.Notfiy.NOTFIY_API_WAI + "/haofu-notfiy", dealOrderApp.getOrderAmount(), create);
			if (StrUtil.isBlank(url)) {
                boolean orderEr = orderEr(dealOrderApp);
                if (orderEr) {
                    return Result.buildFailMessage("????????????");
                }
            } else {
				return Result.buildSuccessResult("???????????????", ResultDeal.sendUrl(url));
			}
		}
		return  Result.buildFailMessage("????????????");
	}

	private String createOrder(String notfiy, BigDecimal orderAmount, String orderId) {
		Map<String,Object> map = new HashMap<String,Object>();
		String key  = HaoFuUtil.KEY;
		map.put("partner", HaoFuUtil.APPID);
		map.put("amount", orderAmount.intValue());
		map.put("request_time", System.currentTimeMillis()/1000);
		map.put("trade_no", orderId);
		map.put("pay_type", "sm");
		map.put("notify_url",notfiy);
		String createParam = createParam(map);
		String md5 = md5(createParam+"&"+key);
		map.put("sign", md5);
		log.info("?????????????????????????????????"+map.toString()+"???");
		String post = HttpUtil.post(HaoFuUtil.URL+"/payCenter/aliPay2", map);
		log.info("???????????????????????????"+post+"???");
		JSONObject parseObj = JSONUtil.parseObj(post);
		Object object = parseObj.get("is_success");
		if(ObjectUtil.isNotNull(object)) {
            log.info("???????????????????????????" + object + "");
            if ("T".equals(object)) {
                Object object2 = parseObj.get("result");
                if (ObjectUtil.isNotNull(object2)) {
                    log.info("?????????????????????" + object2 + "???");
                    return object2.toString();
                }
            }
        }
		return "";
	}
}
