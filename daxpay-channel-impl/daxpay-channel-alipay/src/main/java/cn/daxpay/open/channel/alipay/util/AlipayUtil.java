package cn.daxpay.open.channel.alipay.util;

import cn.hutool.core.util.StrUtil;

import java.util.Map;

public class AlipayUtil {

    public static String getCallbackType(Map<String, String> params) {
        if (StrUtil.isNotBlank(params.get("refund_flag")) || params.containsKey("refund_fee")) {
            return "refund";
        }
        return "pay";
    }
}
