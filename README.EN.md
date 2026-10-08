[Español](README.md) · [Français](README.FR.md) · [Deutsch](README.DE.md)

<p>
  <a href="https://github.com/antonio-castellon/Grok_Android/releases/latest"><img src="https://img.shields.io/github/v/release/antonio-castellon/Grok_Android?label=version" alt="version"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT license"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" alt="Android 9 or newer">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

![An older man, happy on the sofa, talking with the phone on the little table](docs/img/banner.jpg)

# Grok Voice

A phone is still a small screen, even with the type set large. For someone whose sight is poor, finding the microphone button, hitting it, and not getting lost in the menus is enough to make the thing nearly useless day to day. Grok will listen, if you can get to that button. Getting there is the hard part.

Grok Voice continues [Grok Assistant](https://github.com/antonio-castellon/Grok_Assistant), on a computer, and [Grok Pi Assistance](https://github.com/antonio-castellon/Grok_Pi_Assistance), on a Raspberry Pi. In my case the person is my father. I wanted him to have Grok as someone to talk to, without having to read.

The screen is almost empty on purpose. One large button in the middle turns the microphone back on. Asking, carrying on, and stopping are done by voice, as naturally as I could make it inside what Android allows. Settings live in a small icon at the upper left, set a little below the status bar, so a swipe down for notifications does not land on it.

The microphone stays open for as long as the system will allow. What it hears in the house does not leave the phone. Only a phrase that is actually addressed to Grok is sent on. You begin with "hola grok" — or "hey grok", if that one is preferred — and you close it whenever you like, with "gracias" or another short goodbye. A stretch of quiet closes it too.

The first time you open it, it does not record your voice and it does not take a voice print. The phrase is already written, as phonemes, not as a recording of the person. It still needs a try on a real microphone: some of them clip the start of a word, and then the phrase is never heard.

Other apps listen, read messages, and place calls. This one is not trying to replace them. It is there so an older person only has to hit one button, and can ask or talk the way they would to someone in the room. Later, if it is needed, WhatsApp reading and calling someone from the address book can be turned on in settings. A new message is never read aloud on its own. The voice asks, and the text is spoken only after a yes. You can also ask for the last few messages from a person, by the name of that chat. Telegram is not there yet.

The official Grok app, even when it is already on the phone, does not share its login. There is no way to link the account without typing anything. An API key is pasted once, from [console.x.ai](https://console.x.ai), and it stays on the phone. If the credit runs out, the voice says it needs more fuel.

Android sometimes closes an app that has been listening for a while. If it kills the process, the microphone does not come back by itself: open the app and press the button. If the app is still alive but the microphone has stopped, it speaks a short warning a few times, a few minutes apart, in case nobody was near the phone the first time. How many times, and how far apart, can be changed in settings.

The app speaks Spanish, English, French, German, and Italian.

<p align="center">
  <img src="docs/img/home.png" width="280" alt="The Listen button, in the middle">
</p>

## What the phone needs

Android 9 or newer, a normal ARM phone (64-bit or 32-bit). It needs a microphone, and a network the first time: it downloads a small model for the phrase, and every talk with Grok goes out to the internet. The app is about 45 MB, and the model a little more. The official Grok app is not required. An API key from [console.x.ai](https://console.x.ai) is.

## How to set it up

1. Download the APK from the [1.0.0 release](https://github.com/antonio-castellon/Grok_Android/releases/tag/v1.0.0).
2. On the phone, allow install from that source and open the file.
3. Open Grok Voice and tap the small icon at the upper left.
4. Paste the key. Change the language or the phrase if you want. It starts as "hola grok".
5. Go back and press **Listen**. Allow the microphone and, if asked, notifications.
6. Say "hola grok" and talk. "Gracias" closes the conversation.
7. If the phone likes to close apps, settings opens the battery exception. It keeps listening longer. It does not wake a process Android has already killed.
8. WhatsApp and calls are switched on from that same screen, and only then ask for their permission. Telegram is not there yet.

## How to build it

You need JDK 17 and Android SDK 35. The `java` on the machine may be older: Gradle has to use 17.

```
git clone https://github.com/antonio-castellon/Grok_Android.git
cd Grok_Android
.\gradlew.bat assembleDebug
```

The package lands in `app/build/outputs/apk/debug/app-debug.apk`. The first build downloads the sherpa-onnx native libraries. With the phone in USB debugging:

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Collaboration

Ideas, bugs, and changes are welcome. The large button and the privacy of the phrase are the part I do not want to lose. Before a change, read [how to contribute](CONTRIBUTING.md). The [way we treat each other](CODE_OF_CONDUCT.md) is short. A key or a security problem does not go in an open issue: use [SECURITY.md](SECURITY.md).

MIT. The sherpa-onnx pieces keep their own Apache-2.0 license, in [NOTICE](NOTICE). The rest is in [LICENSE](LICENSE).
