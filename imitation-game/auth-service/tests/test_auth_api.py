import os
from pathlib import Path

os.environ.setdefault("DATABASE_URL", "sqlite:///./auth_test.db")

from fastapi import FastAPI
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.database import Base, get_db
from app.routers.auth import router


SQLALCHEMY_DATABASE_URL = "sqlite:///./auth_test.db"
engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()


app = FastAPI()
app.include_router(router)
app.dependency_overrides[get_db] = override_get_db
client = TestClient(app)


def setup_module():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)


def teardown_module():
    Base.metadata.drop_all(bind=engine)
    engine.dispose()
    db_file = Path("auth_test.db")
    if db_file.exists():
        try:
            db_file.unlink()
        except PermissionError:
            pass


def register_user(username="user1", email="user1@example.com", password="secret123"):
    return client.post(
        "/auth/register",
        json={"username": username, "email": email, "password": password},
    )


def login_user(username="user1", password="secret123"):
    return client.post("/auth/login", json={"username": username, "password": password})


def test_health_ok():
    res = client.get("/auth/health")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"


def test_jwks_ok():
    res = client.get("/auth/jwks")
    assert res.status_code == 200
    assert "keys" in res.json()


def test_register_and_login_ok():
    res = register_user()
    assert res.status_code == 201

    login = login_user()
    assert login.status_code == 200
    body = login.json()
    assert "access_token" in body
    assert "refresh_token" in body


def test_refresh_ok():
    register_user("user2", "user2@example.com", "secret123")
    login = login_user("user2", "secret123")
    refresh_token = login.json()["refresh_token"]

    res = client.post("/auth/refresh", json={"refresh_token": refresh_token})
    assert res.status_code == 200
    assert "access_token" in res.json()


def test_userinfo_ok():
    register_user("user3", "user3@example.com", "secret123")
    token = login_user("user3", "secret123").json()["access_token"]

    res = client.get("/auth/userinfo", headers={"Authorization": f"Bearer {token}"})
    assert res.status_code == 200
    assert res.json()["username"] == "user3"


def test_logout_ok():
    register_user("user4", "user4@example.com", "secret123")
    token = login_user("user4", "secret123").json()["access_token"]

    res = client.put("/auth/logout", headers={"Authorization": f"Bearer {token}"})
    assert res.status_code == 200


def test_delete_account_ok():
    register_user("user5", "user5@example.com", "secret123")
    token = login_user("user5", "secret123").json()["access_token"]

    res = client.request(
        "DELETE",
        "/auth/delete-account",
        json={"password": "secret123"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert res.status_code == 200
