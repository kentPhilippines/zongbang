package alipay.manage.contorller;

import alipay.config.redis.RedisUtil;
import alipay.manage.bean.DealOrder;
import alipay.manage.bean.UserInfo;
import alipay.manage.bean.util.PageResult;
import alipay.manage.service.MediumService;
import alipay.manage.service.OrderService;
import alipay.manage.service.WithdrawService;
import alipay.manage.util.QueueUtil;
import alipay.manage.util.SessionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import otc.bean.alipay.Medium;
import otc.bean.dealpay.Withdraw;
import otc.common.PayApiConstant;
import otc.common.RedisConstant;
import otc.result.Result;
import otc.util.RSAUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/qrcode")
public class QrcodeContorller {
    Logger log= LoggerFactory.getLogger(QrcodeContorller.class);
    private static final String MARS = "SHENFU";
    private static final String MARK = ":";
    @Autowired
    private SessionUtil sessionUtil;
    @Autowired
    private MediumService mediumServiceImpl;
    @Autowired
    WithdrawService withdrawServiceImpl;
    @GetMapping("/findIsMyQrcodePage")
    @ResponseBody
    public Result findIsMyQrcodePage(HttpServletRequest request, String pageNum, String pageSize) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user)) {
            return Result.buildFailResult("用户未登录");
        }
        PageHelper.startPage(Integer.valueOf(pageNum), Integer.valueOf(pageSize));
        List<Medium> qmList = mediumServiceImpl.findIsMyMediumPage(user.getUserId());
        //List<QrCode> qrList = qrCodeServiceImpl.findIsMyQrcodePage(qrcode);
        PageInfo<Medium> pageInfo = new PageInfo<Medium>(qmList);
        PageResult<Medium> pageR = new PageResult<Medium>();
        pageR.setContent(pageInfo.getList());
        pageR.setPageNum(pageInfo.getPageNum());
        pageR.setTotal(pageInfo.getTotal());
        pageR.setTotalPage(pageInfo.getPages());
        return Result.buildSuccessResult(pageR);
    }
    /**
     * <p>远程队列入列</p>
     * @param request
     * @param id
     * @return
     */
    @GetMapping("/updataMediumStatusSu")
    @ResponseBody
    public Result updataMediumStatusSu(HttpServletRequest request, String id) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user)) {
            return Result.buildFailResult("用户未登录");
        }
        Medium med = new Medium();
        med.setId(Integer.valueOf(id));
        Result addNode = queueUtil.addNode(med);
        return addNode;
    }

    @Autowired
    private QueueUtil queueUtil;
    @Autowired
    private OrderService orderServiceImpl;
    @Autowired
    private WithdrawService withdrawService;
    @Autowired
    private RedisUtil redis;

    @GetMapping("/getBankCardList")
    @ResponseBody
    public Result getBankCardList(HttpServletRequest request, String id) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user)) {
            return Result.buildFailResult("用户未登录");
        }
        List<Medium> isMyMediumPage = mediumServiceImpl.findIsMyMediumPage(user.getUserId());
        return Result.buildSuccessResult(isMyMediumPage);
    }

    /**
     * <p>远程队列出列</p>
     *
     * @param request
     * @param id
     * @return
     */
    @GetMapping("/updataMediumStatusEr")
    @ResponseBody
    public Result updataMediumStatusEr(HttpServletRequest request, String id) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user)) {
            return Result.buildFailResult("用户未登录");
        }
        Medium med = new Medium();
        med.setId(Integer.valueOf(id));
        Result addNode = queueUtil.pop(med);
        return addNode;
    }

    @Autowired
    private RedisUtil redisUtil;
    @GetMapping("/setBankCard")
    @ResponseBody
    public Result setBankCard(HttpServletRequest request, String bankCard, String orderId) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user)) {
            return Result.buildFailResult("用户未登录");
        }
        log.info("银行卡号："+bankCard);
        try {
            DealOrder order = orderServiceImpl.findOrderByOrderId(orderId);
            String mediumNumber = "";
            String mediumHolder = "";
            String account = "";
            String mediumPhone = "";
            Withdraw wit = withdrawServiceImpl.findOrderId(order.getAssociatedId());
            if (StrUtil.isEmpty(order.getOrderQr())) {// 第一次进入绑定 银行卡， 这个地方需要 验证 银行卡是否在线
                if(StrUtil.isEmpty(bankCard)){
                    bankCard =order.getOrderQr().split(MARK)[2];
                }

                Result result = enterWit(order, bankCard, orderId);
                if(!result.isSuccess()){
                    return  result;
                }
            } else {
                String[] split = order.getOrderQr().split(MARK);
                mediumNumber = split[2];//卡号
                mediumHolder = split[1];//开户人
                account = split[0];//开户行
                Medium mediumByMediumNumber = mediumServiceImpl.findMediumByMediumNumber(mediumNumber);
                Result result = enterWit(order, mediumByMediumNumber.getId().toString(), orderId);
                if(!result.isSuccess()){
                    return  result;
                }
            }
            Map cardmap = new HashMap();
            cardmap.put("bank_name", wit.getBankName());
            cardmap.put("card_no", wit.getBankNo());
            cardmap.put("card_user", wit.getAccname());
            cardmap.put("money_order", order.getDealAmount());
            cardmap.put("no_order", orderId);
            cardmap.put("oid_partner", orderId);
            redis.hmset(MARS + orderId, cardmap, 6000);
        }catch (Throwable t ){
            log.error("选卡异常",t);
            log.info("出款信息异常，当前订单号："+orderId);
        }
        try {
            ThreadUtil.execute(()->{
                log.info("出款卡锁定，当前订单号："+orderId);
                orderServiceImpl.enterOrderLock( orderId);
            });
        }catch (Throwable e ){
            log.info("出款卡锁定异常");
        }
        return Result.buildSuccessResult(PayApiConstant.Notfiy.OTHER_URL + "/pay?orderId=" + orderId + "&type=203");
    }


    String getAmount(BigDecimal dealAmount) {
        String amount = "";
        String[] split = dealAmount.toString().split("\\.");
        if (split.length == 1) {
            String s = dealAmount.toString();
            s += ".0";
            split = s.split("\\.");
        }
        String startAmount = split[0];
        String endAmount = split[1];
        int length = endAmount.length();
        if (length == 1) {//当交易金额为整小数的时候        补充0
            endAmount += "0";
        } else if (endAmount.length() > 2) {
            endAmount = "00";
        }
        amount = startAmount + "." + endAmount;//得到正确的金额
        return amount;
    }





    Result  enterWit( DealOrder order ,String bankCard,String orderId){
        String mediumNumber = "";
        String mediumHolder = "";
        String account = "";
        String mediumPhone = "";
        Medium mediumId = mediumServiceImpl.findMediumId(bankCard);
        mediumNumber = mediumId.getMediumNumber();//卡号
        mediumHolder = mediumId.getMediumHolder();//开户人
        account = mediumId.getAccount();//开户行
        mediumPhone = mediumId.getMediumPhone();
        String bankInfo = "";

        String isWit = mediumNumber + mediumPhone + getAmount(order.getDealAmount());

        boolean b1 = redisUtil.hasKey(isWit);
        if (b1) {
            return Result.buildFailMessage("当前银行卡限制出款，请等待");
        }
        String bankCheck = RSAUtils.md5(RedisConstant.Queue.HEARTBEAT + mediumNumber);// 验证银行 卡在线标记
        boolean hasKey = redisUtil.hasKey(bankCheck);
        if (hasKey) {
            return Result.buildFailMessage("当前银行卡未绑定监控，无法出款");
        }
        String amount1 = getAmount(order.getDealAmount());
        String witNotify1 = mediumNumber + mediumPhone + amount1 ; //验证当前 银行卡是否处于出款状态
        Object o = redisUtil.get("WIT:" + witNotify1);
        if (null != o) {
            return Result.buildFailMessage("当前银行卡 正在出款， 请更换银行卡出款");
        }
        bankInfo = account + MARK + mediumHolder + MARK + mediumNumber + MARK + "电话" + MARK + mediumPhone;
        boolean b = orderServiceImpl.updateBankInfoByOrderId(bankInfo, orderId);
        if (b) {
            String amount = getAmount(order.getDealAmount());
            String witNotify = mediumNumber + mediumPhone + amount; //代付回调成功 标记
            redisUtil.set("WIT:" + witNotify, order.getOrderId(), 600);
        }

        return Result.buildSuccess();
    }
}
