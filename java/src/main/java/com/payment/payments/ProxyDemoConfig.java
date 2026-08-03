package com.payment.payments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đây là chỗ quyết định THỨ GÌ được cất vào container.
 *
 * Bean tên "paymentService", nhưng object cất vào là PROXY.
 * Object thật chỉ nằm ẩn bên trong proxy.
 *
 * Spring làm đúng việc này cho @Transactional — chỉ khác là tự động,
 * ở pha post-process, và proxy do nó sinh ra lúc chạy.
 */
@Configuration
public class ProxyDemoConfig {

    @Bean
    public PaymentService paymentService(CallStats stats) {
        PaymentService real = new PaymentService(stats);              // object thật
        return new PaymentServiceLoggingProxy(real, stats);           // ← cất PROXY vào container
    }
}
