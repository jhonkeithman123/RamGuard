#!/system/bin/sh
# Magisk module installer: fix permissions
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/service.sh 0 0 0755
set_perm $MODPATH/bin/ramguard 0 0 0755
