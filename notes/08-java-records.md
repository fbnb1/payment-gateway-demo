# 08 — `record`: thay thế DTO cũ

> Java 16+ (preview từ 14). Đây là thay đổi lớn nhất về cách viết class dữ liệu trong Java hiện đại.
> Liên quan: [01-jvm-memory-model.md](01-jvm-memory-model.md) (`equals`), [05-stateless-thread-safety.md](05-stateless-thread-safety.md) (bất biến).

---

## 1. `record` là gì

> **`record` = một loại type dùng để CHỞ dữ liệu bất biến, trong đó trạng thái của object CHÍNH LÀ danh sách thành phần khai báo ở đầu.**

Từ khoá: **transparent carrier** — "cái thùng trong suốt". Nhìn vào khai báo là biết hết những gì nó chứa; không có trạng thái ẩn.

```java
public record PaymentRequest(BigDecimal amount, String currency) {}
```

Một dòng này tự sinh ra:

| Sinh ra | Chi tiết |
|---|---|
| **Canonical constructor** | `PaymentRequest(BigDecimal amount, String currency)` |
| **Accessor** | `amount()`, `currency()` — **không phải** `getAmount()` |
| **`equals()`** | so sánh **tất cả** thành phần |
| **`hashCode()`** | tính từ **tất cả** thành phần |
| **`toString()`** | `PaymentRequest[amount=100.50, currency=VND]` |
| **field `private final`** | cho từng thành phần |

### So với lối cũ

```java
// Java 8 — ~40 dòng
public class PaymentRequest {
    private BigDecimal amount;
    private String currency;
    public PaymentRequest(BigDecimal amount, String currency) { ... }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    @Override public boolean equals(Object o) { ...12 dòng... }
    @Override public int hashCode() { ... }
    @Override public String toString() { ... }
}
```

**40 dòng → 1 dòng.** Và bản record **không thể sai** — `equals`/`hashCode` do compiler sinh, không phải do người viết tay quên mất một field.

> Đây là lỗi kinh điển: thêm field mới vào DTO, quên cập nhật `equals`/`hashCode` → object "bằng nhau" trong khi thực ra khác. Record không có lỗi đó.

---

## 2. Vì sao accessor là `amount()` chứ không `getAmount()`

Quy ước `getXxx()`/`setXxx()` gọi là **JavaBean convention**, sinh ra từ thời cần object **có thể thay đổi**, với setter để framework nhét dữ liệu vào sau khi tạo.

`record` **cố tình vứt bỏ quy ước đó** vì nó không phải bean:

| | JavaBean cũ | `record` |
|---|---|---|
| Đổi giá trị được không | có, qua setter | **không** — bất biến |
| Tên accessor | `getAmount()` | **`amount()`** |
| Ý nghĩa | "object có trạng thái" | "gói dữ liệu trong suốt" |

> ⚠️ Không phải "Java phiên bản mới đổi cách đặt tên". Là **một loại type khác, cho mục đích khác**.

---

## 3. Compact constructor — chỗ đặt validation

Đây là thứ khiến record dùng được thật, không chỉ là cú pháp ngắn.

```java
public record PaymentRequest(BigDecimal amount, String currency) {

    public PaymentRequest {                      // ← KHÔNG có danh sách tham số
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        currency = currency == null ? "VND" : currency.toUpperCase();   // chuẩn hoá
    }
}
```

Hai điểm dễ nhầm:

- **Không viết danh sách tham số** — chỉ `public PaymentRequest {`. Đó là "compact form".
- **Không viết `this.amount = amount;`** — compiler tự gán **sau khi** khối này chạy xong. Bạn chỉ cần kiểm tra hoặc sửa giá trị của **tham số**.

⇒ Record đảm bảo: **không tồn tại một object record nào ở trạng thái không hợp lệ.** Object vừa sinh ra là đã hợp lệ. Với hệ thống payment thì đây là tính chất rất đáng giá.

---

## 4. Được và không được

### ✅ Được

```java
public record Money(BigDecimal amount, String currency) implements Comparable<Money> {

    public static final Money ZERO_VND = new Money(BigDecimal.ZERO, "VND");   // static field OK

    public Money {                                        // compact constructor
        Objects.requireNonNull(amount);
    }

    public Money(long amount) {                           // constructor phụ — phải gọi canonical
        this(BigDecimal.valueOf(amount), "VND");
    }

    public Money plus(Money other) {                      // method riêng OK
        return new Money(this.amount.add(other.amount), this.currency);
    }

    @Override public int compareTo(Money o) { ... }       // implement interface OK
}
```

### ❌ Không được

