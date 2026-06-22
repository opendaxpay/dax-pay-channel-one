package cn.daxpay.open.channel.core.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// # 追踪ID响应头过滤器
///
/// 基于 OpenTelemetry 自动注入的 MDC traceId, 将其写入响应头 x-trace-id,
/// 便于主应用(dax-pay-open)在日志中关联子调用。
/// 执行顺序晚于 OTel ServerHttpObservationFilter(HIGHEST_PRECEDENCE + 1),
/// 以确保 MDC 中已有 traceId。
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ChannelTraceResponseFilter extends OncePerRequestFilter implements Ordered {

    /// 追踪ID响应头名称(与主应用保持一致)
    public static final String X_TRACE_ID = "x-trace-id";

    /// MDC 中 traceId 的 key(由 OTel 自动注入)
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            // 读取 OTel ServerHttpObservationFilter 已注入的 traceId, 写入响应头
            String traceId = MDC.get(MDC_TRACE_ID);
            if (StrUtil.isNotBlank(traceId)) {
                response.setHeader(X_TRACE_ID, traceId);
            }
            chain.doFilter(request, response);
        } finally {
            // 注意: 不要调用 MDC.clear(), OTel 自行管理 MDC 生命周期
        }
    }
}
