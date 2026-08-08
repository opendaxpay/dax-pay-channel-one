package cn.daxpay.open.channel.douyin.resp;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/// # 抖音转账原始响应(发起/同步查询共用)
///
/// 对应抖音 /v1/fund_trade/mch-transfer/transfer-bills 系列接口的原始返回,
/// 字段名按抖音文档 snake_case, 通过 Gson @SerializedName 映射为 camelCase。
/// 仅用于 SDK 反序列化, 不直接对外; 对外领域模型见 [DouyinTransferResp]。
///
/// 响应为扁平结构(无 data 外层包裹), 业务字段在顶层; 异常响应(code/message/detail)
/// 伴随 4xx/5xx 状态码, 已由 SDK [com.douyinpay.api.DouyinpayResponse#validate] 拦截。
@Data
public class DouyinTransferApiResp {

    /// 商户号
    @SerializedName("mch_id")
    private String mchId;

    /// 商户订单号
    @SerializedName("out_bill_no")
    private String outBillNo;

    /// 抖音转账单号
    @SerializedName("transfer_bill_no")
    private String transferBillNo;

    /// 商户 AppID
    // 注意: 抖音文档表格为 appid, 示例 JSON 为 AppID, 大小写待联调确认, 暂按示例 JSON 为准
    @SerializedName("AppID")
    private String appId;

    /// 转账状态: ACCEPTED / PROCESSING / TRANSFERING / SUCCESS / FAIL
    @SerializedName("state")
    private String state;

    /// 转账金额, 单位: 分
    @SerializedName("transfer_amount")
    private Long transferAmount;

    /// 转账备注
    @SerializedName("transfer_remark")
    private String transferRemark;

    /// 失败原因(仅 state=FAIL 时返回)
    @SerializedName("fail_reason")
    private String failReason;

    /// 收款人手机号(手机号模式返回, 已加密)
    @SerializedName("phone_number")
    private String phoneNumber;

    /// 收款人 OpenID(OpenID 模式返回)
    // 注意: 抖音文档表格为 openid, 示例 JSON 为 OpenID, 大小写待联调确认, 暂按示例 JSON 为准
    @SerializedName("OpenID")
    private String openid;

    /// 收款人姓名(发起时传了才返回, 已加密)
    @SerializedName("user_name")
    private String userName;

    /// 订单创建时间(RFC3339, 如 2025-02-01T12:00:00+08:00)
    @SerializedName("create_time")
    private String createTime;

    /// 订单最后更新时间(RFC3339)
    @SerializedName("update_time")
    private String updateTime;
}
