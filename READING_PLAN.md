# READING_PLAN — "vừa đọc vừa làm" cho Unified Payment Platform

> Nguyên tắc: **đọc just-in-time** — chỉ đọc chương của một phase *ngay trước khi* bắt tay làm phase đó. Không front-load; kiến thức không có chỗ bám sẽ trôi.
>
> DDIA map theo **TÊN chương** (đúng dù bản PDF đánh số khác). Numbering chuẩn: Ch4 Encoding · Ch5 Replication · Ch6 Partitioning · Ch7 Transactions · Ch8 Distributed troubles · Ch9 Consensus · Ch10 Batch · Ch11 Stream.
>
> Ký hiệu: 📘 DDIA · 🏛️ system design/distributed · 🧩 pattern · 🐍 Python · 🛠️ DevOps/infra. **★ = bắt buộc trước phase** · ◦ = tham khảo khi cần.

## Trạng thái đọc
- ✅ **Partitioning (Ch.6)** — xong.
- ✅ **Transactions (Ch.7)** — xong → **đủ điều kiện vào Phase 1**.

---

## Phase 1 — Ledger correctness
| | |
|---|---|
| 📘 | ★ **Transactions** ✅ — trọng tâm *Weak Isolation* (Snapshot, **Write Skew**), *Serializability* (2PL, **SSI**) |
| 🧩 | ★ *Architecture Patterns with Python* (Percival & Gregory) **Ch.1–6**: Repository, **Unit of Work**, Service Layer — khớp SQLAlchemy session |
| 🐍 | ◦ *Effective Python* — context managers, transactions; docs **pytest + testcontainers** |
| 🛠️ | ◦ *Continuous Delivery* (Humble) ch. mở đầu + **GitHub Actions** docs (Ruff+pytest CI) |
| 🏛️ | ◦ Double-entry accounting invariants (đã mạnh) |

## Phase 2 — Idempotency + Outbox + CDC
| | |
|---|---|
| 📘 | ★ **Stream Processing** (mục *Change Data Capture*, log-based) · ◦ **Replication** (log-based → hiểu WAL Debezium đọc) |
| 🧩 | ★ **Transactional Outbox** + **Idempotent Receiver** (microservices.io; *Enterprise Integration Patterns*) · *Arch. Patterns w/ Python* **Ch.8–9** (Events, Message Bus) |
| 🏛️ | ★ Stripe engineering blog — *Idempotency keys* |
| 🐍 | ★ decorators; `aiokafka` async producer/consumer; background poller |
| 🛠️ | ◦ Debezium docs; Confluent Kafka docs |

## Phase 3 — Event sourcing + CQRS
| | |
|---|---|
| 📘 | ★ **Encoding and Evolution** (Avro/Protobuf, schema evolution) · ◦ **Stream Processing** (event sourcing, derived state) đọc lại |
| 🧩 | ★ *Arch. Patterns w/ Python* **Ch.8–13** (Domain Events, **CQRS**, Aggregate) — cuốn đúng nhất · Fowler: *Event Sourcing* & *CQRS* |
| 🐍 | ★ dataclass vs Pydantic cho event; `typing.Protocol`; generics; lib Avro/Protobuf |

## Phase 4 — Saga + 2PC + gRPC *(nặng lý thuyết nhất)*
| | |
|---|---|
| 📘 | ★ **The Trouble with Distributed Systems** (mạng/clock, partial failure) · ★ **Consistency and Consensus** (linearizability, **2PC**, consensus) |
| 🧩 | ★ *Building Microservices* (Newman) — **Sagas**, orchestration vs choreography, compensation · paper *Sagas* (Garcia-Molina) |
| 🐍 | ★ `grpcio` + protobuf; **`asyncio.TaskGroup`** — *Using Asyncio* (Hattingh) |
| 🏛️ | ◦ 2PC vs Saga trade-off |

## Phase 5 — Reconciliation (batch + stream) + PSP simulator
| | |
|---|---|
| 📘 | ★ **Batch Processing** (MapReduce, joins) · ★ **Stream Processing** (stream joins, windowing, exactly-once) |
| 🐍 | ★ **Polars/Pandas** (batch join); **Quix Streams / Bytewax** (stream join); fault injection |
| 🏛️ | ◦ Settlement/reconciliation domain (đã mạnh) |

## Phase 6 — Hardening
| | |
|---|---|
| 📘 | ★ **Replication** + **Partitioning** (đọc lại bằng lăng kính vận hành) · ◦ **Reliable/Scalable/Maintainable** (Ch.1) |
| 🛠️ | ★ *The Kubernetes Book* (Poulton); *Docker Deep Dive* · **OpenTelemetry** docs |
| 🏛️ | ◦ Google SRE book — SLO/monitoring (chọn chương) |
| 🐍 | ◦ profiling; structured logging |

---

## Nền xuyên suốt (đọc rải, không gắn 1 phase)
- 🏛️ **System Design Interview Vol 1 & 2** (Alex Xu) — trước mỗi phase, đọc 1 case liên quan.
- 🏛️ **Understanding Distributed Systems** (Vitillo) — bản đồ distributed gọn, bổ trợ DDIA.
- 🐍 **Fluent Python** (Ramalho) — nhặt idiom mỗi khi bị flag "Java-ism"; không đọc một mạch.
- 🧩 **Enterprise Integration Patterns** — tra cứu khi gặp messaging pattern cụ thể.
- 💾 **Database Internals** (Petrov) — đào sâu B-Tree/WAL/MVCC khi Phase 1 hoặc 6 cần.
