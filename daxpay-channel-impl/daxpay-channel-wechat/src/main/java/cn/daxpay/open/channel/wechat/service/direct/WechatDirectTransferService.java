package cn.daxpay.open.channel.wechat.service.direct;

import cn.daxpay.open.channel.wechat.config.WechatSdkConfig;
import cn.daxpay.open.channel.wechat.req.WechatTransferReq;
import cn.daxpay.open.channel.wechat.resp.WechatTransferResp;
import cn.daxpay.open.platform.core.exception.ChannelErrorCode;
import cn.daxpay.open.platform.core.exception.ChannelServiceException;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsGetResult;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsRequest;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/// # 微信通道转账服务
///
/// 商家转账到零钱(V3 /fund-app/mch-transfer/transfer-bills)。
/// 发起返回 transferBillNo 与 package_info(需商户二次确认时用于拉起确认页),
/// 同步查询返回状态(state)映射见主应用 [cn.daxpay.open.channel.wechat.service.payment.transfer.WechatTransferService]。
/// 金额单位: 分。
@Slf4j
@Service
public class WechatDirectTransferService {

    /// 微信时间格式(RFC3339, 带毫秒与东八区偏移)
    private static final DateTimeFormatter WECHAT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /// 发起商家转账到零钱
    public WechatTransferResp transfer(WechatTransferReq req) {
        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        var request = TransferBillsRequest.newBuilder()
                .appid(req.getCredential().getWxAppId())
                .outBillNo(req.getOutBillNo())
                .transferSceneId(req.getScene())
                .openid(req.getOpenid())
                .transferAmount(req.getAmount().intValue())
                .transferRemark(StrUtil.sub(req.getRemark(), 0, 32))
                .notifyUrl(req.getNotifyUrl())
                .build();
        if (StrUtil.isNotBlank(req.getUserName())) {
            request.setUserName(req.getUserName());
        }
        WechatTransferResp resp = new WechatTransferResp();
        try {
            TransferBillsResult result = service.getTransferService().transferBills(request);
            resp.setTransferBillNo(result.getTransferBillNo());
            resp.setPackageInfo(result.getPackageInfo());
        } catch (WxPayException e) {
            log.error("微信转账调用失败: outBillNo={}, errCode={}, errMsg={}",
                    req.getOutBillNo(), e.getErrCode(), e.getErrCodeDes());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatTransferFailed", e.getMessage());
        }
        return resp;
    }

    /// 同步查询转账状态
    public WechatTransferResp sync(WechatTransferReq req) {
        WxPayService service = WechatSdkConfig.buildService(req.getCredential());
        WechatTransferResp resp = new WechatTransferResp();
        // 优先按通道转账单号查询, 缺失时按商户单号查询
        String billNo = req.getOutBillNo();
        try {
            TransferBillsGetResult result;
            if (StrUtil.isNotBlank(billNo) && !StrUtil.equals(billNo, req.getTransferNo())) {
                result = service.getTransferService().getBillsByTransferBillNo(billNo);
            } else {
                result = service.getTransferService().getBillsByOutBillNo(req.getTransferNo());
            }
            resp.setTransferBillNo(result.getTransferBillNo());
            resp.setState(result.getState());
            resp.setFailReason(result.getFailReason());
            // 完成时间(微信 RFC3339 东八区时间戳)
            if (StrUtil.isNotBlank(result.getUpdateTime())) {
                try {
                    resp.setFinishTime(OffsetDateTime.parse(result.getUpdateTime(), WECHAT_TIME_FORMATTER));
                } catch (Exception e) {
                    log.warn("微信转账时间解析失败: updateTime={}", result.getUpdateTime());
                }
            }
            resp.setComplete(isTerminal(resp.getState()));
        } catch (WxPayException e) {
            log.error("微信转账查询失败: billNo={}, errCode={}, errMsg={}",
                    billNo, e.getErrCode(), e.getErrCodeDes());
            throw new ChannelServiceException(ChannelErrorCode.SDK_CALL_FAILED.getCode(),
                    "channel.error.wechatTransferQueryFailed", e.getMessage());
        }
        return resp;
    }

    /// 是否终态
    private boolean isTerminal(String state) {
        return "SUCCESS".equals(state) || "FAIL".equals(state) || "CANCELLED".equals(state);
    }
}
