package com.payment.payments;

/**
 * PROXY viết tay.
 *
 * Hai điều kiện để nó thay thế được object thật:
 *   1. extends PaymentService  → cùng KIỂU, nên nhét vừa field của controller
 *   2. giữ tham chiếu `target` tới object thật, và chuyển lời gọi vào trong
 *
 * Spring sinh một class y hệt thế này lúc chạy — chỉ khác là không có file để mở.
 */
public class PaymentServiceLoggingProxy extends PaymentService {

    private final PaymentService target;      // object THẬT nằm bên trong
    private final CallStats stats;

    public PaymentServiceLoggingProxy(PaymentService target, CallStats stats) {
        super(stats);                          // bắt buộc, nhưng proxy không tự dùng
        this.target = target;
        this.stats = stats;
    }

    @Override
    public PaymentResponse create(PaymentRequest request) {
        stats.countProxyIntercept();                          // ← proxy CHẶN ĐƯỢC
        System.out.println(">>> PROXY: trước create()");       // chỗ @Transactional mở transaction
        PaymentResponse response = target.create(request);
        System.out.println("<<< PROXY: sau create()");         // chỗ @Transactional commit
        return response;
    }

    @Override
    public PaymentResponse createTwice(PaymentRequest request) {
        return target.createTwice(request);   // chuyển thẳng vào object thật, không chặn gì
    }
}
