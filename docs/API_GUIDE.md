# 📡 API Reference Guide

Shop1의 주요 API 엔드포인트 요약입니다.
상세한 Request/Response 명세는 서버 실행 후 Swagger UI에서 확인 가능합니다.

* **Swagger URL:** `http://localhost:8080/swagger-ui/index.html` (Local)

---

## 1. Authentication (인증)
| Method | URI | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| POST | `/api/auth/login` | 이메일 로그인 | ❌ |
| POST | `/api/auth/signup` | 회원가입 | ❌ |
| POST | `/api/auth/refresh` | Access Token 재발급 | ❌ |

## 2. Items (상품)
| Method | URI | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| GET | `/api/items` | 상품 목록 조회 (검색/필터) | ❌ |
| GET | `/api/items/{id}` | 상품 상세 조회 | ❌ |
| POST | `/api/items` | 상품 등록 | ✅ (Seller) |
| PUT | `/api/items/{id}` | 상품 수정 | ✅ (Seller) |

## 3. Orders (주문)
| Method | URI | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| POST | `/api/orders` | 주문 생성 | ✅ |
| GET | `/api/orders/{id}` | 주문 상세 조회 | ✅ |
| POST | `/api/orders/payment` | 결제 승인 요청 | ✅ |

## 4. Coupons (쿠폰)
| Method | URI | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| GET | `/api/coupons` | 발급 가능 쿠폰 목록 | ✅ |
| POST | `/api/coupons/{id}/issue` | 쿠폰 발급받기 | ✅ |