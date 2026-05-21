#!/usr/bin/env python3
"""Auth System API Test Client"""
import json
import requests
from typing import Optional, Dict, Any

class AuthClient:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base = base_url.rstrip('/')
        self.token: Optional[str] = None
        self.refresh_token: Optional[str] = None
        self.session = requests.Session()
        self.session.headers.update({'Content-Type': 'application/json'})

    def _headers(self) -> Dict[str, str]:
        h = {}
        if self.token:
            h['Authorization'] = f'Bearer {self.token}'
        return h

    def _request(self, method: str, path: str, **kwargs) -> Dict[str, Any]:
        url = f"{self.base}{path}"
        resp = self.session.request(method, url, headers=self._headers(), **kwargs)
        try:
            return resp.json()
        except:
            return {'_status': resp.status_code, '_body': resp.text}

    def get(self, path: str) -> Dict[str, Any]:
        return self._request('GET', path)

    def post(self, path: str, data: Dict = None) -> Dict[str, Any]:
        return self._request('POST', path, json=data or {})

    def put(self, path: str, data: Dict = None) -> Dict[str, Any]:
        return self._request('PUT', path, json=data or {})

    def delete(self, path: str) -> Dict[str, Any]:
        return self._request('DELETE', path)

    def patch(self, path: str, data: Dict = None) -> Dict[str, Any]:
        return self._request('PATCH', path, json=data or {})

    # Convenience methods
    def login(self, identifier: str, credential: str, captcha: Dict = None) -> bool:
        body = {'identifier': identifier, 'credential': credential}
        if captcha:
            body.update(captcha)
        result = self.post('/api/v1/auth/login', body)
        if result.get('code') == 0 and 'data' in result:
            self.token = result['data'].get('accessToken')
            self.refresh_token = result['data'].get('refreshToken')
            return True
        return False

    def register(self, username: str, email: str, password: str, code: str, identity_type: str = 'email') -> Dict:
        return self.post('/api/v1/auth/register', {
            'username': username, 'email': email, 'password': password,
            'identityType': identity_type, 'verificationCode': code
        })

    def send_code(self, identifier: str, identity_type: str = 'email', purpose: str = 'register') -> Dict:
        return self.post('/api/v1/auth/code/send', {
            'identifier': identifier, 'identityType': identity_type, 'purpose': purpose
        })

    def verify_code(self, identifier: str, code: str, purpose: str = 'register') -> Dict:
        return self.post('/api/v1/auth/code/verify', {
            'identifier': identifier, 'code': code, 'purpose': purpose
        })

    def health(self) -> Dict:
        return self.get('/actuator/health/liveness')

# Example usage
if __name__ == '__main__':
    client = AuthClient()
    print("Health:", client.health())
