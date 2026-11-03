# 협업 규칙

## 브랜치

- `{타입}/{이슈번호}-{내용}` (예: `feat/9-login`, `fix/62-block-inactive-user`)
- `dev`에서 브랜치를 만들고, PR은 `dev`로 올립니다. `main`은 `dev` → `main` PR만 가능합니다.
- `dev`로 머지되면 브랜치는 자동 삭제됩니다. 추가 수정은 새 브랜치로 합니다.

## 커밋

- `[타입] 내용 #이슈번호` (예: `[feat] 로그인 구현 #9`)
- 타입은 소문자, 대괄호로 감쌉니다. 리뷰 반영 커밋에도 이슈번호를 붙입니다.
- 테스트는 해당 기능 커밋에 함께 포함합니다.

| 타입 | 언제 쓰나요 | 예시 |
|---|---|---|
| `feat` | 새 기능 (API, 화면) | `[feat] 동 상세 조회 API 추가 #55` |
| `fix` | 버그 수정, 리뷰 반영 | `[fix] 요청 본문 파싱 오류 400 처리 #56` |
| `refactor` | 동작 변경 없이 구조·이름·규칙 변경 | `[refactor] User @Setter 제거 및 도메인 메서드 추가 #10` |
| `style` | 포맷만 변경 (Google Style, 공백·줄바꿈) | `[style] 동 상세 코드 정리 #55` |
| `docs` | README 등 문서 수정 | `[docs] README의 binteum 이름 pixel로 변경 #61` |
| `chore` | 설정·잡일 (의존성, .env.example, .gitignore, SQL·데이터, 파일 정리) | `[chore] 부천 동 경계 SQL 추가 #41` |
| `deploy` | 배포용 `dev` → `main` PR | `[deploy] 배포용 dev, main 머지` |

### 커밋 템플릿

레포 최상위의 `.gitmessage`를 커밋 템플릿으로 쓰면 커밋 창에 형식 안내가 표시됩니다.
레포 최상위 폴더에서 한 번만 실행합니다.

```powershell
git config commit.template "$(Get-Location)\.gitmessage"
```

## 이슈

- 제목: `[타입] 요약`
- 본문: `## 작업 내용` / `## 배경` / `## 완료 기준`
- Assignee는 본인으로 지정합니다.

## PR

- 제목: `[타입] 기능 요약 (#이슈번호)`
- 본문

  ```markdown
  ## 이슈 및 작업중인 브랜치
  - close #이슈번호
  - {브랜치} → dev

  ## 주요내용
  - 팀원들이 참고할 내용 및 주요 변경점

  ## PR CheckList
  - [ ] Reviewers 등록
  - [ ] Assignees 등록
  - [ ] Label 등록
  - [ ] 로컬 실행 / 테스트 확인
  ```

- `dev` 머지는 2명 이상 승인이 필요합니다.
- CodeRabbit 리뷰는 PR 댓글 `@coderabbitai review`로 요청합니다.

## 코드 스타일

- Google Java Style (들여쓰기 2칸, 한 줄 100자)
