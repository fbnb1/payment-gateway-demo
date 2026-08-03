# 09 — Proxy: vì sao `@Transactional` đôi khi im lặng không chạy

> Yêu cầu đọc trước: [02-spring-ioc-container.md](02-spring-ioc-container.md) (container là một `Map`), [03-classloader-reflection.md](03-classloader-reflection.md) (reflection).
> Đây là câu hỏi tách **senior thật** khỏi **senior theo bản năng**.

---

## Tóm tắt 30 giây

1. Spring cần chạy code **trước/sau** method của bạn (mở transaction, commit, rollback) mà **không được sửa class của bạn**.
2. Giải pháp: **bọc** object thật bằng một object khác **cùng kiểu** → gọi là **proxy**.
3. Container **cất proxy vào map**, không cất object thật.
4. Mọi lời gọi **từ bên ngoài** đi qua proxy → `@Transactional` chạy.
5. Lời gọi **`this.method()` bên trong** thì `this` = **object thật**, không phải proxy → **bỏ qua proxy** → `@Transactional` **vô hiệu, im lặng**.

---

## 1. Bài toán

```java
@Service
public class PaymentService {

    @Transactional
    public void transfer() { ... }
}
```

Bạn **chưa bao giờ** viết `connection.setAutoCommit(false)` hay `commit()`. Vậy code đó ở đâu?

Nó **không nằm trong class của bạn** — bạn tự viết class đó, bạn biết trong đó có gì. Nó cũng không thể được nhét vào, vì Spring là thư viện viết từ nhiều năm trước, không sửa được file `.java` của bạn.

⇒ Nó phải nằm **bên ngoài**, và phải **chặn được** lời gọi trước khi lời gọi chạm tới method của bạn.

---

## 2. Lời giải: bọc lại — proxy

> **Proxy = một object CÙNG KIỂU với object thật, giữ tham chiếu tới object thật, và chèn thêm hành vi trước/sau khi chuyển lời gọi vào trong.**

Điều kiện sống còn: **cùng kiểu**. Vì `PaymentController` khai báo:

```java
private final PaymentService paymentService;
```

Cái bọc không phải `PaymentService` thì không nhét vừa field đó. Nên nó phải **kế thừa** `PaymentService` — để bên gọi **không phân biệt nổi**.

### Viết tay ra thì thế này

```java
// Object THẬT
class PaymentService {
    public void transfer() { System.out.println("chuyển tiền"); }
}

// PROXY — cùng kiểu, ôm object thật bên trong
class PaymentServiceProxy extends PaymentService {

    private final PaymentService target;              // ← object thật

    PaymentServiceProxy(PaymentService target) { this.target = target; }

    @Override
    public void transfer() {
        beginTransaction();                            // ← TRƯỚC
        try {
            target.transfer();                         // ← gọi object thật
            commit();                                  // ← SAU
        } catch (Exception e) {
            rollback();
            throw e;
        }
    }
}
```

**Đó là toàn bộ `@Transactional`.** Spring không viết tay class này — nó **sinh ra lúc chạy** trong bộ nhớ. Không có ma thuật, giống hệt kết luận ở [note 03](03-classloader-reflection.md).

---

## 3. Hai kiểu proxy

| | **JDK dynamic proxy** | **CGLIB** |
|---|---|---|
| Cách tạo | tạo class implement **interface** | tạo **class con** của class thật |
| Yêu cầu | bean phải có interface | class không `final`, method không `final`/`private` |
| Có sẵn trong JDK | ✅ | ❌ (thư viện, Spring đã nhúng sẵn) |

**Spring Boot mặc định dùng CGLIB.** Bằng chứng trong CONDITIONS EVALUATION REPORT của chính project này:

```
AopAutoConfiguration.ClassProxyingConfiguration matched:
   - @ConditionalOnBooleanProperty (spring.aop.proxy-target-class=true) matched
```

`proxy-target-class=true` = *"tạo proxy bằng cách kế thừa class"*.

---

## 4. Mảnh ghép quyết định

> **Container KHÔNG cất object thật vào map. Nó cất PROXY.**

```
   singletonObjects Map
   ┌──────────────────────────────────────────────┐
   │  "paymentService"  →  PaymentServiceProxy    │  ← thứ nằm trong map
   │                            │                 │
   │                            │ target          │
   │                            ▼                 │
   │                     PaymentService (thật)    │  ← ẩn bên trong proxy
   └──────────────────────────────────────────────┘
```

- `ctx.getBean(PaymentService.class)` → trả về **proxy**
- Field trong controller → giữ **proxy**
- Mọi lời gọi **từ bên ngoài** → qua lớp bọc

Proxy được tạo ở **pha 2 — post-process** của quá trình khởi động ([note 02 mục 5](02-spring-ioc-container.md)).

---

## 5. ⚠️ Bẫy self-invocation

```java
@Service
public class PaymentService {

    public void doTransfer() {
        this.transfer();          // ← ĐÂY
    }

    @Transactional
    public void transfer() { ... }
}
```

Controller gọi `doTransfer()` → từ ngoài vào → qua proxy → proxy gọi `target.doTransfer()`.

Giờ code đang chạy **bên trong object thật**. Đến `this.transfer()`:

> **`this` trỏ tới object THẬT, không phải proxy.**

Object thật **không biết proxy tồn tại** — nó là class bạn viết, y nguyên. Nên lời gọi nhảy **thẳng** vào method của chính nó.

