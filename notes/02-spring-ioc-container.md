# 02 — Spring IoC Container, Bean, Singleton Scope

> Yêu cầu đọc trước: [01-jvm-memory-model.md](01-jvm-memory-model.md) (reference vs object).

---

## Tóm tắt 30 giây

1. Tự gọi `new` để tạo dependency → code bị dính chặt, khó test, dễ tạo trùng object.
2. **IoC** = đưa việc gọi `new` ra khỏi class của bạn, giao cho framework. Class chỉ **khai báo mình cần gì**.
3. **DI** = *cách* framework đưa dependency vào (thường qua constructor).
4. Cái framework đó tên là **ApplicationContext**, và ruột nó chỉ là một `Map<String, Object>`.
5. Mỗi object trong map đó gọi là một **bean**. Vì là `map.get()` chứ không phải `new`, nên mọi nơi xin đều nhận **cùng một object** → đó chính là **singleton scope**.

Phần còn lại của note chỉ là mở rộng 5 dòng trên.

---

## 1. Bắt đầu từ nỗi đau, không phải từ định nghĩa

Code không có IoC, tự `new` bằng tay:

```java
class PaymentController {
    private final PaymentService service = new PaymentService(
        new PaymentRepository(new DataSource("jdbc:...", "user", "pass"))
    );
}
```

Ba vấn đề, và đều là vấn đề thật trong hệ thống payment:

| Vấn đề | Vì sao đau |
|---|---|
| **Biết quá nhiều** | `PaymentController` phải biết `DataSource` cần URL + password. Đó không phải việc của nó. |
| **Không test được** | Muốn thay `PaymentRepository` bằng mock → không thay được, vì `new` đã hard-code trong class. |
| **Tạo trùng** | 10 controller cùng `new DataSource` → 10 connection pool, 10 lần mở kết nối DB. |

Ghi nhớ: **`new` là một lời cam kết cứng.** Viết `new X()` nghĩa là "tôi chốt luôn, mãi mãi dùng đúng class X này". Mọi vấn đề trên đều sinh ra từ đó.

---

## 2. Ý tưởng IoC — chỉ đảo một thứ duy nhất

**IoC — Inversion of Control (đảo ngược quyền điều khiển).**

- Bình thường: **code của bạn** gọi `new` để lấy dependency.
- Đảo ngược: **framework** gọi `new`, rồi đưa cho bạn.

Chữ "đảo ngược" chỉ nói về **ai là người gọi `new`**. Không huyền bí hơn thế.

```
TRƯỚC (không IoC)              SAU (có IoC)
────────────────────           ──────────────────────────
Controller                     Controller
   │ new Service()                │ "tôi cần một Service"
   │ new Repository()             ▼
   ▼                           Container  ──► new Service(...)
tự lo hết                                 ──► đưa vào Controller
```

> **Neo về nghề của bạn:** connection pool. Bạn chưa bao giờ viết `new Connection()` — bạn *xin* từ pool. Pool quyết định tạo bao nhiêu, khi nào, tái sử dụng ra sao. Spring là đúng ý tưởng đó, nhưng áp lên **mọi object** của bạn chứ không riêng connection.

### IoC ≠ DI

**DI — Dependency Injection** là **cách thực hiện** IoC, **không phải từ đồng nghĩa**.

- IoC = **nguyên tắc** ("framework nắm quyền tạo object").
- DI = **kỹ thuật cụ thể** ("truyền dependency vào qua constructor / setter / field").

Phỏng vấn hay bắt lỗi đúng chỗ này. Chi tiết về DI ở [note 04](04-dependency-injection.md).

---

## 3. Ba từ vựng — quy về một ví von

| Từ | Ví von cái bếp | Nghĩa kỹ thuật |
|---|---|---|
| **Bean definition** | **công thức** món ăn | Metadata: class nào, scope gì, cần dependency nào. Chưa có object. |
| **Bean** | **món ăn đã nấu xong** | Object thật trong heap, **vòng đời do container quản lý**. |
| **ApplicationContext** | **cái bếp + tủ giữ món** | Chính cái container. Bên trong là map `tên bean → object`. |

Hai điều dễ nhầm, nhớ kỹ:

- **Bean không phải một loại class đặc biệt.** Không cần `extends` gì, không cần `implements` gì. Class của bạn y nguyên; "bean" chỉ mô tả *ai đang quản nó*.
- **Bean definition ≠ bean.** Công thức không ăn được. Phân biệt được hai cái này thì phần "hai pha" bên dưới tự hiểu.

