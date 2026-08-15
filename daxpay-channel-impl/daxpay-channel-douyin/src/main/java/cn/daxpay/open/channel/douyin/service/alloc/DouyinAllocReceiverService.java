package cn.daxpay.open.channel.douyin.service.alloc;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinAllocReceiverReq;
import cn.daxpay.open.channel.douyin.resp.DouyinAllocReceiverResp;
import cn.hutool.core.util.StrUtil;
import com.douyinpay.api.splitfund.ApiSplitFundPaymentsService;
import com.douyinpay.api.splitfund.models.ApiAddSplitReceiverRequest;
import com.douyinpay.api.splitfund.models.ApiDeleteSplitReceiverRequest;
import com.douyinpay.exception.DouyinpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 抖音通道分账接收方服务
///
/// 接收方通道侧注册/删除(addSplitReceiver / deleteSplitReceiver), 经 SDK [ApiSplitFundPaymentsService] 调用。
/// 状态回写与失败处理见主应用 [cn.daxpay.open.channel.douyin.service.direct.DouyinDirectAllocReceiverService]。
/// 绑定/解绑均为同步调用, 失败填充错误信息由主应用决策。
@Slf4j
@Service
public class DouyinAllocReceiverService {

    /// 添加分账接收方(addSplitReceiver)
    public DouyinAllocReceiverResp bind(DouyinAllocReceiverReq req) {
        ApiSplitFundPaymentsService service = new ApiSplitFundPaymentsService.Builder()
                .douyinpayClient(DouyinSdkConfig.buildClient(req.getCredential()))
                .build();
        ApiAddSplitReceiverRequest apiReq = new ApiAddSplitReceiverRequest();
        apiReq.setMerchantId(req.getCredential().getMchId());
        apiReq.setAppId(req.getCredential().getDouyinAppId());
        apiReq.setType(req.getReceiverType());
        apiReq.setAccount(req.getReceiverAccount());
        if (StrUtil.isNotBlank(req.getReceiverName())) {
            apiReq.setName(req.getReceiverName());
        }
        apiReq.setRelationType(this.toDouyinRelationType(req.getRelationType()));
        if (StrUtil.isNotBlank(req.getCustomRelation())) {
            apiReq.setCustomRelation(req.getCustomRelation());
        }
        try {
            service.addSplitReceiver(apiReq);
            return new DouyinAllocReceiverResp();
        } catch (DouyinpayException e) {
            log.error("抖音分账接收方绑定失败: account={}", req.getReceiverAccount(), e);
            DouyinAllocReceiverResp resp = new DouyinAllocReceiverResp();
            resp.setErrorCode(e.getMessage());
            resp.setErrorMsg(e.getMessage());
            return resp;
        }
    }

    /// 删除分账接收方(deleteSplitReceiver)
    public DouyinAllocReceiverResp unbind(DouyinAllocReceiverReq req) {
        ApiSplitFundPaymentsService service = new ApiSplitFundPaymentsService.Builder()
                .douyinpayClient(DouyinSdkConfig.buildClient(req.getCredential()))
                .build();
        ApiDeleteSplitReceiverRequest apiReq = new ApiDeleteSplitReceiverRequest();
        apiReq.setMerchantId(req.getCredential().getMchId());
        apiReq.setAppId(req.getCredential().getDouyinAppId());
        apiReq.setType(req.getReceiverType());
        apiReq.setAccount(req.getReceiverAccount());
        try {
            service.deleteSplitReceiver(apiReq);
            return new DouyinAllocReceiverResp();
        } catch (DouyinpayException e) {
            log.error("抖音分账接收方解绑失败: account={}", req.getReceiverAccount(), e);
            DouyinAllocReceiverResp resp = new DouyinAllocReceiverResp();
            resp.setErrorCode(e.getMessage());
            resp.setErrorMsg(e.getMessage());
            return resp;
        }
    }

    /// 平台小写关系类型转抖音原生大写
    private String toDouyinRelationType(String relationType) {
        return relationType.toUpperCase();
    }
}
