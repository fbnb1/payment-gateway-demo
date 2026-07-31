# 04 — Dependency Injection: được đưa đồ, thay vì tự đi kiếm

> Yêu cầu đọc trước: [02-spring-ioc-container.md](02-spring-ioc-container.md), [03-classloader-reflection.md](03-classloader-reflection.md).

---

## 1. "Dependency" nghĩa là gì

> Object A cần object B mới làm được việc → **B là dependency của A** (*thứ A phụ thuộc vào*).

`PaymentService` cần `PaymentRepository` để ghi xuống database ⇒ `PaymentRepository` là dependency của `PaymentService`.

---

## 2. Cách cũ: tự đi kiếm

```java
public class PaymentService {
    private PaymentRepository repo = new PaymentRepository();   // tự tạo lấy
}
```

Chạy được, nhưng ba vấn đề:

| # | Vấn đề | Hậu quả |
|---|---|---|
| 1 | **Hàn chết** | Gắn cứng vào đúng một loại repository. Muốn đổi → phải sửa code bên trong `PaymentService`. |
| 2 | **Không test được** | Test sẽ nối vào database thật. Không thay được bằng bản giả. |
| 3 | **Không kiểm soát nổi** | Có bao nhiêu `PaymentRepository` được tạo? Không ai biết — chỗ nào cũng `new` được. |

> Ví von: đầu bếp tự trồng rau. Muốn đổi nhà cung cấp → phải đào lại cả vườn.

---

## 3. Cách mới: được đưa đồ

```java
public class PaymentService {
    private final PaymentRepository repo;

    public PaymentService(PaymentRepository repo) {   // ← nhận từ bên ngoài
        this.repo = repo;
    }
}
```

`PaymentService` **không tự tạo** repository nữa. Nó chỉ tuyên bố: *"tôi cần một cái, ai đó đưa cho tôi."*

- Việc **"đưa cho"** đó gọi là **inject** (tiêm vào).
- Toàn bộ cơ chế gọi là **Dependency Injection**.

> Ví von: đầu bếp nhận nguyên liệu từ nhà cung cấp. Đổi nhà cung cấp → đổi ở khâu giao hàng, bếp không sửa gì.

**Ai đưa?** Container. Và nó đưa **bằng reflection**. Ba mảnh khớp vào nhau:

```
classloader  →  tìm và nạp class của bạn
reflection   →  đọc annotation, xem constructor cần gì, gọi constructor
container    →  cất object kết quả vào Map, và đưa nó cho ai cần
```

---

## 4. Ba cách inject

### Cách 1 — Constructor ✅ được khuyến nghị

```java
@Service
public class PaymentService {
    private final PaymentRepository repo;

    public PaymentService(PaymentRepository repo) {
        this.repo = repo;
    }
}
```

### Cách 2 — Field ⚠️ tiện nhưng có hại

```java
@Service
public class PaymentService {
    @Autowired
    private PaymentRepository repo;
}
```

### Cách 3 — Setter — cho dependency KHÔNG bắt buộc

```java
@Service
public class PaymentService {
    private PaymentRepository repo;

    @Autowired
    public void setRepo(PaymentRepository repo) { this.repo = repo; }
}
```

---

## 5. Vì sao constructor thắng — bốn lý do

**1. Dùng được `final`.**
`final` = gán một lần, không đổi được nữa. Chỉ constructor cho phép điều đó. Field injection **không thể** dùng `final` — vì object phải tạo xong trước, rồi Spring mới thò tay vào gán. Không `final` = ai đó đổi được dependency lúc chạy.

**2. Object không bao giờ tồn tại ở trạng thái dở dang.**
Constructor xong là object đủ mọi thứ. Field injection có một khoảng thời gian object đã tồn tại nhưng field còn `null` → dùng lúc đó là `NullPointerException`.

**3. Dependency lộ ra trước mắt.**
Nhìn constructor biết ngay class cần gì. Constructor 8 tham số → **thấy ngay** class ôm quá nhiều việc. Field injection giấu 8 dependency rải rác, trông vẫn "sạch" — che mất vấn đề thiết kế.

**4. Test được mà không cần Spring.**

```java
var svc = new PaymentService(repoGia);   // xong, không cần khởi động Spring
```

Field injection thì field `private`, **không gán vào được** — trừ khi dùng reflection hoặc khởi động cả Spring. Test chậm và phức tạp hơn.

---

## 6. `@Autowired` và `final` KHÔNG liên quan gì đến nhau

Hiểu nhầm rất phổ biến: tưởng có `final` rồi nên bỏ được `@Autowired`. **Sai.** Hai thứ này độc lập hoàn toàn — cả bốn tổ hợp đều hợp lệ.

