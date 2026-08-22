package cn.daxpay.open.channel.douyin.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

/// # 抖音通道分账回调验签解析响应
///
/// 与主应用 dax-pay-open 的 `DouyinAllocCallbackParseResp` 镜像, 字段对齐。
@Data
@Accessors(chain = true)
public class DouyinAllocCallbackParseResp {

    /// 通道分账单号(抖音 order_id)
    private String orderId;

    /// 商户分账单号(平台 allocNo, 发起时上送的 out_trade_no, 回调定位分账单主键)
    private String outTradeNo;

    /// 分账状态(SUCCESS/PROCESSING/CLOSED/FAIL)
    private String state;

    /// 分账完成时间(子应用侧已解析, 无时区字面量按东八区补偏移)
    private OffsetDateTime splitFinishTime;

    /// 逐明细结果
    private List<ReceiverResult> receiverResults;

    /// 验签是否通过
    private boolean verified;

    @Data
    @Accessors(chain = true)
    public static class ReceiverResult {

        /// 接收方账号
        private String account;

        /// 明细结果(PENDING/SUCCESS/CLOSED)
        private String splitStatus;

        /// 失败原因
        private String failReason;

        /// 明细完成时间(子应用侧已解析)
        private OffsetDateTime finishTime;
    }
}
