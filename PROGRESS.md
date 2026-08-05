# PROGRESS — Unified Payment Platform (mentored by "Bridge")

> Sổ tay tiến độ cho các phiên sau bám theo. Cập nhật mỗi khi xong một increment.
> **Cập nhật lần cuối: 2026-08-05**

---

## 0. ⚠️ ĐỔI HƯỚNG — đọc trước tiên

Ngày **2026-07-30** project chuyển từ **Python** sang **Java/Spring Boot**.

- **Lý do:** learner đi phỏng vấn và phát hiện mình không giải thích được nền tảng Java (bean, autowire, `==` vs `equals`). Tự mô tả: *"9 năm làm thuần theo bản năng, đọc code rồi bắt chước"*. **Mục tiêu thật của project bây giờ là lấy kiến thức đi phỏng vấn**, không phải hoàn thành platform.
- **Python: TẠM DỪNG HOÀN TOÀN** (quyết định 2026-08-05). Code cũ giữ nguyên ở `python/`, không xoá, không phát triển tiếp. Quay lại sau khi Java vững.
- **Không có deadline** → ưu tiên hiểu sâu, xếp topic theo **thứ tự phụ thuộc** thay vì theo tần suất phỏng vấn.

---

## 1. Đang ở đâu

- **Phase hiện tại:** Phase 0 (Java) — **XONG**.
- **Increment vừa xong:** demo `@Transactional` với 4 kịch bản, đo thật — 3/4 mất tiền mà không báo lỗi. Commit `ed896e5`.
- **▶ RESUME HERE:** **Phase 1 — Ledger correctness**. Việc đầu tiên: bật Docker Desktop → `docker compose up -d` → đổi từ H2 sang Postgres thật.

### Stack đã chốt

| | |
|---|---|
| Java | **25 LTS** (Temurin, `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot`) |
| Spring Boot | **4.1.0** → kéo theo Spring Framework **7.0.x** |
| Build | Maven wrapper (`.\mvnw.cmd`) — **luôn dùng wrapper**, không dùng `mvn` toàn cục |
| DB (demo) | **H2 in-memory** — không cần Docker |
| DB (Phase 1+) | **Postgres 16** qua `compose.yaml` ở gốc repo |
| Starter | `spring-boot-starter-webmvc` *(Boot 4 đổi tên từ `-web`)*, `-data-jpa` |

### Layout repo

```
payment system/
├─ compose.yaml        ← hạ tầng dùng chung (Postgres, host port 5433)
├─ java/               ← TRACK CHÍNH
├─ python/             ← tạm dừng, giữ nguyên để đối chiếu
├─ notes/              ← sổ khái niệm (9 note) — xem notes/README.md
├─ PROGRESS.md
└─ READING_PLAN.md
```

### Connection facts (Phase 1 sẽ cần)

- Từ máy: **`localhost:5433`** · trong container: `5432`
- user / password / db = **`payments` / `payments` / `payments`**
- JDBC: `jdbc:postgresql://localhost:5433/payments`
- **Repo:** https://github.com/fbnb1/payment-gateway-demo (nhánh `main`)

### ⚠️ Bẫy môi trường đã gặp

- **Cổng 8080 hay bị chiếm** bởi lần chạy trước chưa tắt hẳn. Kiểm tra: `Get-NetTCPConnection -LocalPort 8080 -State Listen`. Chạy cổng khác: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"`.
- **IntelliJ cache `pom.xml`.** Sửa pom bằng editor ngoài → IntelliJ vẫn báo đỏ. Phải **Reload All Maven Projects**.
- Cổng 5432 và 8000 đã bị stack "infra" của project khác chiếm → vì thế Postgres map host `5433`.

---

## 2. Đã hoàn thành (track Java)

- [x] Restructure repo → `java/` + `python/`, `compose.yaml` giữ ở gốc (commit `abfe896`)
- [x] Scaffold Spring Boot 4.1.0 + Java 25 từ start.spring.io
- [x] In toàn bộ bean trong container, chứng minh **singleton scope** bằng `==` → `true`
- [x] Đo auto-configuration: **1 class khai báo → 145 bean**; bỏ starter web → còn **50**
- [x] Đọc `CONDITIONS EVALUATION REPORT` (`debug=true`)
- [x] `POST /payments` — `@RestController` + `@Service` + constructor injection, DTO bằng **record**
- [x] Proxy viết tay 2 lớp để nhìn thấy cơ chế (commit `5dd523c` — đã xoá khỏi code, còn trong git)
- [x] Thêm JPA + H2, `Account` **entity** (class thường, không record)
- [x] **Demo `@Transactional` 4 kịch bản** với nhật ký tuyến tính: `GET /demo/run`

