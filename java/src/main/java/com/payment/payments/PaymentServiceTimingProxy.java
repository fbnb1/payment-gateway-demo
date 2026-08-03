package com.payment.payments;

/**
 * PROXY thứ hai — đo thời gian.
 *
 * Nó bọc NGOÀI PaymentServiceLoggingProxy, chứ không đứng cạnh.
 * Đây là cách Spring xử lý khi một bean có nhiều annotation
 * (@Transactional + @Cacheable + @Async): lồng vào nhau, không tạo nhiều bean.
 */
public class PaymentServiceTimingProxy extends PaymentService {

    private final PaymentService target;      // có thể là object thật, hoặc MỘT PROXY KHÁC

    public PaymentServiceTimingProxy(PaymentService target, CallStats stats) {
        super(stats);
        this.target = target;
    }

    @Override
    public PaymentResponse create(PaymentRequest request) {
        long start = System.nanoTime();
        try {
            return target.create(request);                  // chuyển vào lớp bên trong
        } finally {
            long micros = (System.nanoTime() - start) / 1000;
            System.out.println("### TIMING: create() mat " + micros + " micro-giay");
        }
    }

    @Override
    public PaymentResponse createTwice(PaymentRequest request) {
        return target.createTwice(request);
    }

    @Override
    public String chain() {
        return getClass().getSimpleName() + "  ->  " + target.chain();
    }
}
