package cn.daxpay.open.channel.alipay.service.transfer;

import cn.daxpay.open.channel.alipay.config.AlipaySdkConfig;
import cn.daxpay.open.channel.alipay.req.AlipayTransferReq;
import cn.daxpay.open.channel.alipay.resp.AlipayTransferResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayFundTransCommonQueryRequest;
import com.alipay.api.request.AlipayFundTransUniTransferRequest;
import com.alipay.api.response.AlipayFundTransCommonQueryResponse;
import com.alipay.api.response.AlipayFundTransUniTransferResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// # 支付宝通道转账服务
///
/// 单笔转账(alipay.fund.trans.uni.transfer)与转账查询(alipay.fund.trans.common.query)。
/// 状态映射见主应用 [cn.daxpay.open.channel.alipay.service.payment.transfer.AlipayTransferService]。
/// 金额单位: 分 → 元字符串。
@Slf4j
@Service
public class AlipayTransferService {

    /// 转账产品码(转账到支付宝账号)
    private static final String PRODUCT_CODE = "TRANS_ACCOUNT_NO_PWD";
    /// 转账业务场景(直接转账)
    private static final String BIZ_SCENE = "DIRECT_TRANSFER";

    /// 发起单笔转账
    public AlipayTransferResp transfer(AlipayTransferReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        AlipayFundTransUniTransferRequest request = new AlipayFundTransUniTransferRequest();
        request.setBizContent(JSONUtil.toJsonStr(buildTransferBizContent(req)));
        try {
            AlipayFundTransUniTransferResponse response =
                    AlipaySdkConfig.execute(client, req.getCredential(), request);
            AlipayTransferResp resp = new AlipayTransferResp();
            resp.setOrderId(response.getOrderId());
            resp.setStatus(response.getStatus());
            resp.setFailReason(response.getSubMsg());
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝转账调用失败: outBizNo={}", req.getOutBizNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayTransferFailed", e.getMessage());
        }
    }

    /// 同步查询转账状态
    public AlipayTransferResp sync(AlipayTransferReq req) {
        AlipayClient client = AlipaySdkConfig.buildClient(req.getCredential());
        AlipayFundTransCommonQueryRequest request = new AlipayFundTransCommonQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.set("product_code", PRODUCT_CODE);
        bizContent.set("biz_scene", BIZ_SCENE);
        bizContent.set("out_biz_no", req.getOutBizNo());
        request.setBizContent(JSONUtil.toJsonStr(bizContent));
        try {
            AlipayFundTransCommonQueryResponse response =
                    AlipaySdkConfig.execute(client, req.getCredential(), request);
            AlipayTransferResp resp = new AlipayTransferResp();
            resp.setOrderId(response.getOrderId());
            resp.setStatus(response.getStatus());
            resp.setFailReason(response.getFailReason());
            resp.setFinishTime(response.getPayDate());
            return resp;
        } catch (AlipayApiException e) {
            log.error("支付宝转账查询失败: outBizNo={}", req.getOutBizNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.alipayTransferQueryFailed", e.getMessage());
        }
    }

    /// 构建转账 biz_content
    private JSONObject buildTransferBizContent(AlipayTransferReq req) {
        JSONObject bizContent = new JSONObject();
        bizContent.set("out_biz_no", req.getOutBizNo());
        // 分 → 元字符串(支付宝金额单位为元)
        bizContent.set("trans_amount", fenToYuan(req.getAmount()));
        bizContent.set("product_code", PRODUCT_CODE);
        bizContent.set("biz_scene", BIZ_SCENE);
        if (StrUtil.isNotBlank(req.getTitle())) {
            bizContent.set("order_title", StrUtil.sub(req.getTitle(), 0, 64));
        }
        // 收款人信息: 账号类型映射支付宝身份类型
        JSONObject payeeInfo = new JSONObject();
        payeeInfo.set("identity", req.getPayeeAccount());
        payeeInfo.set("identity_type", mapIdentityType(req.getPayeeType()));
        bizContent.set("payee_info", payeeInfo);
        if (StrUtil.isNotBlank(req.getPayeeName())) {
            bizContent.set("payee_name", req.getPayeeName());
        }
        if (StrUtil.isNotBlank(req.getRemark())) {
            bizContent.set("remark", StrUtil.sub(req.getRemark(), 0, 200));
        }
        return bizContent;
    }

    /// 平台收款人类型 → 支付宝身份类型
    ///
    /// user_id → ALIPAY_USER_ID / open_id → ALIPAY_OPEN_ID / login_name → ALIPAY_LOGON_ID
    private String mapIdentityType(String payeeType) {
        return switch (payeeType) {
            case "open_id" -> "ALIPAY_OPEN_ID";
            case "login_name" -> "ALIPAY_LOGON_ID";
            default -> "ALIPAY_USER_ID";
        };
    }

    /// 分 → 元字符串(支付宝金额单位为元, 保留两位)
    private String fenToYuan(Long amount) {
        return BigDecimal.valueOf(amount).movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
