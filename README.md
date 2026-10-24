# Fold StandBy

> An iPhone-style StandBy screen for the Galaxy Z Fold8: a calendar with a square analog clock, weather, now playing, and photos. It turns on as the charging screen saver, fits the cover screen and the unfolded inner screen, and adds a night mode, a soft glow before alarms, and wake on motion. Kotlin + Jetpack Compose. Personal project, sideloaded (not on Play Store).

갤럭시 Z Fold8을 가로로 세워 충전하면 아이폰 스탠바이처럼 시계·달력·날씨·음악·사진을 크게 보여주는 앱.

## 스크린샷

데모 데이터로 찍은 화면이다(커버 화면 1972×1248 · 펼친 화면 2448×1848 에뮬레이터). 일정·사진은 예시, 날씨는 실제 서울 날씨.

| 커버 화면 · 달력 + 시계 (기본) | 디지털 |
|---|---|
| ![달력과 정사각 아날로그 시계](docs/screenshots/01-cover-clock.png) | ![둥근 굵은 숫자 디지털 시계, 날짜·날씨·알람](docs/screenshots/02-cover-digital.png) |
| **달력 · 오늘·내일 일정** | **날씨** |
| ![요일·큰 날짜·시각과 이번 주 줄, 오늘·내일 일정](docs/screenshots/03-cover-calendar.png) | ![화면 전체 날씨와 시간별 예보](docs/screenshots/04-cover-weather.png) |
| **펼친 화면 · 사진 카드** | **펼친 화면 · 날씨 카드** |
| ![왼쪽 달력, 오른쪽 사진 카드](docs/screenshots/07-unfolded-photo.png) | ![왼쪽 달력, 오른쪽 날씨 카드](docs/screenshots/08-unfolded-weather.png) |
| **야간 모드** | **알람 전 은은한 빛** |
| ![어두운 빨강 야간 모드](docs/screenshots/09-night.png) | ![호박색으로 밝아지는 알람 전 화면](docs/screenshots/10-alarm-glow.png) |

<p>
  <img src="docs/screenshots/05-cover-portrait.png" width="240" alt="커버 화면 세로: 달력과 시계">
  <img src="docs/screenshots/06-cover-portrait-calendar.png" width="240" alt="커버 화면 세로: 달력 스타일">
</p>

## 화면

좌우로 밀면 보기가 바뀌고, 시계 보기에서 위아래로 밀면 스타일이 바뀐다. 열 때마다(아이콘·루틴·화면 보호기, 다른 앱에서 돌아올 때) 달력+시계로 시작한다. 펼친 화면 오른쪽 스택만 마지막 카드를 기억한다.

| 보기 | 내용 |
|---|---|
| 시계 | ① 달력 + 정사각 둥근 아날로그 시계(기본, 아이폰 듀오 스탠바이 모양) ② 디지털(둥근 굵은 숫자, 흰색→강조색 그라디언트, 오전/오후, 날짜·날씨·다음 알람) ③ 달력(요일·큰 날짜·월, 시각+오전/오후, 이번 주 줄과 오늘·내일 일정, 일정이 없으면 이번 달). 오른쪽 세로 점이 현재 스타일 |
| 날씨 | 화면 전체: 지역, 하늘 그림(밤엔 달), 큰 기온, 상태, 최고·최저, 체감·습도·바람, 시간별 예보 7칸 |
| 재생 중 | 앨범 아트, 곡 정보, 진행 막대, 이전/재생/다음 |

모든 보기 위로 새 알림이 6초간 크게 뜬다(잠겨 있으면 앱 이름만).

- **커버 화면(1972×1248)**: 화면 전체를 쓴다.
- **펼친 화면(2448×1848, 4:3)**: 왼쪽 반은 날짜·달력 고정, 오른쪽 반은 위아래로 넘기는 스택(사진 · 시계 · 날씨 · 재생 중). 사진은 고른 앨범에서 30초마다 바뀌고 왼쪽 아래에 시각. 음악을 틀면 재생 중 카드로 넘어간다. 전면 카메라(화면 아래 카메라) 자리는 피한다. 아래 띠에 다음 일정.
- **반접기**: 윗면에 보기, 아랫면에 다음 일정.
- **야간 모드**: 조도 센서가 어두우면 전부 어두운 빨강, 밝기 최저, 초침 숨김.
- **번인 방지**: 1분마다 화면 전체를 3dp씩 옮긴다.
- **알람 전 은은한 빛**: 폰 시계 앱의 다음 알람 15분 전부터 화면이 호박색으로 천천히 밝아지고(밝기도 함께), 알람이 울리는 동안 최대로 유지된다. 알람을 끄면 원래대로. 울리는 동안 유지하려면 알림 접근이 필요하다(없으면 알람 시각 1분 뒤 꺼짐).
- **움직이면 깨우기**: 밤(야간 모드)에 1분 동안 움직임도 터치도 없으면 화면을 거의 끈다. 폰이 흔들리거나 화면을 만지면 다시 켜진다. 깨우는 터치는 넘김으로 전달하지 않는다.

## 빌드와 설치

