# fold-standby 작업 규칙

공통 규칙은 `~/Projects/AGENTS.md`에 있습니다. 먼저 그 문서를 읽고, 이어서 이 폴더의 STATUS.md와 DECISIONS.md를 읽으세요.

## 이 프로젝트만의 규칙

- 빌드: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug`
- adb 서버는 `ADB_LIBUSB=1 adb start-server`로 켠다. 그냥 켜면 멈춘다.
- 커버 화면 크기 에뮬레이터: `fold8_cover` (1248×1972, 가로로 돌려서 확인).
- 계획 페이지는 `docs/plan-vN.html`로 버전을 나누고, 게시본은 https://claude.ai/artifact/McyAbkWkxRCw6WyKXyk2DK 하나에 계속 갱신한다.
