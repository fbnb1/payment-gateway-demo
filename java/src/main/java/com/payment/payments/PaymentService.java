package com.payment.payments;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public PaymentResponse create(PaymentRequest request) {
        String id = UUID.randomUUID().toString();
        return new PaymentResponse(id, request.amount(), "CREATED");
    }
}