---

## 4. Container chỉ là một cái Map — không có ma thuật

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

Điểm mấu chốt: **`getBean` chỉ `get` từ map — nó KHÔNG `new`.** Gọi bao nhiêu lần cũng trả về đúng một địa chỉ trong heap.

Spring làm y hệt. Class thật tên `DefaultSingletonBeanRegistry`, có field:

```java
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
```

Dòng đó có thật trong source Spring — mở ra đọc được. Cả framework đồ sộ, phần lõi của "IoC container" đúng là một cái map.

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

## 5. Spring khởi động theo HAI PHA

Quay lại ví von cái bếp: **đọc hết công thức trước, rồi mới nấu.**

| Pha | Spring làm gì | Trạng thái map |
|---|---|---|
| **1. Registration** | Quét package, đọc annotation, đăng ký **toàn bộ** *bean definition* | **Rỗng** — chưa object nào |
| **2. Instantiation** | Gọi `new`, inject dependency, **post-process**, rồi `map.put(name, object)` | Được lấp đầy |

Vì sao phải tách hai pha? Vì lúc tạo `PaymentController`, Spring cần biết `PaymentService` **tồn tại** — mà chưa chắc đã tạo nó xong. Đọc hết công thức trước thì mới xếp được thứ tự nấu.

Hai chi tiết đáng nhớ:

- `getBeanDefinitionNames()` đọc danh sách của **pha 1** — danh sách *công thức*. Chú ý chữ **Definition** trong tên method.
- Chữ **post-process** ở pha 2 là chỗ **proxy** được tạo → đây là nền móng của `@Transactional`, `@Async`, `@Cacheable`. *(sẽ có note riêng)*

---

## 6. Khai báo bean — hai cách, một quy tắc chọn

### Cách 1 — ngầm: `@Component` + component scanning

Dùng khi class là **của bạn** (bạn sửa được source để gắn annotation).

```java
@Service        // ← đây là một lời khai báo bean
@Repository     // ← bean
@Component      // ← bean
@RestController // ← bean
```

Cả bốn **đều là** `@Component`, chỉ đổi tên theo vai trò để người đọc code hiểu ý đồ.
Ngoại lệ duy nhất: `@Repository` có thêm tác dụng thật — dịch exception của JDBC/JPA sang hệ exception của Spring.

### Cách 2 — tường minh: method `@Bean` trong class `@Configuration`

Dùng khi object là **của thư viện bên thứ ba** — bạn không sửa được source của nó để gắn `@Component`.

```java
@Configuration
class AppConfig {
    @Bean
    ObjectMapper objectMapper() {          // class của Jackson, không phải của bạn
        return new ObjectMapper().findAndRegisterModules();
    }
}
```

Ở đây bạn **tự gọi `new`** — nhưng object trả về được **giao cho container quản**. Vẫn là IoC, vì quyền quyết định "tạo lúc nào, giữ bao lâu, đưa cho ai" nằm ở Spring.

> **Câu phỏng vấn kinh điển:** *"khi nào dùng `@Component`, khi nào dùng `@Bean`?"*
> → `@Component` cho class của mình; `@Bean` cho class của bên thứ ba (hoặc khi cần cấu hình phức tạp trước khi giao).

---

## 7. Vì sao class `main` cũng là một bean?

`@SpringBootApplication` là **annotation gộp** — bóc ra thì thấy:

```
@SpringBootApplication
 ├── @SpringBootConfiguration  →  @Configuration  →  @Component   ← làm class bạn thành bean
 ├── @EnableAutoConfiguration                                     ← nguồn gốc của ~vài trăm bean
 └── @ComponentScan                                               ← quét package tìm @Component khác
```

Chuỗi `@SpringBootApplication → @SpringBootConfiguration → @Configuration → @Component` đáng thuộc lòng — đó là toàn bộ câu trả lời cho câu hỏi này.

Tên bean = tên class viết thường chữ đầu → `paymentsApplication`.

### Ba thứ khác nhau, đừng gộp

| Thứ | Là gì | Có phải bean? |
|---|---|---|
| `SpringApplication` | **Bộ khởi động** của Boot. Chạy *trước khi* container tồn tại; việc của nó là **tạo ra** container. | **Không** |
| `ApplicationContext` | **Chính cái container** | Không phải bean thường |
| `PaymentsApplication` | **Class của bạn** | **Có** |

