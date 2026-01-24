# 🎭 Grapefield 2.0

> **GrapeField 1.0의 핵심 기능을 개선하고 경량화한 버전**
> 웹 크롤링 → KOPIS 공식 API로 전환하여 안정성과 효율성 향상

## 📌 프로젝트 소개

국내 모든 공연 정보를 한 곳에서 검색하고, 장르별 실시간 채팅방에서 공연 정보를 공유할 수 있는 통합 플랫폼입니다.

### 🆕 v1.0 → v2.0 주요 개선사항

| 항목 | GrapeField 1.0 | GrapeField 2.0 |
|------|---------------|---------------|
| **데이터 수집** | 웹 크롤링 (불안정) | **KOPIS 공식 API** (안정적) |
| **아키텍처** | Kubernetes, Kafka, ELK | **경량화된 Spring Boot** |
| **인프라** | 복잡한 MSA 구조 | Docker Compose 기반 단순화 |
| **검색 엔진** | Elasticsearch | OpenSearch |
| **핵심 기능** | 유지 | 유지 (채팅, 검색, 인증) |

### ✨ 왜 2.0을 만들었나요?

1. **안정성**: 웹 크롤링 → 공식 API로 변경하여 HTML 구조 변경에 영향받지 않음
2. **효율성**: API 호출로 필요한 데이터만 정확하게 수집
3. **유지보수성**: 복잡한 인프라 제거, 핵심 기능에 집중
4. **빠른 배포**: Docker Compose로 간편한 로컬/서버 배포

## 🎯 주요 기능

### 1. 공연 정보 자동 수집
- **KOPIS API** (공연예술통합전산망) 연동
- 뮤지컬, 연극, 콘서트, 클래식 등 전체 장르 지원
- 매일 자동 업데이트 스케줄러

### 2. 실시간 검색
- **OpenSearch** 기반 빠른 검색
- 공연 제목, 공연장, 장르 통합 검색
- 페이징 처리

### 3. 박스오피스
- 장르별 실시간 순위
- KOPIS 공식 순위 데이터

### 4. 실시간 채팅
- WebSocket (STOMP) 기반
- 장르별 채팅방
- Redis 세션 관리
- JWT 인증 통합

### 5. 소셜 로그인
- 카카오 OAuth 2.0
- 이메일 인증 시스템

## 🛠️ 기술 스택

### Backend
- **Java 17**
- **Spring Boot 3.5.7**
- Spring Security + JWT
- Spring Data JPA
- WebSocket (STOMP)

### Database & Infrastructure
- **MariaDB** (메인 DB)
- **Redis** (채팅 세션 관리)
- **OpenSearch** (검색 엔진)
- **Docker Compose**

### External APIs
- **KOPIS API** (공연예술통합전산망) _ 핵심 개선점
- Kakao OAuth 2.0

## 🚀 시작하기

### 필수 요구사항

- Java 17+
- Docker & Docker Compose
- MariaDB

### 1. 환경 변수 설정

`.env.example` 파일을 `.env`로 복사하고 실제 값 입력:

```bash
cp .env.example .env
```

```env
# .env
DB_URL=jdbc:mariadb://localhost:3306/grapefield
DB_USER=your_username
DB_PASSWORD=your_password

# KOPIS API 키 (https://www.kopis.or.kr/por/cs/openapi/openApiList.do)
KOPIS_API_KEY=your_kopis_api_key

KAKAO_REST_API_KEY=your_kakao_key
JWT_SECRET=your_jwt_secret_min_32_chars
...
```

### 2. application.yml 설정

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

### 3. Docker 서비스 실행

```bash
cd src/main/resources
docker-compose up -d
```