### Kết quả demo `/demo/run` (số liệu thật)

| # | Cách viết | TỔNG sau | |
|---|---|---|---|
| 1 | không `@Transactional` | 1900.00 | ❌ mất 100 |
| 2 | có `@Transactional` | 2000.00 | ✅ |
| 3 | có `@Transactional` + `try/catch` nuốt exception | 1900.00 | ❌ mất 100 |
| 4 | có `@Transactional` + **checked** exception | 1900.00 | ❌ mất 100 |

> **3/4 mất tiền, cả ba không báo lỗi, HTTP đều 200.**

---

## 3. Sổ khái niệm — `notes/` (9 note)

Quy ước: mỗi khái niệm giải thích xong → một file `notes/NN-*.md` + cập nhật `notes/README.md`.
Mỗi note có: định nghĩa chuẩn · ví von/sơ đồ · **câu hỏi phỏng vấn** · **còn nợ** · **liên quan**.

| # | Note |
|---|---|
| 01 | JVM memory model — stack/heap · `==` vs `equals` · `null` |
| 02 | Spring IoC container — bean · container là một `Map` · singleton |
| 03 | Classloader & reflection — cách Spring thật sự hoạt động |
| 04 | Dependency injection — 4 lý do constructor thắng · `@Autowired` vs `final` |
| 05 | Stateless & thread safety — singleton + thread = race condition |
| 06 | Git chuyên nghiệp — Conventional Commits · branch · tag · CI/CD |
| 07 | Auto-configuration — 2 tầng lọc · `@Conditional...` · eager vs lazy |
| 08 | Java `record` — thay DTO · vì sao KHÔNG làm entity · wither pattern |
| 09 | **Proxy & `@Transactional`** — 4 kịch bản · proxy không rollback · self-invocation |

---

## 4. Phase 1 — kế hoạch (RESUME HERE)

**Tên:** Ledger correctness — *tiền phải đúng khi nhiều người cùng chuyển một lúc*.

Vùng mạnh sẵn của learner (DDIA Ch.7 đã đọc xong). Cần **Postgres thật** — H2 không mô phỏng đủ hành vi khoá.

**Các bước dự kiến:**

- [ ] Bật Docker Desktop → `docker compose up -d` → đổi H2 sang Postgres
- [ ] Thêm Flyway (migration) thay `ddl-auto=create-drop`
- [ ] Dựng **lost update**: hai transaction cùng đọc số dư rồi cùng ghi → mất một giao dịch
- [ ] `SELECT FOR UPDATE` — khoá bi quan
- [ ] Isolation level: `READ COMMITTED` vs `REPEATABLE READ` vs `SERIALIZABLE`
- [ ] **Write skew** — bất biến bị phá dù không có ghi đè trực tiếp
- [ ] Bất biến thật: **số dư không bao giờ âm**, kể cả 100 giao dịch đồng thời
- [ ] `propagation` (`REQUIRED` / `REQUIRES_NEW` / `NESTED`) — park từ Phase 0
- [ ] (Skill) **CI/CD** — GitHub Actions chạy `mvnw test` trên mỗi PR

---

## 5. Cách dạy đang áp dụng — "Lộ trình B2"

- **Lớp build:** mỗi increment một bước nhỏ, learner tự chạy và tự đo.
- **Lớp drill:** mỗi increment kèm câu hỏi "tại sao" mức phỏng vấn, rút từ chính code vừa gõ.
- **Topic lấp lỗ hổng:** mỗi tuần một chủ đề project không chạm tới (`HashMap` internals, GC, JMM…).

**Lưu ý khi mentor:**
- Learner hay xin đáp án thẳng ("viết luôn cho tôi"). Từ chối nhẹ nhưng **hạ tải bằng trắc nghiệm** — việc *chọn* mới là chỗ học. Riêng boilerplate đã thạo (controller/service) thì viết hộ được, đổi lại bắt **review** và trả lời 5 câu.
- **Giải thích ngắn thôi.** Learner đã phản hồi "khó hiểu" nhiều lần khi một turn nhồi >3 ý. Một khái niệm một lượt.
- **Cho nhìn thấy, đừng chỉ nói.** Mọi bước ngoặt đều đến từ số liệu tự đo: 145 bean, `a == b → true`, `real=3 proxy=1`, TỔNG 1900 vs 2000.
- Ngôn ngữ: **tiếng Việt**; code + thuật ngữ kỹ thuật giữ tiếng Anh.

