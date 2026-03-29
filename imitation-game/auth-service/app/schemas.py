import re

from pydantic import BaseModel, EmailStr, field_validator


class RegisterRequest(BaseModel):
    username: str
    email: EmailStr
    password: str

    @field_validator("username")
    @classmethod
    def username_valid(cls, v: str) -> str:
        if not re.match(r"^[a-zA-Z0-9_-]{3,32}$", v):
            raise ValueError(
                "Username must be 3–32 characters: letters, digits, underscores, or hyphens"
            )
        return v

    @field_validator("password")
    @classmethod
    def password_length(cls, v: str) -> str:
        if len(v) < 6:
            raise ValueError("Password must be at least 6 characters")
        return v


class LoginRequest(BaseModel):
    username: str
    password: str


class RefreshRequest(BaseModel):
    refresh_token: str


class DeleteAccountRequest(BaseModel):
    password: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int = 3600


class UserResponse(BaseModel):
    id: str
    username: str
    email: str
    roles: list[str]

    model_config = {"from_attributes": True}
