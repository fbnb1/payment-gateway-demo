# src/payments/models.py
# ORM model (entity ↔ bảng). KHÁC schemas.py (Pydantic = DTO biên API).

import uuid
from datetime import datetime
from decimal import Decimal

from sqlalchemy import DateTime, Numeric, String, Uuid, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from uuid6 import uuid7


class Base(DeclarativeBase):
    """Base khai báo chung — giữ metadata của mọi bảng (≈ gốc @Entity)."""


class Payment(Base):
    __tablename__ = "payments"

    # Surrogate PK: UUIDv7 — time-ordered (giữ right-append locality) + non-enumerable.
    #   default=uuid7: callable sinh UUIDv7 mới ở Python LÚC INSERT (client-side default).
    #   Uuid: kiểu generic của SQLAlchemy → ánh xạ sang UUID native của Postgres.
    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid7)

    # ★ IDEMPOTENCY: UNIQUE ở tầng DB — đảm bảo 1 request_id chỉ tạo 1 payment.
    #   index=True để tra cứu nhanh khi check trùng.
    request_id: Mapped[str] = mapped_column(String(64), unique=True, index=True)

    payer_id: Mapped[str] = mapped_column(String(64))
    service_id: Mapped[str] = mapped_column(String(64))

    # Tiền: Numeric(precision, scale) ↔ Decimal — KHÔNG float (khớp Pydantic).
    amount: Mapped[Decimal] = mapped_column(Numeric(18, 2))
    currency: Mapped[str] = mapped_column(String(3))

    # Trạng thái vòng đời; mặc định PENDING ở tầng app.
    status: Mapped[str] = mapped_column(String(16), default="PENDING")

    # created_at: DB tự điền thời điểm INSERT (server_default = now() của Postgres).
    #   timezone=True: luôn lưu timestamptz cho payment (best practice, tránh lệch TZ).
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # TODO: thêm cột `updated_at` theo mẫu created_at, NHƯNG thêm `onupdate=func.now()`
    #   để nó tự cập nhật mỗi lần UPDATE. Gợi ý tham số: server_default=func.now(), onupdate=func.now()
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )
