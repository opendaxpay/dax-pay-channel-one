package cn.daxpay.open.platform.system.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// # 追踪ID响应头过滤器
///
/// 对标主项目 `common-request-context` 的 `TraceIdFilter`, 子应用未引入该模块,
/// 在此本地实现一份精简版: 仅将 OTel 注入到 MDC 的 traceId 透传到 `x-trace-id` 响应头,
/// 供主应用/调用方关联链路。子应用 DaxResult 不再写入 traceId 字段。
///
/// 执行顺序晚于 OTel ServerHttpObservationFilter(HIGHEST_PRECEDENCE + 1), 确保 MDC 中已有 traceId。
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter implements Ordered {

    /// 响应头名称(与主应用 [cn.daxpay.open.platform.core.code.WebHeaderCode#X_TRACE_ID] 一致)
    public static final String HEADER_X_TRACE_ID = "x-trace-id";

    /// MDC 中 traceId 的 key(OTel 约定)
    public static final String MDC_TRACE_ID = "traceId";

    /// 过滤器优先级: 晚于 OTel ServerHttpObservationFilter, 确保 MDC 已注入 traceId
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 读取 OTel ServerHttpObservationFilter 已注入的 traceId, 写入响应头
        String traceId = MDC.get(MDC_TRACE_ID);
        if (StrUtil.isNotBlank(traceId)) {
            response.setHeader(HEADER_X_TRACE_ID, traceId);
        }
        chain.doFilter(request, response);
    }
}
