# notes/ — Sổ khái niệm

Mỗi lần Bridge giải thích một khái niệm, nó được lưu thành một file ở đây để xem lại.
Mục tiêu: **kiến thức mang đi phỏng vấn được**, không phải ghi chép cho có.

## Quy ước

- Một file cho một **cụm khái niệm** liên quan, đánh số theo thứ tự học.
- Mỗi file có:
  - **Định nghĩa chuẩn** — dùng được nguyên văn khi phỏng vấn
  - **Ví von / sơ đồ** — để hiểu, không phải để học thuộc
  - **Câu hỏi phỏng vấn** ở cuối — tự kiểm tra
  - **Còn nợ / sẽ học sau** — thứ đã park lại
  - **Liên quan** — link chéo, để kiến thức thành mạng lưới chứ không phải danh sách
- Tiếng Việt cho phần giải thích; **thuật ngữ kỹ thuật giữ nguyên tiếng Anh** (để tra tài liệu và nói chuyện phỏng vấn được).

---

## Mục lục

| # | File | Nội dung chính |
|---|---|---|
| 01 | [JVM Memory Model](01-jvm-memory-model.md) | stack vs heap vs metaspace · GC · biến ≠ object · `==` vs `.equals()` · `null` · String pool |
| 02 | [Spring IoC Container](02-spring-ioc-container.md) | IoC vs DI · bean · ApplicationContext · container chỉ là một `Map` · 2 pha khởi động · `@Component` vs `@Bean` · `@SpringBootApplication` · singleton scope · Spring singleton ≠ GoF singleton |
| 03 | [Classloader & Reflection](03-classloader-reflection.md) | bytecode · classloader · lazy loading · metaspace · reflection · **cách Spring thật sự hoạt động** · phá `private` · cái giá của reflection |
| 04 | [Dependency Injection](04-dependency-injection.md) | dependency là gì · 3 kiểu inject · **4 lý do constructor thắng** · khi nào bỏ được `@Autowired` |
| 05 | [Stateless & Thread Safety](05-stateless-thread-safety.md) | state là gì · singleton + nhiều thread = race condition · dữ liệu request để đâu · quy tắc `final` |

---

## Mạch logic xuyên suốt 5 note

```
   classloader   nạp bản thiết kế class từ đĩa vào bộ nhớ
        ↓
   reflection    đọc annotation, hỏi constructor cần gì, gọi constructor
        ↓
   container     cất object vào Map  →  singleton: một object, phát chung reference
        ↓
   inject        đưa dependency vào qua constructor  →  final  →  bất biến
        ↓
   stateless     không giữ dữ liệu request  →  an toàn khi nhiều thread dùng chung
```

---

## Hàng đợi — khái niệm đã park, sẽ viết note sau

- **Proxy** — trả lời: `@Transactional` mà gọi `this.method()` thì có mở transaction không? *(ưu tiên cao nhất)*
- Vòng đời bean đầy đủ: `@PostConstruct`, `@PreDestroy`, `BeanPostProcessor`
- Các scope khác: `prototype`, `request`, `session`
- `@Qualifier` / `@Primary` / circular dependency
- Hợp đồng `equals` / `hashCode` · `Integer` cache −128..127
- `@EnableAutoConfiguration` hoạt động ra sao *(topic lấp lỗ hổng tuần này)*
- Collections internals: `HashMap` bên trong là gì
- `synchronized` · `volatile` · Java Memory Model · `ThreadLocal` · virtual threads

---

## Xem thêm

- [../PROGRESS.md](../PROGRESS.md) — tiến độ build
- [../READING_PLAN.md](../READING_PLAN.md) — kế hoạch đọc theo phase
