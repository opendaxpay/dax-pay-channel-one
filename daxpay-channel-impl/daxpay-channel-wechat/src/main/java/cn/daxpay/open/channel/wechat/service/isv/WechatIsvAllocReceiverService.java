package cn.daxpay.open.channel.wechat.service.isv;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatAllocReceiverReq;
import cn.daxpay.open.channel.wechat.resp.WechatAllocReceiverResp;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.profitsharing.request.ProfitSharingReceiverV3Request;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 微信服务商分账接收方服务
///
/// 接收方通道侧注册/删除(V3 profitsharing/receivers/add / delete, 服务商模式),
/// 请求体携带 sub_mchid/sub_appid(特约商户维度), 经 WxJava 调用。
/// 状态回写与失败处理见主应用 [cn.daxpay.open.channel.wechat.service.isv.WechatIsvAllocReceiverService]。
///
/// 注: 微信 ISV 的分账**执行**链路(profitsharing/orders)当前未接, 本服务仅覆盖接收方注册。
@Slf4j
@Service
public class WechatIsvAllocReceiverService {

    /// 添加分账接收方(服务商, sub_mchid 维度)
    public WechatAllocReceiverResp bind(WechatAllocReceiverReq req) {
        WxPayService wxPayService = WechatSdkConfig.buildService(req.getCredential());
        ProfitSharingReceiverV3Request v3Req = new ProfitSharingReceiverV3Request();
        // 服务商身份: sp_appid(凭证 wxAppId) + sub_mchid + sub_appid(可选)
        v3Req.setAppid(req.getCredential().getWxAppId());
        v3Req.setSubMchId(req.getCredential().getSubMchId());
        if (StrUtil.isNotBlank(req.getCredential().getSubAppId())) {
            v3Req.setSubAppid(req.getCredential().getSubAppId());
        }
        this.fillReceiver(v3Req, req);
        try {
            wxPayService.getProfitSharingService().addReceiverV3(v3Req);
            return new WechatAllocReceiverResp();
        } catch (WxPayException e) {
            log.error("微信服务商分账接收方绑定失败: subMchId={}, account={}",
                    req.getCredential().getSubMchId(), req.getReceiverAccount(), e);
            WechatAllocReceiverResp resp = new WechatAllocReceiverResp();
            resp.setErrorCode(e.getErrCode());
            resp.setErrorMsg(StrUtil.blankToDefault(e.getErrCodeDes(), e.getMessage()));
            return resp;
        }
    }

    /// 删除分账接收方(服务商, sub_mchid 维度)
    public WechatAllocReceiverResp unbind(WechatAllocReceiverReq req) {
        WxPayService wxPayService = WechatSdkConfig.buildService(req.getCredential());
        ProfitSharingReceiverV3Request v3Req = new ProfitSharingReceiverV3Request();
        v3Req.setAppid(req.getCredential().getWxAppId());
        v3Req.setSubMchId(req.getCredential().getSubMchId());
        if (StrUtil.isNotBlank(req.getCredential().getSubAppId())) {
            v3Req.setSubAppid(req.getCredential().getSubAppId());
        }
        this.fillReceiver(v3Req, req);
        try {
            wxPayService.getProfitSharingService().removeReceiverV3(v3Req);
            return new WechatAllocReceiverResp();
        } catch (WxPayException e) {
            log.error("微信服务商分账接收方解绑失败: subMchId={}, account={}",
                    req.getCredential().getSubMchId(), req.getReceiverAccount(), e);
            WechatAllocReceiverResp resp = new WechatAllocReceiverResp();
            resp.setErrorCode(e.getErrCode());
            resp.setErrorMsg(StrUtil.blankToDefault(e.getErrCodeDes(), e.getMessage()));
            return resp;
        }
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
