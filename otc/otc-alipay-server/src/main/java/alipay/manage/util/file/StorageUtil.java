package alipay.manage.util.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otc.util.number.Number;

public class StorageUtil {
	private static final String IMGTYPE = ".jpg";
	static Logger log = LoggerFactory.getLogger(StorageUtil.class);

	public static String uploadGatheringCode(Object key) {
		String img = Number.getImg();
		String path = "/img";
		log.info(" localStoragePath ::::  " + path);
		boolean base64ToImage = Base64Utils.Base64ToImage(key.toString(), path + "/" + img);
		if (base64ToImage) {
			return img;
		}
		return "";
	}
}
