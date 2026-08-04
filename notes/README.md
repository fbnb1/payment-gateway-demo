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
| 02 | [Spring IoC Container](02-spring-ioc-container.md) | **vì sao cần IoC (3 nỗi đau của `new`)** · IoC vs DI · bean vs bean definition · ApplicationContext · container chỉ là một `Map` · 2 pha khởi động · `@Component` vs `@Bean` · `@SpringBootApplication` · singleton scope · Spring singleton ≠ GoF singleton |
| 03 | [Classloader & Reflection](03-classloader-reflection.md) | bytecode · classloader · lazy loading · metaspace · reflection · **cách Spring thật sự hoạt động** · phá `private` · cái giá của reflection |
| 04 | [Dependency Injection](04-dependency-injection.md) | dependency là gì · 3 kiểu inject · **4 lý do constructor thắng** · khi nào bỏ được `@Autowired` |
| 05 | [Stateless & Thread Safety](05-stateless-thread-safety.md) | state là gì · singleton + nhiều thread = race condition · dữ liệu request để đâu · quy tắc `final` |
| 06 | [Git chuyên nghiệp](06-git-professional.md) | Conventional Commits · atomic commit · Git Flow vs GitHub Flow vs Trunk-Based · merge vs rebase · tag + SemVer · `reset` vs `revert` · `reflog` · `bisect` · branch protection · hooks · release tự động |
| 07 | [Auto-configuration](07-spring-boot-autoconfiguration.md) | 1 class → 145 bean · 2 tầng lọc · `AutoConfiguration.imports` · họ `@Conditional...` · **auto-config tự lùi bước** · starter · transitive dependency + `dependency:tree` · eager vs lazy + fail-fast · đọc CONDITIONS EVALUATION REPORT · daemon vs non-daemon thread |
| 08 | [Java `record`](08-java-records.md) | transparent carrier · compiler sinh gì · accessor `amount()` vs `getAmount()` · compact constructor · **có thay thế DTO không** · vì sao KHÔNG làm JPA entity · Jackson · Bean Validation · record ≠ bean |
| 09 | [Proxy & `@Transactional`](09-proxy-and-transactional.md) | proxy là gì · JDK vs CGLIB · **proxy KHÔNG rollback, database mới rollback** · ThreadLocal giữ Connection · **4 kịch bản đo thật: 3/4 mất tiền không báo lỗi** (không annotation · nuốt exception · checked exception) · self-invocation · `private`/`final` · annotation nào là proxy · A gọi B · ACID · đừng gọi API mạng trong transaction |

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

- `propagation` / `isolation` của `@Transactional` *(Phase 1)*
- Vòng đời bean đầy đủ: `@PostConstruct`, `@PreDestroy`, `BeanPostProcessor`
- Các scope khác: `prototype`, `request`, `session`
- `@Qualifier` / `@Primary` / circular dependency
- Hợp đồng `equals` / `hashCode` · `Integer` cache −128..127
- Collections internals: `HashMap` bên trong là gì
- `synchronized` · `volatile` · Java Memory Model · `ThreadLocal` · virtual threads

---

## Xem thêm

- [../PROGRESS.md](../PROGRESS.md) — tiến độ build
- [../READING_PLAN.md](../READING_PLAN.md) — kế hoạch đọc theo phase