---

## 6. Đã học được gì (knowledge web)

| Chủ đề | Ý chính đã nắm |
|---|---|
| Bean vs object dữ liệu | bean *làm việc* (một cái, stateless) · record *chở dữ liệu* (nhiều cái, bất biến) |
| Container | chỉ là `Map<String,Object>`; khởi động 2 pha: definition → instantiation |
| Auto-configuration | 2 tầng lọc: có phải ứng viên (classpath) → điều kiện `@Conditional...` |
| `@ConditionalOnMissingBean` | bạn khai bean → auto-config **tự lùi**, không phải "ghi đè" |
| Proxy | container cất **proxy**, không cất object thật; `this` không đi qua proxy |
| `@Transactional` | **không tự hoàn tác** — chỉ bật/tắt `autoCommit`, database mới rollback |
| Bốn kiểu mất tiền im lặng | thiếu annotation · self-invocation · nuốt exception · checked exception |
| ThreadLocal | cách proxy gắn 1 Connection cho mọi DAO trong cùng thread |
| Eager vs lazy | classloader **lazy**, bean singleton **eager** → fail-fast |
| Record vs entity | entity cần no-arg ctor + mutable + không `final` → record chặn cả ba |

### "Java-ism" đã được nhắc chỉnh
- Vòng lặp `for` thủ công → `Arrays.stream().filter().forEach()` + method reference
- POJO 40 dòng có getter/setter → `record` 1 dòng
- `@Autowired` trên field → constructor injection, không cần annotation
- Tưởng `final` là lý do bỏ được `@Autowired` → **hai thứ độc lập hoàn toàn**

---

## 7. Park lại — sẽ quay lại

- `propagation` / `isolation` chi tiết → **Phase 1**
- Vòng đời bean: `@PostConstruct`, `@PreDestroy`, `BeanPostProcessor`
- Scope khác: `prototype`, `request`, `session`
- `@Qualifier` / `@Primary` / circular dependency
- Hợp đồng `equals`/`hashCode` · `Integer` cache −128..127
- Collections internals: `HashMap` bên trong
- `synchronized` · `volatile` · Java Memory Model · `ThreadLocal` · virtual threads
- `@EnableAutoConfiguration` tự viết cho thư viện riêng
- Python: quay lại sau khi Java vững

---

## 8. Thước đo tiến độ (ước lượng thô)

### % build platform end-to-end
**~5%** — Phase 0 xong (API + entity + transaction). Còn 5 subsystem + PSP simulator.

```
[#·····················] 5%
```

### % nền tảng Java/Spring cho phỏng vấn
**~35%** — container/DI/proxy/transaction đã vững và **chứng minh được bằng số liệu tự đo**.
Còn: concurrency (JMM, `synchronized`, virtual threads), collections internals, JVM/GC, JPA sâu, testing.

```
[#######···············] 35%
```

| Kỹ năng | % | Ghi chú |
|---|---|---|
| Spring core (IoC/DI/bean/proxy) | ~70% | đã: container, scan, auto-config, DI, proxy, `@Transactional`. Còn: lifecycle, scope, `@Qualifier`, tự viết aspect |
| Java ngôn ngữ | ~30% | đã: record, stream cơ bản, `final`, `==` vs `equals`, kế thừa/override. Còn: generics, collections internals, concurrency, `Optional` |
| JPA / Hibernate | ~15% | đã: entity, repository, `@Transactional`. Còn: quan hệ, lazy loading, N+1, Flyway, query |
| Git chuyên nghiệp | ~55% | đã: note 06 đầy đủ + Conventional Commits thực hành. Còn: branch/PR workflow thật, tag, hooks |
| Docker / Compose | ~25% | `compose.yaml` sẵn nhưng **chưa chạy lần nào ở track Java** |
| CI/CD | 0% | Phase 1 |
| Kubernetes | 0% | Phase 6 |
| DDIA / distributed systems | — | Ch.6 + Ch.7 đã đọc; **Phase 1 mới bắt đầu áp dụng** |

> Các % trên chỉ tính phần **giải thích được / chứng minh được**, không tính kinh nghiệm 9 năm sẵn có về payment domain, SQL, Kafka, DDD/EDA.
