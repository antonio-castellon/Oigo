[Español](README.md) · [English](README.EN.md) · [Français](README.FR.md)

<p>
  <a href="https://github.com/antonio-castellon/Oigo/releases/latest"><img src="https://img.shields.io/github/v/release/antonio-castellon/Oigo?label=Version" alt="Version"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/Lizenz-MIT-blue" alt="MIT-Lizenz"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" alt="Android 9 oder neuer">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

![Ein älterer Herr, zufrieden auf dem Sofa, im Gespräch mit dem Telefon auf dem Tischchen](docs/img/banner.jpg)

# Oigo

Ich habe die App auf Spanisch ausprobiert. Dieses Deutsch habe ich nicht mit jemandem geprüft, der es als Muttersprache spricht, weder in der Stimme noch auf dieser Seite. Wenn Deutsch Ihre Sprache ist, freue ich mich über Rückmeldungen, Verbesserungen und Fehler.

Ein Telefon bleibt ein kleiner Bildschirm, auch mit sehr großer Schrift. Für einen Menschen, der schlecht sieht, genügt es, den Mikrofonknopf zu suchen, ihn mit dem Finger zu treffen und sich nicht in den Menüs zu verlieren, und das Gerät ist im Alltag fast unbrauchbar. Grok hört zu, wenn man diesen Knopf erreicht. Das Erreichen ist das Schwere.