| | Nói với ai | Trả lời câu hỏi gì | Tác dụng lúc nào |
|---|---|---|---|
| **`@Autowired`** | **Spring** | *"Dùng constructor nào để tạo object này?"* | lúc **chạy** |
| **`final`** | **Compiler** | *"Biến này gán một lần, cấm đổi."* | lúc **biên dịch** |

Một cái là lời nhắn cho framework, một cái là luật cho trình biên dịch.

### Quy tắc thật để bỏ được `@Autowired`

> **Class chỉ có ĐÚNG MỘT constructor → Spring tự dùng constructor đó, không cần annotation.**
> (từ Spring 4.3, ~2016)

> Ví von: giao hàng đến căn nhà **chỉ có một cửa** — không ai cần dán bảng "vào cửa này".

Nhà **hai cửa** thì bắt buộc phải chỉ:

```java
@Service
public class PaymentService {

    public PaymentService() { ... }                        // cửa 1

    @Autowired                                             // ← BẮT BUỘC
    public PaymentService(PaymentRepository repo) { ... }  // cửa 2
}
```

Hai constructor mà không đánh dấu → Spring **chết lúc khởi động**.

*(Ghi chú: constructor thứ hai buộc phải bỏ `final`, vì `final` bắt **mọi** constructor phải gán giá trị. Đó là bằng chứng ngược lại rằng hai thứ độc lập — `final` ràng buộc compiler, còn Spring thì vẫn chỉ đang phân vân chọn cửa.)*

---

## 7. `@Component` vs `@Service` vs `@Repository`

**`@Service` ≡ `@Component` về mặt kỹ thuật.** `@Service` được meta-annotate bằng `@Component`; đổi qua lại chương trình chạy y nguyên.

Vẫn nên viết `@Service` vì:

1. **Truyền đạt vai trò** — người đọc biết ngay đây là tầng nghiệp vụ.
2. **AOP nhắm được** — pointcut kiểu "log mọi method trong mọi `@Service`"; nếu tất cả là `@Component` thì không phân biệt nổi.
3. **Spring để ngỏ khả năng thêm ý nghĩa** trong tương lai (tài liệu nói rõ).

### ⚠️ Nhưng `@Repository` thì KHÁC THẬT

`@Repository` có **hành vi thực sự**: Spring bọc bean đó lại để **dịch exception** — biến `SQLException` (JDBC) hay exception của JPA thành hệ `DataAccessException` thống nhất. Nhờ vậy tầng nghiệp vụ không cần biết bên dưới là JDBC hay Hibernate.

> **Câu phỏng vấn:** *"ba annotation này khác nhau gì?"*
> Rất nhiều người trả lời "giống nhau, chỉ khác tên" → **sai**.
> Đáp án đúng: `@Service` ≡ `@Component`; **`@Repository` thì không** — nó thêm exception translation.

*(Cơ chế "bọc bean lại" đó chính là **proxy** — xem hàng đợi trong [README](README.md).)*

---

## Câu hỏi phỏng vấn từ phần này

1. Dependency Injection là gì? → *dependency được đưa từ bên ngoài vào, thay vì object tự tạo*
2. Ba kiểu injection, khác nhau ra sao?
3. Vì sao constructor injection được khuyến nghị? → *4 lý do ở mục 5*
4. Vì sao field injection không dùng được `final`?
5. Khi nào không cần `@Autowired`? → *khi class chỉ có **một** constructor — **không** liên quan tới `final`*
6. Spring inject bằng cơ chế kỹ thuật nào? → *reflection*
7. Setter injection dùng khi nào? → *dependency không bắt buộc / cần thay đổi sau khi tạo*
8. `@Autowired` và `final` thay thế nhau được không? → *không, hoàn toàn độc lập: một nói với Spring lúc chạy, một nói với compiler lúc biên dịch*
9. Class có 2 constructor mà không đánh dấu gì thì sao? → *Spring chết lúc khởi động, không biết chọn cái nào*
10. `@Component`, `@Service`, `@Repository` khác nhau gì? → *`@Service` ≡ `@Component`; **`@Repository` khác thật** — có exception translation*

## Còn nợ / sẽ học sau

- `@Qualifier`, `@Primary` — khi có nhiều bean cùng kiểu
- Circular dependency (A cần B, B cần A) — vì sao constructor injection làm nó nổ ngay lúc khởi động, và đó là điều **tốt**
- `@Value` và inject giá trị cấu hình

## Liên quan

- [03-classloader-reflection.md](03-classloader-reflection.md) — cơ chế thật thực hiện việc inject
- [05-stateless-thread-safety.md](05-stateless-thread-safety.md) — vì sao field inject vào phải là `final`
