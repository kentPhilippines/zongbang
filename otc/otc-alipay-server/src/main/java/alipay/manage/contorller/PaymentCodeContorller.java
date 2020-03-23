package alipay.manage.contorller;

import alipay.config.exception.OtherErrors;
import alipay.config.redis.RedisUtil;
import alipay.manage.bean.FileList;
import alipay.manage.bean.Medium;
import alipay.manage.bean.UserInfo;
import alipay.manage.bean.util.OnlineVO;
import alipay.manage.bean.util.PageResult;
import alipay.manage.service.FileListService;
import alipay.manage.service.MediumService;
import alipay.manage.service.UserInfoService;
import alipay.manage.service.impl.CorrelationServiceImpl;
import alipay.manage.util.QueueQrcodeUtil;
import alipay.manage.util.SessionUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import otc.common.RedisConstant;
import otc.exception.user.UserException;
import otc.result.Result;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;
/**
 * 付款码
 */
@Controller
@RequestMapping("/statisticalAnalysis")
public class PaymentCodeContorller {
    Logger log = LoggerFactory.getLogger(PaymentCodeContorller.class);
    @Autowired
    SessionUtil sessionUtil;
    @Autowired
    MediumService mediumServicel;
    @Autowired
    FileListService fileListService;
    @Autowired
    UserInfoService userInfoService;
    @Autowired
    QueueQrcodeUtil queueQrcodeUtil;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    CorrelationServiceImpl correlationService;

    @GetMapping("/findMediumsByPage")
    @ResponseBody
    public Result findMediumsByPage(Medium medium, HttpServletRequest request, String pageNum, String pageSize) {
        UserInfo user = sessionUtil.getUser(request);
        PageHelper.startPage(Integer.valueOf(pageNum), Integer.valueOf(pageSize));
        if (ObjectUtil.isNull(user))
            throw new OtherErrors("当前用户未登录");
        medium.setQrcodeId(user.getUserId());
        List<Medium> list = mediumServicel.findMedium(medium);
        PageInfo<Medium> pageInfo = new PageInfo<Medium>(list);
        PageResult<Medium> pageR = new PageResult<Medium>();
        pageR.setContent(pageInfo.getList());
        pageR.setPageNum(pageInfo.getPageNum());
        pageR.setTotal(pageInfo.getTotal());
        pageR.setTotalPage(pageInfo.getPages());
        return Result.buildFailResult(pageR);
    }

    /**
     * <p>获取与当前登录用户相关的二维码图片账号</p>
     *
     * @param qr
     * @return
     */
    @GetMapping("/findMyGatheringCodeByPage")
    @ResponseBody
    public Result findMyGatheringCodeByPage(FileList qr, HttpServletRequest request, String pageNum, String pageSize) {
        UserInfo user = sessionUtil.getUser(request);
        PageHelper.startPage(Integer.valueOf(pageNum), Integer.valueOf(pageSize));
        if (ObjectUtil.isNull(user))
            throw new OtherErrors("当前用户未登录");
        qr.setFileholder(user.getUserId());
        List<FileList> list = fileListService.findQrPage(qr);
        PageInfo<FileList> pageInfo = new PageInfo<FileList>(list);
        PageResult<FileList> pageR = new PageResult<FileList>();
        pageR.setContent(pageInfo.getList());
        pageR.setPageNum(pageInfo.getPageNum());
        pageR.setTotal(pageInfo.getTotal());
        pageR.setTotalPage(pageInfo.getPages());
        return Result.buildSuccessResult(pageR);
    }

