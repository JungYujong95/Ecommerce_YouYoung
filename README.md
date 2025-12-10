# 🛒 E-commerce 주문/재고 시스템

## 1. 실행 방법

### 1-1. 사전 요구 사항
- Docker, Docker Compose 설치
- 기본 실행 포트: http://localhost:8080

---

### 1-2. Docker Compose로 MySQL / Redis 실행
# 프로젝트 루트에서 실행
docker compose up -d

# ⚙️ docker-compose.yml 주요 설정
------------------------------------------------------------
# ✔ MySQL
- 이미지: mysql:8.0
- 컨테이너 이름: youyoung-mysql
- 포트: 3306:3306
- 환경 변수:
  MYSQL_ROOT_PASSWORD=root123!
  MYSQL_DATABASE=ecommerce
  MYSQL_USER=ecommerce
  MYSQL_PASSWORD=ecommerce123!
  TZ=Asia/Seoul
- 볼륨: mysql-data:/var/lib/mysql
- 문자셋: utf8mb4 / utf8mb4_unicode_ci
- Healthcheck 포함

------------------------------------------------------------
# ✔ Redis
- 이미지: redis:7-alpine
- 컨테이너 이름: youyoung-redis
- 포트: 6379:6379
- 비밀번호: redis123!
- 주요 옵션:
  --appendonly yes
  --maxmemory 256mb
  --maxmemory-policy allkeys-lru
- 볼륨: redis-data:/data
- Healthcheck 포함

---

### 1-3. 애플리케이션 실행
./gradlew bootRun

---

## 2. API 명세
# Swagger UI 확인:
http://localhost:8080/swagger-ui/index.html#/

# 제공 정보:
- 요청 URL
- HTTP Method
- Request/Response 스키마
- 예시 값
- 테스트 요청 실행

---

## 3. 설계 시 고민했던 부분

### 3-1. 주문 시 상품 스냅샷 저장
# Product 직접 참조 ❌ → 주문 시점 상품정보 스냅샷 저장 방식 사용

# 스냅샷 사용 이유:
- 상품명 변경 시 주문 내역 보존
- 가격 변경 시 정산 금액 불변
- 상품 삭제 후에도 주문 데이터 유지

# 비교표
구분 | Product 직접 참조 | 스냅샷 저장
---- | ---------------- | -------------
상품명 변경 | 주문내역 손상됨 | 주문내역 그대로
가격 변경 | 주문금액 꼬임 | 주문금액 유지
상품 삭제 | 주문조회 불가 | 조회 가능

# 요약
- 과거 주문 시점 상품명/가격 유지
- 정산/회계 데이터 무결성 보존
- 오버셀 및 데이터 왜곡 방지

---

### 3-2. 재고 관리에서 레이스 컨디션 방지
# → 비관적 락 + 데드락 최소화 전략 적용

# 방지하려던 문제
- 동시에 재고 차감 → 음수 재고 발생
- 재고보다 많은 주문 발생(oversell)

# 적용한 전략
1) 비관적 락(PESSIMISTIC_WRITE)
    - DB: SELECT ... FOR UPDATE
    - 한 순간에 한 트랜잭션만 재고 수정 가능

2) 데드락 방지: 락 획득 순서 통일
    - 여러 상품의 재고 수정 시 product_id ASC 정렬로 조회
    - 서로 반대 순서로 락을 잡아 데드락 나는 문제 제거

3) 트랜잭션 최소화
    - "재고 조회 → 차감 → 저장"만 트랜잭션 안에서 실행
    - 외부 API 호출/계산 로직은 트랜잭션 밖으로 분리
      → 락 보유 시간 최소화

---

## ✔ 최종 요약
- 스냅샷 기반 주문 저장 → 과거 데이터 무결성 유지
- 비관적 락으로 재고 정합성 보장
- 락 순서 통일 + 트랜잭션 단축으로 데드락 최소화
- Docker 기반 환경 → 누구나 동일 환경에서 즉시 실행 가능
rm 