| Không được | Vì sao |
|---|---|
| **Thêm instance field ngoài danh sách thành phần** | phá vỡ "transparent carrier" — sẽ có trạng thái ẩn |
| **Kế thừa class khác** | record đã ngầm kế thừa `java.lang.Record` |
| **Cho class khác kế thừa nó** | record ngầm định `final` |
| **Setter** | bất biến |

> **Câu phỏng vấn:** *"record có kế thừa được không?"*
> → Không kế thừa class được (đã extends `java.lang.Record`), không bị kế thừa được (implicitly `final`), **nhưng implement interface thì được**.

---

## 5. Có thay thế DTO cũ được không?

### ✅ CÓ — và đây đúng là chỗ nó sinh ra để dùng

| Trường hợp | Dùng record? |
|---|---|
| **Request/Response DTO của REST API** | ✅ **lý tưởng** |
| Object giá trị trong domain (`Money`, `AccountId`) | ✅ rất hợp |
| Message/event của Kafka | ✅ hợp |
| Kết quả projection từ query | ✅ hợp |
| Kiểu trả về nhiều giá trị của một method | ✅ hợp |

### ❌ KHÔNG — bốn chỗ phải dùng class thường

**1. JPA `@Entity` — tuyệt đối không.** JPA yêu cầu:
- constructor không tham số → record không có
- class không `final` → record luôn `final`
- field thay đổi được → record bất biến
- proxy cho lazy loading → cần kế thừa được

> ⚠️ Đây là câu phỏng vấn hay gài. *"Dùng record làm entity được không?"* → **Không.**
> Nhưng record **rất hợp** làm kiểu trả về cho projection/DTO query từ JPA.

**2. Cần thay đổi giá trị sau khi tạo** — ví dụ builder pattern có nhiều bước, hoặc form nhiều trang.

**3. Framework cũ bắt buộc JavaBean setter** — một số thư viện đời cũ.

**4. Cần kế thừa** — phân cấp class thì record không làm được. *(Cân nhắc `sealed interface` + record — đó là cách làm hiện đại.)*

---

## 5b. Cần một object trạng thái đi qua nhiều bước thì sao?

Câu hỏi thực tế: *"một transaction đi qua nhiều bước, mỗi bước ghi thêm vài field — record có làm được không?"*

**Sửa tại chỗ thì không.** Nhưng có cách khác, dựa đúng vào [note 01](01-jvm-memory-model.md).

### "Object bất biến" ≠ "biến không đổi được"

```java
var ctx = new PaymentContext(id, amount, "CREATED", null);
ctx = ctx.withStatus("VALIDATED");     // ← HỢP LỆ
```

Record cấm sửa **object**. Nó **không** cấm biến trỏ sang object khác.

```
   ctx = ctx.withStatus("VALIDATED")

        ┌──────────┐        ┌─────────────────────┐
        │ ctx      │    ┌─► │ status = "CREATED"  │ ← object cũ, KHÔNG bị sửa
        └────┬─────┘    │   └─────────────────────┘
             │          └── (hết người trỏ tới → GC dọn)
             │              ┌──────────────────────┐
             └────────────► │ status = "VALIDATED" │ ← object MỚI
                            └──────────────────────┘
```

### Wither pattern

```java
public record PaymentContext(String id, BigDecimal amount, String status, String authCode) {

    public PaymentContext withStatus(String status) {
        return new PaymentContext(id, amount, status, authCode);
    }

    public PaymentContext withAuthCode(String authCode) {
        return new PaymentContext(id, amount, status, authCode);
    }
}
```

```java
var ctx = new PaymentContext(id, amount, "CREATED", null);
ctx = ctx.withStatus("VALIDATED");
ctx = ctx.withAuthCode("AUTH123").withStatus("AUTHORIZED");
```

Quy ước tên `withXxx()` — gọi là **wither**, đối xứng với setter.

### Ba lựa chọn

| | Ưu | Nhược |
|---|---|---|
| **(A) `class` + setter** | đơn giản, không cấp phát thêm | không biết ai sửa gì; nguy hiểm khi chia sẻ giữa thread; giá trị cũ **mất vĩnh viễn** |
| **(B) `record` + wither** | bất biến, an toàn đa luồng, **giữ được trạng thái từng bước** | nhiều field thì viết lặp; mỗi bước cấp phát một object (rẻ nhưng khác 0) |
| **(C) builder → record** | trong thì tiện, ra thì bất biến | thêm một class phải bảo trì |

### Với payment thì nghiêng về (B)

`CREATED → VALIDATED → AUTHORIZED → CAPTURED`, mỗi bước một object mới ⇒ có sẵn **chuỗi trạng thái đầy đủ, không bị ghi đè**. Điều tra giao dịch lỗi thì mọi bước còn nguyên.

