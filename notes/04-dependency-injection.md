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

## 6. Chi tiết hay bị hỏi

> Nếu class chỉ có **đúng một** constructor thì **không cần viết `@Autowired`** — Spring tự hiểu.

Đó là lý do code Spring hiện đại gần như không còn thấy `@Autowired`.

---

## Câu hỏi phỏng vấn từ phần này

1. Dependency Injection là gì? → *dependency được đưa từ bên ngoài vào, thay vì object tự tạo*
2. Ba kiểu injection, khác nhau ra sao?
3. Vì sao constructor injection được khuyến nghị? → *4 lý do ở mục 5*
4. Vì sao field injection không dùng được `final`?
5. Khi nào không cần `@Autowired`? → *khi class chỉ có một constructor*
6. Spring inject bằng cơ chế kỹ thuật nào? → *reflection*
7. Setter injection dùng khi nào? → *dependency không bắt buộc / cần thay đổi sau khi tạo*

## Còn nợ / sẽ học sau

- `@Qualifier`, `@Primary` — khi có nhiều bean cùng kiểu
- Circular dependency (A cần B, B cần A) — vì sao constructor injection làm nó nổ ngay lúc khởi động, và đó là điều **tốt**
- `@Value` và inject giá trị cấu hình

## Liên quan

- [03-classloader-reflection.md](03-classloader-reflection.md) — cơ chế thật thực hiện việc inject
- [05-stateless-thread-safety.md](05-stateless-thread-safety.md) — vì sao field inject vào phải là `final`
