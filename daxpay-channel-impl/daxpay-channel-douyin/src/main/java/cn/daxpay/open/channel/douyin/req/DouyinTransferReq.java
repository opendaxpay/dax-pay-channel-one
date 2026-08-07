package cn.daxpay.open.channel.douyin.req;

import cn.daxpay.open.channel.douyin.config.DouyinSdkCredential;
import lombok.Data;

import java.util.List;

/// # 抖音通道转账请求(发起/同步共用)
///
/// 与主应用 dax-pay-open 的 `DouyinTransferReq` 镜像, 字段对齐。
@Data
public class DouyinTransferReq {

    /// 商户转账单号(发起=平台转账单号 transferNo, 同步=通道转账单号 outTransferNo)
    private String outBillNo;

    /// 平台转账单号(同步反查备用)
    private String transferNo;

    /// 转账金额(分, 同步可空)
    private Long amount;

    /// 收款人 openid(与手机号二选一, 不可同时填入)
    private String openid;

    /// 收款人手机号(与 openid 二选一, 不可同时填入; 需平台证书加密上送)
    private String phoneNumber;

    /// 转账场景ID(transfer_scene_id, 主数据枚举1001-1007)
    private String scene;

    /// 收款人姓名(金额>=2000元必填, 加密上送)
    private String userName;

    /// 转账备注(对应 transfer_remark)
    private String remark;

    /// 收款感知文案(对应 user_recv_perception, 按场景枚举选项)
    private String perception;

    /// 转账场景报备信息(对应 transfer_scene_report_infos, 按场景要求填写)
    private List<ReportInfo> reportInfos;

    /// 异步通知地址(抖音→平台)
    private String notifyUrl;

    /// 通道调用凭证
    private DouyinSdkCredential credential;

    /// 转账场景报备信息明细
    @Data
    public static class ReportInfo {

        /// 信息类型(抖音协议固定中文 infoType)
        private String infoType;

        /// 信息内容(商户自定义)
        private String infoContent;
    }
}
