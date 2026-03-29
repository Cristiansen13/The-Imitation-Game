import logging
import os

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from passlib.context import CryptContext

from .database import Base, SessionLocal, engine
from .models import User
from .routers.auth import router as auth_router

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def _seed_bot_users() -> None:
    """Create bot users (aibot1–aibot5) on first startup so ai-bot-service can log in."""
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
    bot_password = os.getenv("BOT_PASSWORD", "aibot123")
    db = SessionLocal()
    try:
        for i in range(1, 6):
            username = f"aibot{i}"
            if not db.query(User).filter(User.username == username).first():
                user = User(
                    username=username,
                    email=f"aibot{i}@imitation-game.internal",
                    hashed_password=pwd_context.hash(bot_password),
                    roles="user",
                )
                db.add(user)
                logger.info("Created bot user: %s", username)
        db.commit()
    finally:
        db.close()


def create_app() -> FastAPI:
    Base.metadata.create_all(bind=engine)
    _seed_bot_users()

    app = FastAPI(
        title="Auth Service",
        description="Custom JWT authentication and authorization service",
        version="1.0.0",
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        errors = []
        for e in exc.errors():
            loc = e.get("loc", [])
            field = str(loc[-1]) if loc else "field"
            msg = e.get("msg", "invalid value")
            errors.append(f"{field}: {msg}" if field != "body" else msg)
        return JSONResponse(
            status_code=422,
            content={"detail": "; ".join(errors)},
        )

    app.include_router(auth_router)
    return app


app = create_app()
