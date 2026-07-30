# 07 — Auto-configuration: 1 class, 145 bean

> Yêu cầu đọc trước: [02-spring-ioc-container.md](02-spring-ioc-container.md), [03-classloader-reflection.md](03-classloader-reflection.md).
> **Số liệu trong note này là đo thật trên project này**, Spring Boot 4.1.0 / Java 25, không phải lý thuyết chép lại.

---

## 1. Câu hỏi xuất phát

```java
@SpringBootApplication
public class PaymentsApplication {
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(PaymentsApplication.class, args);
        System.out.println("Tổng số bean: " + ctx.getBeanDefinitionNames().length);
    }
}
```

```
Tổng số bean: 145
```

**Khai báo 1 class → container có 145 bean.** 144 cái kia ở đâu ra?

Trả lời: nhánh `@EnableAutoConfiguration` bên trong `@SpringBootApplication` — xem [note 02 mục 6](02-spring-ioc-container.md).

---

## 2. Cơ chế: HAI tầng lọc

Sai lầm phổ biến là tưởng chỉ có một tầng. Thực tế:

| Tầng | Câu hỏi | Trượt thì sao |
|---|---|---|
| **0. Có phải ứng viên không?** | Jar chứa auto-config đó có trên classpath không? | **Không xuất hiện trong báo cáo, ở bất kỳ mục nào** |
| **1. Điều kiện có khớp không?** | `@ConditionalOnClass`, `@ConditionalOnMissingBean`… | Nằm ở **Negative matches**, kèm lý do |

### Tầng 0 — danh sách ứng viên

Mỗi jar auto-configuration chứa một file text:

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Mỗi dòng là tên đầy đủ của một class auto-configuration. `@EnableAutoConfiguration` đọc **mọi** file này trên classpath → ra danh sách ứng viên.

> Không có jar Redis → không có file imports của Redis → `DataRedisRepositoriesAutoConfiguration` **chưa từng được xem xét**. Nó không nằm trong Negative matches, vì Negative matches chỉ liệt kê ứng viên **đã được đánh giá rồi loại**.

### Tầng 1 — người gác cửa

| Annotation | Chỉ chạy khi |
|---|---|
| `@ConditionalOnClass` | class X **có trên classpath** |
| `@ConditionalOnMissingClass` | class X **không** có |
| `@ConditionalOnMissingBean` | **bạn CHƯA tự khai** bean loại đó |
| `@ConditionalOnBean` | đã có sẵn bean loại đó |
| `@ConditionalOnBooleanProperty` / `@ConditionalOnProperty` | thuộc tính cấu hình khớp |
| `@ConditionalOnWebApplication` | đang là ứng dụng web |
| `@ConditionalOnThreading` | PLATFORM hay VIRTUAL thread |
| `@ConditionalOnResource` | file/resource tồn tại |

---

## 3. Bằng chứng thật từ báo cáo của project này

Bật báo cáo: thêm `debug=true` vào `application.properties` rồi chạy. Tìm `CONDITIONS EVALUATION REPORT`.

### `@ConditionalOnClass` khớp → bật

```
TomcatServletWebServerAutoConfiguration matched:
   - @ConditionalOnClass found required classes 'jakarta.servlet.ServletRequest',
     'org.apache.catalina.startup.Tomcat', ...
```

> Spring Boot **không "biết"** bạn cần Tomcat. Nó chỉ thấy class `org.apache.catalina.startup.Tomcat` có trên classpath rồi suy ra.

### `@ConditionalOnClass` trượt → bỏ

```
GsonHttpMessageConvertersConfiguration:
   Did not match:
      - @ConditionalOnClass did not find required class 'com.google.gson.Gson'
```

Cùng cơ chế với `Jsonb`, `KotlinSerialization`, `AspectJ`.

### `@ConditionalOnMissingBean` điền vào chỗ trống

```
JacksonAutoConfiguration#jacksonJsonMapper matched:
   - @ConditionalOnMissingBean ... did not find any beans
```

*"Bạn chưa khai `JsonMapper` à? Được, tôi làm hộ."*

### ⭐ Và đây là bằng chứng nó thật sự LÙI BƯỚC

```
WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter#beanNameViewResolver:
   Did not match:
      - @ConditionalOnMissingBean ... FOUND beans of type ... beanNameViewResolver
```

Một auto-config khác đã tạo `beanNameViewResolver` trước → auto-config này **tự rút lui**.

> **Bạn không "ghi đè" auto-configuration. Bạn chỉ đến trước, và nó tự né.**
> Đây là ý quan trọng nhất của cả note. Không có tranh chấp, không có ưu tiên — chỉ là "đã có rồi thì thôi".

---

## 4. Starter là gì

`spring-boot-starter-webmvc` **gần như không chứa dòng code nào**. Nó chỉ là một `pom` liệt kê một bộ thư viện đã chọn sẵn và kiểm thử hợp nhau.

