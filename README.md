# tada

> 일기를 쓰면 AI가 제목·키워드·인물/장소/활동을 추출하고, 키워드로 스티커 이미지를 생성해 주는 일기 서비스의 백엔드 서버입니다.

## 주요 기능

- **일기**: 작성 / 조회 / 수정, 휴지통 이동·복구·영구 삭제
- **AI 분석**: 본문에서 제목, 키워드, 인물·장소·활동 추출 (n8n + Gemini)
- **AI 스티커**: 선택한 키워드로 스티커 이미지 생성 후 Supabase Storage에 저장
- **소셜 로그인**: Google / Kakao / Naver OAuth2 + JWT (Access / Refresh)
- **캘린더**: 날짜별 일기 조회
- **검색**: 임베딩(Voyage) 기반 의미 검색 (pgvector 코사인 거리)
- **인물 큐레이션 / 기억 회상**: 일기에서 추출한 인물 매칭 및 회상

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.5.16 |
| Data | Spring Data JPA, PostgreSQL (Supabase), pgvector |
| Auth | Spring Security, OAuth2 Client, JWT (jjwt) |
| Storage | Supabase Storage |
| AI / Automation | n8n Webhook, Gemini, Voyage Embedding |
| API Docs | springdoc-openapi (Swagger UI) |
| Build | Gradle |
| Deploy | Railway (Backend), Vercel (Frontend) |

## 시작하기

### 요구사항

- JDK 17
- PostgreSQL (pgvector 확장 활성화)
- 실행 중인 n8n 워크플로우 (스티커 생성 / 일기 분석 웹훅)

### 환경변수

프로젝트 루트에 `.env.local`을 만들고 아래 값을 채웁니다. (`.env.local`은 커밋하지 않습니다.)

| 이름 | 설명 |
|---|---|
| `DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | Kakao OAuth2 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | Naver OAuth2 |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase Storage 서비스 키 |
| `SUPABASE_STORAGE_URL` | (선택) Storage URL |
| `SUPABASE_STICKER_BUCKET` | (선택) 스티커 버킷 이름, 기본값 `stickers` |
| `VOYAGE_API_KEY` | 임베딩 API 키 |
| `STICKER_WEBHOOK_URL` | n8n 스티커 생성 웹훅 |
| `DIARY_ANALYSIS_WEBHOOK_URL` | n8n 일기 분석 웹훅 |
| `PORT` | (선택) 서버 포트, 기본값 `8080` |

### 실행

```bash
# 실행
./gradlew bootRun

# 테스트
./gradlew test

# 실제 n8n / Supabase를 호출하는 수동 통합 테스트
./gradlew manualTest

# 빌드
./gradlew build
```

서버 실행 후 Swagger UI에서 API를 확인할 수 있습니다: `http://localhost:8080/swagger-ui/index.html`

## 프로젝트 구조

도메인형 패키지 구조를 사용합니다.

```
src/main/java/com/tada/tada/
├── global/     # 설정, 보안(JWT), 공통 응답/예외, 이벤트
├── auth/       # 소셜 로그인, 토큰
├── diary/      # 일기
├── calendar/   # 캘린더
├── search/     # 검색
└── curator/    # 인물 큐레이션
```

자세한 구조와 네이밍 컨벤션은 [tada_directory_structure.md](./tada_directory_structure.md)를 참고하세요.

## 문서

- [일기 작성 · AI 생성 동작 흐름](./DIARY_AI_FLOW.md)
- [작업 순서 및 동작 흐름](./implementation_order.md)
- [디렉토리 구조 / 컨벤션](./tada_directory_structure.md)

## 배포

- Railway에 배포하며 설정은 [railway.json](./railway.json)에 있습니다.
- 환경변수는 Railway 대시보드에 위 표와 동일하게 등록합니다.

## 브랜치 전략

- `main`: 배포 브랜치
- `develop`: 통합 개발 브랜치 (PR base)
- `feature/*`, `fix/*`: 작업 브랜치 → `develop`으로 PR

## 팀

| 도메인 | 담당 |
|---|---|
| global (공통) | 상훈 |
| auth (소셜 로그인) | 진경 |
| diary / calendar | 민혁 |
| search | 형호 |
| curator | 한영 |
