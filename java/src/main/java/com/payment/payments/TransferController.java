package com.payment.payments;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {

    private static final BigDecimal SO_DU_BAN_DAU = new BigDecimal("1000.00");
    private static final BigDecimal SO_TIEN_CHUYEN = new BigDecimal("100.00");

    /** Mot hanh dong co the nem CHECKED exception. */
    @FunctionalInterface
    private interface HanhDong {
        void chay(String from, String to) throws Exception;
    }

    private final TransferService transferService;
    private final AccountRepository accounts;
    private final DemoLog log;

    public TransferController(TransferService transferService, AccountRepository accounts, DemoLog log) {
        this.transferService = transferService;
        this.accounts = accounts;
        this.log = log;
    }

    @GetMapping(value = "/demo/run", produces = MediaType.TEXT_PLAIN_VALUE)
    public String run() {
        StringBuilder out = new StringBuilder();

        out.append("BOI CANH\n");
        out.append("  Alice chuyen 100.00 cho Bob. Giua chung nha cung cap thanh toan (PSP) bao loi.\n");
        out.append("  CUNG MOT SU CO, chay bon lan voi bon cach viet khac nhau.\n");
        out.append("  Con so can nhin la TONG - tien chi doi cho, tong PHAI luon = 2000.00\n\n");
        out.append("  Bean TransferService thuc su la: ").append(transferService.getClass().getName()).append('\n');
        out.append("  ($$SpringCGLIB$$ = Spring da boc bean cua ban bang mot proxy)\n");

        chay(out, "KICH BAN 1  --  KHONG co @Transactional",
                (f, t) -> transferService.transferWithoutTransaction(f, t, SO_TIEN_CHUYEN));

        chay(out, "KICH BAN 2  --  CO @Transactional",
                (f, t) -> transferService.transferWithTransaction(f, t, SO_TIEN_CHUYEN));

        chay(out, "KICH BAN 3  --  CO @Transactional + try/catch NUOT exception",
                (f, t) -> transferService.transferButSwallowsException(f, t, SO_TIEN_CHUYEN));

        chay(out, "KICH BAN 4  --  CO @Transactional + CHECKED exception",
                (f, t) -> transferService.transferWithCheckedException(f, t, SO_TIEN_CHUYEN));

        out.append('\n').append("=".repeat(74)).append("\nBAI HOC\n").append("=".repeat(74)).append('\n');
        out.append("""
                  Ba trong bon kich ban mat tien. CA BA DEU KHONG BAO LOI GI.

                  1. KB1 - khong @Transactional:  moi save() TU COMMIT ngay.
                     Luc su co xay ra, lenh tru tien DA LUU VINH VIEN.

                  2. KB2 - co @Transactional:      proxy goi setAutoCommit(false).
                     Moi lenh nam CHO. Exception bay ra -> proxy goi rollback().
                     LUU Y: proxy KHONG tu hoan tac gi. DATABASE moi la ben hoan tac.

                  3. KB3 - nuot exception:         proxy dung NGOAI method.
                     No chi biet co loi khi exception BAY RA KHOI method.
                     Bat lai va khong nem tiep = proxy thay method chay em = COMMIT.
                     -> Muon vua bat vua rollback: nem lai unchecked exception,
                        hoac goi setRollbackOnly().

                  4. KB4 - checked exception:      mac dinh Spring CHI rollback voi
                     RuntimeException va Error. Checked exception thi van COMMIT.
                     -> Sua: @Transactional(rollbackFor = Exception.class)

                  CHECKLIST khi debug loi "mat tien":
                     - method co @Transactional khong?
                     - co bi goi qua this.method() khong?   (self-invocation)
                     - method co private / final khong?
                     - ben trong co try/catch nuot loi khong?
                     - exception nem ra la checked hay unchecked?
                  """);
        return out.toString();
    }

    private void chay(StringBuilder out, String tieuDe, HanhDong hanhDong) {
        out.append('\n').append("=".repeat(74)).append('\n');
        out.append(tieuDe).append('\n');
        out.append("=".repeat(74)).append('\n');

        reset();
        log.batDau();

        LedgerReport truoc = report();
        out.append(dongSoDu("SO DU BAN DAU", truoc)).append('\n');
        out.append("     ").append("-".repeat(62)).append('\n');

        try {
            hanhDong.chay("alice", "bob");
            log.ghi("KET THUC: khong co exception nao bay ra khoi method");
        } catch (Exception e) {
            StackTraceElement noiNo = e.getStackTrace()[0];
            log.ghi("*** EXCEPTION BAY RA: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            log.ghi("    tai " + noiNo.getMethodName() + "()  dong " + noiNo.getLineNumber());
            log.ghi("    loai: " + (e instanceof RuntimeException ? "UNCHECKED" : "CHECKED"));
        }

        log.doc().forEach(dong -> out.append("     ").append(dong).append('\n'));

        LedgerReport sau = report();
        out.append("     ").append("-".repeat(62)).append('\n');
        out.append(dongSoDu("SO DU CUOI", sau)).append('\n');

        BigDecimal chenh = sau.total().subtract(truoc.total());
        if (chenh.signum() == 0) {
            out.append("  KET QUA: TONG KHONG DOI  ->  AN TOAN. Da rollback.\n");
        } else {
            out.append("  KET QUA: TONG LECH ").append(chenh)
                    .append("   ->  MAT TIEN. Khong co gi rollback ca.\n");
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
