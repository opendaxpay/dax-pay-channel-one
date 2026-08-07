package cn.daxpay.open.channel.alipay.req;

import cn.daxpay.open.channel.alipay.config.AlipaySdkCredential;
import lombok.Data;

import java.util.List;

/// # 支付宝通道转账请求(发起/同步共用)
///
/// 与主应用 dax-pay-open 的 `AlipayTransferReq` 镜像, 字段对齐。
@Data
public class AlipayTransferReq {

    /// 商户转账单号(发起=平台转账单号 transferNo, 同步按此反查)
    private String outBizNo;

    /// 转账金额(分, 同步可空)
    private Long amount;

    /// 转账标题(对应 order_title)
    private String title;

    /// 转账备注(对应 remark)
    private String remark;

    /// 收款人账号类型(user_id/open_id/login_name)
    private String payeeType;

    /// 收款人账号
    private String payeeAccount;

    /// 收款人姓名
    private String payeeName;

    /// 异步通知地址(支付宝→平台)
    private String notifyUrl;

    /// 转账场景名称(2026新商户必填,由通道商户转账场景配置注入)
    private String transferSceneName;

    /// 转账场景上报信息列表(对应支付宝 transfer_scene_report_infos,多条)
    private List<ReportInfo> reportInfos;

    /// 通道调用凭证
    private AlipaySdkCredential credential;

    /// 转账场景上报信息项(与主应用 TransferSceneReportInfo 镜像)
    @Data
    public static class ReportInfo {

        /// 转账场景信息类型(由场景决定)
        private String infoType;

        /// 转账场景信息描述(商户自定义)
        private String infoContent;
    }
}