Cách (A) thì `ctx.setStatus("AUTHORIZED")` **xoá vĩnh viễn** giá trị cũ.

> 🌱 Đây chính là hạt giống của **Event Sourcing (Phase 3)**: thay vì ghi đè trạng thái, lưu chuỗi thay đổi.

**Vẫn chọn (A) khi:** object có 15–20 field, hoặc nằm trong vòng lặp nóng chạy hàng triệu lần.

---

## 6. Record với Jackson

Hoạt động sẵn, không cần cấu hình. Jackson dùng **tên thành phần** làm tên field JSON.

```java
public record PaymentResponse(String id, BigDecimal amount, String status) {}
```

```json
{ "id": "3f2b...", "amount": 100.50, "status": "CREATED" }
```

Đổi tên field JSON:

```java
public record PaymentResponse(
        @JsonProperty("payment_id") String id,
        BigDecimal amount,
        String status) {}
```

---

## 7. Record với Bean Validation

Đặt annotation ngay trong danh sách thành phần:

```java
public record PaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency) {}
```

Rồi ở controller thêm `@Valid`:

```java
@PostMapping("/payments")
public PaymentResponse create(@Valid @RequestBody PaymentRequest request) { ... }
```

**Compact constructor vs Bean Validation — khi nào dùng cái nào:**

| | Dùng cho |
|---|---|
| **Bean Validation** (`@NotNull`…) | dữ liệu **từ ngoài vào** — trả lỗi HTTP 400 gọn gàng cho client |
| **Compact constructor** | **bất biến của domain** — thứ không bao giờ được phép sai, dù gọi từ đâu |

Dùng cả hai không thừa: một cái chặn ở biên giới, một cái bảo vệ lõi.

---

## 8. Record và thread safety

Record **bất biến** ⇒ **tự động an toàn đa luồng**. Không thread nào sửa được nó sau khi tạo, nên không có race condition.

Đó là lý do record hợp hoàn hảo với kiến trúc ở [note 05](05-stateless-thread-safety.md):

```
Bean singleton   →  stateless, không giữ gì      →  dùng chung an toàn
Record           →  bất biến, chở dữ liệu đi     →  truyền qua thread an toàn
```

Hai mảnh khớp nhau: **bean làm việc, record chở dữ liệu.**

---

## 9. Record KHÔNG phải bean

Ranh giới cần thuộc:

| | Ai tạo | Bao nhiêu lần | Sống bao lâu |
|---|---|---|---|
| `@Service`, `@RestController` | **container** | một lần, lúc khởi động | suốt đời app |
| `record` request | **Jackson** khi đọc JSON | mỗi request một cái | vài mili giây |
| `record` response | **code của bạn** bằng `new` | mỗi lần gọi | vài mili giây |

> **Bean = thứ *làm việc*.** **Record = thứ *mang việc đi*.**
> Đừng nhét dữ liệu request vào bean — đó là quả bom ở [note 05](05-stateless-thread-safety.md).

---

## Câu hỏi phỏng vấn từ phần này

1. `record` là gì, compiler sinh sẵn những gì?
2. Vì sao accessor tên `amount()` chứ không `getAmount()`?
3. Compact constructor là gì, khác canonical constructor thế nào?
4. Record kế thừa được không? Implement interface được không? → *không kế thừa, có implement*
5. Record thêm được instance field không? → *không — phá vỡ transparent carrier*
6. **Dùng record làm JPA `@Entity` được không? Vì sao?** → *không: cần no-arg constructor, không `final`, field mutable, proxy*
7. Record có thread-safe không? Vì sao? → *có, vì bất biến*
8. Record có phải bean không? → *không — không có stereotype annotation, và do Jackson/code tạo chứ không phải container*
9. Đặt validation ở compact constructor hay bằng `@NotNull`? → *ngoài biên dùng Bean Validation, bất biến domain dùng compact constructor*
10. Record giải quyết lỗi kinh điển nào của DTO viết tay? → *quên cập nhật `equals`/`hashCode` khi thêm field*

## Còn nợ / sẽ học sau

- `sealed interface` + record → pattern matching, thay cho phân cấp kế thừa
- Pattern matching cho record (record deconstruction) — Java 21+
- Record trong `switch` biểu thức
- Builder cho record có nhiều thành phần

## Liên quan

- [01-jvm-memory-model.md](01-jvm-memory-model.md) — `equals` mặc định chỉ là `==`; record thì override thật
- [04-dependency-injection.md](04-dependency-injection.md) — stereotype annotation, thứ record **không** có
- [05-stateless-thread-safety.md](05-stateless-thread-safety.md) — bất biến ⇒ an toàn đa luồng
