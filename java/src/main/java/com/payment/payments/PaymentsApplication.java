package com.payment.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class PaymentsApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(PaymentsApplication.class, args);

		String[] names = ctx.getBeanDefinitionNames();
		System.out.println("Tổng số bean: " + names.length);

		// ── CHỖ TRỐNG 1 ───────────────────────────────
		// Ví dụ của tôi lọc "Controller". Bạn phải đổi thành: tên có chứa
		// "payment", KHÔNG phân biệt hoa thường. Gợi ý: toLowerCase() + contains()
		Arrays.stream(names)
				.filter(n -> n.toLowerCase().contains("payment"))   // ← sửa dòng này
				.forEach(System.out::println);

		var a = ctx.getBean(PaymentsApplication.class);
		var b = ctx.getBean(PaymentsApplication.class);
		System.out.println("a ?? b -> " + (a == b));

	}   // ← main() đóng ở ĐÂY, sau tất cả
}       // ← class đóng ở đây