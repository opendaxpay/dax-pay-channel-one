package cn.daxpay.open.platform.core.enums;

/// # 通道结果状态枚举
///
/// 统一表示通道订单/操作结果的终态与中间态, 用于同步与回调场景。
public enum ChannelResultStatusEnum {
    /// 成功
    SUCCESS,
    /// 处理中
    PROCESSING,
    /// 失败
    FAIL,
    /// 已关闭
    CLOSE,
    /// 已撤销
    CANCEL,
    /// 超时
    TIMEOUT,
    /// 订单不存在
    NOT_EXIST
}
