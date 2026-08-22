package cn.daxpay.open.channel.douyin.service.alloc;

import com.douyinpay.api.splitfund.models.ApiSplitFundRequest;
import com.google.gson.annotations.SerializedName;

/// # 请求分账参数扩展(SDK 补丁)
///
/// 抖音线上 API 要求请求分账 body 必须携带非空 `description`(分账描述),
/// 否则返回 PARAM_ERROR("description is empty"),
/// 但官方 SDK douyinpay-java(1.0.6~1.0.10) 的 [ApiSplitFundRequest] 尚未提供该字段。
/// 借助 SDK GsonUtil 按运行时实际类型序列化全部字段的特性,
/// 通过子类补充该字段, 使其随请求体一并序列化。
/// SDK 后续版本补齐该字段后可移除本类, 恢复直接使用 [ApiSplitFundRequest]。
public class ApiSplitFundRequestExt extends ApiSplitFundRequest {

    /// 分账描述(必填)
    @SerializedName("description")
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