필요한 것: Android Studio(이 Mac에 설치됨). Play 스토어 없이 폰에 바로 설치한다.

1. 폴드8: 설정 › 휴대전화 정보 › 소프트웨어 정보 › 빌드번호를 7번 탭해 개발자 옵션을 켠다.
2. 개발자 옵션 › 무선 디버깅을 켠다. Mac과 같은 와이파이여야 한다.
3. Android Studio에서 이 폴더를 열고 Device Manager › Pair Devices Using Wi-Fi로 QR 페어링.
4. Run(▶) 버튼으로 설치.

터미널로 하려면:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew installDebug
```

## 처음 설정

앱 아이콘은 **Fold StandBy** 하나다. 처음 열면 설정 화면이 먼저 뜬다. 설정 화면에서:

1. **화면 보호기 설정 열기** → Fold StandBy 선택 → 시작 조건 “충전 중”. 이제 충전 중 화면이 꺼질 때 자동으로 켜진다.
2. 권한 허용: 캘린더(일정), 대략적 위치(날씨, 거부하면 서울), 알림 접근(음악·알림).
   알림 접근이 회색으로 막혀 있으면 설정 › 애플리케이션 › Fold StandBy › ⋮ › 제한된 설정 허용을 먼저 누른다.
3. 언어(English/한국어), 주 시작(월/일), 강조색(기본 주황), 야간 모드, 펼친 화면 오른쪽(사진/시계, 앨범 고르기), 침대 옆 시계(알람 전 빛, 움직이면 깨우기)를 고른다.

**Fold StandBy** 아이콘은 충전기 없이 바로 스탠바이 화면을 연다. 충전 중일 때만 화면을 켜 둔다. 폰의 화면 회전 잠금과 상관없이 폰 방향대로 돈다.

무선 충전 때는 삼성 모드 및 루틴의 자동 루틴 "Wireless StandBy"(충전 상태 › 무선 충전 → Fold StandBy 열기)가 앱을 바로 연다. 폰이 잠겨 있으면 잠금 화면 뒤에 열린다.

화면 보호기 창은 회전 잠금을 따르므로, 회전이 잠겨 있으면 화면 보호기가 시작되자마자 같은 화면을 앱으로 넘기고(잠금 화면 위에 보임) 화면 보호기는 끝낸다. 회전 잠금이 꺼져 있으면 화면 보호기 그대로 쓴다.

충전 중이던 폰을 충전기에서 떼면 스탠바이 화면은 저절로 닫힌다(충전 없이 직접 연 경우는 그대로). 뒤로 가기나 아래에서 위로 밀기로도 바로 닫힌다.

스탠바이 화면(앱이든 충전 중 화면 보호기든)을 **길게 누르면**(약 0.7초, 움직이지 않고) 설정 화면이 열린다. 화면 보호기에서는 화면 보호기가 끝나고 열리며, 잠겨 있으면 잠금을 푼 뒤 보인다.

## 글꼴

디지털·달력 스타일은 Nunito(SIL Open Font License 1.1, `app/src/main/res/font/nunito.ttf`, 라이선스 `licenses/OFL-Nunito.txt`)를 쓴다. 한글은 시스템 글꼴로 그린다.

## 구조

```
app/src/main/java/com/marksnd/foldstandby/
├─ MainActivity.kt          설정·권한 안내
├─ StandbyActivity.kt       전체화면 스탠바이, 접힘 자세 감지
├─ StandbyDream.kt          충전 중 화면 보호기 (Compose를 DreamService에 연결)
├─ service/NotifListener.kt 알림 접근: 새 알림 전달, 미디어 세션 권한
├─ data/                    설정, 시각, 캘린더, 날씨(Open-Meteo), 배터리·알람·조도·움직임, 미디어, 사진
└─ ui/                      StandbyScreen(페이저), 시계 3종(달력+아날로그·디지털·달력), 날씨, 재생 중,
                            펼친 화면 스택, 사진 카드, 안쪽 카메라 피하기, 알림 배너
```

기획 배경(아이폰 듀오 스탠바이 조사, 범위 결정, 화면 사양)은 [docs/plan-v6.html](docs/plan-v6.html)에 있다. GitHub에서는 HTML 원문으로 보이므로, 내려받아 브라우저로 연다.

## 실기기에서 확인할 것

- 접은 상태로 충전할 때 커버 화면에 화면 보호기가 뜨는지. 안 뜨면 삼성 “모드 및 루틴”으로 “충전 시작 → StandBy 앱 열기”를 만든다.
- Always On Display가 켜져 있을 때 화면 보호기와의 우선순위.
- 화면 보호기 안에서는 반접기 감지를 하지 않는다(Activity에서만 동작).

## 2차에 할 것

사진 보기, 타이머·배달 같은 진행 중 알림 크게 보기, 유선/무선 충전별 마지막 보기 기억, 다른 앱 위젯 넣기, 공휴일 표시.

## 라이선스

- 코드: [MIT](LICENSE)
- 글꼴 Nunito: SIL Open Font License 1.1 ([licenses/OFL-Nunito.txt](licenses/OFL-Nunito.txt))
