#!/data/data/com.termux/files/usr/bin/sh
# Poll the phone's own wlan address for an Android 11+ wireless-debugging port.
#
# Why this exists: the build machine IS the Galaxy A56 (Claude Code runs in
# Termux under PRoot). `adb devices` lists nothing because there is no USB host.
# But adb is a TCP client, and PRoot shares the phone's network namespace, so
# adb running here can reach the phone's own wireless-debugging port. That gives
# a fully automated `adb install` / `adb shell` / `adb logcat` with no USB.
#
# Android 11+ assigns the wireless-debugging port from the dynamic range
# 32768-60999. We watch that range and report the moment a port opens.
#
# Usage: tools/wireless_debug_watch.sh [seconds]   (default 900)
# Exit:  0 a port was found, 1 timed out with no port.

set -eu

DURATION="${1:-900}"
SELF_IP="${SELF_IP:-}"
POLL="${POLL:-2}"
LOW=32768
HIGH=60999

if [ -z "$SELF_IP" ]; then
  SELF_IP=$(python3 -c "
import socket
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
try:
    s.connect(('8.8.8.8', 80)); print(s.getsockname()[0])
except Exception:
    print('')
" 2>/dev/null || true)
fi

if [ -z "$SELF_IP" ]; then
  echo "watch: cannot determine this phone's wlan address" >&2
  exit 1
fi

echo "watch: watching $SELF_IP:$LOW-$HIGH for ${DURATION}s"
echo "watch: enable Settings > About phone > tap Build number 7x,"
echo "watch: then Developer options > Wireless debugging > Pair device with a code"
echo "watch: pairing opens a port in the ADB range; the connect port differs."

DEADLINE=$(( $(date +%s) + DURATION ))
while [ "$(date +%s)" -lt "$DEADLINE" ]; do
  FOUND=$(python3 - "$SELF_IP" "$LOW" "$HIGH" <<'PY'
import socket, sys
ip, lo, hi = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])

def probe(p):
    s = socket.socket()
    s.settimeout(0.30)
    try:
        s.connect((ip, p))
        return p
    except Exception:
        return None
    finally:
        s.close()

hits = []
for p in range(lo, hi + 1):
    if probe(p):
        hits.append(p)
print(' '.join(str(h) for h in hits))
PY
)
  if [ -n "$FOUND" ]; then
    echo "watch: OPEN -> $SELF_IP:$FOUND"
    exit 0
  fi
  sleep "$POLL"
done

echo "watch: timed out, no wireless-debugging port appeared" >&2
exit 1
