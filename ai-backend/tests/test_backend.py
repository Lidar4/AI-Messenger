import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["runtime"] == "self-hosted-messenger-and-ai"

def test_auth_registration_and_login():
    # Register unique user
    reg_response = client.post("/v1/auth/register", json={
        "username": "testuser_unique_123",
        "displayName": "Test User",
        "password": "securepassword123"
    })
    assert reg_response.status_code == 200
    data = reg_response.json()
    assert "userId" in data
    assert "token" in data

    # Login user
    login_response = client.post("/v1/auth/login", json={
        "username": "testuser_unique_123",
        "password": "securepassword123"
    })
    assert login_response.status_code == 200
    login_data = login_response.json()
    assert "token" in login_data
