[Español](README.md) · [Français](README.FR.md) · [Deutsch](README.DE.md)

![The Listen button, in the middle of an almost empty screen](docs/img/home.png)

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

The APK to try is on the [1.0.0 release](https://github.com/antonio-castellon/Grok_Android/releases/tag/v1.0.0). On the phone, allow install from that source, open the file, and the first time you press Listen, allow the microphone.

To build it you need JDK 17 and Android SDK 35. `.\gradlew.bat assembleDebug` leaves the package in `app/build/outputs/apk/debug/`. The first build downloads the sherpa-onnx libraries, and the first listen downloads the phrase model.

MIT. The sherpa-onnx pieces keep their own Apache-2.0 license. See `NOTICE`.
