# tada Git 컨벤션

브랜치 전략은 `tada_공통_전달사항.md` 1번 항목 참고 (Fork → 본인 fork의 develop → feature/기능명 → 본인 fork develop 병합 → upstream develop으로 PR).

---

## Git Flow

```
1. 작업 시작 전 upstream/develop으로 동기화
   git checkout develop
   git fetch upstream
   git merge upstream/develop

2. develop에서 새 feature 브랜치 생성
   git checkout -b feature/기능명

3. 작업 단위를 잘게 쪼개서 커밋 (한 커밋 = 하나의 의미 있는 변경)

4. feature 브랜치를 본인 fork의 develop으로 병합
   git checkout develop
   git merge feature/기능명
   git push origin develop

5. GitHub에서 PR 생성 (본인 fork의 develop → upstream의 develop)

6. 상훈 리뷰 → 수정 요청 있으면 feature 브랜치에서 마저 고친 뒤 다시 develop에 병합 → push (자동으로 PR에 반영됨)

7. 병합 후에는 다시 1번부터 반복 (새 기능은 새 feature 브랜치로)
```

**커밋은 기능 단위로 자주, 작게 나눠서** — "하루종일 작업한 걸 한 커밋에 몰아넣기" 금지. 리뷰하기 어렵고, 문제 생겼을 때 원인 추적이 안 됨.

---

## 커밋 메시지 양식

```
{타입}: {요약} (50자 이내, 마침표 없음)

{선택사항 — 본문에 왜 이렇게 했는지 설명, 필요할 때만}
```

### 타입 목록

| 타입 | 용도 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없이 코드 구조만 개선 |
| `docs` | 문서 수정 (README, 주석 등) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드 설정, 의존성 추가 등 잡일 |
| `style` | 코드 포맷팅, 세미콜론 등 (로직 변경 없음) |

### 예시

```
feat: 일기 작성 API 구현
fix: 회원가입 시 중복 아이디 검증 안 되는 버그 수정
refactor: DiaryService의 조회 로직을 Repository 쿼리로 이동
docs: auth 도메인 API 명세 주석 추가
chore: jjwt 의존성 추가
```

### 지키면 좋은 것

- 제목은 명령문으로 ("~함", "~했음"이 아니라 "~추가", "~수정")
- 한 커밋에 여러 타입 섞지 않기 (기능 추가랑 버그 수정은 커밋 분리)
- 커밋 메시지만 보고도 "무슨 변경인지" 파악 가능하게

---

## PR 양식

PR 생성 시 아래 템플릿을 설명란에 채워서 작성. (반복 작업이니 GitHub PR 템플릿 기능으로 자동 채워지게 설정 예정 — 우선 수동으로 작성)

```markdown
## 작업 내용
- 무엇을 구현/수정했는지 간단히

## 관련 이슈/문서
- (있다면) Notion 기능명세서 링크 또는 관련 항목

## 변경 사항
- [ ] Entity 추가/수정
- [ ] API 추가/수정 (엔드포인트 명시)
- [ ] 이벤트 발행/구독 추가
- [ ] 프론트 화면 추가/수정

## 테스트 방법
- Swagger에서 어떤 API를 어떻게 호출해서 확인했는지
- (스크린샷 있으면 첨부)

## 확인 요청 사항
- 리뷰어가 특히 봐줬으면 하는 부분 (있으면)
```

### PR 크기 원칙

- **하나의 PR = 하나의 기능 단위** (예: "회원가입 API" PR과 "로그인 API" PR은 분리)
- 너무 커지면(파일 20개 이상, 여러 기능 섞임) 리뷰가 느려지고 부정확해짐 — 잘게 쪼개서 자주 PR 올릴 것
- 제목은 커밋 메시지와 동일한 규칙 (`feat: 회원가입 API 구현` 등)

### 리뷰 관련

- 상훈이 PR 리뷰 후 코멘트 남기면, 같은 브랜치에 수정 커밋 추가 → 자동으로 PR에 반영됨 (새 PR 만들 필요 없음)
- JWT 관련 PR(진경)은 특히 꼼꼼히 리뷰 예정 (SECRET_KEY 환경변수 분리, 만료시간, 서명 검증 로직 위주)
- 리뷰 승인 후 병합은 상훈이 진행
