package cn.daxpay.open.channel.wechat.service.direct;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatAllocReceiverReq;
import cn.daxpay.open.channel.wechat.resp.WechatAllocReceiverResp;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingReceiverV3Request;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 微信直连分账接收方服务
///
/// 接收方通道侧注册/删除(V3 profitsharing/receivers/add / delete), 经 WxJava 调用。
/// 状态回写与失败处理见主应用 [cn.daxpay.open.channel.wechat.service.direct.WechatDirectAllocReceiverService]。
/// 绑定/解绑均为同步调用, 失败填充错误信息由主应用决策。
/// 解绑遇"接收方不存在"视为幂等成功(对齐支付宝 USER_NOT_EXIST 容错)。
@Slf4j
@Service
public class WechatDirectAllocReceiverService {

    /// 微信"资源不存在"类错误码(解绑幂等容错)
    private static final String ERR_CODE_NOT_FOUND = "NOT_FOUND";
    private static final String ERR_CODE_RESOURCE_NOT_EXISTS = "RESOURCE_NOT_EXISTS";

    /// 添加分账接收方(V3 profitsharing/receivers/add)
    public WechatAllocReceiverResp bind(WechatAllocReceiverReq req) {
        WxPayService wxPayService = WechatSdkConfig.buildService(req.getCredential());
        ProfitSharingReceiverV3Request v3Req = new ProfitSharingReceiverV3Request();
        // 直连身份: 商户应用 appid
        v3Req.setAppid(req.getCredential().getWxAppId());
        this.fillReceiver(v3Req, req);
        try {
            wxPayService.getProfitSharingService().addReceiverV3(v3Req);
            return new WechatAllocReceiverResp();
        } catch (WxPayException e) {
            log.error("微信分账接收方绑定失败: account={}", req.getReceiverAccount(), e);
            WechatAllocReceiverResp resp = new WechatAllocReceiverResp();
            resp.setErrorCode(e.getErrCode());
            resp.setErrorMsg(StrUtil.blankToDefault(e.getErrCodeDes(), e.getMessage()));
            return resp;
        }
    }

    /// 删除分账接收方(V3 profitsharing/receivers/delete)
    public WechatAllocReceiverResp unbind(WechatAllocReceiverReq req) {
        WxPayService wxPayService = WechatSdkConfig.buildService(req.getCredential());
        ProfitSharingReceiverV3Request v3Req = new ProfitSharingReceiverV3Request();
        v3Req.setAppid(req.getCredential().getWxAppId());
        this.fillReceiver(v3Req, req);
        try {
            wxPayService.getProfitSharingService().removeReceiverV3(v3Req);
            return new WechatAllocReceiverResp();
        } catch (WxPayException e) {
            // 分账接收方不存在视为幂等成功(通道侧已无绑定关系)
            if (this.isReceiverNotExist(e)) {
                log.info("微信分账接收方解绑幂等成功(接收方不存在): account={}", req.getReceiverAccount());
                return new WechatAllocReceiverResp();
            }
            log.error("微信分账接收方解绑失败: account={}", req.getReceiverAccount(), e);
            WechatAllocReceiverResp resp = new WechatAllocReceiverResp();
            resp.setErrorCode(e.getErrCode());
            resp.setErrorMsg(StrUtil.blankToDefault(e.getErrCodeDes(), e.getMessage()));
            return resp;
        }
    }

    /// 是否为"分账接收方不存在"(解绑幂等容错)
    ///
    /// 微信对该场景无文档化错误码, 采用错误码精确匹配 + 描述组合匹配:
    /// 描述须同时含"接收方/receiver"与"不存在/not exist"语义, 避免误吞"商户不存在"等其他错误。
    private boolean isReceiverNotExist(WxPayException e) {
        String code = e.getErrCode();
        if (ERR_CODE_NOT_FOUND.equals(code) || ERR_CODE_RESOURCE_NOT_EXISTS.equals(code)) {
            return true;
        }
        String text = StrUtil.blankToDefault(e.getErrCodeDes(), e.getMessage());
        boolean cnHit = text.contains("接收方") && text.contains("不存在");
        String lower = text.toLowerCase();
        boolean enHit = lower.contains("receiver") && lower.contains("not exist");
        return cnHit || enHit;
    }

    /// 填充接收方公共字段(类型/账号/名称/关系)
    ///
    /// 关系类型映射: 微信 V3 原生为大写; service_provider 为服务商保留值,
    /// 映射为 CUSTOM + 自定义关系名"服务商"(对齐商业版行为), 其余大写直传。
    private void fillReceiver(ProfitSharingReceiverV3Request v3Req, WechatAllocReceiverReq req) {
        v3Req.setType(req.getReceiverType());
        v3Req.setAccount(req.getReceiverAccount());
        if (StrUtil.isNotBlank(req.getReceiverName())) {
            v3Req.setName(req.getReceiverName());
        }
        if ("service_provider".equals(req.getRelationType())) {
            v3Req.setRelationType("CUSTOM");
            v3Req.setCustomRelation(StrUtil.blankToDefault(req.getCustomRelation(), "服务商"));
        } else {
            v3Req.setRelationType(req.getRelationType().toUpperCase());
            if (StrUtil.isNotBlank(req.getCustomRelation())) {
                v3Req.setCustomRelation(req.getCustomRelation());
            }
        }
    }
}
