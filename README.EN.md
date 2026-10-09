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

Other apps listen, read messages, or place calls. This one is not trying to take their place. It is made so an older person only has to hit one button, and can ask or tell something by speaking.

## Screen
The screen is almost empty on purpose. One large button in the middle turns the microphone back on. Asking, carrying on, and stopping are done by voice, as naturally as I could make it inside what Android allows. Settings live in a small icon at the upper left, set a little below the status bar, so a swipe down for notifications does not land on it.

<p align="center">
  <img src="docs/img/home.png" width="250" alt="The Listen button, in the middle">
</p>

## Microphone
The microphone stays open for as long as the system will allow. What it hears in the house does not leave the phone. Only a phrase that is actually addressed to Grok is sent on. You open it with "hola grok" — or "hey grok", if that one is preferred.

When the conversation opens, the app sets the speaker to the voice volume saved in settings. It starts at 80%. Nobody has to open the phone's volume bar and turn it up by hand each time. It is changed in settings, in steps of ten, under "Voice volume".

Once the conversation is open, every phrase for the phone starts with the word grok. "Grok, what's the weather." "Grok, tell me a story." The words after that are the request. You close it with "grok, gracias", or with another short goodbye that starts the same way. The conversation closes by itself after a while of silence (1 minute by default, and that can be changed).

Grok is how you address the phone, the agent. There is no voice print to tell who is speaking. The microphone keeps hearing the room: the television, another person, and the assistant's own voice while it answers. Only a phrase that starts with grok is taken as meant for it, and only that phrase can leave the phone. Anything else stays on the device.

The chat is not stored at xAI. Of what was said it keeps a short summary of the day and the latest lines, on the phone, as the context of the conversation, and that covers about twelve hours, the daytime. It is changed in settings, under "Hours the chat remembers", one hour at a time, up to twenty-four. "Grok, gracias" and silence close the listening, not the day's context. "Grok, forget" does clear the chat and its local context.

## Alarms / notifications
Notes and alarms are stored locally on the phone itself, and they do not go to the cloud the way questions in a chat do. "Grok, remember that…" repeats it and saves it if you say "grok, yes". "Grok, wake me at eight" offers an alarm: it says the time and the reminder, and saves it only after "grok, yes". "Grok, what do you remember" says them. "Grok, forget the notes" and "grok, cancel the alarms" remove them.

In settings you can turn on WhatsApp reading and also calling someone from the address book by voice. New messages are not read on their own. It says a text message has arrived and, after a tone, asks for a spoken yes before reading. Then you can say "yes" or "grok, yes", and without opening a conversation it reads the message from the notification. If the notifications are cleared, nothing can be read, because the system does not open the WhatsApp chat or the Telegram chat. I have not yet managed to make the same thing work for Telegram. That stays as a task for a later version.

The command "call …" or "grok, call …" dials, and it does not open the chat either.

Android sometimes closes an app that has been listening for a while. If it kills the app process, unfortunately for everyday use the microphone does not come back by itself: open the app and press the button. If the app is still alive but the microphone is no longer in listen mode, it speaks a short warning a few times, a few minutes apart, in case nobody was near the phone the first time. How many times, and how far apart, can be changed in settings.

## Settings
The official Grok app, even when it is already on the phone, does not share its login. An API key from [console.x.ai](https://console.x.ai) is required, and it is stored encrypted on the phone. If the credit runs out, the voice says it needs more fuel on any interaction.

The app speaks Spanish, English, French, German, and Italian.

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