Ghi nhớ: `ApplicationContext` thuộc `org.springframework.context` — **của Spring Framework lõi**, không phải của Boot. Boot chỉ là lớp tự-cấu-hình bọc ngoài.

---

## 8. Singleton scope

> **Singleton scope** = container tạo **đúng một object** trong heap cho bean đó, rồi **phát cùng một reference** cho mọi nơi xin nó.

Đây không phải một tính năng phải bật — nó là **hệ quả tự nhiên** của việc `getBean` chỉ gọi `map.get()` (mục 4). Muốn có object mới mỗi lần thì mới phải khai báo thêm (`prototype` scope).

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

Dùng `==` chứ không `.equals()` — vì ta hỏi *"cùng ô nhớ không"*, không hỏi *"giá trị bằng nhau không"*. Xem [note 01](01-jvm-memory-model.md).

### Hai cái bẫy

**Bẫy 1 — "một" tính theo mỗi ApplicationContext, KHÔNG phải mỗi JVM.**
Hai context trong cùng một JVM → hai object riêng biệt. Hay gặp trong integration test: mỗi test class có thể dựng một context khác nhau.

**Bẫy 2 — Spring singleton ≠ Singleton pattern (GoF).** Trùng tên, khác hẳn bản chất:

| | Singleton pattern (GoF) | Spring singleton scope |
|---|---|---|
| Ép buộc bằng | `private` constructor + `static getInstance()` | Container tự quản; constructor vẫn `public` |
| Phạm vi "một" | mỗi **classloader** | mỗi **container** |
| Tạo được cái thứ hai? | Không (trừ reflection) | **Được** — `new PaymentsApplication()` vẫn chạy |
| Mock khi test | Khó (trạng thái toàn cục) | Dễ (nạp bean khác vào context) |

Dòng thứ ba hay làm người ta trượt: Spring **không cấm** bạn `new`. Nó chỉ đảm bảo **object nó quản lý** thì chỉ có một. Object bạn tự `new` nằm **ngoài** container → không được inject gì cả, mọi field dependency đều `null`.

### Hệ quả nguy hiểm — nhớ ngay bây giờ

Một object dùng chung cho **mọi request**, mà web server thì chạy **nhiều thread**. Nghĩa là: nếu bean có field thay đổi được, hai request sẽ ghi đè lẫn nhau. Đó là lý do bean phải **stateless** → [note 05](05-stateless-thread-safety.md).

---

## Câu hỏi phỏng vấn từ phần này

1. IoC giải quyết vấn đề gì? → *code tự `new` thì dính chặt, khó test, dễ tạo trùng object*
2. IoC và DI khác nhau thế nào? → *IoC là nguyên tắc, DI là cách thực hiện*
3. Bean là gì? → *object có vòng đời do container quản lý*
4. Bean và bean definition khác nhau ra sao? → *món ăn vs công thức*
5. `@Component` vs `@Bean` dùng khi nào?
6. Vì sao class gắn `@SpringBootApplication` cũng là bean? → *chuỗi annotation gộp dẫn về `@Component`*
7. Scope mặc định là gì, phạm vi "một" tính theo gì? → *singleton, mỗi ApplicationContext*
8. Spring singleton có giống Singleton pattern không? → *không — bảng so sánh ở mục 8*
9. Spring khởi động qua mấy pha? → *registration definition, rồi instantiation + post-process*

## Còn nợ / sẽ học sau

- Các scope khác: `prototype`, `request`, `session`
- Vòng đời bean đầy đủ: `@PostConstruct`, `@PreDestroy`, `BeanPostProcessor`
- **Proxy** — trả lời `@Transactional` gọi `this.method()` thì có mở transaction không

## Liên quan

- [03-classloader-reflection.md](03-classloader-reflection.md) — cơ chế thật giúp Spring tạo được object của bạn
- [04-dependency-injection.md](04-dependency-injection.md) — cách container đưa dependency vào
- [05-stateless-thread-safety.md](05-stateless-thread-safety.md) — hệ quả nguy hiểm của singleton
- [07-spring-boot-autoconfiguration.md](07-spring-boot-autoconfiguration.md) — vì sao một class sinh ra 145 bean
