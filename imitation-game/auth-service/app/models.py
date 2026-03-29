import uuid

from sqlalchemy import Boolean, Column, String
from sqlalchemy.sql.sqltypes import TIMESTAMP
from sqlalchemy.sql import func

from .database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    username = Column(String(64), unique=True, nullable=False, index=True)
    email = Column(String(256), unique=True, nullable=False, index=True)
    hashed_password = Column(String, nullable=False)
    roles = Column(String, default="user")  # comma-separated role names
    is_active = Column(Boolean, default=True)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())
