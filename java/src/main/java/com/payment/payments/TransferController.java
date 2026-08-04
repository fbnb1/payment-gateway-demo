package com.payment.payments;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {

    private static final BigDecimal SO_DU_BAN_DAU = new BigDecimal("1000.00");
    private static final BigDecimal SO_TIEN_CHUYEN = new BigDecimal("100.00");

    private final TransferService transferService;
    private final AccountRepository accounts;

    public TransferController(TransferService transferService, AccountRepository accounts) {
        this.transferService = transferService;
        this.accounts = accounts;
    }

    /** Đưa sổ cái về trạng thái đầu: Alice 1000, Bob 1000, tổng 2000. */
    @PostMapping("/demo/reset")
    public LedgerReport reset() {
        accounts.deleteAll();
        accounts.saveAll(List.of(
                new Account("alice", SO_DU_BAN_DAU),
                new Account("bob", SO_DU_BAN_DAU)));
        return report();
    }

    @GetMapping("/demo/ledger")
    public LedgerReport ledger() {
        return report();
    }

    /** Chuyển 100 từ Alice sang Bob — KHÔNG có transaction. Sẽ lỗi giữa chừng. */
    @PostMapping("/demo/transfer-without-transaction")
    public String withoutTransaction() {
        try {
            transferService.transferWithoutTransaction("alice", "bob", SO_TIEN_CHUYEN);
            return "khong loi (khong mong doi)";
        } catch (RuntimeException e) {
            return "LOI: " + e.getMessage();
        }
    }

    /** Y hệt trên, nhưng method service có @Transactional. */
    @PostMapping("/demo/transfer-with-transaction")
    public String withTransaction() {
        try {
            transferService.transferWithTransaction("alice", "bob", SO_TIEN_CHUYEN);
            return "khong loi (khong mong doi)";
        } catch (RuntimeException e) {
            return "LOI: " + e.getMessage();
        }
    }

    /** Tên class thật của bean — bằng chứng Spring đã bọc proxy. */
    @GetMapping("/demo/bean-class")
    public String beanClass() {
        return transferService.getClass().getName();
    }

    private LedgerReport report() {
        BigDecimal alice = accounts.findById("alice").map(Account::getBalance).orElse(BigDecimal.ZERO);
        BigDecimal bob = accounts.findById("bob").map(Account::getBalance).orElse(BigDecimal.ZERO);
        return new LedgerReport(alice, bob, alice.add(bob));
    }
}
