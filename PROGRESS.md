# PROGRESS — Unified Payment Platform (mentored by "Bridge")

> Sổ tay tiến độ cho các phiên sau bám theo. Cập nhật mỗi khi xong một increment.
> Cập nhật lần cuối: 2026-06-18

---

## 1. Đang ở đâu

- **Phase hiện tại:** Phase 0 — Foundation.
- **Increment vừa xong:** verify UNIQUE constraint khai hỏa (gửi `r1` lần 2 → 500 `IntegrityError`, idempotency ở DB); dọn import thừa; commit `f8c2e4e`.
- **▶ RESUME HERE (phiên sau):** (1) ĐỌC **DDIA Ch.7 Transactions** trước — Phase 1 chính là chương này viết thành code. (2) Vào **Phase 1 — Ledger correctness** (isolation, write skew, `SELECT FOR UPDATE`, + CI GitHub Actions, testcontainers).
- **Polish Phase 0 còn nợ (nhẹ, làm lúc nào cũng được):** `response_model` cho output, biến 500→409 cho duplicate, 1 test pytest đầu tiên.

### Trạng thái đọc DDIA
- Đã đọc hết **Ch.6 Partitioning (sharding)**. **Chưa** đọc Ch.7 Transactions → đọc TRƯỚC Phase 1.
- Lưu ý numbering: Ch.6=Partitioning, **Ch.7=Transactions**, Ch.8=Distributed troubles (để Phase 4), Ch.9=Consensus (Phase 4).
- **Cổng đã biết bị chiếm:** 8000 = container `infra-app-1` (minifeed); 5432 = container `infra-postgres-1`. Vì thế: app FastAPI ở `--port 8080`, Postgres của ta map host `5433`.

### Connection facts (cho SQLAlchemy)
- Host:port từ máy = **`localhost:5433`** · trong container = `5432`.
- user / password / db = **`payments` / `payments` / `payments`**.
- DSN async dự kiến: `postgresql+asyncpg://payments:payments@localhost:5433/payments`.
- **Repo:** https://github.com/fbnb1/payment-gateway-demo (nhánh `main`).

---

## 2. Đã hoàn thành (Done)

- [x] Đổi `claude.md` → `CLAUDE.md` (portable cho Linux CI/container).
- [x] `git init` + `.gitignore` cho Python/uv (+ `.claude/`, `.venv/`).
- [x] `uv init --package --name payments --python 3.12` → `src/payments/`.
- [x] `uv run payments` → tạo `.venv` + `uv.lock`, chạy `main()`.
- [x] Sửa tác giả trong `pyproject.toml` (bỏ leftover trading-bot).
- [x] Commit đầu tiên (amend gộp sửa-author) → `git push -u origin main --force`.
- [x] Xác minh remote khớp local (`git ls-remote`).
- [x] App FastAPI tối thiểu (`src/payments/main.py`) + endpoint `GET /health`.
- [x] Chạy bằng `fastapi dev ... --port 8080`; gỡ lỗi port-conflict (Docker giữ 8000).
- [x] Pydantic v2: `Currency` Enum + `CreatePaymentRequest` (amount=Decimal, currency=Enum).
- [x] Endpoint `POST /payments` dùng model → FastAPI tự validate (422 khi input sai).
- [x] Học sâu **async**: concurrency vs parallelism, I/O- vs CPU-bound, GIL, mapping Java↔Python.
- [x] Học nền **Docker**: image vs container, registry, port mapping, named volume.
- [x] `compose.yaml` chạy Postgres 16 (map host 5433); gỡ port-conflict với stack "infra".
- [x] SQLAlchemy 2.0 async: engine + `async_sessionmaker` + DI `get_session` (`db.py`).
- [x] ORM `Payment` (`models.py`): PK **UUIDv7** (lib `uuid6`), `request_id` UNIQUE (idempotency ở DB), `amount Numeric(18,2)`, `created_at/updated_at timestamptz`.
- [x] `POST /payments` ghi thật xuống Postgres (add + await commit); verify 1 hàng trong DB.

---

## 3. Việc còn lại của Phase 0

- [ ] `uv add fastapi` + một app FastAPI tối thiểu, 1 endpoint.
- [ ] Pydantic v2 models cho request/response.
- [ ] Postgres qua Docker Compose.
- [ ] SQLAlchemy 2.0 async + sổ cái single-node (happy path: tạo payment → ghi ledger).
- [ ] (Skill) Docker Compose chạy app + Postgres.

