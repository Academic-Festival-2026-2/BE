# 협업 규칙

## 브랜치

- `{타입}/{이슈번호}-{내용}` (예: `feat/9-login`, `fix/62-block-inactive-user`)
- `dev`에서 브랜치를 만들고, PR은 `dev`로 올립니다. `main`은 `dev` → `main` PR만 가능합니다.
- `dev`로 머지되면 브랜치는 자동 삭제됩니다. 추가 수정은 새 브랜치로 합니다.

## 커밋

- `[타입] 내용 #이슈번호` (예: `[feat] 로그인 구현 #9`)
- 타입은 소문자, 대괄호로 감쌉니다. 리뷰 반영 커밋에도 이슈번호를 붙입니다.
- 리뷰 반영 커밋도 변경 내용에 맞는 타입을 씁니다. (버그 지적 → `fix`, 포맷 지적 → `style`, 구조 변경 → `refactor`)
- 기능과 함께 작성한 테스트는 해당 기능 커밋에 포함하고, 테스트만 따로 추가·수정할 때는 `test`를 씁니다.

| 타입 | 언제 쓰나요 | 예시 |
|---|---|---|
| `feat` | 새 기능 (API, 화면) | `[feat] 동 상세 조회 API 추가 #55` |
| `fix` | 버그 수정 | `[fix] 요청 본문 파싱 오류 400 처리 #56` |
| `refactor` | 동작 변경 없이 구조·이름·규칙 변경 | `[refactor] User @Setter 제거 및 도메인 메서드 추가 #10` |
| `style` | 포맷만 변경 (Google Style, 공백·줄바꿈) | `[style] 동 상세 코드 정리 #55` |
| `test` | 테스트만 추가·수정 | `[test] 마이페이지 흐름 테스트 추가 #10` |
| `docs` | README 등 문서 수정 | `[docs] README의 binteum 이름 pixel로 변경 #61` |
| `chore` | 설정·잡일 (의존성, .env.example, .gitignore, SQL·데이터, 파일 정리) | `[chore] 부천 동 경계 SQL 추가 #41` |
| `deploy` | 배포용 `dev` → `main` PR | `[deploy] 배포용 dev, main 머지 (#배포이슈번호)` |

### 커밋 템플릿

레포 최상위의 `.gitmessage`를 커밋 템플릿으로 쓰면 커밋 창에 형식 안내가 표시됩니다.
레포 안 어느 폴더에서든 한 번만 실행합니다. (PowerShell, cmd, Git Bash, macOS 터미널 공통)

```bash
git config commit.template .gitmessage
git config commit.cleanup strip
```

- 두 번째 줄은 IntelliJ 커밋 창처럼 메시지를 파일로 넘기는 방식에서도 `#` 안내 줄이 커밋 메시지에 남지 않게 합니다.
- 이 설정을 쓰면 메시지에서 `#`으로 시작하는 줄은 모두 지워지므로, 이슈번호는 제목 끝에 붙입니다.

## 이슈

- 제목: `[타입] 요약`
- 본문: `## 작업 내용` / `## 배경` / `## 완료 기준`
- Assignee는 본인으로 지정합니다.

## PR

- 제목: `[타입] 기능 요약 (#이슈번호)`
- 배포 PR(`dev` → `main`)은 `[deploy] 배포용 dev, main 머지 (#배포이슈번호)`로 올립니다.
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
  - [ ] CI 정상 작동 확인
  ```

- `dev` 머지는 2명 이상 승인이 필요합니다.
- CodeRabbit 리뷰는 PR 댓글 `@coderabbitai review`로 요청합니다.

## CI

- `dev`·`main`으로 올린 PR과 `dev` push마다 GitHub Actions가 `./gradlew test`를 실행합니다.
- `BE test` 체크가 통과해야 `dev`에 머지할 수 있습니다.
- 실패하면 PR의 Checks 탭에서 로그를 확인합니다. 실패한 테스트 목록은 실행 페이지의 Artifacts → `be-test-report`(7일 보관)에 있습니다.
- PR 전에 `pixel` 폴더에서 `./gradlew test`(PowerShell은 `.\gradlew test`)로 먼저 확인합니다.

## 코드 스타일

- Google Java Style (들여쓰기 2칸, 한 줄 100자)