```
Controller
    │
    ▼
[ PROXY ]  ──► doTransfer() không có @Transactional → không mở gì
    │
    ▼
PaymentService thật . doTransfer()
    │
    └── this.transfer()        ← this = OBJECT THẬT
            │
            ▼
        transfer() chạy TRỰC TIẾP
        KHÔNG qua proxy  →  KHÔNG có transaction  →  @Transactional VÔ HIỆU
```

### Vì sao nguy hiểm

**Không lỗi. Không cảnh báo. Không log.** Code chạy, test qua, deploy xong. Chỉ khi có sự cố giữa chừng — dữ liệu ghi được một nửa và **không rollback** — bạn mới biết. Với hệ thống payment thì đó là tiền thật.

Cùng loại với bẫy `toLowerCase().contains("Payment")`: **thất bại im lặng**.

---

## 6. Hai hệ quả nữa, cùng một nguyên nhân

Proxy CGLIB = **class con**. Muốn chèn hành vi thì phải **override** được method:

| Đặt `@Transactional` lên | Kết quả |
|---|---|
| method `private` | ❌ vô hiệu — không override được |
| method `final` | ❌ vô hiệu — không override được |
| class `final` | ❌ vô hiệu — không kế thừa được |

Cả ba **im lặng thất bại**.

> Đây cũng là lý do [note 08](08-java-records.md) nói `record` không làm được JPA `@Entity`: record luôn `final` → Hibernate không tạo được proxy cho lazy loading. **Cùng một nguyên nhân gốc.**

---

## 7. Sửa thế nào

**(A) Tách sang bean khác** — khuyến nghị

```java
@Service
public class PaymentService {
    private final PaymentProcessor processor;

    public void doTransfer() {
        processor.transfer();      // gọi bean KHÁC → "từ ngoài vào" → qua proxy ✅
    }
}
```
✅ Sạch, không mẹo mực. Thường lộ ra rằng class đang ôm hai trách nhiệm.
❌ Thêm một class.

**(B) Tự inject chính mình**

```java
public PaymentService(@Lazy PaymentService self) { this.self = self; }
...
self.transfer();               // self = PROXY ✅
```
✅ Không phải tách class.
❌ Đọc rất khó hiểu. `@Lazy` bắt buộc — thiếu thì circular dependency.

**(C) `AopContext.currentProxy()`** — cần `@EnableAspectJAutoProxy(exposeProxy = true)`
✅ Không đổi cấu trúc.
❌ Code dính cứng vào API Spring AOP. Ít dùng.

> Nghiêng về **(A)**: khi thấy mình cần gọi `this.someTransactionalMethod()`, thường đó là dấu hiệu class đang làm hai việc.

---

## 8. Không chỉ `@Transactional`

**Mọi** annotation dưới đây chạy bằng đúng cơ chế proxy, nên **dính đúng bẫy self-invocation**:

| Annotation | Proxy làm gì |
|---|---|
| `@Transactional` | mở / commit / rollback transaction |
| `@Async` | đẩy lời gọi sang thread khác |
| `@Cacheable` | tra cache trước, chỉ gọi method khi cache trượt |
| `@Retryable` | gọi lại khi lỗi |
| `@PreAuthorize` | kiểm tra quyền trước |
| `@Repository` | **dịch exception** JDBC/JPA sang `DataAccessException` |

Dòng cuối chính là "tác dụng thật" của `@Repository` ở [note 04 mục 7](04-dependency-injection.md) — giờ đã rõ nó thật sự làm gì.

---

## Câu hỏi phỏng vấn từ phần này

1. `@Transactional` hoạt động thế nào? → *proxy bọc bean, mở/commit/rollback quanh method*
2. Container cất object thật hay proxy vào map? → **proxy**
3. **`this.transactionalMethod()` có mở transaction không? Vì sao?** → *không — `this` là object thật, bỏ qua proxy*
4. JDK dynamic proxy khác CGLIB thế nào? Spring Boot mặc định dùng cái nào? → *interface vs class con; mặc định CGLIB*
5. `@Transactional` trên method `private` có chạy không? → *không, không override được*
6. Vì sao class `final` không proxy được?
7. Kể vài annotation khác cũng dính bẫy self-invocation → *`@Async`, `@Cacheable`, `@Retryable`, `@PreAuthorize`*
8. Ba cách sửa self-invocation, ưu nhược từng cách?
9. Proxy được tạo ở giai đoạn nào khi Spring khởi động? → *pha 2, post-process*
10. Vì sao bẫy này nguy hiểm hơn một exception? → *thất bại im lặng — không lỗi, không log*

## Còn nợ / sẽ học sau

- `propagation` (`REQUIRED`, `REQUIRES_NEW`, `NESTED`) và `isolation` — **Phase 1**
- Vì sao `@Transactional` mặc định chỉ rollback với unchecked exception
- AspectJ weaving — cách tránh hoàn toàn hạn chế của proxy
- Viết aspect của riêng mình: `@Aspect`, pointcut, advice

## Liên quan

- [02-spring-ioc-container.md](02-spring-ioc-container.md) — pha post-process, nơi proxy sinh ra
- [03-classloader-reflection.md](03-classloader-reflection.md) — sinh class lúc chạy
- [04-dependency-injection.md](04-dependency-injection.md) — `@Repository` và exception translation
- [08-java-records.md](08-java-records.md) — `final` ⇒ không proxy được ⇒ record không làm entity
