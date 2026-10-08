# Cómo colaborar

Grok voz es pequeña a propósito. Un botón, una frase, una conversación. Una mejora que convierta la pantalla en un panel de opciones no encaja, aunque esté bien hecha.

Sirve un fallo que hayas visto en un teléfono de verdad, una frase que el detector no oye, una traducción que suena a máquina, o una idea que deje el uso igual de simple. WhatsApp, las llamadas y, más adelante, Telegram caben si siguen siendo opcionales y piden el permiso solo al encenderse.

Para proponer un cambio:

1. Abre un issue si todavía no está claro el problema.
2. Parte de `main` y deja el cambio en una rama corta.
3. No subas claves, `local.properties`, ni el APK.
4. Cuenta en el texto del cambio qué teléfono probaste, si lo probaste.

Hace falta JDK 17 y el Android SDK 35. `.\gradlew.bat assembleDebug` tiene que seguir compilando.

## Contributing

The app is small on purpose. One button, one phrase, one conversation. A change that turns the screen into a panel of options does not fit, even if it is well made.

A bug seen on a real phone, a phrase the detector misses, a translation that sounds like a machine, or an idea that keeps the use this simple: those fit. WhatsApp, calls, and later Telegram belong if they stay optional and ask for permission only when switched on.

Open an issue when the problem is not clear yet. Branch from `main`. Do not commit keys, `local.properties`, or the APK. Say which phone you tried, if you tried one. JDK 17, Android SDK 35, and `.\gradlew.bat assembleDebug` should still build.
