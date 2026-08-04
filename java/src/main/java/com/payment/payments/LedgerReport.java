package com.payment.payments;

import java.math.BigDecimal;

/**
 * Ảnh chụp toàn bộ sổ cái.
 * `total` là con số cần nhìn: tiền chỉ CHUYỂN CHỖ, tổng phải KHÔNG ĐỔI.
 */
public record LedgerReport(BigDecimal alice, BigDecimal bob, BigDecimal total) {}