    /**
     * <p>获取当前收款媒介下所有的二维码</p>
     * @param mediumId
     * @param request
     * @return
     */
    @PostMapping("/findQrByMediumId")
    @ResponseBody
    public Result findQrByMediumId(@RequestBody String mediumId, HttpServletRequest request) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user))
            throw new OtherErrors("当前用户未登录");
        log.info("接受的介质参数为：" + mediumId);
        List<FileList> qrList = fileListService.findQrByMediumId(mediumId);
        log.info("获取结果集合 " + qrList);
        return Result.buildSuccessResult(qrList);
    }

    /**
     * 根据媒介ID查询当前媒介详情
     * @param medium
     * @param request
     * @return
     */
    @GetMapping("/findMyMediumById")
    @ResponseBody
    public Result findMyMediumById(Medium medium, HttpServletRequest request) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user))
            throw new OtherErrors("当前用户未登录");
        Medium mediumBean = mediumServicel.findMediumById(medium.getMediumId());
        return Result.buildSuccessResult(mediumBean);
    }

    /**
     * 修改媒介详情
     * @param medium
     * @param request
     * @return
     */
    @PostMapping("/editMedium")
    @ResponseBody
    public Result editMedium(@RequestBody Medium medium,HttpServletRequest request){
       UserInfo user=sessionUtil.getUser(request);
       if (ObjectUtil.isNull(user))
           throw new UserException("当前用户未登录",null);
       Medium oldMedium=mediumServicel.findMediumById(medium.getMediumId());
       if(ObjectUtil.isNull(oldMedium))
           throw new UserException("获取用户信息为null",null);
       if(queueQrcodeUtil.getList().contains(oldMedium.getMediumNumber()))
          return Result.buildFailResult("当前收款媒介正在接单排队，禁止操作");
       if (oldMedium.getMediumNumber().equals(medium.getMediumNumber())&&ObjectUtil.isNotNull(mediumServicel.findMediumByMediumNumber(medium.getMediumNumber())))
           return Result.buildFailResult("修改账号重复");
        Boolean mediumBean = mediumServicel.updataMediumById(medium);
        if (mediumBean)
            return  Result.buildSuccessResult();
        return Result.buildFailResult("修改账号重复或其他原因");
    }

    /**
     * <p>添加收款媒介</p>
     * @param medium
     * @param request
     * @return
     */
    @PostMapping("/addMedium")
    @ResponseBody
    public Result addMedium(@RequestBody Medium medium, HttpServletRequest request){
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user))
            throw new UserException("未获取到登录用户",null);
        medium.setQrcodeId(user.getUserId());
        if (queueQrcodeUtil.getList().contains(medium.getMediumNumber()))
            return Result.buildFailResult("当前收款媒介正在接单排队，禁止操作");
        boolean flag = mediumServicel.addMedium(medium);
     /*
        if (!redisUtil.hasKey(medium.getMediumNumber() + RedisConstant.User.QUEUEQRNODE)) {
            queueQrcodeUtil.addNode(medium.getMediumNumber());
            redisUtil.set(medium.getMediumNumber() + RedisConstant.User.QUEUEQRNODE, medium.getMediumNumber());//支付宝放入标记
        }
        */
        if (flag) {
            return Result.buildSuccessResult();
        }
        return Result.buildFailResult("支付宝账户重复或其他原因");
    }

    /**
     * 删除一个收款媒介
     * @param request
     * @return
     */
    @GetMapping("/delMedium")
    @ResponseBody
    public Result delMedium(HttpServletRequest request,String mediumId){
        UserInfo user = sessionUtil.getUser(request);
        if(ObjectUtil.isNull(user))
            throw new UserException("当前用户未登录",null);
        log.info("当前删除二维码编号："+mediumId);
        Medium findMediumById = mediumServicel.findMediumById(mediumId);
        if (queueQrcodeUtil.getList().contains(findMediumById.getMediumNumber()))
            return Result.buildFailResult("当前收款媒介正在接单排队，禁止操作");
        /**逻辑   1,删除媒介  2,删除二维码  */
        Boolean mFlag = mediumServicel.deleteMedium(mediumId);
        Boolean qflag = fileListService.deleteQrByMediumId(mediumId);
        UserInfo user2 = userInfoService.getUser(findMediumById.getQrcodeId());
        correlationService.deleteAccountMedium(user2.getUserId(), user2.getUserId(), findMediumById.getId());
        if (mFlag && qflag)
            return Result.buildFail();
        return Result.buildFail();
    }
    /**
     * <p>添加一个二维码</p>
     *
     * @param mediumId
     * @param request
     * @return
     */
    @GetMapping("/addQrInfo")
    @ResponseBody
    public Result addQrInfo(HttpServletRequest request, String qrcodeId, String mediumId, String amount, String flag) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user))
            throw new OtherErrors("当前用户未登录");
        if (StrUtil.isBlank(mediumId) || StrUtil.isBlank(qrcodeId))
            return Result.buildFailResult("必传参数为空");
        Result result = userInfoService.addQrByMedium(qrcodeId, mediumId, amount, user.getUserId(), flag);
        if (result.isSuccess())
            return Result.buildSuccessResult();
        return result;
    }

    /**
     * 上级查询下级在线人数
     *
     * @param request
     * @return
     */
    @GetMapping("/querySubOnline")
    @ResponseBody
    public Result querySubOnline(HttpServletRequest request) {
        UserInfo user = sessionUtil.getUser(request);
        if (ObjectUtil.isNull(user)) {
            throw new UserException("未获取到登录用户",null);
        }
        //登陆状态的Key
        Set<String> loginKeys = redisUtil.keys(RedisConstant.User.LOGIN_PARENT + "*");
        log.info("loginKeys ===>"+ loginKeys);

        //获取所有key的值
        List<Object> loginKeysValue = redisUtil.multiGet(loginKeys);
        log.info("loginKeysValue ===>"+ loginKeysValue);
        //查询用户的下级用户
        List<String> subLevelMembers = userInfoService.findSubLevelMembers(user.getUserId());
        log.info("subLevelMembers ===>"+ subLevelMembers);
        String str = CollUtil.getFirst(subLevelMembers);
        log.info("str ===>"+ str);
        //两个集合的交集 在线人员集合
        List<String> loginMembers = subLevelMembers.stream().filter(item -> loginKeysValue.contains(item)).collect(toList());
        log.info("loginMembers ===>"+ loginMembers);
        //接单状态队列
        Set<String> bizKeyMembers = redisUtil.keys(RedisConstant.User.BIZ_QUEUE + "*");
        log.info("bizKeyMembers ===>"+ bizKeyMembers);
        //获取所有key的值
        List<Object> bizMembersValue = redisUtil.multiGet(bizKeyMembers);
        log.info("bizMembersValue ===>"+ bizMembersValue);
        //取两个集合的交集 接单人员集合
        List<String> bizingMembers = subLevelMembers.stream().filter(item -> bizMembersValue.contains(item)).collect(toList());
        log.info("bizingMembers ===>"+ bizingMembers);

        OnlineVO onlineVO = new OnlineVO();
        onlineVO.setLoginOnlineCount(loginMembers.size());
        onlineVO.setBizOnlineCount(bizingMembers.size());
        onlineVO.setOnlineList(loginMembers.size() == 0 ? "" : StringUtils.join(loginMembers.toArray(), "，"));
        onlineVO.setBizList(bizingMembers.size() == 0 ? "" : StringUtils.join(bizingMembers.toArray(), "，"));
        onlineVO.setIsAgent(user.getIsAgent());
        return Result.buildSuccessResult("数据获取成功", onlineVO);
    }
}