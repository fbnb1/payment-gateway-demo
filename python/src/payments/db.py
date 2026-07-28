# src/payments/db.py
# Lớp truy cập DB: async engine + session factory. Comment để HỌC.

from collections.abc import AsyncIterator

from sqlalchemy import text
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

# DSN = "dialect+driver://user:password@host:port/dbname"
#   dialect+driver = postgresql+asyncpg  (Postgres qua driver async asyncpg)
# TODO: điền DSN từ "Connection facts" trong PROGRESS.md
#   (host = localhost, port = cổng host đã map, user/pass/db = payments)
DATABASE_URL = "postgresql+asyncpg://payments:payments@localhost:5433/payments"

# ENGINE: tạo MỘT LẦN toàn app — giữ connection pool (≈ EntityManagerFactory).
#   echo=True: in câu SQL thật ra console — bật khi học để thấy SQLAlchemy sinh gì.
engine = create_async_engine(DATABASE_URL, echo=True)

# SESSION FACTORY: "khuôn" sinh AsyncSession mới mỗi request (≈ cách lấy EntityManager).
#   expire_on_commit=False: sau commit, object vẫn đọc được field (tránh lazy-load lỗi trong async).
SessionFactory = async_sessionmaker(engine, expire_on_commit=False)


async def get_session() -> AsyncIterator[AsyncSession]:
    """FastAPI dependency: tiêm 1 AsyncSession/request, tự đóng khi request xong.

    `async with`: mở session; `yield`: trao cho endpoint; khi endpoint trả xong,
    code sau yield chạy → session.close() (trả connection về pool). ≈ request-scoped bean.
    """
    async with SessionFactory() as session:
        yield session


async def check() -> None:
    """Test kết nối tới Postgres thật bằng SELECT 1."""
    # `async with`: context manager bất đồng bộ — tự mở & ĐÓNG connection (trả về pool).
    async with engine.connect() as conn:
        result = await conn.execute(text("SELECT 1"))  # await: nhả event loop khi chờ DB
        print("DB says:", result.scalar())


async def create_all() -> None:
    """Tạo mọi bảng theo metadata của Base (chỉ dùng dev; production sẽ là Alembic)."""
    # import models để Base.metadata "biết" bảng Payment.
    from payments.models import Base

    # engine.begin(): mở transaction + tự COMMIT khi thoát (khác .connect() rollback).
    async with engine.begin() as conn:
        # DEV-ONLY: drop trước để đổi schema thoải mái (Phase 1 sẽ thay bằng Alembic migration).
        await conn.run_sync(Base.metadata.drop_all)
        # create_all là API ĐỒNG BỘ → run_sync bắc cầu nó chạy trên async connection.
        await conn.run_sync(Base.metadata.create_all)
    print("Tables recreated.")
