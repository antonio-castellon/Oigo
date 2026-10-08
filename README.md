# Grok voz (Android)

Aplicación de voz para el teléfono. Un botón grande en el centro vuelve a abrir el micrófono. La frase clave se detecta por fonemas en el propio teléfono. Solo después de esa frase el texto sale hacia la API de Grok. La conversación sigue abierta hasta un silencio largo o un adiós corto.

La pantalla de ajustes está en un icono pequeño, arriba a la izquierda, separado de la barra de estado para que un desliz hacia abajo no lo pulse.

## Primera vez: no hay que grabar nada

No hace falta grabar la voz ni una huella. La frase ya está escrita como fonemas del modelo. No es una grabación de la persona.

Frase por defecto: **hola grok**. También se puede elegir **hey grok**.

Línea que se usa, en el formato del modelo zh-en de sherpa-onnx:

```
HH OW1 L AA1 G R OW1 K @HOLA_GROK
HH EY1 G R OW1 K @HEY_GROK
```

Solo se escribe la línea de la frase elegida. Es una candidata. Hay que probarla con el micrófono real del teléfono. Un filtro de ruido puede cortar los primeros 50–150 ms y la frase no salta.

El reconocedor de Android no puede leer el audio que ya capturó el detector. La pregunta dicha en la misma respiración puede perder el principio. Por eso, al oír la frase, el reconocedor arranca enseguida y la aplicación no dice «Dime» antes.

Mientras la conversación está abierta, las frases siguientes van a Grok sin repetir la frase clave. El micrófono se cierra mientras habla la voz del teléfono.

## La cuenta de Grok

Si el teléfono ya tiene la aplicación oficial de Grok, esta aplicación no puede usar ese inicio de sesión ni leer sus ficheros. No hay un enlace de cuenta sin escribir nada.

La clave de la API se pega una vez en ajustes. Sale de [console.x.ai](https://console.x.ai). Se guarda cifrada en el teléfono cuando el sistema lo permite. La suscripción de la aplicación oficial no incluye el crédito de la API.

Si la API responde 401, 402 o 429, la voz dice que necesita más gasolina y esa vuelta no sigue.

El modelo por defecto es `grok-4.7`. Se puede cambiar en ajustes.

## Instalar el APK

El APK de depuración queda en:

`app/build/outputs/apk/debug/app-debug.apk`

En el teléfono: Ajustes, instalar aplicaciones desconocidas, abrir el APK. La primera vez que se pulsa **Escuchar**, Android pide el micrófono. También pide las notificaciones, para el aviso de escucha y para el aviso si el micrófono se para.

La primera escucha descarga el modelo de fonemas (unos pocos megabytes) desde GitHub. Hace falta red esa vez.

## Compilar

Hace falta JDK 17 (el `java` por defecto de esta máquina puede ser el 8) y el Android SDK 35. La aplicación se instala con target 34.

```
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew.bat assembleDebug
```

La primera compilación descarga las librerías nativas de sherpa-onnx 1.13.8 (arm64-v8a y armeabi-v7a). No van en el repositorio. Ver `NOTICE`.

## Micrófono en segundo plano

Mientras escucha hay una notificación fija. El servicio es de tipo micrófono y hay que haberlo arrancado con la aplicación visible.

Si el proceso sigue vivo pero el micrófono ya no escucha, la voz avisa. Por defecto son 3 avisos, separados 5 minutos, por si la persona no está junto al teléfono en el primero. Las dos cifras se cambian en ajustes.

Si Android mata el proceso, el micrófono no vuelve a abrirse solo. `START_STICKY` no es fiable para un servicio de micrófono. Tras reiniciar el teléfono solo aparece una notificación: hay que abrir la aplicación y pulsar el botón. Quitar la optimización de batería alarga la escucha, pero no resucita un proceso muerto.

## Llamadas y mensajes

Apagados al instalar. El permiso se pide solo al activar cada opción.

- Llamar: contactos y teléfono. Una sola coincidencia de nombre, y un solo móvil si hay varios números. La frase puede ser «puedes llamar a Antonio Javier». El nombre no se envía a Grok si la orden ya se entiende en el teléfono.
- WhatsApp y Telegram: solo los avisos que llegan mientras la opción está activa. No es el historial completo del chat.
- Un mensaje nuevo no se lee solo. La voz pregunta si quieres oírlo. El texto se dice después de sí, yes, oui, ja o vale.
- También vale «lee los últimos 5 mensajes de Ana», con los avisos guardados de ese nombre.
- Enviar un WhatsApp abre un borrador. El envío lo pulsa la persona. Telegram no se puede dirigir por el nombre del chat; leer sus avisos sí.

No hay servicio de accesibilidad ni lectura de otra aplicación por dentro.

## English

One button restarts listening. The wake phrase is a written phoneme line, so the first launch records nothing. Text leaves the phone only after that phrase. The official Grok app cannot share its login: paste an xAI API key once. Calls, WhatsApp, and Telegram stay off until enabled, and a message body is spoken only after a yes. If Android kills the process, the microphone does not reopen by itself.

## License

MIT. The sherpa-onnx keyword classes and native libraries are Apache-2.0. See `NOTICE`.
