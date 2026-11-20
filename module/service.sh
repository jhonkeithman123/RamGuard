#!/system/bin/sh
# RamGuard boot service
MODDIR=/data/adb/modules/ramguard
LOG=$MODDIR/boot.log
MGR=com.opencode.ramguard

log() { echo "$(date +%H:%M:%S) $*" >> "$LOG" 2>/dev/null; }

log "service started"

# wait for boot to settle
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 2; done
sleep 5

# ---- seed app-private config (the companion app edits this; root daemon reads it) ----
CFG=/data/data/com.opencode.ramguard/files/config.txt
APPDIR=/data/data/com.opencode.ramguard
U=$(stat -c %u $APPDIR 2>/dev/null)
G=$(stat -c %g $APPDIR 2>/dev/null)
mkdir -p $APPDIR/files
chown $U:$G $APPDIR/files 2>/dev/null
chmod 755 $APPDIR/files 2>/dev/null
if [ ! -f "$CFG" ]; then
    cp "$MODDIR/config.txt" "$CFG" 2>/dev/null
    chown $U:$G "$CFG" 2>/dev/null
    chmod 600 "$CFG" 2>/dev/null
    log "seeded $CFG"
fi
# daemon writes scope output - keep it readable by the app
chown $U:$G $APPDIR/files/rewardskip_scope.txt 2>/dev/null
chmod 644 $APPDIR/files/rewardskip_scope.txt 2>/dev/null

# ---- expand swap via a second zram device ----
# Bootloop safety: this runs only AFTER boot_completed (above), uses no swapoff,
# has a bounded retry loop, and every failure returns quietly so the manager
# service and daemon below still start.
setup_zram1() {
    Z1=/sys/block/zram1
    DEV=/dev/block/zram1

    # already enabled -> nothing to do
    grep -q "$DEV" /proc/swaps 2>/dev/null && return 0

    # Everything inside one bounded retry loop: the sysfs dir, block node,
    # disksize write and swapon can all lag right after a fresh boot/hot_add.
    i=0
    while [ $i -lt 8 ]; do
        if [ ! -d "$Z1" ]; then
            if [ -e /sys/class/zram-control/hot_add ]; then
                cat /sys/class/zram-control/hot_add >/dev/null 2>&1
            fi
        fi
        if [ -d "$Z1" ]; then
            # only set disksize when it is unset/zero (never resize a live device)
            CUR=$(cat $Z1/disksize 2>/dev/null)
            if [ -z "$CUR" ] || [ "$CUR" = "0" ]; then
                echo 1073741824 > $Z1/disksize 2>/dev/null
            fi
            if [ -b "$DEV" ]; then
                mkswap $DEV >/dev/null 2>&1
                swapon $DEV >/dev/null 2>&1
                if grep -q "$DEV" /proc/swaps 2>/dev/null; then
                    log "zram1 swap active (1 GB)"
                    return 0
                fi
            fi
        fi
        i=$((i + 1))
        sleep 1
    done
    log "zram1 swap not enabled (non-fatal, leaving boot alone)"
    return 0
}
setup_zram1

# ---- start the hidden manager notification ----
if pm list packages "$MGR" 2>/dev/null | grep -q "$MGR"; then
    # the app has no launcher icon, so grant notification permission from root
    pm grant "$MGR" android.permission.POST_NOTIFICATIONS 2>/dev/null
    am start-foreground-service -n "$MGR/.GuardService" >/dev/null 2>&1
    log "manager notification service started"
else
    log "manager app not installed systemlessly"
fi

# ---- start native daemon ----
if [ -x "$MODDIR/bin/ramguard" ]; then
    "$MODDIR/bin/ramguard" >/dev/null 2>&1 &
    log "ramguard daemon launched pid=$!"
else
    log "ramguard binary missing or not executable"
fi
