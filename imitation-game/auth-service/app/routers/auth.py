from fastapi import APIRouter, Depends, HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError
from passlib.context import CryptContext
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import User
from ..schemas import (
    DeleteAccountRequest,
    LoginRequest,
    RefreshRequest,
    RegisterRequest,
    TokenResponse,
    UserResponse,
)
from ..security import (
    ACCESS_TOKEN_EXPIRE_MINUTES,
    create_access_token,
    create_refresh_token,
    decode_token,
    get_jwks,
)

router = APIRouter(prefix="/auth", tags=["auth"])
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
bearer_scheme = HTTPBearer(auto_error=False)


@router.get("/health")
def health():
    return {"status": "ok"}


@router.get("/jwks")
def jwks():
    """Public JWK Set — Spring Boot NimbusJwtDecoder fetches this to validate tokens."""
    return get_jwks()


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    if db.query(User).filter(User.username == req.username).first():
        raise HTTPException(status_code=400, detail="Username already taken")
    if db.query(User).filter(User.email == req.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")

    user = User(
        username=req.username,
        email=req.email,
        hashed_password=pwd_context.hash(req.password),
        roles="user",
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return UserResponse(
        id=user.id,
        username=user.username,
        email=user.email,
        roles=user.roles.split(","),
    )


@router.post("/login", response_model=TokenResponse)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == req.username).first()
    if not user or not pwd_context.verify(req.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if not user.is_active:
        raise HTTPException(status_code=403, detail="Account disabled")

    roles = [r.strip() for r in user.roles.split(",")]
    return TokenResponse(
        access_token=create_access_token(user.id, user.username, user.email, roles),
        refresh_token=create_refresh_token(user.id),
        expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60,
    )


@router.post("/refresh", response_model=TokenResponse)
def refresh(req: RefreshRequest, db: Session = Depends(get_db)):
    try:
        payload = decode_token(req.refresh_token)
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired refresh token")

    if payload.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="Not a refresh token")

    user = db.query(User).filter(User.id == payload["sub"]).first()
    if not user or not user.is_active:
        raise HTTPException(status_code=401, detail="User not found or disabled")

    roles = [r.strip() for r in user.roles.split(",")]
    return TokenResponse(
        access_token=create_access_token(user.id, user.username, user.email, roles),
        refresh_token=create_refresh_token(user.id),
        expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60,
    )


@router.get("/userinfo", response_model=UserResponse)
def userinfo(
    credentials: HTTPAuthorizationCredentials = Security(bearer_scheme),
    db: Session = Depends(get_db),
):
    if not credentials:
        raise HTTPException(status_code=401, detail="Missing Authorization header")
    try:
        payload = decode_token(credentials.credentials)
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    user = db.query(User).filter(User.id == payload["sub"]).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return UserResponse(
        id=user.id,
        username=user.username,
        email=user.email,
        roles=[r.strip() for r in user.roles.split(",")],
    )


@router.put("/logout")
def logout(credentials: HTTPAuthorizationCredentials = Security(bearer_scheme)):
    if not credentials:
        raise HTTPException(status_code=401, detail="Missing Authorization header")
    try:
        decode_token(credentials.credentials)
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")
    return {"message": "Logged out successfully"}


@router.delete("/delete-account")
def delete_account(
    req: DeleteAccountRequest,
    credentials: HTTPAuthorizationCredentials = Security(bearer_scheme),
    db: Session = Depends(get_db),
):
    if not credentials:
        raise HTTPException(status_code=401, detail="Missing Authorization header")
    try:
        payload = decode_token(credentials.credentials)
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid token")

    user = db.query(User).filter(User.id == payload["sub"]).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    if not pwd_context.verify(req.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    db.delete(user)
    db.commit()
    return {"message": "Account deleted successfully"}
