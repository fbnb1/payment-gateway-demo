package com.payment.payments;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HAI method, THÂN HÀM GIỐNG HỆT NHAU TỪNG DÒNG.
 * Khác biệt duy nhất: một cái có @Transactional, một cái không.
 *
 * Cả hai đều mô phỏng cùng một sự cố: trừ tiền xong thì PSP báo lỗi.
 */
@Service
public class TransferService {

    private final AccountRepository accounts;

    public TransferService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    // ─────────────────────────────────────────────────────────────
    //  KHÔNG có @Transactional
    // ─────────────────────────────────────────────────────────────
    public void transferWithoutTransaction(String fromId, String toId, BigDecimal amount) {

        Account from = accounts.findById(fromId).orElseThrow();
        from.debit(amount);
        accounts.save(from);                       // ← lưu xuống DB

        neSuCoTuPSP();                             // ← 💥 lỗi xảy ra ở ĐÂY

        Account to = accounts.findById(toId).orElseThrow();
        to.credit(amount);
        accounts.save(to);                         // ← dòng này KHÔNG BAO GIỜ chạy
    }

    // ─────────────────────────────────────────────────────────────
    //  CÓ @Transactional  — thân hàm y hệt trên
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public void transferWithTransaction(String fromId, String toId, BigDecimal amount) {

        Account from = accounts.findById(fromId).orElseThrow();
        from.debit(amount);
        accounts.save(from);

        neSuCoTuPSP();                             // ← 💥 lỗi y hệt, cùng vị trí

        Account to = accounts.findById(toId).orElseThrow();
        to.credit(amount);
        accounts.save(to);
    }

    /** Giả lập nhà cung cấp thanh toán bên ngoài timeout giữa chừng. */
    private void neSuCoTuPSP() {
        throw new IllegalStateException("PSP timeout giua chung!");
    }
}
