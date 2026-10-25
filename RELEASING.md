# 릴리스 순서

새 버전을 낼 때마다 이 순서를 그대로 따라요. 명령은 저장소 폴더에서 실행해요.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## 버전 번호

| 바뀐 것 | 예 |
|---|---|
| 버그만 고침 | 1.1.0 → 1.1.1 |
| 기능이나 지원 기기가 늘어남 | 1.1.1 → 1.2.0 |
| 실기기로 확인하지 않은 변경 | 1.2.0-beta.1, 1.2.0-beta.2 … |
| 베타를 확인하고 정식으로 | 1.2.0-beta.2 → 1.2.0 |

`versionCode`는 배포하는 빌드마다(베타 포함) 1씩 올려요. 내려가면 덮어 설치가 안 돼요.

## 1. 테스트

```bash
./gradlew testDebugUnitTest      # 단위 테스트와 화면 테스트
./gradlew verifyRoborazziDebug   # 화면을 app/screenshots/의 기준 그림과 비교
```

화면을 일부러 바꿨다면 `./gradlew recordRoborazziDebug`로 기준 그림을 다시 만들고, 바뀐 그림을 눈으로 확인한 뒤 커밋해요.

## 2. 버전과 변경 기록

1. `app/build.gradle.kts`에서 `versionCode`를 1 올리고 `versionName`을 바꿔요.
2. `CHANGELOG.md` 맨 위에 새 버전과 바뀐 점을 적어요.

## 3. 정식 빌드와 서명 확인

```bash
./gradlew assembleRelease --no-configuration-cache
"$(ls -d ~/Library/Android/sdk/build-tools/* | sort -V | tail -1)/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
```

- 서명 비밀번호는 키체인에서 읽어요. 설정 캐시에 남지 않도록 `--no-configuration-cache`를 꼭 붙여요.
- 인증서 SHA-256이 `03:72:0C:74:B0:EA:33:BC:…:3F:FE`인지 확인해요. 다르면 배포하지 않아요.

## 4. 기기 확인

- 에뮬레이터에 정식 APK를 설치해 열어 보고, 시계, 일정, 날씨, 넘김, 길게 누르기를 확인해요. R8 축소가 켜져 있어 개발 빌드와 다를 수 있어요.
- 폰에 덮어 설치해요(`adb install -r`). 지우고 설치하면 설정이 사라지니, 지워야 할 때는 먼저 물어봐요.

## 5. 커밋과 푸시

```bash
git add -A && git commit -m "vX.Y.Z: …"
git push
```

GitHub Actions의 테스트가 통과하는지 확인해요.

## 6. GitHub 릴리스

```bash
cp app/build/outputs/apk/release/app-release.apk /tmp/FoldStandBy-X.Y.Z.apk
gh release create vX.Y.Z /tmp/FoldStandBy-X.Y.Z.apk --repo svmark/fold-standby \
  --title "Fold StandBy X.Y.Z" --notes-file notes.md --target main
```

- 베타는 `--prerelease`를 붙여요. 그러면 README의 "최신 버전 받기"는 계속 정식 버전을 가리켜요.
- 릴리스 노트는 `CHANGELOG.md`에서 옮기고, 서명 인증서 SHA-256과 APK SHA-256을 함께 적어요.

## 7. 올린 파일 확인

```bash
curl -sL -o /tmp/check.apk https://github.com/svmark/fold-standby/releases/download/vX.Y.Z/FoldStandBy-X.Y.Z.apk
shasum -a 256 /tmp/check.apk
```

해시가 빌드한 파일과 같은지 확인해요. 정식 릴리스라면 `releases/latest`가 새 버전을 가리키는지도 봐요.
