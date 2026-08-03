package com.payment.payments;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * Bộ đếm dùng chung cho object thật và proxy.
 *
 * ⚠️ Lưu ý về note 05: bean singleton KHÔNG nên có state thay đổi được.
 * Ở đây ta cố tình vi phạm, vì đây là DỤNG CỤ ĐO, không phải code nghiệp vụ.
 * Và để an toàn khi nhiều thread cùng đếm, ta dùng AtomicInteger —
 * kiểu số nguyên mà thao tác tăng/giảm là nguyên tử, không bị race condition.
 * Nếu dùng `int` thường ở đây thì chính bộ đếm sẽ đếm sai.
 */
@Component
public class CallStats {

    private final AtomicInteger realCreateCalls = new AtomicInteger();
    private final AtomicInteger proxyIntercepted = new AtomicInteger();

    /** Gọi từ BÊN TRONG object thật — đếm số lần create() thực sự chạy. */
    void countRealCall() {
        realCreateCalls.incrementAndGet();
    }

    /** Gọi từ PROXY — đếm số lần proxy chặn được. */
    void countProxyIntercept() {
        proxyIntercepted.incrementAndGet();
    }

    public StatsSnapshot snapshot() {
        int real = realCreateCalls.get();
        int seen = proxyIntercepted.get();
        return new StatsSnapshot(real, seen, real - seen);
    }

    public void reset() {
        realCreateCalls.set(0);
        proxyIntercepted.set(0);
    }
}
