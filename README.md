Pocket Engima Machine for Android

# Build instructions

Download Android SDK -> place in $HOME/android-sdks

Set path:

    $ . bin/use

Confirm Android SDK manager working:

    $ android sdk

Install at least Android 4.0 (latest) and Android 2.1 SDK Platforms.

Find our platform numbers:

    $ android list

Try to build:

    $ ant debug

Created and run emulated device for platform N:

    $ android create avd -n test -t N
    $ emulator -avd test

Install on emulator:

    $ adb install bin/EnigmaApp-debug.apk

Should be able to run app from emulator!

Making and signing a release.

    $ ant release
    $ cd bin
    $ jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore startpad-release.keystore \
        EnigmaApp-release-unsigned.apk startpad_release
    $ mv EnigmaApp-release-unsigned.apk EnigmaApp.apk
