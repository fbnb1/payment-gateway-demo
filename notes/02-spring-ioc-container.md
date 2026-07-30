# 02 — Spring IoC Container, Bean, Singleton Scope

> Yêu cầu đọc trước: [01-jvm-memory-model.md](01-jvm-memory-model.md) (reference vs object).

---

## 1. IoC là gì

**IoC — Inversion of Control (đảo ngược quyền điều khiển).**

- Bình thường: code của bạn kiểm soát việc tạo dependency → bạn gọi `new`.
- Đảo ngược: **framework tạo, rồi đưa cho bạn.**

**DI — Dependency Injection** là *cách thực hiện* IoC, **không phải từ đồng nghĩa**. Phỏng vấn hay bắt lỗi chỗ này.

> **Neo:** connection pool. Bạn chưa bao giờ viết `new Connection()` — bạn *xin* từ pool. Pool quyết định tạo bao nhiêu, khi nào, tái sử dụng ra sao. Spring là đúng ý tưởng đó, áp lên chính các object của bạn.

---

## 2. Ba thuật ngữ

| Thuật ngữ | Định nghĩa chuẩn |
|---|---|
| **Bean** | Một object mà **vòng đời do container quản lý**. Không phải loại class đặc biệt, không cần implement gì cả. |
| **ApplicationContext** | Chính **cái container**. Bên trong là map `tên bean → definition → instance`. |
| **Bean definition** | Cái **công thức**, không phải cái bánh: class nào, scope gì, cần dependency nào. |

---

## 3. Container chỉ là một cái Map — không có ma thuật

Bạn tự viết được container tối giản trong 12 dòng:

```java
class MiniContainer {
    private final Map<String, Object> singletons = new HashMap<>();

    void register(String name, Object bean) {
        singletons.put(name, bean);           // cất object vào map
    }

    Object getBean(String name) {
        return singletons.get(name);          // LUÔN trả về CÙNG một reference
    }
}
```

`getBean` chỉ `get` từ map — nó **không** `new`. Nên gọi bao nhiêu lần cũng trả về **đúng một địa chỉ**.

Spring làm y hệt. Lớp thật tên `DefaultSingletonBeanRegistry`, có field:

```java
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
```

Dòng đó có thật trong source Spring — mở ra đọc được.

### Toàn cảnh trong heap

```
                       HEAP
   ┌──────────────────────────────────────────────────────┐
   │  ApplicationContext @0x1000                          │
   │  ┌────────────────────────────────────────────────┐  │
   │  │ singletonObjects : Map<String, Object>         │  │
   │  │   "paymentsApplication"  → 0x2000 ─────────┐   │  │
   │  │   "paymentController"    → 0x3000 ──────┐  │   │  │
   │  │   "dataSource"           → 0x4000 ───┐  │  │   │  │
   │  └──────────────────────────────────────┼──┼──┼───┘  │
   │                                         ▼  ▼  ▼      │
   │   ┌──────────┐  ┌──────────────┐  ┌──────────────┐   │
   │   │DataSource│  │PaymentCtrl   │  │PaymentsApp   │   │
   │   └──────────┘  └──────────────┘  └──────────────┘   │
   └──────────────────────────────────────────────────────┘
```

**Bean = một mục trong map đó.** Hết.

---

## 4. Spring khởi động theo HAI PHA

| Pha | Spring làm gì |
|---|---|
| **1. Registration** | Quét, đọc, đăng ký **toàn bộ** *bean definition*. Map còn **rỗng** — chưa object nào. |
| **2. Instantiation** | Gọi `new`, inject dependency, **post-process** từng bean, rồi `map.put(name, object)`. |

- `getBeanDefinitionNames()` đọc danh sách của **pha 1** — danh sách *công thức*, chú ý chữ **Definition**.
- Chữ **post-process** ở pha 2 là chỗ **proxy** được tạo → nền cho `@Transactional`. *(sẽ có note riêng)*

---

## 5. Khai báo bean — hai cách

### Ngầm — `@Component` + component scanning

Dùng khi class là **của bạn**.

```java
@Service        // ← đây là một lời khai báo bean
@Repository     // ← bean
@Component      // ← bean
@RestController // ← bean
```

Cả bốn **đều là** `@Component`, chỉ đặt tên theo vai trò.
Riêng `@Repository` có thêm tác dụng thật: dịch exception của JDBC/JPA sang hệ exception của Spring.

### Tường minh — method `@Bean` trong class `@Configuration`

Dùng khi object là **của thư viện bên thứ ba** — bạn không sửa source được để gắn annotation.

> **Câu phỏng vấn kinh điển:** *"khi nào dùng `@Component`, khi nào dùng `@Bean`?"*
> → `@Component` cho class của mình; `@Bean` cho class của bên thứ ba.

