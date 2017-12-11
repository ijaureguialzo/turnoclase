# https://github.com/fastlane/fastlane/issues/9471

# !/bin/bash

#$(which emulator) -avd Nexus_5_API_23 -no-skin -no-audio -no-window -gpu off -ports 5556,5557 > /dev/null &

if $(which adb) devices | grep -q "emulator-$2"; then
  echo "Emulator ready!"
else
  $(which emulator) -avd $1 -no-audio -gpu on -port $2 > /dev/null 2>&1 &
fi
exit 0
