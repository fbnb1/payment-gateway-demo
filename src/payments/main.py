# src/payments/main.py
# App FastAPI tối thiểu cho Phase 0. Các comment dưới đây là để HỌC —
# ở code thật ta sẽ bỏ bớt; giờ cứ giữ để bạn đọc hiểu từng dòng.

# 1) IMPORT: lấy class FastAPI từ package `fastapi` (đã cài bằng `uv add`).
#    Cú pháp `from <package> import <thứ>` ≈ `import com.x.FastAPI;` bên Java.
from fastapi import FastAPI
from payments.schemas import CreatePaymentRequest
from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession

from payments.db import get_session
from payments.models import Payment

# 2) TẠO INSTANCE: tạo một object FastAPI tên là `app`.
#    Python KHÔNG có từ khoá `new` — gọi thẳng tên class kèm `()` là tạo object.
#    `app` chính là ứng dụng; uvicorn sẽ tìm đúng biến tên này để chạy.
app = FastAPI(title="Payments API")


# 3) ENDPOINT: decorator `@app.get(...)` gắn hàm bên dưới vào tuyến GET /health.
#    Nó ≈ `@GetMapping("/health")` trong Spring, nhưng đặt thẳng trên hàm, không cần class Controller.
@app.get("/health")
async def health_check() -> dict[str, str]:
    # `async def` = hàm bất đồng bộ (có thể `await` mà không block thread).
    # `-> dict[str, str]` là TYPE HINT: khai báo hàm trả về dict {str: str}.
    #   Python không bắt buộc, nhưng project này YÊU CẦU type hint đầy đủ (xem CLAUDE.md).
    # Trả về một dict thường — FastAPI tự convert sang JSON, bạn KHÔNG serialize tay.
    return {"status": "ok", "service": "payments"}


@app.post("/payments")
async def create_payment(
    payment: CreatePaymentRequest,
    session: AsyncSession = Depends(get_session),
) -> dict[str, str]:
    record = Payment(
        request_id=payment.request_id,
        payer_id=payment.payer_id,
        service_id=payment.service_id,
        amount=payment.amount,
        currency=payment.currency.value,
        status="PENDING",
    )
    session.add(record)  # stage INSERT (unit of work)
    await session.commit()  # FLUSH + COMMIT — nhớ bài học ROLLBACK: không commit là mất
    return {"id": str(record.id), "status": record.status}
