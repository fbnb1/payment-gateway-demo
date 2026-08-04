# 09 — Proxy & `@Transactional`

> Yêu cầu đọc trước: [02](02-spring-ioc-container.md) (container là một `Map`), [03](03-classloader-reflection.md) (reflection).
> **Mọi con số trong note này đo thật** trên `java/` của project — chạy `GET /demo/run`.

---

## Tóm tắt 30 giây

1. Spring cần chạy code **trước/sau** method của bạn mà **không sửa được class của bạn**.
2. Giải pháp: **bọc** object thật bằng object **cùng kiểu** → **proxy**. Container cất **proxy**, không cất object thật.
3. Proxy chỉ làm 3 việc: `setAutoCommit(false)` → chạy code của bạn → `commit()` hoặc `rollback()`.
4. ⚠️ **Proxy KHÔNG hoàn tác gì. DATABASE mới là bên hoàn tác.**
5. Proxy đứng **ngoài** method, nên nó **chỉ biết có lỗi khi exception BAY RA KHỎI method**. Đây là gốc của mọi cái bẫy bên dưới.

---

## 1. Bài toán

```java
@Transactional
public void transfer() { ... }
```

Bạn **chưa bao giờ** viết `setAutoCommit(false)` hay `commit()`. Code đó không nằm trong class của bạn — bạn tự viết class đó. Nó cũng không thể được nhét vào: Spring là thư viện viết từ nhiều năm trước, không sửa file `.java` của bạn được.

⇒ Nó phải nằm **bên ngoài**, và phải **chặn được** lời gọi trước khi chạm tới method của bạn.

---

## 2. Proxy

> **Proxy = object CÙNG KIỂU với object thật, giữ tham chiếu tới object thật, chèn hành vi trước/sau rồi chuyển lời gọi vào trong.**

Điều kiện sống còn: **cùng kiểu**. Vì controller khai báo `private final TransferService transferService` — cái bọc không phải `TransferService` thì không nhét vừa.

```java
class TransferServiceProxy extends TransferService {      // ← cung kieu

    private final TransferService target;                  // ← object that

    @Override
    public void transfer(...) {
        conn.setAutoCommit(false);                         // TRUOC
        try {
            target.transfer(...);                          // goi object that
            conn.commit();                                 // SAU
        } catch (RuntimeException e) {
            conn.rollback();
            throw e;
        }
    }
}
```

Spring sinh class này **lúc chạy**, không có file để mở.

### Bằng chứng

`GET /demo/bean-class` trên project này in ra:

```
com.payment.payments.TransferService$$SpringCGLIB$$0
```

Class đó **bạn không viết**.

---

## 3. Hai kiểu proxy

| | **JDK dynamic proxy** | **CGLIB** |
|---|---|---|
| Cách tạo | class implement **interface** | **class con** của class thật |
| Yêu cầu | bean phải có interface | class không `final`, method không `final`/`private` |

**Spring Boot mặc định CGLIB.** Bằng chứng trong CONDITIONS EVALUATION REPORT của project:

```
AopAutoConfiguration.ClassProxyingConfiguration matched:
   - @ConditionalOnBooleanProperty (spring.aop.proxy-target-class=true) matched
```

> Interface **không thay thế** proxy — nó là **một trong hai con đường** để proxy đội lốt được.

---

## 4. ⭐ Proxy KHÔNG rollback — database rollback

Hiểu nhầm phổ biến nhất.

Proxy **không nhớ số dư cũ, không lưu bản sao, không undo dòng nào**. Nó chỉ **gọi lệnh** lên connection. **Proxy là cái công tắc, không phải cỗ máy.**

### Vì sao database hoàn tác được?

Khi `autoCommit = false`, DB **không ghi đè dữ liệu thật ngay**. Nó ghi vào vùng chờ và đánh dấu *"phiên này chưa chốt"*.

- Ai khác đọc lúc đó → thấy **giá trị cũ**
- `commit` → thành chính thức
- `rollback` → **vứt vùng chờ**, dữ liệu thật chưa hề bị đụng

> "Rollback" không phải *hoàn tác*. Là **chưa từng làm**.
> Ví von: viết bút chì trên giấy nháp. `commit` = chép sang sổ chính. `rollback` = vò tờ nháp.

### Có proxy vs không proxy

| | Không proxy | Có proxy |
|---|---|---|
| Ai gọi `setAutoCommit(false)` | **không ai** | proxy |
| Ai gọi `commit`/`rollback` | không ai | proxy |
| DB có rollback được không | **có** — nhưng không ai bảo nó | có |

Mất tiền **không phải vì thiếu proxy**, mà vì **không ai tắt autocommit** → mỗi `UPDATE` commit ngay tại chỗ → lúc lỗi thì đã chép sang sổ chính rồi.

Viết tay vẫn rollback được y hệt, không cần Spring:

```java
conn.setAutoCommit(false);
try { debit(...); credit(...); conn.commit(); }
catch (Exception e) { conn.rollback(); throw e; }
```

Proxy chỉ giúp **không phải viết 200 lần** và **không thể quên**.

---

## 5. Làm sao mọi DAO dùng chung một Connection?

Code của bạn không truyền `conn` xuống:

```java
accountDao.debit(from, amount);     // khong nhan Connection
accountDao.credit(to, amount);      // khong nhan Connection
```

**Từ ThreadLocal** — một cái hộp gắn riêng từng thread.

```
Thread #1 (request A)  →  hop rieng  →  Connection@111
Thread #2 (request B)  →  hop rieng  →  Connection@222
```

- Proxy **bỏ** connection vào hộp của thread hiện tại
- Mọi DAO chạy trên thread đó **mở hộp lấy ra** — cùng một connection
- Proxy **dọn** hộp khi xong

⇒ 3 DAO, 1 connection, 1 lần commit. **Atomicity.**

> Đây cũng là lý do `@Async` phá transaction: **thread khác = hộp khác = connection khác**.

---

## 6. 🔥 Bốn kịch bản — đo thật trên project này

Cùng một sự cố (PSP lỗi sau khi trừ tiền), bốn cách viết. Chạy `GET /demo/run`.

| # | Cách viết | TỔNG sau | Kết quả |
|---|---|---|---|
| 1 | **không** `@Transactional` | 1900.00 | ❌ **MẤT 100** |
| 2 | có `@Transactional` | 2000.00 | ✅ an toàn |
| 3 | có `@Transactional` + `try/catch` **nuốt** exception | 1900.00 | ❌ **MẤT 100** |
| 4 | có `@Transactional` + **checked** exception | 1900.00 | ❌ **MẤT 100** |

> **Ba trên bốn mất tiền. Cả ba đều không báo lỗi gì. HTTP vẫn 200.**
> Có `@Transactional` **KHÔNG** đồng nghĩa với an toàn.

### Kịch bản 3 — nuốt exception

```java
@Transactional
public void transfer() {
    accountDao.debit(...);
    try {
        callPsp();
    } catch (Exception e) {
        log.error("PSP loi", e);      // ← NUOT, khong nem lai
    }
}                                      // ← method thoat BINH THUONG -> COMMIT
```

Proxy đứng **ngoài** method. Không có exception bay ra → nó thấy method chạy êm → commit phần đã trừ tiền.

⚠️ Đây là bug mất tiền phổ biến nhất trong hệ thống payment thật. **Log có ghi "loi", tiền vẫn trừ.**

**Không phải "cấm `try/catch`".** Quy tắc đúng:

> **`try/catch` thoải mái. Nhưng NUỐT exception thì mất rollback.**

Hai cách sửa:

```java
// Cach 1 - bat, xu ly, roi nem tiep  (dung nhieu hon)
catch (PspException e) {
    log.error("PSP loi", e);
    throw new TransferFailedException(e);       // unchecked -> proxy thay -> rollback
}

// Cach 2 - bat, khong nem, nhung danh dau
catch (PspException e) {
    log.error("PSP loi", e);
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
}
```

### Kịch bản 4 — checked exception

> **Mặc định Spring CHỈ rollback với `RuntimeException` và `Error`.** Checked exception thì **vẫn COMMIT**.

```java
@Transactional(rollbackFor = Exception.class)      // ← sua
```

---

## 7. ⚠️ Bẫy self-invocation

```java
@Service
public class PaymentService {

    public void doTransfer() {
        this.transfer();          // ← DAY
    }

    @Transactional
    public void transfer() { ... }
}
```

Trong bộ nhớ có **hai object**:

```
   B (proxy)  ──target──►  A (object that)
```

**B biết A. A KHÔNG biết B.** `A` là class bạn viết — trong đó không có field nào trỏ tới `B`, và `A` không biết `B` tồn tại.

Code trong `createTwice` đang chạy **bên trong A**, nên `this` = **A**. `this.transfer()` = `A.transfer()`. **B không xuất hiện** — không phải bị bỏ qua, mà là `A` **không có đường nào gọi tới B**.

```
paymentService.transfer()   =  B.transfer()  →  mo transaction  →  A.transfer()   ✅
this.transfer()             =  A.transfer()                                       ❌
```

> **`this` luôn là object đang chạy. Object thật không giữ tham chiếu tới proxy.**

### Sửa

