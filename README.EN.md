[Español](README.md) · [Français](README.FR.md) · [Deutsch](README.DE.md)

<p>
  <a href="https://github.com/antonio-castellon/Oigo/releases/latest"><img src="https://img.shields.io/github/v/release/antonio-castellon/Oigo?label=version" alt="version"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT license"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" alt="Android 9 or newer">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

![An older man, happy on the sofa, talking with the phone on the little table](docs/img/banner.jpg)

# Oigo

I have tried the app in Spanish. I have not tried this English with a native speaker, neither in the voice nor on this page. If English is your language, I would be glad to receive feedback, improvements, and bugs.

A phone is still a small screen, even with the type set large. For someone whose sight is poor, finding the microphone button, hitting it, and not getting lost in the menus is enough to make the thing nearly useless day to day. Grok will listen, if you can get to that button. Getting there is the hard part.

Oigo continues [Grok Assistant](https://github.com/antonio-castellon/Grok_Assistant), on a computer, and [Grok Pi Assistance](https://github.com/antonio-castellon/Grok_Pi_Assistance), on a Raspberry Pi. In my case the person is my father. I wanted him to have Grok as someone to talk to, without having to read.

The screen is almost empty on purpose. One large button in the middle turns the microphone back on. Asking, carrying on, and stopping are done by voice, as naturally as I could make it inside what Android allows. Settings live in a small icon at the upper left, set a little below the status bar, so a swipe down for notifications does not land on it.

The microphone stays open for as long as the system will allow. What it hears in the house does not leave the phone. Only a phrase that is actually addressed to Grok is sent on. You open it with "hola grok" — or "hey grok", if that one is preferred.

Once the conversation is open, every phrase for the phone starts with the word grok. "Grok, what's the weather." "Grok, tell me a story." The words after that are the request. You close it with "grok, gracias", or with another short goodbye that starts the same way. A stretch of quiet closes it too.

Grok is how you address the phone, the agent. There is no voice print to tell who is speaking. The microphone keeps hearing the room: the television, another person, and the assistant's own voice while it answers. Only a phrase that starts with grok is taken as meant for it, and only that phrase can leave the phone. Anything else stays on the device, and it does not break the quiet that ends the conversation.

The chat is not stored at xAI. Each question goes out on its own. The phone does not send the searched pages again. Of what was said it keeps a short summary of the day and the latest lines, and that covers about twelve hours, the daytime. It is changed in settings, under "Hours the chat remembers", one hour at a time, up to twenty-four. "Grok, gracias" and silence close the listening, not that day. "Grok, forget" does clear the chat.

Notes and alarms are a different box, on the phone itself, and they do not leave with the chat. "Grok, remember that…" repeats it and saves it if you say "grok, yes". "Grok, wake me at eight" offers an alarm: it says the time and the reminder, and saves it only after "grok, yes". "Grok, what do you remember" says them. "Grok, forget the notes" and "grok, cancel the alarms" remove them.

When the conversation opens, the app sets the speaker to the voice volume saved in settings. It starts at 80%. Nobody has to open the phone's volume bar and turn it up by hand each time. It is changed in settings, in steps of ten, under "Voice volume".

The first time you open it, it does not record your voice and it does not take a voice print. The phrase is already written, as phonemes, not as a recording of the person. It still needs a try on a real microphone: some of them clip the start of a word, and then the phrase is never heard.

Other apps listen, read messages, and place calls. This one is not trying to replace them. It is there so an older person only has to hit one button, and can ask or talk the way they would to someone in the room. Later, if it is needed, WhatsApp reading and calling someone from the address book can be turned on in settings. A new message is never read aloud on its own. Even with the conversation closed, it asks who it is from. When the question ends, a short tone plays. After it, "yes" or "grok, yes" is enough, and that does not open the conversation. With listening on, "call …" or "grok, call …" dials without opening the chat. Later it has to be "grok, yes", and the same listening that waits for "hola grok" hears it. If the chat is already open, "grok, yes" confirms it too, and the chat goes on. The text is spoken only after that yes. Another phrase in the conversation does not throw the notice away. If the listen button is off, it still gives the notice, and the question waits until listening is on. You can also ask for the last few messages from a person, by the name of that chat. That, and a new notice, comes from the phone notification. The chat inside WhatsApp or Telegram is not opened. If the notice is missing, or it was cleared, that conversation cannot be read. Sending on Telegram is not there yet.

The official Grok app, even when it is already on the phone, does not share its login. There is no way to link the account without typing anything. An API key is pasted once, from [console.x.ai](https://console.x.ai), and it stays on the phone. If the credit runs out, the voice says it needs more fuel.

Android sometimes closes an app that has been listening for a while. If it kills the process, the microphone does not come back by itself: open the app and press the button. If the app is still alive but the microphone has stopped, it speaks a short warning a few times, a few minutes apart, in case nobody was near the phone the first time. How many times, and how far apart, can be changed in settings.

The app speaks Spanish, English, French, German, and Italian.

<p align="center">
  <img src="docs/img/home.png" width="280" alt="The Listen button, in the middle">
</p>

## Every phrase starts with grok

The conversation opens with "hola grok". From then on, everything said to the phone starts with the word grok. If it does not, the phone hears it and does not answer: it may be the television, another person, or its own voice. It closes with "grok, gracias".

- **Person.** Hola grok.
- **Grok.** Hello.
- **Person.** Grok, what's the weather in Barcelona.
- **Grok.** It is cloudy today.
- **Person.** Grok, tell me a short story.
- **Grok.** Once there was a lighthouse that went out when the sun rose.
- **Person.** Grok, wake me at eight for the pill.
- **Grok.** At 8:00, the pill. If that is right, say grok, yes.
- **Person.** Grok, yes.
- **Grok.** I will remind you at 8:00.
- **Person.** Grok, gracias.
- **Grok.** Goodbye.

## What you can ask

The weather and the news. "Grok, what's the weather in Barcelona today." "Grok, what's in the news." That is looked up on the internet.

A historical fact. "Grok, who was Napoleon." "Grok, in what year did the Berlin wall fall." That is looked up too. A date or a fact is not invented.

A story, or an explanation. "Grok, tell me a story." "Grok, explain what a rainbow is." That is not looked up. It tells it.

An alarm or a reminder. "Grok, wake me at eight to call the doctor." "Grok, in ten minutes, the pill." "Grok, remember that the keys are in the drawer." It first repeats what it understood. It saves it only if you answer "grok, yes". "Grok, no" leaves it unsaved.

A call to someone in the address book. "Grok, call María." Calls must be turned on in settings. The phone asks for contacts and the permission to call. Without that, it does not dial.

## What the phone needs

Android 9 or newer, a normal ARM phone (64-bit or 32-bit). It needs a microphone, and a network the first time: it downloads a small model for the phrase, and every talk with Grok goes out to the internet. The app is about 45 MB, and the model a little more. The official Grok app is not required. An API key from [console.x.ai](https://console.x.ai) is.

## How to set it up

1. Download the APK from the [1.0.35 release](https://github.com/antonio-castellon/Oigo/releases/tag/v1.0.35).
2. On the phone, allow install from that source and open the file.
3. Open Oigo and tap the small icon at the upper left.
4. Paste the key. Change the language, the phrase, or the voice volume if you want. It starts as "hola grok", and the volume at 80%.
5. Go back and press **Listen**. Allow the microphone and, if asked, notifications.
6. Say "hola grok". In the conversation, every phrase starts with grok. "Grok, gracias" closes it.
7. The permissions below are not optional extras. Without them the button is pressed and the phone closes listening as soon as the screen goes off.

## Permissions and restrictions

So it can hear, and keep hearing:

- **Microphone.** Asked when you press Listen. If it is denied, there is no phrase and no conversation.
- **Notifications.** Needed for the "listening" notice and for the spoken warning if the microphone stops. On Xiaomi, Redmi, and POCO, leave Oigo notifications on, not silenced.
- **Battery, unrestricted.** On the app's page, battery saver must be set to no restrictions. Oigo settings opens that screen too. This keeps listening longer. It does not wake a process the system has already killed.
- **Autostart**, on Xiaomi, Redmi, and POCO. If it is off, HyperOS closes listening as soon as the screen goes off. It is on the app's page, or under Security.
- **Pin the app** in the recent apps, the lock, so a swipe does not clear it.
- **Alarms and reminders**, if the phone asks. They repeat the warning that the microphone is no longer listening. Without that permission the warning can arrive late.

To install the file by hand, and especially on a POCO or a Xiaomi:

- Allow **install unknown apps** from wherever you open the APK.
- If you send it over USB: **USB debugging**, **USB debugging (Security settings)**, and **Install via USB**. On HyperOS, without Install via USB the phone cancels the install even after this computer is allowed. That option sometimes stays locked until a Mi account is signed in.

Only if you turn the extras on. While they are off, they ask for nothing:

- **Calls:** contacts and phone. Without both, it does not dial.
- **WhatsApp:** contacts, to open a draft, and notification access. What is read is the notification, not the chat inside WhatsApp. If the notice is missing or was cleared, that conversation cannot be read. The text is not spoken until you answer "grok, yes".
- **Telegram:** the same. Only the notification, not the chat inside the app. If there is no notice, there is nothing to read. Sending a message on Telegram is not there yet.

None of this uses the official Grok app's login.

## How to build it

You need JDK 17 and Android SDK 35. The `java` on the machine may be older: Gradle has to use 17.

```
git clone https://github.com/antonio-castellon/Oigo.git
cd Oigo
.\gradlew.bat assembleDebug
```

The package lands in `app/build/outputs/apk/debug/app-debug.apk`. The first build downloads the sherpa-onnx native libraries. With the phone in USB debugging:

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Collaboration

Ideas, bugs, and changes are welcome. The large button and the privacy of the phrase are the part I do not want to lose. Before a change, read [how to contribute](CONTRIBUTING.md). The [way we treat each other](CODE_OF_CONDUCT.md) is short. A key or a security problem does not go in an open issue: use [SECURITY.md](SECURITY.md).

MIT. The sherpa-onnx pieces keep their own Apache-2.0 license, in [NOTICE](NOTICE). The rest is in [LICENSE](LICENSE).
