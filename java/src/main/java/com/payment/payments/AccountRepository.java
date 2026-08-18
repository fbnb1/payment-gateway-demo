package com.payment.payments;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Bạn chỉ khai báo interface này. KHÔNG viết class hiện thực nào cả.
 * Spring Data sinh ra class hiện thực lúc chạy — lại là proxy, đúng cơ chế note 09.
 */
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Khoá BI QUAN. Sinh ra dung cau: SELECT ... FROM accounts WHERE id = ? FOR UPDATE
     *
     * LUU Y: findById() co san VAN KHONG KHOA. @Lock chi anh huong dung method
     * duoc gan annotation. Muon doc de hien thi thi dung findById; muon doc de
     * GHI DE LEN thi dung method nay.
     *
     * PESSIMISTIC_WRITE = khoa doc quyen (FOR UPDATE).
     * PESSIMISTIC_READ  = khoa chia se  (FOR SHARE) - nhieu nguoi doc duoc,
     *                     nhung khong ai ghi duoc. Khong dung o day.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockById(String id);
}