| Cách | Ưu | Nhược |
|---|---|---|
| **(A) Tách sang bean khác** ✅ | sạch, không mẹo; thường lộ ra class đang ôm 2 trách nhiệm | thêm một class |
| (B) Tự inject `@Lazy PaymentService self` | không phải tách class | đọc khó hiểu; thiếu `@Lazy` là circular dependency |
| (C) `AopContext.currentProxy()` | không đổi cấu trúc | dính cứng API Spring AOP |

---

## 8. `private` / `final` — chỉ là luật kế thừa Java

Không liên quan Spring. Proxy là **class con**, muốn chèn code thì phải **override**:

```java
class A {
    private void x() { }      // class con KHONG NHIN THAY
    final   void y() { }      // Java CAM override
            void z() { }      // binh thuong
}
class B extends A {
    @Override void x() { }    // ❌ compile error
    @Override void y() { }    // ❌ compile error
    @Override void z() { }    // ✅
}
```

| `@Transactional` đặt lên | Kết quả |
|---|---|
| method `public` | ✅ chạy |
| method `private` | ❌ **im lặng không chạy** |
| method `final` | ❌ **im lặng không chạy** |
| class `final` | ❌ không tạo được proxy nào cả |

> Cùng lý do khiến `record` không làm được JPA `@Entity`: record luôn `final` → Hibernate không tạo được proxy cho lazy loading. Xem [note 08](08-java-records.md).

---

## 9. Annotation nào dùng proxy, annotation nào không

**Câu hỏi tự phân loại:** *"annotation này có cần chạy code TRƯỚC hoặc SAU method của tôi không?"*

| Loại | Cơ chế | Ví dụ |
|---|---|---|
| **Thêm hành vi QUANH lời gọi** | **proxy** ✅ | `@Transactional` `@Async` `@Cacheable` `@Retryable` `@PreAuthorize` `@Validated` |
| Đánh dấu để container biết | không proxy | `@Component` `@Service` `@Configuration` |
| Bơm giá trị vào | reflection | `@Autowired` `@Value` |
| Ánh xạ URL | bảng tra cứu | `@RestController` `@GetMapping` |
| Ánh xạ bảng DB | Hibernate | `@Entity` `@Id` |

⇒ **Mọi annotation dùng proxy đều dính bẫy self-invocation.** `@Async` gọi qua `this` cũng chạy đồng bộ, không báo lỗi.

*(`@Repository` là ngoại lệ trong nhóm "đánh dấu": nó **có** tạo proxy, để dịch exception JDBC/JPA sang `DataAccessException`.)*

### Nhiều annotation trên cùng một bean?

**Không có nhiều proxy song song — chúng LỒNG NHAU.**

```
   Map: "service" → proxy ngoai cung
                        │
                        ▼  (chuoi interceptor)
                    ... cac lop trong ...
                        ▼
                    object that
```

`getClass()` chỉ thấy lớp ngoài cùng. Thứ tự điều khiển bằng `@Order`, và **thứ tự quan trọng**: cache nằm ngoài transaction thì trả cache mà không mở transaction; ngược lại thì mở transaction rồi mới phát hiện đã có cache — lãng phí một connection.

Xem chuỗi thật: `((Advised) bean).getAdvisors()`.

---

## 10. A gọi B — transaction chạy tới đâu?

Nguyên tắc: **transaction bắt đầu ở `@Transactional` NGOÀI CÙNG được gọi từ bên ngoài.**

| A | B (bean khác, `REQUIRED`) | Kết quả |
|---|---|---|
| **không** `@Transactional` | có | B mở transaction **mới**, chỉ bao code của B. Code của A **nằm ngoài** |
| **có** `@Transactional` | có | B **tham gia** transaction của A → **một transaction chung** ✅ |
| **có** `@Transactional` | có, `REQUIRES_NEW` | B mở transaction **riêng, độc lập** |
| cùng class, gọi `this.b()` | có | **không có transaction nào cả** |

⚠️ Dòng 1 hay bị hiểu ngược. Muốn A và B chung transaction thì **A phải có `@Transactional`**.

`REQUIRED` (mặc định) nghĩa là: *"có transaction sẵn thì nhảy vào dùng chung, chưa có thì mở mới."*

### `UnexpectedRollbackException`

A gọi B (B có `@Transactional`), B ném lỗi, **A bắt lại và đi tiếp**. Đến lúc A commit → **nổ `UnexpectedRollbackException`**.

Vì B đã đánh dấu transaction là `rollback-only`, và A không huỷ dấu đó được. Transaction đã "chết" từ lúc B lỗi.

---

## 11. ACID — gắn vào chính demo này

