# Commit & PR Convention

## 커밋 규칙

### 형식
```
type(scope): 설명

- 상세 설명 (선택사항)
```

### Type 종류

| type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | `feat(dashboard): 2D 경로 맵 추가` |
| `fix` | 버그 수정 | `fix(ros): rosbridge 재연결 오류 수정` |
| `refactor` | 코드 리팩토링 | `refactor(service): 상태 캐시 로직 분리` |
| `chore` | 설정, 환경 변경 | `chore(docker): Dockerfile 멀티스테이지 설정` |
| `docs` | 문서 수정 | `docs: README 업데이트` |
| `style` | 코드 스타일 변경 | `style: 들여쓰기 정리` |
| `test` | 테스트 추가/수정 | `test(api): 통계 API 단위 테스트 추가` |
| `perf` | 성능 개선 | `perf(ws): 상태 푸시 주기 최적화` |
| `security` | 보안 수정 | `security(config): CORS 설정 강화` |

### Scope 종류
`ros` `websocket` `dashboard` `api` `domain` `service` `config` `docker` `common`

### 주의사항
- `main` 브랜치에서는 커밋하지 않는다
- 반드시 feature 브랜치에서 작업 후 PR로 머지
- 커밋 메시지에 `Co-Authored-By` 라인을 추가하지 않는다
- 한국어로 작성
- `application.yml`의 민감 정보(비밀번호, 시크릿키)는 절대 커밋하지 않는다

---

## PR 규칙

### 제목 형식
```
[type] 한글 설명 (50자 이내)
```

### 본문 형식
```
## Summary
- 변경 이유와 내용을 한국어로 작성
- 핵심 변경사항 위주로 bullet points

## 변경된 모듈
- ros / websocket / dashboard / api / ... (해당하는 것만)

## Test
변경사항을 확인한 방법 간단히 서술 (선택사항)
```

### 주의사항
- PR 제목과 본문은 한국어로 작성
- 하나의 PR은 하나의 기능/수정에 집중
- base 브랜치 확인 후 PR 생성
