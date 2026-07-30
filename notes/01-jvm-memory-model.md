# 01 — JVM: Stack, Heap, và tại sao `==` khác `.equals()`

> Nền tảng. Mọi thứ khác trong Java đứng trên phần này.

---

## 1. JVM chia bộ nhớ thành mấy vùng

Khi chạy chương trình Java, **JVM** (Java Virtual Machine) khởi động và chia bộ nhớ ra:

```
┌─────────────────────────────────────────────────────────┐
│                         JVM                             │
│                                                         │
│   ┌──────────────┐   ┌──────────────────────────────┐   │
│   │    STACK     │   │            HEAP              │   │
│   │ (mỗi thread  │   │      (dùng chung mọi thread) │   │
│   │  một cái)    │   │                              │   │
│   │              │   │   Nơi MỌI object thật sự     │   │
│   │ biến cục bộ  │   │   nằm. Sinh ra bởi `new`.    │   │
│   │ tham số      │   │   Bị GC dọn khi hết ai dùng. │   │
│   └──────────────┘   └──────────────────────────────┘   │
│                                                         │
│   ┌──────────────────────────────────────────────────┐  │
│   │  METASPACE — bản thiết kế class, biến static     │  │
│   └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

| Vùng | Chứa gì | Phạm vi |
|---|---|---|
| **Stack** | biến cục bộ, tham số method | **riêng từng thread** |
| **Heap** | mọi object (`new`) | **dùng chung toàn bộ thread** |
| **Metaspace** | bản thiết kế class, field `static` | toàn ứng dụng |

- Mỗi lần gọi method → JVM đẩy một *frame* lên stack (chứa tham số + biến cục bộ). Method kết thúc → frame bị pop, biến biến mất.
- **GC (Garbage Collector)**: bạn không bao giờ giải phóng bộ nhớ tay. Object nào không còn ai trỏ tới thì GC dọn. Vì thế Java không có `free()` / `delete`.

> ⚠️ **Stack riêng từng thread, heap dùng chung** — đây là gốc rễ của mọi bug concurrency. Xem [05-stateless-thread-safety.md](05-stateless-thread-safety.md).

---

## 2. Biến KHÔNG phải object

Hiểu nhầm phổ biến nhất về Java nằm ở đây.

```java
Payment p = new Payment(100);
```

Một dòng, **ba** việc:

1. `new Payment(100)` → cấp phát object trong **heap**, giả sử địa chỉ `0x7A3F`
2. Tạo biến `p` trong **stack frame** của method hiện tại
3. Gán vào `p` **địa chỉ** `0x7A3F` — không phải object

```
      STACK                          HEAP
   ┌──────────┐              ┌────────────────────┐
   │ p        │───────────►  │ Payment @0x7A3F    │
   │ =0x7A3F  │              │   amount = 100     │
   └──────────┘              └────────────────────┘
```

**Biến chứa một tham chiếu (reference), không chứa object.** Object luôn ở heap. Biến chỉ là tờ giấy ghi địa chỉ.

### Gán biến = sao chép địa chỉ

```java
Payment a = new Payment(100);
Payment b = a;              // sao chép ĐỊA CHỈ, không sao chép object
```

```
   ┌──────────┐
   │ a=0x7A3F │──────┐        ┌────────────────────┐
   └──────────┘      ├──────► │ Payment @0x7A3F    │
   ┌──────────┐      │        │   amount = 100     │
   │ b=0x7A3F │──────┘        └────────────────────┘
   └──────────┘
      hai biến              MỘT object duy nhất
```

Sửa qua `a` thì nhìn qua `b` cũng thấy.

### `new` lần hai = object thứ hai

```java
Payment a = new Payment(100);
Payment c = new Payment(100);   // object THỨ HAI, giá trị giống hệt
```

```
   ┌──────────┐        ┌────────────────────┐
   │ a=0x7A3F │──────► │ Payment @0x7A3F    │  amount=100
   └──────────┘        └────────────────────┘
   ┌──────────┐        ┌────────────────────┐
   │ c=0x9B21 │──────► │ Payment @0x9B21    │  amount=100
   └──────────┘        └────────────────────┘
```

---

## 3. `==` vs `.equals()`

| | So sánh cái gì | `a` và `b` (cùng object) | `a` và `c` (hai object) |
|---|---|---|---|
| `a == b` | **địa chỉ** trong biến | `true` | `false` |
| `a.equals(b)` | **do class tự định nghĩa** | tuỳ class | tuỳ class |

- `==` hỏi: *"hai biến có trỏ tới cùng một ô nhớ không?"* → **identity** (danh tính)
- `.equals()` hỏi: *"hai object có được coi là bằng nhau không?"* → **equality** (bằng nhau về giá trị)

### Điều ít người biết: `equals` mặc định CHÍNH LÀ `==`

`.equals()` chỉ là một method bình thường nằm trên class `Object`. Bản mặc định:

```java
public boolean equals(Object obj) {
    return (this == obj);      // ← mặc định, equals chính là ==
}
```

⇒ Class **không override** `equals` thì `a.equals(c)` trả `false` y hệt `==`.
`String`, các class bọc số (`Integer`, `Long`…), và `record` thì **có** override → so sánh theo nội dung.

### Ví dụ kinh điển (hay bị hỏi phỏng vấn)

```java
String x = "pay";
String y = "pay";
String z = new String("pay");

x == y        // true  — cùng object trong String pool
x == z        // false — `new` ép tạo object mới trong heap
x.equals(z)   // true  — String override equals, so sánh từng ký tự
```

---

## 4. `null`

**`null` = một tham chiếu không trỏ đi đâu cả.** Tờ giấy trắng, không ghi địa chỉ nào.

```java
Payment p = null;

p == null           // true   — so sánh địa chỉ, hợp lệ
p.equals(other)     // 💥 NullPointerException — gọi method trên hư vô
```

`null` và `equals` **không cùng loại**: `null` là một *giá trị* mà tham chiếu có thể mang; `equals` là một *method*. Không thể gọi method trên `null`.

An toàn với null:

```java
Objects.equals(a, b)    // null==null → true, không nổ NPE
```

---

## Câu hỏi phỏng vấn từ phần này

1. Object nằm ở đâu — stack hay heap? Còn biến cục bộ? → *object ở heap, biến ở stack*
2. `==` khác `.equals()` thế nào? → *identity vs equality*
3. Nếu class không override `equals` thì `equals` làm gì? → *gọi `==`, so sánh địa chỉ*
4. `new String("a") == "a"` ra gì, vì sao? → *`false`, vì `new` tạo object mới ngoài String pool*
5. Vì sao `Objects.equals()` tồn tại? → *an toàn với null*
6. Ai giải phóng bộ nhớ trong Java? → *GC, khi object không còn ai tham chiếu*

## Còn nợ / sẽ học sau

- `Integer` cache −128..127 (`Integer a=127, b=127; a==b` → `true`; với `128` → `false`)
- Hợp đồng `equals` / `hashCode` — *Effective Java* Items 10–11

## Liên quan

- [02-spring-ioc-container.md](02-spring-ioc-container.md) — singleton scope dựa trên đúng cơ chế reference ở đây
- [05-stateless-thread-safety.md](05-stateless-thread-safety.md) — stack riêng từng thread là lý do biến cục bộ an toàn