| | Nghĩa | Trong demo |
|---|---|---|
| **A**tomicity | Tất cả cùng thành công, hoặc **không gì cả** | KB2: debit bị huỷ vì credit không chạy được |
| **C**onsistency | Bất biến của dữ liệu **không bị phá** | **TỔNG = 2000.00** — KB1/3/4 phá, ra 1900 |
| **I**solation | Nhiều transaction đồng thời **không giẫm lên nhau** | chưa chạm — **Phase 1** |
| **D**urability | Đã commit thì **không mất**, kể cả mất điện | DB ghi nhật ký xuống đĩa trước khi báo "xong" |

Hai chi tiết mức senior:

- **Về C:** Kleppmann nói thẳng trong **DDIA Ch.7** — *consistency* không thực sự thuộc về database. DB chỉ ép được ràng buộc bạn khai (khoá ngoại, `CHECK`). Bất biến kiểu "tổng tiền không đổi" là **của ứng dụng**. Chữ C bị nhét vào cho từ ACID đọc xuôi.
- **Về D:** không tuyệt đối. Commit rồi mà đĩa hỏng, hoặc replica chưa kịp nhận mà primary chết → vẫn mất. Đó là lý do có replication — **DDIA Ch.5**, Phase 6.

⇒ `A` và `I` database làm thật. `C` là việc của bạn. `D` là việc của hạ tầng.

---

## 12. ⚠️ Đừng gọi API mạng bên trong transaction

```java
@Transactional
public void transfer() {
    accountDao.debit(...);
    pspClient.charge(...);         // ← goi mang, co the mat 30 giay
    accountDao.credit(...);
}
```

Suốt 30 giây đó **connection database bị giữ chặt**. 100 request đồng thời → **pool cạn** → cả hệ thống đứng, kể cả request không liên quan.

> **Transaction phải ngắn nhất có thể. Gọi mạng để NGOÀI transaction.**

Đây chính là lý do tồn tại của **outbox pattern** — Phase 2 trong lộ trình.

---

## Bản trả lời phỏng vấn — học thuộc 3 đoạn này

> `@Transactional` cho Spring bọc bean bằng một proxy. Proxy gọi `setAutoCommit(false)`, gắn connection vào **ThreadLocal** để mọi DAO trong luồng dùng chung, rồi `commit` hoặc `rollback` khi method kết thúc. **Bản thân proxy không hoàn tác gì — database mới làm việc đó.**
>
> Bốn chỗ nó **im lặng** không chạy: gọi qua `this` (self-invocation), method `private`/`final`, **nuốt exception bằng `try/catch`**, và **checked exception** (mặc định chỉ rollback với unchecked).
>
> Transaction bắt đầu ở `@Transactional` ngoài cùng **được gọi từ bên ngoài** — muốn A và B chung transaction thì A phải có annotation.

---

## Câu hỏi phỏng vấn từ phần này

1. `@Transactional` hoạt động thế nào? → *proxy bọc bean, `setAutoCommit(false)` → commit/rollback*
2. **Proxy tự hoàn tác dữ liệu à?** → *không — database hoàn tác; proxy chỉ gọi lệnh*
3. Container cất object thật hay proxy? → **proxy**
4. **`this.transactionalMethod()` có mở transaction không?** → *không*
5. **`try/catch` nuốt exception thì sao?** → *proxy không thấy lỗi → COMMIT*
6. **Checked exception có rollback không?** → *không, trừ khi `rollbackFor`*
7. `@Transactional` trên method `private`/`final`? → *im lặng không chạy*
8. Làm sao mọi DAO dùng chung một connection? → **ThreadLocal**
9. JDK proxy vs CGLIB? Spring Boot mặc định? → *interface vs class con; CGLIB*
10. Annotation nào dùng proxy? → *cái nào chạy code quanh method*
11. Nhiều annotation trên một bean → mấy proxy? → *một proxy, chuỗi interceptor lồng nhau*
12. A không có `@Transactional` gọi B có → chung transaction không? → **không**
13. `UnexpectedRollbackException` sinh ra khi nào?
14. Gọi API bên ngoài trong transaction có sao không? → *giữ connection, cạn pool*
15. ACID là gì? Chữ nào database không thực sự lo? → **C**

## Còn nợ / sẽ học sau

- `propagation` (`REQUIRED` / `REQUIRES_NEW` / `NESTED`) và `isolation` — **Phase 1**, cần Postgres thật
- `@Transactional(readOnly = true)` làm gì
- `LazyInitializationException` và Open Session In View
- Tự viết aspect: `@Aspect`, pointcut, advice
- AspectJ weaving — tránh hoàn toàn hạn chế của proxy

## Liên quan

- [02](02-spring-ioc-container.md) — pha post-process, nơi proxy sinh ra
- [03](03-classloader-reflection.md) — sinh class lúc chạy
- [04](04-dependency-injection.md) — `@Repository` và exception translation
- [08](08-java-records.md) — `final` ⇒ không proxy được ⇒ record không làm entity
