package alipay.config.listener;

import alipay.manage.api.feign.QueueServiceClien;
import alipay.manage.bean.UserInfo;
import alipay.manage.service.UserInfoService;
import cn.hutool.core.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class EnvironmentConfig implements EnvironmentAware {
    Logger log = LoggerFactory.getLogger(EnvironmentConfig.class);

    @Autowired
    private QueueServiceClien queueServiceClienFeignImpl;
    @Autowired
    private UserInfoService userInfoServiceImpl;

    /**
     * 初始化队列数据
     *
     * @param environment
     */
    @Override
    public void setEnvironment(Environment environment) {

        List<UserInfo> list = userInfoServiceImpl.findAgentQr();
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(10000);
            List<String> srt = new ArrayList<>();
            for (int a = 0; a < list.size(); a++) {
                srt.add(list.get(a).getUserId());
                queueServiceClienFeignImpl.getQueue(srt);
            }
        });
        log.info("项目启动正常，初始化完成");
    }
}
