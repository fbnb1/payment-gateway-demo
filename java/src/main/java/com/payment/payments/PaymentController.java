package com.payment.payments;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final CallStats stats;

    public PaymentController(PaymentService paymentService, CallStats stats) {
        this.paymentService = paymentService;
        this.stats = stats;
        // Controller KHÔNG biết mình đang cầm proxy hay object thật.
        System.out.println("Controller nhận được: " + paymentService.getClass().getName());
    }

    /** Gọi TỪ NGOÀI vào → đi qua proxy → proxy chặn được. */
    @PostMapping("/payments")
    public PaymentResponse create(@RequestBody PaymentRequest request) {
        return paymentService.create(request);
    }

    /** createTwice tự gọi this.create() hai lần → hai lần đó LỌT QUA proxy. */
    @PostMapping("/payments/twice")
    public PaymentResponse createTwice(@RequestBody PaymentRequest request) {
        return paymentService.createTwice(request);
    }

    /** Bằng chứng nằm ở đây, trong response — không phải trong log. */
    @GetMapping("/payments/stats")
    public StatsSnapshot stats() {
        return stats.snapshot();
    }

    /** Tên class THẬT của bean đang nằm trong container. */
    @GetMapping("/payments/bean-class")
    public String beanClass() {
        return paymentService.getClass().getName();
    }

    /** Toàn bộ chuỗi proxy, từ lớp ngoài cùng vào tới object thật. */
    @GetMapping("/payments/chain")
    public String chain() {
        return paymentService.chain();
    }

    @PostMapping("/payments/stats/reset")
    public StatsSnapshot resetStats() {
        stats.reset();
        return stats.snapshot();
    }
}
