package com.payment.payments;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BON kich ban. Ca bon deu gap DUNG MOT su co: tru tien xong thi PSP bao loi.
 *
 *   1. KHONG @Transactional                      -> mat tien
 *   2. CO @Transactional                         -> an toan
 *   3. CO @Transactional + try/catch nuot loi    -> MAT TIEN
 *   4. CO @Transactional + checked exception     -> MAT TIEN
 *
 * Kich ban 3 va 4 la ly do "co @Transactional" KHONG dong nghia voi "an toan".
 */
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final DemoLog log;

    public TransferService(AccountRepository accounts, DemoLog log) {
        this.accounts = accounts;
        this.log = log;
    }

    // ─── 1. KHONG co @Transactional ──────────────────────────────
    public void transferWithoutTransaction(String fromId, String toId, BigDecimal amount) {
        log.ghi("VAO  transferWithoutTransaction(" + fromId + " -> " + toId + ", " + amount + ")");
        log.ghi("     KHONG co @Transactional  ->  proxy khong mo transaction nao");
        log.ghi("     ==> moi lenh ghi TU COMMIT ngay lap tuc (autocommit = true)");
        chuyenTien(fromId, toId, amount);
    }

    // ─── 2. CO @Transactional ────────────────────────────────────
    @Transactional
    public void transferWithTransaction(String fromId, String toId, BigDecimal amount) {
        log.ghi("VAO  transferWithTransaction(" + fromId + " -> " + toId + ", " + amount + ")");
        log.ghi("     CO @Transactional  ->  proxy da goi setAutoCommit(false)");
        log.ghi("     ==> moi lenh ghi CHO, chi thuc su luu khi commit");
        chuyenTien(fromId, toId, amount);
    }

    // ─── 3. CO @Transactional NHUNG nuot exception ───────────────
    @Transactional
    public void transferButSwallowsException(String fromId, String toId, BigDecimal amount) {
        log.ghi("VAO  transferButSwallowsException(" + fromId + " -> " + toId + ", " + amount + ")");
        log.ghi("     CO @Transactional  ->  proxy da goi setAutoCommit(false)");
        log.ghi("     NHUNG ben trong co try/catch bat loi va KHONG nem lai");
        try {
            chuyenTien(fromId, toId, amount);
        } catch (RuntimeException e) {
            log.ghi("     >>> try/catch BAT duoc: " + e.getMessage());
            log.ghi("     >>> chi log ra roi DI TIEP, khong nem lai");
        }
        log.ghi("     method KET THUC BINH THUONG (khong exception nao bay ra)");
        log.ghi("     ==> proxy dung NGOAI method, no khong thay gi -> se COMMIT");
    }

    // ─── 4. CO @Transactional nhung nem CHECKED exception ────────
    @Transactional
    public void transferWithCheckedException(String fromId, String toId, BigDecimal amount) throws Exception {
        log.ghi("VAO  transferWithCheckedException(" + fromId + " -> " + toId + ", " + amount + ")");
        log.ghi("     CO @Transactional  ->  proxy da goi setAutoCommit(false)");
        log.ghi("     NHUNG su co nem ra la CHECKED exception (java.lang.Exception)");

        Account from = accounts.findById(fromId).orElseThrow();
        log.ghi("     accounts.findById(\"" + fromId + "\")   -> so du " + from.getBalance());

        from.debit(amount);
        log.ghi("     " + fromId + ".debit(" + amount + ")        -> trong bo nho con " + from.getBalance());

        accounts.save(from);
        log.ghi("     accounts.save(\"" + fromId + "\")       -> gui UPDATE xuong database");

        log.ghi("     goi neSuCoChecked()  <-- cho sap no");
        neSuCoChecked();
    }

    /**
     * Phan nghiep vu that su — dung chung cho kich ban 1, 2, 3
     * de chac chan khong co khac biet nao ngoai cai annotation / try-catch.
     */
    private void chuyenTien(String fromId, String toId, BigDecimal amount) {

        Account from = accounts.findById(fromId).orElseThrow();
        log.ghi("     accounts.findById(\"" + fromId + "\")   -> so du " + from.getBalance());

        from.debit(amount);
        log.ghi("     " + fromId + ".debit(" + amount + ")        -> trong bo nho con " + from.getBalance());

        accounts.save(from);
        log.ghi("     accounts.save(\"" + fromId + "\")       -> gui UPDATE xuong database");

        log.ghi("     goi neSuCoTuPSP()  <-- cho sap no");
        neSuCoTuPSP();

        // ── tu day tro xuong KHONG BAO GIO chay ──
        Account to = accounts.findById(toId).orElseThrow();
        to.credit(amount);
        accounts.save(to);
    }

    /** Su co dang UNCHECKED — Spring rollback voi loai nay. */
    private void neSuCoTuPSP() {
        throw new IllegalStateException("PSP timeout giua chung!");
    }

    /** Su co dang CHECKED — Spring KHONG rollback voi loai nay. */
    private void neSuCoChecked() throws Exception {
        throw new Exception("PSP timeout (checked exception)!");
    }
}
