package cn.daxpay.open.channel.lakala.code;

/// # 拉卡拉通道常量
///
/// 集中管理拉卡拉开放平台网关地址、请求头、成功码等常量。
public final class LakalaCode {

    private LakalaCode() {
    }

    // ===== 网关地址 =====

    /// 生产环境网关
    public static final String PRODUCTION_SERVER_URL = "https://s2.lakala.com/api/";

    /// 沙箱环境网关
    public static final String SANDBOX_SERVER_URL = "https://test.wsmsd.cn/sit/api/";

    // ===== API 路径(交易类, v3/labs 体系) =====

    /// 预下单(扫码/JSAPI/APP/小程序)
    public static final String PATH_PREORDER = "/v3/labs/trans/preorder";

    /// 条码支付(付款码被扫)
    public static final String PATH_MICROPAY = "/v3/labs/trans/micropay";

    /// 订单查询
    public static final String PATH_QUERY = "/v3/labs/query/tradequery";

    /// 关闭订单
    public static final String PATH_CLOSE = "/v3/labs/relation/close";

    /// 退款
    public static final String PATH_REFUND = "/v3/labs/relation/refund";

    // ===== 请求头 =====

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_LKL_OP_SDK = "lkl-op-sdk";
    public static final String HEADER_LKL_OP_FLOWGROUP = "lkl-op-flowgroup";
    public static final String HEADER_LKL_OP_APPID = "lkl-op-appid";

    /// SDK 标识
    public static final String LKL_OP_SDK = "daxpay-channel-one-lakala";
    /// 流量分组
    public static final String LKL_OP_FLOWGROUP = "daxpay";

    // ===== 响应码 =====

    /// 交易类成功码
    public static final String CODE_SUCCESS_1 = "BBS00000";
    /// 交易类成功码(部分接口)
    public static final String CODE_SUCCESS_2 = "000000";

    // ===== Authorization 前缀 =====

    /// 拉卡拉 V3 签名认证方案前缀
    public static final String AUTH_SCHEME = "LKLAPI-SHA256withRSA";

    // ===== 版本 =====

    /// 接口版本
    public static final String VERSION = "3.0";
}
