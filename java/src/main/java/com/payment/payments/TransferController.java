package com.payment.payments;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {

    private static final BigDecimal SO_DU_BAN_DAU = new BigDecimal("1000.00");
    private static final BigDecimal SO_TIEN_CHUYEN = new BigDecimal("100.00");

    private final TransferService transferService;
    private final AccountRepository accounts;
    private final DemoLog log;

    public TransferController(TransferService transferService, AccountRepository accounts, DemoLog log) {
        this.transferService = transferService;
        this.accounts = accounts;
        this.log = log;
    }

    /** Chay ca hai kich ban va tra ve mot ban bao cao doc tu tren xuong. */
    @GetMapping(value = "/demo/run", produces = MediaType.TEXT_PLAIN_VALUE)
    public String run() {
        StringBuilder out = new StringBuilder();

        out.append("BOI CANH\n");
        out.append("  Alice chuyen 100.00 cho Bob. Giua chung nha cung cap thanh toan (PSP) timeout.\n");
        out.append("  Cung mot su co, chay hai lan. Khac biet DUY NHAT: mot lan co @Transactional.\n");
        out.append("  Con so can nhin la TONG - tien chi doi cho, tong PHAI luon = 2000.00\n\n");
        out.append("  Bean TransferService thuc su la: ").append(transferService.getClass().getName()).append('\n');
        out.append("  (ten co $$SpringCGLIB$$ = Spring da boc bean cua ban bang mot proxy)\n");

        chay(out, "KICH BAN 1  --  KHONG co @Transactional",
                (from, to) -> transferService.transferWithoutTransaction(from, to, SO_TIEN_CHUYEN));

        chay(out, "KICH BAN 2  --  CO @Transactional (than ham y het)",
                (from, to) -> transferService.transferWithTransaction(from, to, SO_TIEN_CHUYEN));

        out.append("\n").append("=".repeat(72)).append("\nBAI HOC\n").append("=".repeat(72)).append('\n');
        out.append("""
                  1. Khong co transaction, moi lenh save() TU COMMIT ngay.
                     Luc su co xay ra, lenh tru tien DA LUU VINH VIEN - khong con gi de huy.

                  2. Co @Transactional, proxy goi setAutoCommit(false) truoc khi vao ham.
                     Moi lenh nam CHO. Gap exception -> ROLLBACK -> nhu chua tung xay ra.

                  3. Ca hai lan deu nem exception giong het nhau. Su co la nhu nhau.
                     Khac biet nam o CHO DA CO AI DO CHUAN BI DUONG LUI HAY CHUA.

                  4. Khi debug loi "mat tien": dung tim trong ham nghiep vu truoc.
                     Hoi truoc: ham nay co thuc su chay trong transaction khong?
                     - co @Transactional khong?
                     - co bi goi qua this.method() khong?  (self-invocation)
                     - method co phai private / final khong?
                     - exception nem ra la checked hay unchecked?
                  """);
        return out.toString();
    }

    private void chay(StringBuilder out, String tieuDe, BiConsumer<String, String> hanhDong) {
        out.append('\n').append("=".repeat(72)).append('\n');
        out.append(tieuDe).append('\n');
        out.append("=".repeat(72)).append('\n');

        reset();
        log.batDau();

        LedgerReport truoc = report();
        out.append(dongSoDu("SO DU BAN DAU", truoc)).append('\n');
        out.append("     ").append("-".repeat(60)).append('\n');

        try {
            hanhDong.accept("alice", "bob");
            log.ghi("KET THUC binh thuong (khong mong doi trong demo nay)");
        } catch (RuntimeException e) {
            StackTraceElement noiNo = e.getStackTrace()[0];
            log.ghi("*** SU CO: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            log.ghi("    tai " + noiNo.getClassName() + "." + noiNo.getMethodName()
                    + "()  dong " + noiNo.getLineNumber());
            log.ghi("    cac dong sau day KHONG chay: findById(bob), bob.credit(), save(bob)");
            log.ghi("THOAT khoi ham bang exception");
        }

        log.doc().forEach(dong -> out.append("     ").append(dong).append('\n'));

        LedgerReport sau = report();
        out.append("     ").append("-".repeat(60)).append('\n');
        out.append(dongSoDu("SO DU CUOI", sau)).append('\n');

        BigDecimal chenh = sau.total().subtract(truoc.total());
        if (chenh.signum() == 0) {
            out.append("  KET QUA: TONG KHONG DOI -> so cai VAN DUNG. Da rollback.\n");
        } else {
            out.append("  KET QUA: TONG LECH ").append(chenh)
                    .append("  -> MAT TIEN. Khong co gi rollback ca.\n");
        }
        log.don();
    }

    private String dongSoDu(String nhan, LedgerReport l) {
        return String.format("  %-14s alice=%8s   bob=%8s   TONG=%8s", nhan, l.alice(), l.bob(), l.total());
    }

    private void reset() {
        accounts.deleteAll();
        accounts.saveAll(List.of(
                new Account("alice", SO_DU_BAN_DAU),
                new Account("bob", SO_DU_BAN_DAU)));
    }

    private LedgerReport report() {
        BigDecimal alice = accounts.findById("alice").map(Account::getBalance).orElse(BigDecimal.ZERO);
        BigDecimal bob = accounts.findById("bob").map(Account::getBalance).orElse(BigDecimal.ZERO);
        return new LedgerReport(alice, bob, alice.add(bob));
    }
}
