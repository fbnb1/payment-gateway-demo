# src/payments/schemas.py
# Pydantic v2 schemas cho Payments API.
# Field `amount` đã viết sẵn làm MẪU. Bạn điền 4 chỗ TODO theo đúng kiểu mẫu đó.

from pydantic_core.core_schema import nullable_schema
from decimal import Decimal
from enum import Enum

from pydantic import BaseModel, Field


class Currency(str, Enum):
    """Các currency hệ thống hỗ trợ (ISO 4217).

    Kế thừa `str` để khi serialize ra JSON nó thành "USD", không phải Currency.USD.
    TODO (tuỳ chọn): thêm currency khác nếu cần.
    """

    USD = "USD"
    EUR = "EUR"
    VND = "VND"


class CreatePaymentRequest(BaseModel):
    """Request body cho POST /payments."""

    # ----- MẪU: field khó nhất, đã viết sẵn -----
    amount: Decimal = Field(gt=0, max_digits=18, decimal_places=2)

    # ----- TODO: bạn điền 4 field dưới đây -----
    # request_id: idempotency key client gửi. Chuỗi, không rỗng.
    request_id: str = Field(min_length=1)
    #   gợi ý: request_id: str = Field(min_length=1)
    # TODO 1:

    # payer_id: ai trả tiền. Chuỗi, không rỗng.
    payer_id: str = Field(min_length=1)
    # TODO 2:

    # service_id: dịch vụ/merchant nhận tiền. Chuỗi, không rỗng.
    # TODO 3:
    service_id: str = Field(min_length=1)

    # currency: dùng Enum Currency ở trên (type-safe).
    #   gợi ý: currency: Currency
    # TODO 4:
    currency: Currency
