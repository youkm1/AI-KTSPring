

## 1. 과제 분석 과정

### 초기 요구사항
- **회사**: sionic-ai VIP 온보딩 팀 (1명의 영업 직원 + 1명의 개발자)
- **목적**: 고객사의 간단한 시연 요청에 대한 응답
- **핵심 기능**: AI 챗봇을 통한 고객 상담 서비스
- **제약 사항**: 3시간 내 기본 시스템 완성, GitHub public repository 허용

### 기술 스택 결정 과정
1. **언어 선택**: Kotlin (Spring과의 호환성, 간결한 문법)
2. **프레임워크**: Spring Boot 3.x+ (빠른 개발, 검증된 생태계)
3. **데이터베이스**: PostgreSQL (메인) + Redis (캐싱 및 실시간 처리)
4. **AI 서비스**: Google Gemini 2.0 Flash (최신 성능, 한국어 지원)

### 아키텍처 설계 변경 과정
- **초기**: 단순한 User → ChatMessage 구조
- **중간**: Chat 엔티티 추가 고려 (3-Layer 구조)
- **최종**: User → ChatThread → ChatMessage (실용적 접근)
- **특별 고려사항**: Redis TTL 기반 실시간 채팅 + PostgreSQL 영구 저장

## 2. AI 개발 툴 사용 과정 중 어려움

### 요구사항 전달의 어려움
- **문제**: "대화" 개념을 AI가 정확히 이해하지 못함
- **해결**: 구체적인 사용 시나리오와 API 예시 제공
- **학습**: 추상적 개념보다는 구체적 기능 명세가 효과적

### 전체 맥락 이해 부족
- **문제**: AI가 프로젝트 전체 구조를 파악하지 못해 일관성 없는 코드 생성
- **해결**: 단계별로 세분화하여 요청, 기존 코드와의 연관성 명시
- **예시**: "MessageRole은 왜 필요한가?" → 구체적인 사용 사례 설명 필요

### 기능 요구사항 해석의 차이
- **문제**: "30분 TTL 후 PostgreSQL 이관" 요구사항을 다양하게 해석
- **해결**: Redis Keyspace Notifications와 구체적 구현 방식 명시
- **교훈**: 기술적 세부사항까지 포함한 명확한 스펙 필요

### 코드 일관성 문제
- **문제**: Controller 중복 생성 (ChatController vs RedisChatController)
- **해결**: 명확한 역할 분담과 통합 방향성 제시
- **개선점**: AI에게 기존 코드 검토 후 수정하도록 요청

## 3. 기술적 어려움

### Entity 설계와 JPA 제약사항
- **문제**: Kotlin의 non-null 특성과 JPA의 no-arg constructor 요구사항 충돌
- **해결**: Entity 필드를 nullable로 선언하되, 비즈니스 로직에서 null 체크
- **기술적 배경**: JPA는 리플렉션을 통해 기본 생성자로 객체 생성 후 필드 주입

### Redis TTL과 PostgreSQL 동기화
- **문제**: 30분 무활동 시 자동 데이터 이관 구현
- **해결책**: 
  - Redis Keyspace Notifications (notify-keyspace-events: Ex)
  - TTL 만료 이벤트 리스너 구현
  - RedisExpirationListener를 통한 자동 이관

### Gemini API 통합 복잡성
- **문제**: OpenAI와 다른 API 형식, 최신 헤더 방식 적용 필요
- **해결**:
  ```kotlin
  // 구식 방식
  .uri("/models/$model:generateContent?key=$apiKey")
  
  // 최신 방식  
  .uri("/models/$model:generateContent")
  .header("X-goog-api-key", apiKey)
  ```

### WebClient 메모리 설정 이슈
- **문제**: `.codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }`
- **원인**: Spring WebFlux의 기본 메모리 제한 (256KB)이 AI 응답에 부족
- **해결**: 10MB로 확장하여 대용량 AI 응답 처리 가능

### MessageRole의 중요성 이해
- **문제**: 단순히 사용자/AI 구분으로만 생각
- **실제 역할**: 
  - OpenAI API 호출 시 대화 맥락 유지
  - 향후 SYSTEM 메시지로 대외비 문서 학습 기능 확장 가능
  - 피드백 시스템에서 AI 응답만 대상으로 구분

## 4. DB 구조

### 최종 엔티티 관계
```
User (users)
└── ChatThread (chat_threads)
    ├── ChatMessage (chat_messages)
    └── UserFeedback (user_feedback)

UserActivity (user_activities)
└── User 참조
```

### 핵심 설계 원칙
1. **User → ChatThread → ChatMessage**: 3단계 계층 구조
2. **Redis 임시 저장 → PostgreSQL 영구 보관**: 하이브리드 방식
3. **JPA nullable 필드**: Entity 제약사항 준수
4. **TimeStampEntity 상속**: 자동 생성일시 관리

### 주요 테이블 상세

#### users 테이블
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### chat_threads 테이블
```sql
CREATE TABLE chat_threads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

#### chat_messages 테이블
```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    thread_id BIGINT NOT NULL REFERENCES chat_threads(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 5. 개발된 기능

### 인증 및 사용자 관리
- **회원가입/로그인**: JWT 기반 인증
- **역할 관리**: ADMIN/MEMBER 권한 구분
- **활동 추적**: 회원가입, 로그인, 대화 생성 기록

### 실시간 채팅 시스템
- **Redis 기반 실시간 처리**: 30분 TTL, 자동 갱신
- **AI 통합**: Google Gemini 2.0 Flash API
- **대화 맥락 유지**: MessageRole 기반 이력 관리
- **자동 이관**: TTL 만료 시 PostgreSQL 저장

### API 엔드포인트

#### 인증 관련
```
POST /api/auth/register  - 회원가입
POST /api/auth/login     - 로그인
```

#### 채팅 관련 (통합 컨트롤러)
```
POST /api/chat/message              - 실시간 메시지 전송
GET  /api/chat/conversation         - 현재 활성 대화 조회
POST /api/chat/complete            - 대화 완료 (Redis→DB 이관)
GET  /api/chat/history             - 과거 대화 목록 (페이징)
GET  /api/chat/history/{threadId}  - 특정 대화 상세 조회
DELETE /api/chat/history/{threadId} - 대화 삭제
GET  /api/chat/status              - 현재 채팅 상태
```

#### 피드백 관리
```
POST /api/feedback                     - 피드백 생성
GET  /api/admin/feedback              - 피드백 조회 (관리자, 페이징/필터링)
PUT  /api/admin/feedback/{id}/status  - 피드백 상태 변경
```

#### 관리자 기능
```
GET /api/admin/activity-stats        - 일일 활동 통계
GET /api/admin/reports/daily-chats   - CSV 보고서 다운로드
```

### 주요 기술적 특징
- **하이브리드 아키텍처**: Spring MVC + WebFlux (외부 API 호출)
- **Redis TTL 관리**: 자동 만료 이벤트 처리
- **JWT Scope**: 토큰에 사용자 역할 정보 포함
- **페이징 및 필터링**: 피드백 조회 시 정렬/필터 지원
- **에러 처리**: 계층별 예외 처리 및 의미있는 응답

### 미구현 기능 (향후 확장)
- SYSTEM 메시지 기반 대외비 문서 학습
- 실시간 스트리밍 응답
- 고급 보안 정책
- 다중 AI 모델 지원
