package cn.daxpay.open.channel.douyin.service.transfer;

import cn.daxpay.open.channel.douyin.config.DouyinSdkConfig;
import cn.daxpay.open.channel.douyin.req.DouyinTransferReq;
import cn.daxpay.open.channel.douyin.resp.DouyinTransferApiResp;
import cn.daxpay.open.channel.douyin.resp.DouyinTransferResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.douyinpay.api.DefaultDouyinpayClient;
import com.douyinpay.api.DouyinpayClient;
import com.douyinpay.api.DouyinpayRequest;
import com.douyinpay.api.DouyinpayResponse;
import com.douyinpay.component.crypto.CryptorFactory;
import com.douyinpay.component.crypto.ICryptor;
import com.douyinpay.component.http.HttpMethod;
import com.douyinpay.exception.DouyinpayException;
import com.douyinpay.util.PemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/// # 抖音通道转账服务
///
/// 商家转账原生 API(/v1/fund_trade/mch-transfer/transfer-bills)经 SDK 通用客户端调用。
/// 金额单位: 分 → 元字符串; 收款人姓名经平台证书 RSA 加密上送。
/// 状态映射见主应用 [cn.daxpay.open.channel.douyin.service.payment.transfer.DouyinTransferService]。
@Slf4j
@Service
public class DouyinTransferService {

    private static final String BASE_URL = "https://api.douyinpay.com";
    private static final String TRANSFER_CREATE_PATH = "/v1/fund_trade/mch-transfer/transfer-bills";
    private static final String TRANSFER_QUERY_BY_BILL_NO = "/v1/fund_trade/mch-transfer/transfer-bills/transfer-bill-no/%s";
    private static final String TRANSFER_QUERY_BY_OUT_BILL_NO = "/v1/fund_trade/mch-transfer/transfer-bills/out-bill-no/%s";

    /// 发起商家转账
    public DouyinTransferResp transfer(DouyinTransferReq req) {
        DouyinpayClient client = DouyinSdkConfig.buildClient(req.getCredential());
        JSONObject body = new JSONObject();
        body.set("appid", req.getCredential().getDouyinAppId());
        body.set("out_bill_no", req.getOutBillNo());
        body.set("transfer_scene_id", req.getScene());
        // 收款人: openid 与手机号二选一(手机号走敏感字段加密)
        if (StrUtil.isNotBlank(req.getPhoneNumber())) {
            body.set("phone_number", req.getPhoneNumber());
        } else {
            body.set("openid", req.getOpenid());
        }
        // 转账金额: 抖音要求整数单位分, req.getAmount() 已为分
        body.set("transfer_amount", req.getAmount());
        body.set("transfer_remark", StrUtil.sub(req.getRemark(), 0, 32));
        body.set("notify_url", req.getNotifyUrl());
        if (StrUtil.isNotBlank(req.getPerception())) {
            body.set("user_recv_perception", StrUtil.sub(req.getPerception(), 0, 64));
        }
        // 转账场景报备信息(按场景要求的 info_type 填写)
        if (req.getReportInfos() != null && !req.getReportInfos().isEmpty()) {
            JSONArray reportArray = new JSONArray();
            for (DouyinTransferReq.ReportInfo info : req.getReportInfos()) {
                JSONObject item = new JSONObject();
                item.set("info_type", info.getInfoType());
                item.set("info_content", info.getInfoContent());
                reportArray.add(item);
            }
            body.set("transfer_scene_report_infos", reportArray);
        }

        // 敏感字段(收款人姓名/手机号): 需平台证书 RSA 加密, 并携带证书序列号
        Map<String, String> extraHeaders = null;
        boolean needSerial = false;
        if (StrUtil.isNotBlank(req.getUserName())) {
            body.set("user_name", encryptSensitive(req.getUserName(), client));
            needSerial = true;
        }
        if (StrUtil.isNotBlank(req.getPhoneNumber())) {
            body.set("phone_number", encryptSensitive(req.getPhoneNumber(), client));
            needSerial = true;
        }
        if (needSerial) {
            X509Certificate platformCert = ((DefaultDouyinpayClient) client).getPlatformCertificate();
            extraHeaders = new HashMap<>();
            // 证书序列号须为十六进制大写(与 SDK PemUtil#getSerialNumber 一致, 分账请求头同源),
            // 直接用 getSerialNumber().toString() 是十进制, 平台会匹配不到证书
            extraHeaders.put("Douyinpay-Serial", PemUtil.getSerialNumber(platformCert));
        }

        var request = new DouyinpayRequest(HttpMethod.POST, BASE_URL, TRANSFER_CREATE_PATH,
                extraHeaders, JSONUtil.toJsonStr(body));
        DouyinTransferResp resp = new DouyinTransferResp();
        try {
            DouyinpayResponse<DouyinTransferApiResp> response = client.execute(request, DouyinTransferApiResp.class);
            response.validate();
            DouyinTransferApiResp data = response.getApiResponse();
            if (data != null) {
                resp.setTransferBillNo(data.getTransferBillNo());
                resp.setState(data.getState());
            }
        } catch (DouyinpayException e) {
            log.error("抖音转账调用失败: outBillNo={}", req.getOutBillNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinTransferFailed", e.getMessage());
        }
        return resp;
    }

    /// 同步查询转账状态
    public DouyinTransferResp sync(DouyinTransferReq req) {
        DouyinpayClient client = DouyinSdkConfig.buildClient(req.getCredential());
        DouyinTransferResp resp = new DouyinTransferResp();
        String queryPath;
        if (StrUtil.isNotBlank(req.getOutBillNo()) && !StrUtil.equals(req.getOutBillNo(), req.getTransferNo())) {
            queryPath = String.format(TRANSFER_QUERY_BY_BILL_NO, req.getOutBillNo());
        } else {
            queryPath = String.format(TRANSFER_QUERY_BY_OUT_BILL_NO, req.getTransferNo());
        }
        var request = new DouyinpayRequest(HttpMethod.GET, BASE_URL, queryPath,
                null, null);
        try {
            DouyinpayResponse<DouyinTransferApiResp> response = client.execute(request, DouyinTransferApiResp.class);
            response.validate();
            DouyinTransferApiResp data = response.getApiResponse();
            if (data != null) {
                resp.setTransferBillNo(data.getTransferBillNo());
                resp.setState(data.getState());
                resp.setFailReason(data.getFailReason());
            }
        } catch (DouyinpayException e) {
            log.error("抖音转账查询失败: billNo={}", req.getOutBillNo(), e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinTransferQueryFailed", e.getMessage());
        }
        return resp;
    }

    /// 平台证书 RSA 加密敏感字段(收款人姓名/手机号)
    private String encryptSensitive(String text, DouyinpayClient client) {
        try {
            X509Certificate platformCert = ((DefaultDouyinpayClient) client).getPlatformCertificate();
            ICryptor cryptor = CryptorFactory.getByName("RSA");
            return cryptor.encrypt(text, platformCert);
        } catch (Exception e) {
            log.error("抖音转账敏感字段加密失败", e);
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.douyinTransferEncryptFailed", e.getMessage());
        }
    }
}
