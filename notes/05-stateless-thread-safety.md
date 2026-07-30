# 05 — Stateless và Thread Safety: vì sao bean không được nhớ gì

> Yêu cầu đọc trước: [01-jvm-memory-model.md](01-jvm-memory-model.md) (stack riêng từng thread), [02-spring-ioc-container.md](02-spring-ioc-container.md) (singleton).

---

## 1. "State" nghĩa là gì

> **State (trạng thái) = dữ liệu mà object nhớ lại giữa các lần được gọi.**

```java
public class DemGiaoDich {
    private int soLuong = 0;          // ← ĐÂY là state

    public void ghiNhan() {
        soLuong++;                     // giá trị được nhớ cho lần gọi sau
    }
}
```

| | Nghĩa |
|---|---|
| **Stateful** | có nhớ — object nhớ đã đếm tới bao nhiêu |
| **Stateless** | không nhớ gì — mỗi lần gọi độc lập hoàn toàn với lần trước |

---

## 2. Vì sao bean Spring phải stateless

Ghép hai điều đã biết:

1. Bean singleton = **chỉ một object** trong toàn ứng dụng → [note 02](02-spring-ioc-container.md)
2. Mỗi HTTP request được xử lý bởi một **thread** riêng
   *(**thread** = một luồng thực thi độc lập; nhiều thread chạy đồng thời)*

⇒ **Một object duy nhất bị hàng nghìn thread dùng cùng lúc.**

### Ví dụ hỏng

```java
@Service
public class PaymentService {
    private BigDecimal soTien;      // 💥 state có thể thay đổi

    public void xuLy(BigDecimal tien) {
        this.soTien = tien;          // thread A ghi vào
        kiemTra();                   // thread B ghi đè MẤT ở đây
        chuyenTien();                // thread A giờ dùng số tiền CỦA THREAD B
    }
}
```

> Ví von: **một cuốn sổ duy nhất trên bàn, một nghìn người cùng viết vào.**
> Bạn ghi "100", quay đi lấy bút, quay lại thấy "5000" — của người khác. Rồi bạn chuyển 5000 đồng.

Tên chính thức: **race condition** — *lỗi do nhiều luồng giành nhau một dữ liệu; kết quả phụ thuộc vào ai nhanh hơn*.

> ⚠️ Tệ nhất: **không bao giờ tái hiện được lúc test.** Một mình bấm thử thì luôn đúng. Chỉ hỏng khi có tải thật.

---

## 3. Vậy dữ liệu của từng request để đâu?

**Trong tham số method và biến cục bộ.**

```java
@Service
public class PaymentService {
    private final PaymentRepository repo;      // ✅ final, gán một lần, không đổi

    public PaymentService(PaymentRepository repo) {
        this.repo = repo;
    }

    public void xuLy(BigDecimal soTien) {      // ✅ tham số — riêng từng lần gọi
        BigDecimal phi = tinhPhi(soTien);      // ✅ biến cục bộ — riêng từng lần gọi
        repo.luu(soTien, phi);
    }
}
```

**Vì sao an toàn?** Quay lại [note 01](01-jvm-memory-model.md):

> Mỗi thread có **stack riêng**. Biến cục bộ và tham số sống trong stack.

Mỗi thread có bản sao riêng của `soTien` và `phi`. Chúng **không thể** giẫm lên nhau — về mặt vật lý chúng nằm ở hai vùng nhớ khác nhau.

> Ví von: thay vì một cuốn sổ chung trên bàn, **mỗi người cầm một tờ giấy nháp riêng.**

```
   Thread A ─ stack riêng ─► soTien=100    ✅ không đụng nhau
   Thread B ─ stack riêng ─► soTien=5000   ✅
                    │
                    └──────► CÙNG một PaymentService object trong heap
                             (nhưng nó không giữ gì cả, nên vô hại)
```

---

## 4. Quy tắc rút gọn

> **Field của bean singleton chỉ nên chứa dependency, và nên là `final`.**
> **Dữ liệu của request đi qua tham số, không bao giờ nằm ở field.**

`final` = không đổi được sau khi tạo → không thread nào ghi đè được → an toàn.

Đây cũng là lý do thứ năm khiến constructor injection thắng — xem [note 04](04-dependency-injection.md).

---

## 5. Toàn bộ chuỗi, ghép lại

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

Năm khái niệm, **một mạch logic** — không phải năm thứ rời rạc phải học thuộc.

---

## Câu hỏi phỏng vấn từ phần này

1. Bean singleton của Spring có thread-safe không? → *bản thân container thì có, nhưng **bean của bạn thì không** nếu nó có mutable state*
2. Vì sao không nên để field có thể thay đổi trong `@Service`?
3. Race condition là gì? Vì sao khó phát hiện lúc test?
4. Dữ liệu riêng của mỗi request nên để đâu, vì sao chỗ đó an toàn? → *tham số/biến cục bộ, vì stack riêng từng thread*
5. `final` giúp gì cho thread safety?

## Còn nợ / sẽ học sau

- Scope `prototype` và `request` — khi nào **thật sự** cần bean có state
- `synchronized`, `volatile`, và Java Memory Model
- `ThreadLocal`
- Virtual threads (Loom) — Java 21+
- **Phase 1 của project:** isolation level, `SELECT FOR UPDATE`, write skew — race condition ở tầng database

## Liên quan

- [01-jvm-memory-model.md](01-jvm-memory-model.md) — stack riêng từng thread
- [02-spring-ioc-container.md](02-spring-ioc-container.md) — singleton scope
- [04-dependency-injection.md](04-dependency-injection.md) — vì sao `final`