Nhiệm vụ duy nhất: **đặt đúng bộ jar lên classpath.**

```
starter  →  đặt jar lên classpath  →  @ConditionalOnClass khớp  →  bean được tạo
```

Ba mắt xích. Không có ma thuật — cùng kết luận với [note 03](03-classloader-reflection.md).

### ⚠️ `pom.xml` KHÔNG được đọc lúc chạy

Chỗ này hay bị hỏi và hay trả lời sai:

```
pom.xml  ──Maven tải──►  các file .jar  ──►  CLASSPATH  ──►  Spring Boot nhìn vào đây
(build time)                                                  (runtime)
```

Lúc ứng dụng chạy, Maven đã xong việc từ lâu. Spring Boot nhìn **classpath**, không nhìn `pom.xml`.

### Hai nghĩa của chữ "dependency" — đừng trộn

| | Nghĩa | Xảy ra lúc nào |
|---|---|---|
| **Maven dependency** | thư viện `.jar` mà project cần | lúc **build** |
| **Dependency injection** | container đưa object B cho object A | lúc **chạy** |

Maven dependency **không được "inject"**. Nó được **tải về và đặt lên classpath**.

---

## 5. Transitive dependency

> **Transitive dependency** = dependency của dependency. Khai A, Maven kéo về cả những gì A cần, và những gì *chúng* cần, đệ quy xuống hết.

Project này khai **2 dòng** trong `pom.xml` nhưng classpath lúc chạy có **hơn 35 jar**.

```
+- spring-boot-starter-webmvc
|  +- spring-boot-starter                ← transitive
|  |  +- spring-boot-autoconfigure       ← transitive  (@SpringBootApplication)
|  +- spring-boot-http-converter
|  |  +- spring-boot                     ← transitive  (SpringApplication)
|  +- spring-boot-starter-tomcat
|  +- spring-boot-starter-jackson
```

> ⚠️ **Bẫy đã dính:** comment `spring-boot-starter-webmvc` ra để thí nghiệm → mất luôn cả cây bên dưới → `SpringApplication` biến mất → lỗi biên dịch.
> Muốn bỏ phần web mà giữ lõi thì **đổi** sang `spring-boot-starter`, đừng xoá.

### Lệnh đáng thuộc

```bash
.\mvnw.cmd dependency:tree
```

Dùng khi: một jar lạ chui vào từ đâu · gỡ xung đột phiên bản · truy lỗ hổng bảo mật đến từ nhánh nào.

> **Câu phỏng vấn:** hai dependency đòi hai version khác nhau của cùng một jar, Maven chọn cái nào?
> → **nearest-wins**: cái nào gần gốc cây hơn thì thắng.

---

## 6. Thí nghiệm đã đo trên project này

Đổi `spring-boot-starter-webmvc` → `spring-boot-starter`, giữ nguyên mọi thứ khác:

| | Có web | Không web |
|---|---|---|
| Tổng số bean | **145** | **50** |
| Chương trình | **treo mãi**, phải `Ctrl+C` | **tự thoát**, `exit code 0` |
| `paymentsApplication` | có | có |
| `a == b` | `true` | `true` |

**95 bean — 65% container — đến từ đúng một dòng trong `pom.xml`.**

Bean của bạn và singleton scope **không đổi** — chúng đến từ `@ComponentScan`, nhánh khác trong `@SpringBootApplication`, không liên quan auto-configuration.

### Vì sao có web thì treo, không web thì thoát?

Không phải chuyện của Spring. **Luật của JVM:**

> **JVM chỉ kết thúc khi TẤT CẢ thread non-daemon đã xong.**

| Loại thread | JVM có chờ không? | Ví dụ |
|---|---|---|
| **non-daemon** (mặc định) | **Có** | `main`, thread nhận request của Tomcat |
| **daemon** | **Không** | GC, thread nền của JIT |

- **Có web:** Tomcat tạo thread **non-daemon** ngồi chờ request. `main()` xong từ lâu nhưng JVM không được phép thoát.
- **Không web:** chỉ có `main`. `main()` xong → hết thread non-daemon → JVM thoát, `exit code 0`.

*(`exit code 0` = bình thường; khác 0 = lỗi. CI/CD dùng chính con số này để biết build đậu hay rớt — [note 06](06-git-professional.md).)*

---

## 7. Bean được tạo EAGER, không lazy

Câu hỏi: 145 bean đó tạo ngay lúc khởi động, hay chờ ai cần mới tạo?

**Tạo hết ngay lúc khởi động.** Đừng nhầm với classloader — hai chữ "lazy" khác nhau:

| | Lazy hay không |
|---|---|
| **Classloader** nạp class từ đĩa | **lazy** ✅ — lần đầu ai cần mới nạp ([note 03](03-classloader-reflection.md)) |
| **Container** tạo bean singleton | **KHÔNG lazy** — eager, tạo hết lúc khởi động |

