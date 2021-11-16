package test.number;

import alipay.manage.util.CheckUtils;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpUtil;
import otc.common.PayApiConstant;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class MD5 {
	public static void main(String[] args) {
      String url = "http://154.222.0.110:9083/jhongbangpay/api/pay-call-back";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("apporderid", "ZF2021111619245FYkN");
		map.put("tradesno", "BA20211116192418961600453");
		map.put("status", 2);
		map.put("amount", 3000.00);
		map.put("appid", "copo168");
		map.put("statusdesc", "成功");
		String sign = CheckUtils.getSign(map,  "2395C21EAA674CEEB29E2A1BB8A7CF3B");
		map.put("sign", sign);
		System.out.println(map.toString());
		send(url,  "BA20211116192418961600453", map, true);















    }
	private static void send(String url, String orderId, Map<String, Object> msg, boolean flag) {
		String result = "";
		try {
			if (url.contains("https")) {
				msg.put("url", url);
				result = HttpUtil.post(PayApiConstant.Notfiy.OTHER_URL + "/forword", msg);
			} else {
				result = HttpUtil.post(url, msg, 2000);
			}
		} catch (Exception e) {
			//加入定时任务推送
		}
		String isNotify = "NO";
		System.out.println(result);
	}
	private static final String UTF_8 = "utf-8";
	private static final String ENCODE_TYPE = "md5";
    /**
     * md5加密
     *
     * @param
     * @return
     */
    public static String md5(String a) {
		String c = "";
		MessageDigest md5;
		String result = "";
		try {
			md5 = MessageDigest.getInstance(ENCODE_TYPE);
			md5.update(a.getBytes(UTF_8));
			byte[] temp;
			temp = md5.digest(c.getBytes(UTF_8));
			for (int i = 0; i < temp.length; i++)
				result += Integer.toHexString((0x000000ff & temp[i]) | 0xffffff00).substring(6);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {


		}
		return result;
    }
}
