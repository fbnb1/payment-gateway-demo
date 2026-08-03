package com.payment.payments;

import java.util.UUID;

/**
 * Object THẬT. Không có @Service — nó được khai báo trong ProxyDemoConfig,
 * để ta tự quyết định thứ gì được cất vào container.
 */
public class PaymentService {

    private final CallStats stats;

    public PaymentService(CallStats stats) {
        this.stats = stats;
    }

    /** Method bình thường. Proxy sẽ bọc method này. */
    public PaymentResponse create(PaymentRequest request) {
        stats.countRealCall();                       // ← đếm ở ĐÂY: bên trong object thật
        String id = UUID.randomUUID().toString();
        return new PaymentResponse(id, request.amount(), "CREATED");
    }

    /** Gọi create() của CHÍNH MÌNH qua `this` — đây là self-invocation. */
    public PaymentResponse createTwice(PaymentRequest request) {
        this.create(request);            // ← this = object THẬT, không phải proxy
        return this.create(request);     // ← this = object THẬT, không phải proxy
    }

    /** Dùng để in ra chuỗi proxy. Object thật là điểm cuối cùng của chuỗi. */
    public String chain() {
        return "PaymentService (OBJECT THAT)";
    }
}
