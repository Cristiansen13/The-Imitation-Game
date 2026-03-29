import base64
import uuid
from datetime import datetime, timedelta, timezone

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import (
    Encoding, NoEncryption, PrivateFormat, PublicFormat,
)
from jose import JWTError
from jose import jwt as jose_jwt

# ---------------------------------------------------------------------------
# RSA-2048 key pair — generated once per container lifetime.
# Spring Boot services validate JWTs by fetching the public key from /auth/jwks.
# ---------------------------------------------------------------------------
_private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=2048,
    backend=default_backend(),
)
_public_key = _private_key.public_key()
_key_id = str(uuid.uuid4())[:8]

_private_pem: str = _private_key.private_bytes(
    Encoding.PEM, PrivateFormat.TraditionalOpenSSL, NoEncryption()
).decode()

_public_pem: str = _public_key.public_bytes(
    Encoding.PEM, PublicFormat.SubjectPublicKeyInfo
).decode()

ISSUER = "http://auth-service:8000"
ACCESS_TOKEN_EXPIRE_MINUTES = 60
REFRESH_TOKEN_EXPIRE_DAYS = 7


def _int_to_base64url(n: int) -> str:
    length = (n.bit_length() + 7) // 8
    return base64.urlsafe_b64encode(n.to_bytes(length, "big")).rstrip(b"=").decode()


def get_jwks() -> dict:
    """Return the public key as a JWK Set — consumed by Spring NimbusJwtDecoder."""
    pub_numbers = _public_key.public_numbers()
    return {
        "keys": [
            {
                "kty": "RSA",
                "use": "sig",
                "alg": "RS256",
                "kid": _key_id,
                "n": _int_to_base64url(pub_numbers.n),
                "e": _int_to_base64url(pub_numbers.e),
            }
        ]
    }


def create_access_token(sub: str, username: str, email: str, roles: list[str]) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": sub,
        "preferred_username": username,
        "email": email,
        "realm_access": {"roles": roles},
        "iss": ISSUER,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)).timestamp()),
        "jti": str(uuid.uuid4()),
    }
    return jose_jwt.encode(payload, _private_pem, algorithm="RS256", headers={"kid": _key_id})


def create_refresh_token(sub: str) -> str:
    now = datetime.now(timezone.utc)
    payload = {
        "sub": sub,
        "type": "refresh",
        "iss": ISSUER,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS)).timestamp()),
        "jti": str(uuid.uuid4()),
    }
    return jose_jwt.encode(payload, _private_pem, algorithm="RS256", headers={"kid": _key_id})


def decode_token(token: str) -> dict:
    return jose_jwt.decode(
        token,
        _public_pem,
        algorithms=["RS256"],
        options={"verify_aud": False},
    )
