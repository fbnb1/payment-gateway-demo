package com.payment.payments;

import java.math.BigDecimal;

public record PaymentResponse(String id, BigDecimal amount, String status) {}
