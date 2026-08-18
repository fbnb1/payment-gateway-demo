package com.payment.payments;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * PHASE 1 — do LOST UPDATE bang so lieu that.
 *
 * Kich ban: so du 1000. Mot tram thread, moi thread rut 1.00.
 *   Ky vong (dung ve nghiep vu): 900.00
 *   Thuc te  (READ COMMITTED):   > 900.00  <- tien tu sinh ra
 *
 * Test NAY PHAI DO. Do la muc dich. Chua chong gi ca — dung loi truoc da.
 */
@SpringBootTest
class LostUpdateTest {

    @Autowired
    TransferService transfers;

    @Autowired
    AccountRepository accounts;

    @Test
    void oneHundredConcurrentWithdrawals() throws Exception {
        accounts.save(new Account("LOST", new BigDecimal("1000.00")));

        int threads = 100;
        var startGate = new CountDownLatch(1);        // cong xuat phat
        var finished = new CountDownLatch(threads);   // dem nguoc den khi ca 100 xong

        try (var pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        // TAT CA dung o day cho lenh xuat phat.
                        // Khong co dong nay: 100 thread khoi dong lech nhau vai ms,
                        // chay xong tuan tu, ra dung 900.00 -> tuong code khong co bug.
                        startGate.await();

                        // TODO 1: goi transfers.withdraw("LOST", <mot dong>)
                        //         Nho: BigDecimal, khong phai double.
                            transfers.withdraw( "LOST", BigDecimal.valueOf(1));
                    } catch (Exception e) {
                        // TODO 2: in exception ra.
                        //         Nuot im lang o day = lap lai dung sai lam cua kich ban 3.
                        System.out.println(e.getMessage());
                    } finally {
                        finished.countDown();
                    }
                });
            }

            startGate.countDown();  // ban phat sung -> 100 thread lao vao CUNG LUC
            finished.await();
        }

        BigDecimal actual = accounts.findById("LOST").orElseThrow().getBalance();
        System.out.println("ky vong 900.00  |  thuc te " + actual);

        // BigDecimal: so sanh bang compareTo, KHONG dung isEqualTo/equals.
        // equals() so ca UNSCALED VALUE lan SCALE:
        //     new BigDecimal("900.0").equals(new BigDecimal("900.00")) -> false
        //     new BigDecimal("900.0").compareTo(new BigDecimal("900.00")) -> 0
        // AssertJ co san usingComparator cho viec nay: isEqualByComparingTo.
        assertThat(actual)
                .as("100 lan rut 1.00 tu 1000.00 thi phai con dung 900.00")
                .isEqualByComparingTo(new BigDecimal("900.00"));
    }
}
