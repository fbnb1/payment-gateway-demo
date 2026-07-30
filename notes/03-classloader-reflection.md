# 03 — Classloader và Reflection: cơ chế thật đằng sau Spring

> Đây là phần phá bỏ ảo giác "Spring là ma thuật". Không có ma thuật — chỉ có classloader + reflection + một cái `Map`.

---

## 1. Classloader — người đi lấy sách

### Vấn đề nó giải quyết

`PaymentService.java` → compiler → `PaymentService.class`

File `.class` chứa **bytecode** — không phải chữ bạn viết, cũng không phải mã máy, mà là dạng trung gian JVM đọc được. *(Đó chính là lý do Java "viết một lần, chạy mọi nơi": bytecode giống nhau, mỗi hệ điều hành chỉ cần một JVM riêng.)*

File `.class` nằm **trên đĩa**. Chương trình chạy trong **bộ nhớ**. Ai chuyển?

### Định nghĩa

> **Classloader = một object Java có nhiệm vụ đọc file `.class` từ đĩa và nạp vào bộ nhớ.**

| Thư viện | Java |
|---|---|
| Sách trên kệ | file `.class` trên đĩa |
| Thủ thư đi lấy sách | **classloader** |
| Sách trên bàn đọc | class đã nạp trong bộ nhớ |

Thủ thư **không bê cả kệ ra ngay từ đầu**. Class chỉ được nạp **lần đầu có ai cần tới** — gọi là *lazy loading* (nạp lười).

### Nạp xong thì có gì?

Không phải object, mà là **bản thiết kế**: class có field nào, method nào, kiểu gì, kế thừa từ đâu. Bản thiết kế nằm ở **Metaspace**.

```
   ĐĨA CỨNG                      BỘ NHỚ (Metaspace)
┌──────────────────┐          ┌───────────────────────────┐
│ PaymentService   │  ──────► │ Bản thiết kế:             │
│    .class        │classloader│  - tên: PaymentService   │
│  (bytecode)      │   đọc     │  - field: repo           │
└──────────────────┘          │  - method: transfer()     │
                              └───────────────────────────┘
                                          │ dùng để tạo object
                                          ▼
                                   HEAP: object thật
```

**Một bản thiết kế → vô số object.** Như một khuôn bánh làm ra nhiều cái bánh.

### Vì sao Singleton pattern chỉ "một" trong phạm vi mỗi classloader

Hai thủ thư khác nhau cùng đi lấy **một cuốn sách** → **hai bản thiết kế riêng biệt** trong bộ nhớ, dù đến từ cùng một file. Java coi chúng là **hai class khác nhau**.

Mỗi bản có vùng `static` riêng. Singleton pattern dựa vào biến `static` để giữ instance duy nhất ⇒ **hai classloader = hai instance**.

Hay gặp ở máy chủ ứng dụng chạy nhiều app cùng lúc.

### Hai lỗi quen thuộc, giờ đã hiểu

| Lỗi | Nghĩa |
|---|---|
| `ClassNotFoundException` | thủ thư tìm mãi không thấy cuốn sách trên kệ |
| `NoClassDefFoundError` | lúc biên dịch còn đó, lúc chạy thì mất |

---

## 2. Reflection — chương trình tự soi gương

### Code bình thường

```java
PaymentService svc = new PaymentService();
svc.transfer();
```

Bạn **gõ sẵn tên** class và method. Compiler kiểm tra ngay lúc biên dịch — gõ sai thì không compile được.

> Ví von: gọi điện bằng số đã thuộc lòng.

### Định nghĩa

> **Reflection = khả năng chương trình tự khảo sát và sử dụng chính mình LÚC ĐANG CHẠY — tìm class, method, field theo TÊN dạng chuỗi ký tự, thay vì gõ sẵn trong code.**

*(Nghĩa đen "reflection" = phản chiếu: chương trình soi gương nhìn chính nó.)*