---

## 4. Ghi chú "đã học" (knowledge web)

| Chủ đề | Ý chính đã nắm |
|---|---|
| uv vs Maven | uv = build tool + venv + lockfile gộp một; `pyproject.toml` ≈ `pom.xml` |
| `pyproject.toml` (PEP 621) | `[project]` metadata+deps · `requires-python` ghim runtime · `[project.scripts]` entry point · `[build-system]` backend |
| src vs flat layout | **src layout** = test chạy trên code đã-cài → bắt lỗi đóng gói sớm; chọn cho service deploy |
| Console entry point | `payments = "payments:main"` → lệnh `payments` gọi `main()` trong package |
| Lockfile policy | **application/service → commit `uv.lock`** (tái lập byte-for-byte); library thì thường không |
| `pyproject` = ý định, `uv.lock` = sự thật đã giải | thêm deps bằng `uv add`, không sửa tay |
| Git: amend | chỉ amend commit **chưa push**; `--amend --no-edit` gộp staged vào commit gần nhất |
| Git: lịch sử rời | remote có lịch sử khác → merge `--allow-unrelated-histories` (an toàn) **hoặc** `push --force` (repo cá nhân) |

### "Java-ism" đã được nhắc chỉnh
- Bỏ `utils` grab-bag; hàm tự do sống trong module domain của nó.
- Không dựng sẵn "củ hành" nhiều tầng (infra/business/adapter) khi chưa có logic — cấu trúc mọc theo nhu cầu. (Park lại cho Phase 3–4.)

---

## 5. Câu hỏi mở / Park lại

- Phân lớp hexagonal (infra/business/adapter) → kích hoạt lại ở Phase 3–4.
- Cảnh báo `Failed to hardlink` của uv (cache ổ C, project ổ D) — vô hại; muốn tắt: `UV_LINK_MODE=copy`.
- 3 probe đang chờ học: lệnh `uv add`, ASGI server cho FastAPI, `async def` cơ bản.

---

## 6. Thước đo tiến độ (ước lượng thô)

### % build project end-to-end
**~3%** — mới có scaffold + repo. Còn cả 5 subsystem (orchestrator, ledger, saga, reconciliation, event-sourcing) + PSP simulator.

```
[#·····················] 3%
```

### % hoàn thành Phase 0
**~90%** — lõi happy-path xong + verify idempotency constraint. Còn polish: response_model, 500→409, vài test, (tuỳ chọn) Dockerfile cho app.

```
[##################··] 90%
```

### % đã học theo kỹ năng
| Kỹ năng | % | Ghi chú |
|---|---|---|
| Git chuyên nghiệp | ~35% | đã: init, commit, amend, remote, force-push, unrelated histories. Còn: branching, PR, rebase workflow |
| uv / packaging Python | ~40% | đã: init, run, layout, lockfile. Còn: `uv add`, dependency groups, build/publish |
| Python ngôn ngữ (type hints, async, Pydantic) | ~30% | đã: type hints, Pydantic v2, `async with`/`await` thực hành, async generator (DI), context manager. Còn: decorators, generics, Protocol |
| SQLAlchemy 2.0 async | ~25% | đã: engine/pool, `async_sessionmaker`, DI session, typed ORM (`Mapped`/`mapped_column`), `create_all`/`run_sync`, add+commit, UUIDv7 PK. Còn: query/select, relationship, Alembic, isolation |
| FastAPI / gRPC | ~20% | đã: app instance, `@app.get`/`@app.post`, async endpoint, body model auto-validate (422), Swagger `/docs`, `fastapi dev`. Còn: path/query params, response_model, DI, gRPC |
| Docker / Compose | ~25% | đã: image/container, port mapping, named volume, `compose up/down/ps/logs/exec`, disk mgmt. Còn: Dockerfile build app, multi-service deps, healthcheck |
| Kubernetes / CI-CD | 0% | Phase 1+ |
| DDIA / distributed systems | 0% | Phase 1+ (vùng mạnh sẵn của learner) |

> Lưu ý: learner đã có ~9 năm backend (Java/.NET, payment domain, Kafka, SQL, DDD/EDA) — các % "đã học" trên chỉ tính phần **Python/tooling mới**, không phản ánh năng lực nền đã vững.
