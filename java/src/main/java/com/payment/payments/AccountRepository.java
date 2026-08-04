package com.payment.payments;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Bạn chỉ khai báo interface này. KHÔNG viết class hiện thực nào cả.
 * Spring Data sinh ra class hiện thực lúc chạy — lại là proxy, đúng cơ chế note 09.
 */
public interface AccountRepository extends JpaRepository<Account, String> {
}