### Vì sao eager? — Fail-fast

`DataSource` cấu hình sai mật khẩu database:

| | Khi nào bạn biết |
|---|---|
| Lazy | 3 giờ sáng, khi request đầu tiên chạm tới. App đã "khởi động thành công", đã lên production, đã nhận traffic. |
| Eager | Giây thứ 2 lúc khởi động. App **không lên được**, deploy thất bại, traffic chưa hề chạm vào. |

> Thà chết lúc khởi động còn hơn chết lúc đang phục vụ khách. Với hệ thống payment, đây không phải sở thích thiết kế — là bắt buộc.

### Bằng chứng trong log

```
15:50:01.557  Tomcat initialized with port 8080
15:50:02.138  Tomcat started on port 8080
15:50:02.158  Started PaymentsApplication in 2.068 seconds
              Application availability state ReadinessState changed to ACCEPTING_TRAFFIC
```

Tomcat mở cổng và lắng nghe **trước khi** khởi động xong, **chưa có request nào**. Không ai "cần" nó — nó vẫn được tạo. Và app chỉ tuyên bố `ACCEPTING_TRAFFIC` **sau khi** mọi bean đã dựng xong.

*(Muốn lazy phải xin riêng bằng `@Lazy`. Mặc định là eager.)*

---

## 8. Đọc báo cáo CONDITIONS EVALUATION REPORT

```properties
# application.properties — chỉ bật khi cần, XOÁ ĐI sau khi xem
debug=true
```

| Mục | Nghĩa |
|---|---|
| **Positive matches** | đã bật, kèm lý do |
| **Negative matches** | đã bỏ, kèm lý do |
| **Exclusions** | bị loại thủ công bằng `@SpringBootApplication(exclude = ...)` |
| **Unconditional classes** | luôn bật, không cần điều kiện |

Đo trên project này: **~52 Positive** vs **~39 Negative**.

> 💡 **Spring Boot 4 khác hẳn Boot 2.x ở đây.** Boot 2.x nhét *toàn bộ* auto-config vào một jar `spring-boot-autoconfigure` → mọi thứ đều là ứng viên → Negative matches dài hàng trăm dòng (Redis, MongoDB, Kafka, JPA…). Boot 4 **chia nhỏ theo module** (`spring-boot-tomcat`, `spring-boot-jackson`, `spring-boot-webmvc`…) → không kéo module thì không thành ứng viên → danh sách gọn hẳn.
> Nếu đọc tài liệu/blog cũ thấy nói "Negative matches rất dài", đó là mô tả Boot 2.

---

## Câu hỏi phỏng vấn từ phần này

1. `@SpringBootApplication` gồm những gì? → *`@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`*
2. Auto-configuration hoạt động thế nào? → *đọc `AutoConfiguration.imports` → lọc bằng `@Conditional...`*
3. Spring Boot có đọc `pom.xml` lúc chạy không? → *không, nó nhìn **classpath***
4. Bạn tự khai một bean thì auto-config có ghi đè không? → *không, nó **lùi bước** nhờ `@ConditionalOnMissingBean`*
5. Starter là gì? → *một pom kéo sẵn bộ jar, gần như không có code*
6. Transitive dependency là gì? Maven xử lý xung đột version ra sao? → *nearest-wins*
7. Bean singleton tạo eager hay lazy? Vì sao? → *eager, để fail-fast*
8. Làm sao xem Spring Boot đã bật/bỏ auto-config nào? → *`debug=true`, đọc CONDITIONS EVALUATION REPORT*
9. Làm sao tắt một auto-configuration? → *`@SpringBootApplication(exclude = XxxAutoConfiguration.class)`*
10. Vì sao app Spring Boot web chạy mãi không thoát? → *Tomcat tạo thread **non-daemon**; JVM chỉ thoát khi hết thread non-daemon*
11. Daemon thread khác non-daemon thread thế nào?

## Còn nợ / sẽ học sau

- Tự viết auto-configuration cho thư viện của mình (`@AutoConfiguration` + file `imports`)
- `@ConditionalOnThreading` và virtual threads — thử `spring.threads.virtual.enabled=true` xem báo cáo đổi thế nào (Java 21+)
- `spring-boot-actuator` — nhiều Negative match trong báo cáo đang chờ nó
- Spring Boot 4 dùng Jackson 3 (`tools.jackson`) thay Jackson 2 (`com.fasterxml.jackson`)

## Liên quan

- [02-spring-ioc-container.md](02-spring-ioc-container.md) — container và `@SpringBootApplication`
- [03-classloader-reflection.md](03-classloader-reflection.md) — classpath và cách Spring đọc nó
- [05-stateless-thread-safety.md](05-stateless-thread-safety.md) — thread
- [06-git-professional.md](06-git-professional.md) — exit code trong CI/CD
