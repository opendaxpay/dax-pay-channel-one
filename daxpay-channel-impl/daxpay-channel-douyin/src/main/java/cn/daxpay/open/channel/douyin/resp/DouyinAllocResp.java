package cn.daxpay.open.channel.douyin.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

/// # 抖音通道分账响应(发起/同步共用)
///
/// 与主应用 dax-pay-open 的 `DouyinAllocResp` 镜像, 字段对齐。
@Data
@Accessors(chain = true)
public class DouyinAllocResp {

    /// 通道分账单号(抖音 orderId)
    private String orderId;

    /// 分账状态(同步返回)
    private String status;

    /// 逐明细结果(同步查询返回)
    private List<ReceiverSplitResult> receiverSplitResultDtos;

    /// 错误码
    private String errorCode;

    /// 错误信息
    private String errorMsg;

    @Data
    @Accessors(chain = true)
    public static class ReceiverSplitResult {

        /// 接收方账号
        private String account;

        /// 分账金额(分)
        private Long amount;

        /// 明细结果(SUCCESS/PROCESSING/CLOSED)
        private String splitStatus;

        /// 失败原因
        private String failReason;

        /// 明细完成时间(子应用侧已解析, 无时区字面量按东八区补偏移)
        private OffsetDateTime finishTime;
    }
}
