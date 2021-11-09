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
      String url = "http://13.228.96.0:18900/api/v1/deposit/zhongbangpay";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("apporderid", "2571886f411e11eca3fe0a240945b966");
		map.put("tradesno", "BA20211109133036232182287");
		map.put("status", 2);
		map.put("amount", 2000.000000);
		map.put("appid", "sk1111");
		map.put("statusdesc", "成功");
		String sign = CheckUtils.getSign(map,  "A0CA646839A94CCD8FE87C3ABF04D786");
		map.put("sign", sign);
		send(url,  "BA20211109132534817873154", map, true);















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
