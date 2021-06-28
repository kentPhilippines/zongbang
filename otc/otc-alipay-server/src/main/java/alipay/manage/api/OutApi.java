package alipay.manage.api;

import alipay.config.redis.RedisUtil;
import alipay.manage.api.feign.QueueServiceClien;
import alipay.manage.bean.UserInfo;
import alipay.manage.service.MediumService;
import alipay.manage.service.UserInfoService;
import alipay.manage.util.QueueUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import otc.bean.alipay.Medium;
import otc.common.RedisConstant;
import otc.result.Result;
import otc.util.RSAUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/out")
@RestController
public class OutApi {
    private static final Log log = LogFactory.get();
    private static final String UTF_8 = "utf-8";
    private static final String ENCODE_TYPE = "md5";
    @Autowired
    UserInfoService userInfoServiceImpl;
    private static final String REDISKEY_QUEUE = RedisConstant.Queue.QUEUE_REDIS;//卡商入列标识

    public static String md5(String a) {
        String c = "";
        MessageDigest md5;
        String result = "";
        try {
            md5 = MessageDigest.getInstance(ENCODE_TYPE);
            md5.update(a.getBytes(UTF_8));
            byte[] temp;
            temp = md5.digest(c.getBytes(UTF_8));
            for (int i = 0; i < temp.length; i++) {
                result += Integer.toHexString((0x000000ff & temp[i]) | 0xffffff00).substring(6);
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
        }
        return result;
    }

    @Autowired
    QueueServiceClien queueServiceClienFeignImpl;
    @Autowired
    RedisUtil redisUtil;

    /**
     * 通过顶代卡商查询信息
     *
     * @param cardInfo
     * @return
     */
    @RequestMapping("/findQueue")
    public Result findQueue(String cardInfo) {
        List<BankInfo> list = new ArrayList<>();
        if (StrUtil.isEmpty(cardInfo)) {
            List<UserInfo> agentQr = userInfoServiceImpl.findAgentQr();
            for (UserInfo info : agentQr) {
                String queueKey = REDISKEY_QUEUE + info.getUserId();
                LinkedHashSet<ZSetOperations.TypedTuple<Object>> zRangeWithScores = redisUtil.zRangeWithScores(queueKey, 0, -1);//linkedhashset 保证set集合查询最快
                List<ZSetOperations.TypedTuple<Object>> collect = zRangeWithScores.stream().collect(Collectors.toList());
                for (ZSetOperations.TypedTuple type : collect) {
                    Object value = type.getValue();
                    Double score = type.getScore();
                    BankInfo bank = new BankInfo();
                    bank.setBankId(value.toString());
                    bank.setScore(score);
                    bank.setGourp(queueKey);
                    list.add(bank);
                }
            }
        } else {
            String queueKey = REDISKEY_QUEUE + cardInfo;
            LinkedHashSet<ZSetOperations.TypedTuple<Object>> zRangeWithScores = redisUtil.zRangeWithScores(queueKey, 0, -1);//linkedhashset 保证set集合查询最快
            List<ZSetOperations.TypedTuple<Object>> collect = zRangeWithScores.stream().collect(Collectors.toList());
            for (ZSetOperations.TypedTuple type : collect) {
                Object value = type.getValue();
                Double score = type.getScore();
                BankInfo bank = new BankInfo();
                bank.setBankId(value.toString());
                bank.setScore(score);
                bank.setGourp(queueKey);
                list.add(bank);
            }
        }
        return Result.buildSuccessResult("请求成功", list);
    }

    @RequestMapping("/pushCard")
    public Result pushCard(String cardInfo, String userId) {
        if (StrUtil.isEmpty(cardInfo)) {
            return Result.buildFailMessage("数据为空");
        }
        boolean a = redisUtil.zRemove(userId, cardInfo) > 0;
        if (a) {
            Boolean zAdd = redisUtil.zAdd(userId, cardInfo, 0);
        }
        return Result.buildSuccessResult("推送成功");
    }


    @GetMapping("/updatePassword")
    public Result updatePassword(String userId) {
        List<UserInfo> userList = userInfoServiceImpl.finauserAll(userId);
        int a = 0;
        Map map = new HashMap<>();
        for (UserInfo user : userList) {
            List<String> strings = RSAUtils.genKeyPair();
            String publickey = strings.get(0);
            String privactkey = strings.get(1);
            String key = md5(IdUtil.objectId().toUpperCase() + IdUtil.objectId().toUpperCase()).toUpperCase();
            log.info("【商户" + user.getUserId() + ",执行更新密钥方法】");
            boolean flag = userInfoServiceImpl.updateDealKey(user.getUserId(), publickey, privactkey, key);
            if (flag) {
                log.info("【商户" + user.getUserId() + ",执行更新密钥方法,成功】");
            } else {
                a++;
                log.info("【商户" + user.getUserId() + ",执行更新密钥方法,失败】");
                map.put(user.getUserId(), user.getUserId());
            }
        }
        if (a == 0) {
            return Result.buildFailMessage("更新密钥成功");
        }
        return Result.buildFailMessage("更换密钥失败,失败个数,失败商户详情：" + map.toString());
    }
}

class BankInfo {
    private String bankId;
    private String gourp;
    private Double score;

    public String getGourp() {
        return gourp;
    }

    public void setGourp(String gourp) {
        this.gourp = gourp;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }
}
