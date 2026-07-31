package com.payment.payments;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount, String currency) {}