> Ví von: được đưa một cuốn danh bạ lúc đang chạy và tra tên bất kỳ.

```java
// tên class nằm trong một CHUỖI — đọc từ file cấu hình, database, đâu cũng được
String tenClass = "com.payment.payments.PaymentService";

Class<?> banThietKe = Class.forName(tenClass);           // xin classloader nạp nó
Object obj = banThietKe.getDeclaredConstructor()
                       .newInstance();                    // tạo object mà KHÔNG gõ chữ "new"
```

Dòng cuối là mấu chốt: **tạo object mà trong code không hề có `new PaymentService()`**.

---

## 3. Đây chính là cách Spring hoạt động

Spring được viết **nhiều năm trước** khi bạn tạo project này. Người viết Spring **không thể** biết bạn sẽ đặt tên class là gì. Vậy làm sao nó tạo được object của bạn?

```
1. Spring dùng CLASSLOADER quét thư mục, tìm mọi file .class
2. Với mỗi class, dùng REFLECTION hỏi: "mày có gắn @Component không?"
3. Nếu có → hỏi tiếp: "constructor của mày cần gì?"
4. Chuẩn bị đủ thứ đó, rồi dùng REFLECTION gọi constructor
5. Cất object vào Map  (chính là container — xem note 02)
```

**Đó là toàn bộ "ma thuật" của Spring.** Reflection + một cái `Map`.

---

## 4. Reflection phá được `private`

```java
var ctor = MotClassNaoDo.class.getDeclaredConstructor();
ctor.setAccessible(true);          // ← bẻ khoá, bỏ qua private
Object obj = ctor.newInstance();   // tạo được, dù constructor là private
```

`setAccessible(true)` = *"tôi biết cái này private, cứ cho tôi vào"*.
→ Đây là lý do **Singleton pattern vẫn bị phá bằng reflection**.

*(Java các bản gần đây siết dần quyền này với thư viện bên ngoài; với class của chính bạn thì vẫn dùng được.)*

---

## 5. Cái giá của reflection

| Ưu | Nhược |
|---|---|
| Framework hoạt động với class nó chưa từng biết | **Chậm hơn** gọi thường — phải tra cứu lúc chạy |
| Cấu hình linh hoạt, đổi được lúc chạy | **Compiler không kiểm tra được** — gõ sai tên thì chạy rồi mới chết |
| | Khó lần theo luồng khi đọc code |

Dòng thứ hai giải thích một hiện tượng bạn chắc chắn đã gặp:

> **Spring lỗi thì không lỗi lúc biên dịch, mà nổ lúc khởi động.**

Vì mọi thứ được ráp bằng reflection lúc chạy.

---

## Câu hỏi phỏng vấn từ phần này

1. Classloader làm gì? → *đọc file `.class` từ đĩa, nạp bản thiết kế vào Metaspace*
2. Class được nạp lúc nào? → *lazy — lần đầu có ai cần tới*
3. Bytecode là gì, vì sao Java chạy được đa nền tảng?
4. Reflection là gì? → *truy cập class/method/field theo tên, lúc chạy*
5. Spring tạo object của bạn bằng cách nào? → *classloader quét + reflection gọi constructor*
6. Singleton pattern có bị phá được không? → *có — reflection với `setAccessible(true)`; và hai classloader cho hai instance*
7. Vì sao lỗi cấu hình Spring chỉ nổ lúc khởi động chứ không lúc compile?
8. `ClassNotFoundException` khác `NoClassDefFoundError` thế nào?

## Còn nợ / sẽ học sau

- Cây phân cấp classloader (bootstrap → platform → application) và mô hình uỷ quyền cha
- **Proxy động** — dùng reflection để bọc bean, nền của `@Transactional`

## Liên quan

- [02-spring-ioc-container.md](02-spring-ioc-container.md) — cái `Map` mà bước 5 cất object vào
- [04-dependency-injection.md](04-dependency-injection.md) — bước 3 và 4 nói chi tiết hơn
