import os
import time

from sqlalchemy import create_engine
from sqlalchemy.exc import OperationalError
from sqlalchemy.orm import DeclarativeBase, sessionmaker

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://authuser:authpass@auth-db:5432/authdb",
)


def _create_engine_with_retry(url: str, retries: int = 10, delay: int = 3):
    for attempt in range(retries):
        try:
            engine = create_engine(url, pool_pre_ping=True)
            engine.connect().close()
            return engine
        except OperationalError:
            if attempt < retries - 1:
                time.sleep(delay)
    raise RuntimeError(f"Could not connect to database after {retries} attempts")


engine = _create_engine_with_retry(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
