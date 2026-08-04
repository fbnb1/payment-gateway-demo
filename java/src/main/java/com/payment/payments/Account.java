package com.payment.payments;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mot tai khoan trong so cai.
 *
 * ĐÂY LÀ CLASS THƯỜNG, KHÔNG PHẢI RECORD — và bạn nhìn thấy đủ 3 lý do ngay tại đây:
 *   1. có constructor KHÔNG THAM SỐ (protected)  → record không có
 *   2. field balance THAY ĐỔI ĐƯỢC              → record bất biến
 *   3. class không final                         → Hibernate cần kế thừa để tạo proxy
 * Xem notes/08-java-records.md muc 5.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String id;

    private BigDecimal balance;

    /** JPA bắt buộc phải có. Bạn không gọi cái này bao giờ. */
    protected Account() {
    }

    public Account(String id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    /** Trừ tiền. */
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    /** Cộng tiền. */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