실행되는 서비스:
- Redis (6379)
- OpenSearch (9200)
- OpenSearch Dashboards (5601)

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/com/example/grapefield2/
│   │   ├── config/          # Security, WebSocket, Redis 등
│   │   ├── controller/      # REST API, WebSocket 컨트롤러
│   │   ├── dto/             # 데이터 전송 객체
│   │   ├── entity/          # JPA 엔티티
│   │   ├── repository/      # DB 레포지토리
│   │   ├── service/         # 비즈니스 로직
│   │   │   ├── KopisApiService.java    # KOPIS API 연동
│   │   │   └── SimpleOpenSearchService.java
│   │   ├── scheduler/       # 공연 정보 자동 수집
│   │   └── document/        # OpenSearch 문서
│   └── resources/
│       ├── application.yml.example
│       └── docker-compose.yml
```

## 🔄 자동 데이터 수집

### 스케줄러 동작 시간

| 시간 | 작업 내용 | 설명 |
|------|-----------|------|
| 00:00 | 공연 상태 변경 | 종료일 지난 공연 → "공연완료" 처리 |
| 01:00 | 전체 수집 | KOPIS API에서 모든 장르 공연 정보 수집 |
| 02:00 | 재수집 | 상세정보 누락된 공연 업데이트 |
| 04:00 | 박스오피스 | 장르별 실시간 순위 갱신 |

> 💡 수집 시간은 KOPIS API 서버 부하를 고려하여 새벽 시간대로 설정

## 🌐 주요 API 엔드포인트

### 인증
```
POST /auth/signup              # 회원가입
POST /auth/login               # 로그인
POST /auth/kakao               # 카카오 로그인
POST /auth/send-verification   # 이메일 인증 전송
POST /auth/verify-code         # 이메일 인증 확인
```

### 공연 정보
```
GET  /performance/list         # 공연 목록 (장르별, 지역별)
GET  /performance/{id}         # 공연 상세 조회
GET  /performance/month        # 월별 공연 조회
GET  /boxoffice/list           # 박스오피스 순위
```

### 검색
```
GET  /search/all?keyword={keyword}    # 통합 검색
POST /search/sync-performances        # 검색 인덱스 동기화
```

### 채팅
```
GET  /api/chat/rooms                      # 채팅방 목록
GET  /api/chat/rooms/{category}/history  # 채팅 히스토리
WS   /ws                                  # WebSocket 연결
```

## 🔐 보안

- JWT 기반 인증
- Spring Security 적용
- 환경 변수를 통한 민감 정보 관리
- WebSocket JWT 인증
- `.gitignore`에 설정 파일 제외

## 📊 데이터 흐름

```
KOPIS API → KopisApiService → MariaDB
                ↓
    PerformanceScheduler (자동 수집)
                ↓
         OpenSearch (검색 인덱싱)
                ↓
         REST API → Frontend
```

## 🔗 관련 프로젝트

- **[GrapeField 1.0 (Original)](https://github.com/beyond-sw-camp/be12-fin-Catcher-GrapeField-BE)**: 웹 크롤링 기반, Kubernetes + Kafka + ELK 스택


## ⚠️ KOPIS API 사용 시 주의사항

### 인증키 발급 필수
- **KOPIS 오픈API** 이용을 위해서는 인증키 발급이 필수입니다
- 발급 방법: [KOPIS 오픈API 신청](https://www.kopis.or.kr/por/cs/openapi/openApiList.do) (PC에서만 가능)
- **1인 1개 발급**, 타인 양도 및 공유 불가

### 출처 표기 의무 ⚠️
본 서비스는 KOPIS API를 사용하며, **법적으로 출처 표기가 필수**입니다.

**프론트엔드에 반드시 표기:**
```
출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)
```

> ⚠️ **중요**: 출처 미표기 시 서비스가 중단될 수 있습니다

본 프로젝트는 Footer에 출처를 표기하여 요구사항을 준수하고 있습니다.

### API 호출 제한
- 인증키별 **일일 쿼리 제한** 존재
- 제한 초과 시 서비스 자동 중지
- 자세한 사항: [KOPIS 개발가이드](https://www.kopis.or.kr/por/cs/openapi/openApiInfo.do) 참고

> 💡 본 프로젝트는 스케줄러를 통한 **배치 수집 방식**으로 API 호출을 최소화했습니다.

## 📝 라이선스

This project is licensed under the MIT License.