Oigo führt das weiter, was [Grok Assistant](https://github.com/antonio-castellon/Grok_Assistant) am Computer ist und [Grok Pi Assistance](https://github.com/antonio-castellon/Grok_Pi_Assistance) auf dem Raspberry Pi. In meinem Fall ist die Person mein Vater. Ich wollte, dass er Grok als Gesprächspartner haben kann, ohne lesen zu müssen.

Andere Anwendungen hören zu, lesen Nachrichten oder telefonieren. Diese hier will ihren Platz nicht einnehmen. Sie ist dafür da, dass ein älterer Mensch nur einen Knopf treffen muss und das, was er fragen oder erzählen will, einfach sagen kann.

## Bildschirm
Der Bildschirm ist absichtlich fast leer. Ein großer Knopf in der Mitte öffnet das Mikrofon wieder. Fragen, weiterreden und aufhören geht mit der Stimme, so natürlich, wie ich es innerhalb dessen lassen konnte, was Android erlaubt. Die Einstellungen liegen in einem kleinen Symbol oben links, etwas unter der Leiste, damit man es nicht aus Versehen trifft, wenn man die Benachrichtigungen herunterzieht.

<p align="center">
  <img src="docs/img/home.png" width="250" alt="Der Knopf Hören, in der Mitte">
</p>

## Mikrofon
Das Mikrofon soll so lange offen bleiben, wie das System es zulässt. Was im Haus zu hören ist, verlässt das Telefon nicht. Nur ein Satz, der an Grok gerichtet ist, geht in die Cloud. Man öffnet mit „hola grok“ — oder mit „hey grok“, wenn einem das lieber ist.

Wenn das Gespräch aufgeht, stellt die App den Lautsprecher auf die Lautstärke der Stimme, die in den Einstellungen gespeichert ist. Voreingestellt sind 80 %. Niemand muss jedes Mal an der Leiste des Telefons von Hand lauter drehen. Man ändert sie in den Einstellungen, in Zehnerschritten, unter „Lautstärke der Stimme“.

Ist das Gespräch offen, beginnt jeder Satz an das Telefon mit dem Wort grok. „Grok, wie wird das Wetter.“ „Grok, erzähl mir eine Geschichte.“ Dahinter steht, was man möchte. Man schließt mit „grok, gracias“, oder mit einem kurzen Abschied, der genauso anfängt. Das Gespräch schließt sich von selbst nach einer Weile Stille (1 Minute als Vorgabe, und das lässt sich ändern).

Grok ist die Art, das Telefon, den Agenten, anzusprechen. Es gibt keinen Stimmabdruck, der unterscheidet, wer spricht. Das Mikrofon hört den Raum weiter: den Fernseher, eine andere Person und die eigene Stimme des Assistenten, während er antwortet. Nur was mit grok beginnt, gilt als an ihn gerichtet, und nur das darf das Telefon verlassen. Der Rest bleibt auf dem Gerät.

Das Gespräch wird bei xAI nicht gespeichert. Vom Gesagten behält es eine kurze Zusammenfassung des Tages und die letzten Sätze, lokal, als Kontext des Gesprächs, und das deckt etwa zwölf Stunden ab, den Tag über. Man ändert es in den Einstellungen unter „Stunden, die das Gespräch behält“, stundenweise, bis vierundzwanzig. „Grok, gracias“ und die Stille beenden das Zuhören, nicht den Kontext des Tages. „Grok, vergiss“ löscht das Gespräch und seinen lokalen Kontext.

## Wecker / Benachrichtigungen
Notizen und Wecker bleiben lokal auf dem Telefon, und sie gehen nicht in die Cloud wie die Fragen in einem Gespräch. „Grok, merke dir…“ wiederholt es und schreibt es auf, wenn du „grok, ja“ sagst. „Grok, weck mich um acht“ schlägt einen Wecker vor: es sagt die Uhrzeit und den Hinweis, und speichert ihn erst nach „grok, ja“. „Grok, woran erinnerst du dich“ sagt sie auf. „Grok, vergiss die Notizen“ und „grok, vergiss die Wecker“ nehmen sie weg.

In den Einstellungen lassen sich das Vorlesen von WhatsApp und ein Anruf an jemanden aus dem Adressbuch per Stimme einschalten. Neue Nachrichten werden nicht von selbst vorgelesen. Sie sagt, dass eine Textnachricht angekommen ist, und bittet nach einem Ton um eine gesprochene Bestätigung, bevor sie liest. Dann kannst du „ja“ oder „grok, ja“ sagen, und ohne ein Gespräch zu öffnen liest sie den Inhalt aus der Benachrichtigung. Sind die Benachrichtigungen gelöscht, lässt sich nichts mehr lesen, weil das System weder den WhatsApp-Chat noch den Telegram-Chat öffnet. Dasselbe mit Telegram habe ich noch nicht hinbekommen. Das bleibt eine Aufgabe für eine spätere Version.

Der Befehl „ruf … an“ oder „grok, ruf … an“ wählt, und er öffnet das Gespräch auch nicht.

Android schließt manchmal, was eine Weile zugehört hat. Wenn es den Prozess der App beendet, kommt das Mikrofon leider für den Alltag nicht von selbst zurück: die App öffnen und den Knopf drücken. Wenn die App noch da ist, das Mikrofon aber nicht mehr im Hörmodus ist, sagt sie ein paar Mal Bescheid, mit einigen Minuten Abstand, falls beim ersten Mal niemand in der Nähe war. Wie oft, und in welchem Abstand, stellt man in den Einstellungen ein.

## Einstellungen
Die offizielle Grok-App teilt ihre Anmeldung nicht, auch wenn sie schon auf dem Telefon liegt. Es braucht einen API-Schlüssel von [console.x.ai](https://console.x.ai), der verschlüsselt auf dem Telefon bleibt. Wenn das Guthaben alle ist, sagt die Stimme bei jedem Austausch, dass sie mehr Treibstoff braucht.

Die App spricht Spanisch, Englisch, Französisch, Deutsch und Italienisch.

## Jeder Satz beginnt mit grok

Das Gespräch öffnet sich mit „hola grok“. Danach beginnt alles, was man dem Telefon sagt, mit dem Wort grok. Sonst hört das Telefon es und antwortet nicht: es kann der Fernseher sein, eine andere Person oder die eigene Stimme. Man schließt mit „grok, gracias“.

- **Person.** Hola grok.
- **Grok.** Hallo.
- **Person.** Grok, wie ist das Wetter in Barcelona.
- **Grok.** Heute ist es bewölkt.
- **Person.** Grok, erzähl mir eine kurze Geschichte.
- **Grok.** Es war einmal ein Leuchtturm, der ausging, wenn die Sonne aufging.
- **Person.** Grok, weck mich um acht wegen der Tablette.
- **Grok.** Um 8:00, die Tablette. Wenn das stimmt, sag grok, ja.
- **Person.** Grok, ja.
- **Grok.** Ich erinnere dich um 8:00.
- **Person.** Grok, gracias.
- **Grok.** Bis später.

## Was man fragen kann

Wetter und Nachrichten. „Grok, wie ist heute das Wetter in Barcelona.“ „Grok, was gibt es Neues.“ Das wird im Internet gesucht.

Eine historische Angabe. „Grok, wer war Napoleon.“ „Grok, in welchem Jahr fiel die Berliner Mauer.“ Das wird auch gesucht. Ein Datum oder eine Tatsache wird nicht erfunden.

Eine Geschichte oder eine Erklärung. „Grok, erzähl mir eine Geschichte.“ „Grok, erklär mir, was ein Regenbogen ist.“ Das wird nicht gesucht. Er erzählt es.

Einen Wecker oder einen Hinweis. „Grok, weck mich um acht, um den Arzt anzurufen.“ „Grok, in zehn Minuten, die Tablette.“ „Grok, merke dir, dass die Schlüssel in der Schublade sind.“ Zuerst wiederholt es, was es verstanden hat. Es speichert es nur, wenn du „grok, ja“ antwortest. „Grok, nein“ lässt es ungespeichert.

Jemanden aus dem Adressbuch anrufen. „Grok, ruf María an.“ Anrufe müssen in den Einstellungen an sein. Das Telefon fragt nach Kontakten und der Erlaubnis zu telefonieren. Ohne das wählt es nicht.

## Was das Telefon braucht

Android 9 oder neuer, ein gewöhnliches ARM-Telefon (64 oder 32 Bit). Es braucht ein Mikrofon, und beim ersten Mal Netz: ein kleines Modell für den Satz wird geladen, und jedes Gespräch mit Grok geht ins Internet. Die App ist etwa 45 MB, das Modell ein wenig mehr. Die offizielle Grok-App ist nicht nötig. Ein Schlüssel von [console.x.ai](https://console.x.ai) schon.

## So wird sie eingerichtet

1. Lade das APK der [Version 1.0.35](https://github.com/antonio-castellon/Oigo/releases/tag/v1.0.35).
2. Erlaube auf dem Telefon die Installation aus dieser Quelle und öffne die Datei.
3. Öffne Oigo und tippe das kleine Symbol oben links.
4. Füge den Schlüssel ein. Sprache, Satz oder Lautstärke der Stimme kannst du ändern. Voreingestellt sind „hola grok“ und 80 % Lautstärke.
5. Geh zurück und drücke **Hören**. Erlaube das Mikrofon und, falls gefragt, die Benachrichtigungen.
6. Sag „hola grok“. Im Gespräch beginnt jeder Satz mit grok. „Grok, gracias“ schließt es.
7. Die Rechte unten sind kein Zusatz. Ohne sie wird der Knopf gedrückt und das Telefon schließt das Zuhören, sobald der Bildschirm aus geht.

## Rechte und Einschränkungen

Damit sie hört und weiterhört:

- **Mikrofon.** Wird beim Tippen auf Hören verlangt. Wird es verweigert, gibt es keinen Satz und kein Gespräch.
- **Benachrichtigungen.** Nötig für den Hinweis „hört zu“ und für die gesprochene Warnung, wenn das Mikrofon stoppt. Auf Xiaomi, Redmi und POCO die Benachrichtigungen von Oigo anlassen, nicht stumm.
- **Akku ohne Einschränkung.** Auf der Seite der App muss der Akku-Sparmodus auf ohne Einschränkungen stehen. Die Einstellungen von Oigo öffnen diesen Schirm auch. Das verlängert das Zuhören. Es weckt keinen Prozess, den das System schon beendet hat.
- **Autostart**, auf Xiaomi, Redmi und POCO. Ist er aus, schließt HyperOS das Zuhören, sobald der Bildschirm aus geht. Das steht auf der Seite der App oder unter Sicherheit.
- **Die App anheften** in den letzten Apps, das Schloss, damit ein Wischen sie nicht entfernt.
- **Wecker und Erinnerungen**, falls das Telefon danach fragt. Sie wiederholen die Warnung, dass das Mikrofon nicht mehr hört. Ohne dieses Recht kann die Warnung spät kommen.

Um die Datei von Hand zu installieren, und besonders auf einem POCO oder Xiaomi:

- **Unbekannte Apps installieren** erlauben, von dort, wo du das APK öffnest.
- Wenn du sie per USB schickst: **USB-Debugging**, **USB-Debugging (Sicherheitseinstellungen)** und **Per USB installieren**. Unter HyperOS bricht das Telefon ohne „Per USB installieren“ ab, auch wenn der Computer schon erlaubt ist. Die Option bleibt manchmal gesperrt, bis ein Mi-Konto angemeldet ist.

Nur wenn du die Extras einschaltest. Ausgeschaltet verlangen sie nichts:

- **Anrufe:** Kontakte und Telefon. Ohne beide wird nicht gewählt.
- **WhatsApp:** Kontakte, um einen Entwurf zu öffnen, und Benachrichtigungszugriff. Gelesen wird die Benachrichtigung, nicht der Chat in WhatsApp. Fehlt der Hinweis oder wurde er gelöscht, kann dieses Gespräch nicht gelesen werden. Der Text wird erst nach „grok, ja“ gesagt.
- **Telegram:** dasselbe. Nur die Benachrichtigung, nicht der Chat der Anwendung. Ohne Hinweis gibt es nichts zu lesen. Eine Nachricht über Telegram zu senden ist bisher nicht da.

Nichts davon benutzt die Anmeldung der offiziellen Grok-App.

## So wird gebaut

Es braucht JDK 17 und das Android SDK 35. Das `java` der Maschine kann älter sein: Gradle muss das 17 benutzen.

```
git clone https://github.com/antonio-castellon/Oigo.git
cd Oigo
.\gradlew.bat assembleDebug
```

Das Paket liegt in `app/build/outputs/apk/debug/app-debug.apk`. Der erste Bau lädt die nativen Bibliotheken von sherpa-onnx. Mit dem Telefon in der USB-Fehlersuche:

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Mitarbeit

Ideen, Fehler und Änderungen sind willkommen. Der große Knopf und die Privatheit des Satzes sind das Stück, das ich nicht verlieren will. Vor einer Änderung lies [wie man mitmacht](CONTRIBUTING.md). Der [Umgang miteinander](CODE_OF_CONDUCT.md) ist kurz. Ein Schlüssel oder ein Sicherheitsproblem gehört nicht in ein offenes Issue: dafür ist [SECURITY.md](SECURITY.md).

MIT. Die sherpa-onnx-Teile behalten ihre Apache-2.0-Lizenz, in [NOTICE](NOTICE). Der Rest steht in [LICENSE](LICENSE).
