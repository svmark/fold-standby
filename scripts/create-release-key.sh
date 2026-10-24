#!/bin/bash
# Fold StandBy 정식(출시용) 서명키 1회 생성. Mac 터미널에서 직접 실행한다.
#   bash ~/Projects/fold-standby/scripts/create-release-key.sh          (비밀번호 직접 입력)
#   bash ~/Projects/fold-standby/scripts/create-release-key.sh --auto   (임의 비밀번호 생성, 키체인에만 저장)
# - 비밀번호는 화면에 표시되지 않고, 이 스크립트 밖으로 출력되지 않는다.
# - 키: ~/.android-keys/foldstandby-release.jks (권한 600)
# - 비밀번호: macOS 키체인 항목 "fold-standby-release-keystore"
# - 이 키를 잃어버리면 이미 설치된 앱을 같은 앱으로 업데이트할 수 없다. 반드시 백업한다.
set -euo pipefail

KEY_DIR="$HOME/.android-keys"
KEY="$KEY_DIR/foldstandby-release.jks"
ALIAS="foldstandby"
KC_SERVICE="fold-standby-release-keystore"
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
KEYTOOL="$JAVA_HOME/bin/keytool"
ICLOUD="$HOME/Library/Mobile Documents/com~apple~CloudDocs"
DEFAULT_BACKUP="$ICLOUD/Keys"

[ -x "$KEYTOOL" ] || { echo "[중단] keytool 없음: $KEYTOOL"; exit 1; }
if [ -e "$KEY" ]; then
  echo "[중단] 이미 키가 있습니다: $KEY"
  echo "기존 키를 지우면 설치된 정식 앱을 업데이트할 수 없게 됩니다. 덮어쓰지 않습니다."
  exit 1
fi

AUTO=0; [ "${1:-}" = "--auto" ] && AUTO=1

if [ $AUTO = 1 ]; then
  echo "== 1/5 임의 비밀번호 생성 (화면에 표시하지 않음)"
  P1=$(openssl rand -base64 30 | tr -d '/+=\n'); P2="$P1"
else
  echo "== 1/5 비밀번호 정하기 (12자 이상, 화면에 표시되지 않음)"
fi
while [ $AUTO = 0 ]; do
  read -r -s -p "비밀번호: " P1; echo
  read -r -s -p "한 번 더: " P2; echo
  if [ "$P1" != "$P2" ]; then echo "두 입력이 다릅니다. 다시 입력하세요."; continue; fi
  if [ ${#P1} -lt 12 ]; then echo "12자 이상으로 정해 주세요."; continue; fi
  break
done
export FS_KS_PASS="$P1"
unset P1 P2

echo "== 2/5 서명키 만들기 (RSA 4096, 30년)"
mkdir -p "$KEY_DIR" && chmod 700 "$KEY_DIR"
"$KEYTOOL" -genkeypair -keystore "$KEY" -storetype PKCS12 -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=Mark Kim, O=Personal, C=KR" \
  -storepass:env FS_KS_PASS -keypass:env FS_KS_PASS >/dev/null
chmod 600 "$KEY"

echo "== 3/5 키체인에 비밀번호 저장"
security add-generic-password -U -a "$USER" -s "$KC_SERVICE" -l "Fold StandBy release keystore" -w "$FS_KS_PASS"
[ "$(security find-generic-password -a "$USER" -s "$KC_SERVICE" -w)" = "$FS_KS_PASS" ] && echo "키체인 저장 확인"

echo "== 4/5 키 파일 백업"
if [ $AUTO = 1 ]; then
  BACKUP=""
elif [ -d "$ICLOUD" ]; then
  read -r -p "백업 폴더 [엔터 = $DEFAULT_BACKUP]: " BACKUP
else
  read -r -p "백업 폴더 (예: /Volumes/USB이름/Keys): " BACKUP
fi
BACKUP="${BACKUP:-$DEFAULT_BACKUP}"
mkdir -p "$BACKUP"
cp "$KEY" "$BACKUP/foldstandby-release.jks"
cmp -s "$KEY" "$BACKUP/foldstandby-release.jks" && echo "백업 확인: $BACKUP/foldstandby-release.jks"

echo "== 5/5 확인용 정보"
FP=$("$KEYTOOL" -list -v -keystore "$KEY" -alias "$ALIAS" -storepass:env FS_KS_PASS | awk -F': ' '/SHA256:/{print $2; exit}')
unset FS_KS_PASS
cat > "$KEY_DIR/foldstandby-release.txt" <<INFO
Fold StandBy 정식 서명키 (비밀 값 없음)
생성: $(date '+%Y-%m-%d %H:%M')
키 파일: $KEY
별칭: $ALIAS
백업: $BACKUP/foldstandby-release.jks
비밀번호: macOS 키체인 "$KC_SERVICE" + Apple 암호 앱
SHA-256: $FP
INFO
chmod 600 "$KEY_DIR/foldstandby-release.txt"
echo "SHA-256 지문: $FP"
echo
if [ $AUTO = 1 ]; then
  echo "완료. 비밀번호는 키체인 앱에서 \"$KC_SERVICE\" 항목으로 볼 수 있습니다."
else
  echo "완료. Apple 암호 앱에도 같은 비밀번호를 'Fold StandBy 서명키'로 저장해 두세요."
fi
