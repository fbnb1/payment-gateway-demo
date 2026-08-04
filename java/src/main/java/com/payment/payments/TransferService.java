package com.payment.payments;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HAI method, THAN HAM GIONG HET NHAU TUNG DONG.
 * Khac biet duy nhat: mot cai co @Transactional, mot cai khong.
 *
 * Ca hai deu mo phong cung mot su co: tru tien xong thi PSP bao loi.
 */
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final DemoLog log;

    public TransferService(AccountRepository accounts, DemoLog log) {
        this.accounts = accounts;
        this.log = log;
    }

    // ─────────────────────────────────────────────────────────────
    //  KHONG co @Transactional
    // ─────────────────────────────────────────────────────────────
    public void transferWithoutTransaction(String fromId, String toId, BigDecimal amount) {
        log.ghi("VAO  TransferService.transferWithoutTransaction(" + fromId + " -> " + toId + ", " + amount + ")");
        log.ghi("     method KHONG co @Transactional  ->  proxy khong mo transaction nao");
        log.ghi("     ==> moi lenh ghi se TU COMMIT ngay lap tuc (autocommit = true)");
        chuyenTien(fromId, toId, amount);
    }

    // ─────────────────────────────────────────────────────────────
    //  CO @Transactional  — than ham y het tren
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public void transferWithTransaction(String fromId, String toId, BigDecimal amount) {
        log.ghi("VAO  TransferService.transferWithTransaction(" + fromId + " -> " + toId + ", " + amount + ")");
        log.ghi("     method CO @Transactional  ->  proxy da goi setAutoCommit(false)");
        log.ghi("     ==> moi lenh ghi se CHO, chi thuc su luu khi commit");
        chuyenTien(fromId, toId, amount);
    }

    /**
     * Phan nghiep vu that su — dung chung cho ca hai kich ban,
     * de chac chan khong co khac biet nao ngoai cai annotation.
     */
    private void chuyenTien(String fromId, String toId, BigDecimal amount) {

        Account from = accounts.findById(fromId).orElseThrow();
        log.ghi("     accounts.findById(\"" + fromId + "\")        -> so du " + from.getBalance());

        from.debit(amount);
        log.ghi("     " + fromId + ".debit(" + amount + ")             -> so du trong bo nho con " + from.getBalance());

        accounts.save(from);
        log.ghi("     accounts.save(\"" + fromId + "\")            -> gui UPDATE xuong database");

        log.ghi("     goi neSuCoTuPSP()  <-- day la cho sap no");
        neSuCoTuPSP();

        // ── tu day tro xuong KHONG BAO GIO chay ──
        Account to = accounts.findById(toId).orElseThrow();
        to.credit(amount);
        accounts.save(to);
        log.ghi("     accounts.save(\"" + toId + "\")              -> (khong bao gio toi day)");
    }

    /** Gia lap nha cung cap thanh toan ben ngoai timeout giua chung. */
    private void neSuCoTuPSP() {
        throw new IllegalStateException("PSP timeout giua chung!");
    }
}
