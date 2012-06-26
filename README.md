Pocket Engima Machine for Android

# Build instructions

Download Android SDK -> place in $HOME/android-sdks

Set path:

    $ . bin/use

Confirm Android SDK manager working:

    $ android sdk

Install at least Android 4.0 (latest) and Android 2.1 SDK Platforms.

Try to build:

    $ ant debug

Install on emulator:

    $ adb install bin/EnigmaApp-debug.apk

Should be able to run app from emulator!
