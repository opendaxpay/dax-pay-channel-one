package cn.daxpay.open.channel.douyin.req;

import cn.daxpay.open.channel.douyin.config.DouyinSdkCredential;
import lombok.Data;
import lombok.experimental.Accessors;

/// # 抖音通道分账接收方绑定请求(绑定/解绑共用)
///
/// 与主应用 dax-pay-open 的 `DouyinAllocReceiverReq` 镜像, 字段对齐。
/// 对应抖音 API: 绑定 addSplitReceiver / 解绑 deleteSplitReceiver。
@Data
@Accessors(chain = true)
public class DouyinAllocReceiverReq {

    /// 接收方类型(MERCHANT_ID / PERSONAL_OPENID)
    private String receiverType;

    /// 接收方账号(商户号或 openid)
    private String receiverAccount;

    /// 接收方名称(MERCHANT_ID 时必填商户全称)
    private String receiverName;

    /// 分账关系类型(平台小写, 转抖音原生大写)
    private String relationType;

    /// 自定义分账关系名(relationType=CUSTOM 时必填)
    private String customRelation;

    /// 通道调用凭证
    private DouyinSdkCredential credential;
}
