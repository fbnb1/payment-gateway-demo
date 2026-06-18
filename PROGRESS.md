# PROGRESS — Unified Payment Platform (mentored by "Bridge")

> Sổ tay tiến độ cho các phiên sau bám theo. Cập nhật mỗi khi xong một increment.
> Cập nhật lần cuối: 2026-06-18

---

## 1. Đang ở đâu

- **Phase hiện tại:** Phase 0 — Foundation.
- **Increment vừa xong:** Pydantic `CreatePaymentRequest` + endpoint `POST /payments` có validation (verify: hợp lệ → 200, amount âm → 422).
- **Increment kế tiếp:** Postgres qua Docker Compose + SQLAlchemy 2.0 async → sổ cái single-node (happy path).
- **Lưu ý cổng:** Docker Desktop chiếm cổng 8000 → chạy app ở `--port 8080`.
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
**~45%** — scaffold + Git + FastAPI (`/health`, `POST /payments`) + Pydantic validation xong; còn Postgres/Docker + SQLAlchemy ledger.

```
[#########···········] 45%
```

### % đã học theo kỹ năng
| Kỹ năng | % | Ghi chú |
|---|---|---|
| Git chuyên nghiệp | ~35% | đã: init, commit, amend, remote, force-push, unrelated histories. Còn: branching, PR, rebase workflow |
| uv / packaging Python | ~40% | đã: init, run, layout, lockfile. Còn: `uv add`, dependency groups, build/publish |
| Python ngôn ngữ (type hints, async, Pydantic) | ~20% | đã: type hints cơ bản, Pydantic v2 (BaseModel, Field, Enum, parse+validate), mental model async/GIL. Còn: decorators, generics, Protocol, asyncio thực hành |
| FastAPI / gRPC | ~20% | đã: app instance, `@app.get`/`@app.post`, async endpoint, body model auto-validate (422), Swagger `/docs`, `fastapi dev`. Còn: path/query params, response_model, DI, gRPC |
| Docker / Compose | 0% | Phase 0 sau |
| Kubernetes / CI-CD | 0% | Phase 1+ |
| DDIA / distributed systems | 0% | Phase 1+ (vùng mạnh sẵn của learner) |

> Lưu ý: learner đã có ~9 năm backend (Java/.NET, payment domain, Kafka, SQL, DDD/EDA) — các % "đã học" trên chỉ tính phần **Python/tooling mới**, không phản ánh năng lực nền đã vững.
