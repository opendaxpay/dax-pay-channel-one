package cn.daxpay.open.channel.wechat.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

/// # 微信通道分账响应(发起/同步共用)
///
/// 与主应用 dax-pay-open 的 `WechatAllocResp` 镜像, 字段对齐。
@Data
@Accessors(chain = true)
public class WechatAllocResp {

    /// 通道分账单号(微信 transaction_id)
    private String transactionId;

    /// 分账状态(同步返回)
    private String state;

    /// 逐明细结果(同步查询返回)
    private List<ReceiverResult> receivers;

    /// 错误码
    private String errorCode;

    /// 错误信息
    private String errorMsg;

    @Data
    @Accessors(chain = true)
    public static class ReceiverResult {

        /// 接收方账号
        private String account;

        /// 分账金额(分)
        private Long amount;

        /// 明细结果(PENDING/SUCCESS/CLOSED)
        private String result;

        /// 失败原因
        private String failReason;

        /// 明细完成时间
        private OffsetDateTime finishTime;
    }
}
