package cn.daxpay.open.channel.alipay.service.alloc;

import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipayAllocReceiverReq;
import cn.daxpay.open.channel.alipay.resp.AlipayAllocReceiverResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.request.AlipayTradeRoyaltyRelationBindRequest;
import com.alipay.api.request.AlipayTradeRoyaltyRelationUnbindRequest;
import com.alipay.api.response.AlipayTradeRoyaltyRelationBindResponse;
import com.alipay.api.response.AlipayTradeRoyaltyRelationUnbindResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// # 支付宝通道分账接收方服务
///
/// 接收方通道侧注册/删除(alipay.trade.royalty.relation.bind / unbind)。
/// 状态回写与失败处理见主应用 [cn.daxpay.open.channel.alipay.service.direct.AlipayDirectAllocReceiverService]。
///
/// 直连与服务商共用本服务: 服务商凭证含 appAuthToken 时经
/// putOtherTextParam 以子商户身份代调用(与支付链路一致)。
/// 解绑遇 USER_NOT_EXIST(分账方不存在)视为幂等成功(对齐商业版容错)。
@Slf4j
@Service
public class AlipayAllocReceiverService {

    /// 支付宝接收方不存在错误码(解绑幂等容错)
    private static final String SUB_CODE_USER_NOT_EXIST = "USER_NOT_EXIST";

    /// 绑定分账接收方(alipay.trade.royalty.relation.bind)
    public AlipayAllocReceiverResp bind(AlipayAllocReceiverReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        JSONObject bizContent = new JSONObject();
        // 幂等请求号(绑定记录 id, 重试同号幂等)
        bizContent.set("out_request_no", req.getOutRequestNo());
        JSONArray receiverList = new JSONArray();
        JSONObject receiver = new JSONObject();
        // 平台大写枚举映射支付宝原生小写类型(userId/loginName)
        receiver.set("type", this.toAlipayReceiverType(req.getReceiverType()));
        receiver.set("account", req.getReceiverAccount());
        if (StrUtil.isNotBlank(req.getReceiverName())) {
            receiver.set("name", req.getReceiverName());
        }
        receiverList.add(receiver);
        bizContent.set("receiver_list", receiverList);

        AlipayTradeRoyaltyRelationBindRequest request = new AlipayTradeRoyaltyRelationBindRequest();
        request.setBizContent(JSONUtil.toJsonStr(bizContent));
        // 服务商模式: 应用授权令牌以子商户身份代调用
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        try {
            AlipayTradeRoyaltyRelationBindResponse response =
                    AlipaySdkConfig.execute(client, req.getCredential(), request);
            AlipayAllocReceiverResp resp = new AlipayAllocReceiverResp();
            resp.setErrorCode(response.getSubCode());
            resp.setErrorMsg(response.getSubMsg());
            if (!response.isSuccess()) {
                log.warn("支付宝分账接收方绑定业务失败: outRequestNo={}, subCode={}, subMsg={}",
                        req.getOutRequestNo(), response.getSubCode(), response.getSubMsg());
            }
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝分账接收方绑定调用失败: outRequestNo={}", req.getOutRequestNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayAllocFailed", e.getMessage());
        }
    }

    /// 解绑分账接收方(alipay.trade.royalty.relation.unbind)
    public AlipayAllocReceiverResp unbind(AlipayAllocReceiverReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        JSONObject bizContent = new JSONObject();
        bizContent.set("out_request_no", req.getOutRequestNo());
        JSONArray receiverList = new JSONArray();
        JSONObject receiver = new JSONObject();
        receiver.set("type", this.toAlipayReceiverType(req.getReceiverType()));
        receiver.set("account", req.getReceiverAccount());
        receiverList.add(receiver);
        bizContent.set("receiver_list", receiverList);

        AlipayTradeRoyaltyRelationUnbindRequest request = new AlipayTradeRoyaltyRelationUnbindRequest();
        request.setBizContent(JSONUtil.toJsonStr(bizContent));
        // 服务商模式: 应用授权令牌以子商户身份代调用
        if (StrUtil.isNotBlank(req.getCredential().getAppAuthToken())) {
            request.putOtherTextParam(AlipayConstants.APP_AUTH_TOKEN, req.getCredential().getAppAuthToken());
        }
        try {
            AlipayTradeRoyaltyRelationUnbindResponse response =
                    AlipaySdkConfig.execute(client, req.getCredential(), request);
            AlipayAllocReceiverResp resp = new AlipayAllocReceiverResp();
            if (!response.isSuccess()) {
                // 分账方不存在视为幂等成功(通道侧已不存在绑定关系)
                if (SUB_CODE_USER_NOT_EXIST.equals(response.getSubCode())) {
                    log.info("支付宝分账接收方解绑幂等成功(分账方不存在): outRequestNo={}", req.getOutRequestNo());
                    return resp;
                }
                resp.setErrorCode(response.getSubCode());
                resp.setErrorMsg(response.getSubMsg());
                log.warn("支付宝分账接收方解绑业务失败: outRequestNo={}, subCode={}, subMsg={}",
                        req.getOutRequestNo(), response.getSubCode(), response.getSubMsg());
            }
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝分账接收方解绑调用失败: outRequestNo={}", req.getOutRequestNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayAllocFailed", e.getMessage());
        }
    }

    /// 平台接收方类型映射支付宝原生小写类型
    private String toAlipayReceiverType(String receiverType) {
        if ("USER_ID".equals(receiverType)) {
            return "userId";
        }
        if ("LOGIN_NAME".equals(receiverType)) {
            return "loginName";
        }
        return receiverType;
    }
}