---

## 6. Vì sao class `main` cũng là bean?

`@SpringBootApplication` là **annotation gộp**:

```
@SpringBootApplication
 ├── @SpringBootConfiguration  →  @Configuration  →  @Component   ← làm class bạn thành bean
 ├── @EnableAutoConfiguration                                     ← nguồn gốc của ~vài trăm bean
 └── @ComponentScan                                               ← quét package tìm @Component khác
```

Chuỗi `@SpringBootApplication → @SpringBootConfiguration → @Configuration → @Component` đáng thuộc lòng.

Tên bean = tên class viết thường chữ đầu → `paymentsApplication`.

### Ba thứ khác nhau, đừng gộp

| Thứ | Là gì | Có phải bean? |
|---|---|---|
| `SpringApplication` | **Bộ khởi động** của Boot. Chạy *trước khi* container tồn tại; việc của nó là **tạo ra** container. | **Không** |
| `ApplicationContext` | **Chính cái container** | Không phải bean thường |
| `PaymentsApplication` | **Class của bạn** | **Có** |

Ghi nhớ: `ApplicationContext` thuộc `org.springframework.context` — **của Spring Framework lõi**, không phải của Boot. Boot chỉ là lớp tự-cấu-hình bọc ngoài.

---

## 7. Singleton scope

> **Singleton scope** = container tạo **đúng một object** trong heap cho bean đó, rồi **phát cùng một reference** cho mọi nơi xin nó.

```java
var a = ctx.getBean(PaymentsApplication.class);   // map.get → 0x2000
var b = ctx.getBean(PaymentsApplication.class);   // map.get → 0x2000
// a == b  →  true
```

```
   ┌──────────┐
   │ a=0x2000 │──────┐        ┌────────────────────────┐
   └──────────┘      ├──────► │ PaymentsApplication    │
   ┌──────────┐      │        │        @0x2000         │
   │ b=0x2000 │──────┘        └────────────────────────┘
   └──────────┘
   hai biến trong stack        MỘT object trong heap
```

Dùng `==` chứ không `.equals()` — vì ta hỏi *"cùng ô nhớ không"*, không hỏi *"giá trị bằng nhau không"*. Xem [01](01-jvm-memory-model.md).

### Hai cái bẫy

**Bẫy 1 — "mỗi ApplicationContext", KHÔNG phải "mỗi JVM".**
Hai context trong cùng một JVM → hai object riêng.

**Bẫy 2 — Spring singleton ≠ Singleton pattern (GoF).**

| | Singleton pattern (GoF) | Spring singleton scope |
|---|---|---|
| Ép buộc bằng | `private` constructor + `static getInstance()` | Container tự quản; constructor vẫn `public` |
| Phạm vi "một" | mỗi **classloader** | mỗi **container** |
| Tạo được cái thứ hai? | Không (trừ reflection) | **Được** — `new PaymentsApplication()` vẫn chạy |
| Mock khi test | Khó (trạng thái toàn cục) | Dễ (nạp bean khác vào context) |

Dòng thứ ba hay làm người ta trượt: Spring **không cấm** bạn `new`. Nó chỉ đảm bảo **object nó quản lý** thì chỉ có một. Object bạn tự `new` nằm ngoài container, không được inject gì cả.

---

## Câu hỏi phỏng vấn từ phần này

1. IoC và DI khác nhau thế nào? → *IoC là nguyên tắc, DI là cách thực hiện*
2. Bean là gì? → *object có vòng đời do container quản lý*
3. `@Component` vs `@Bean` dùng khi nào?
4. Vì sao class gắn `@SpringBootApplication` cũng là bean? → *chuỗi annotation gộp dẫn về `@Component`*
5. Scope mặc định là gì, phạm vi "một" tính theo gì? → *singleton, mỗi ApplicationContext*
6. Spring singleton có giống Singleton pattern không? → *không*
7. Spring khởi động qua mấy pha? → *registration definition, rồi instantiation + post-process*

## Còn nợ / sẽ học sau

- Các scope khác: `prototype`, `request`, `session`
- Vòng đời bean đầy đủ: `@PostConstruct`, `@PreDestroy`, `BeanPostProcessor`
- **Proxy** — trả lời `@Transactional` gọi `this.method()` thì có mở transaction không

## Liên quan

- [03-classloader-reflection.md](03-classloader-reflection.md) — cơ chế thật giúp Spring tạo được object của bạn
- [04-dependency-injection.md](04-dependency-injection.md) — cách container đưa dependency vào
- [05-stateless-thread-safety.md](05-stateless-thread-safety.md) — hệ quả nguy hiểm của singleton
