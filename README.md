[English](README.EN.md) · [Français](README.FR.md) · [Deutsch](README.DE.md)

<p>
  <a href="https://github.com/antonio-castellon/Grok_Android/releases/latest"><img src="https://img.shields.io/github/v/release/antonio-castellon/Grok_Android?label=versi%C3%B3n" alt="versión"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/licencia-MIT-blue" alt="licencia MIT"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" alt="Android 9 o más">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

![Un señor mayor, a gusto en el sofá, charlando con el móvil de la mesita](docs/img/banner.jpg)

# Grok voz

Un teléfono, aunque se le ponga la letra muy grande, sigue siendo una pantalla pequeña. Para una persona con la vista justa, encontrar el botón del micrófono, acertar con el dedo y no perderse por los menús hace que el aparato acabe siendo casi inservible en el día a día. Grok ya escucha si uno llega a pulsar ese botón. El problema es llegar.

Grok voz sigue el camino de [Grok Assistant](https://github.com/antonio-castellon/Grok_Assistant), en el ordenador, y de [Grok Pi Assistance](https://github.com/antonio-castellon/Grok_Pi_Assistance), en la Raspberry. En mi caso la persona es mi padre. Quería que pudiera tener a Grok como compañero de conversación, y que para eso no tuviera que leer.

La pantalla, a propósito, casi no tiene nada. Un botón grande en el centro vuelve a abrir el micrófono. Preguntar, seguir hablando y terminar se hace con la voz, de la forma más natural que he podido dejar dentro de lo que Android permite. Los ajustes están en un icono pequeño, arriba a la izquierda y un poco separado de la barra, para no pulsarlo sin querer al bajar las notificaciones.

El micrófono procura quedarse abierto todo el tiempo que el sistema lo consiente. Lo que se oye en casa no sale del teléfono. Solo una frase dirigida a Grok viaja a la nube. Se empieza con «hola grok» —o con «hey grok», si se prefiere esa— y se cierra cuando uno quiere, con un «gracias» o con un adiós corto. Si pasa un rato en silencio, también se cierra sola.

La primera vez no hay que grabar la voz ni dejar una huella. La frase ya está escrita, en fonemas, no es una grabación de la persona. Aun así hay que probarla con el micrófono de verdad: algunos recortan el principio de la palabra, y entonces la frase no llega a oírse.

Hay otras aplicaciones que escuchan, leen mensajes o llaman. Esta no intenta ocupar su sitio. Está hecha para que una persona mayor solo tenga que acertar un botón, y para que lo que quiera preguntar o contar pueda decirlo hablando. Si más adelante hace falta, en ajustes se puede encender la lectura de WhatsApp y también llamar a alguien de la agenda por la voz. Un mensaje nuevo no se lee solo: pregunta si quieres oírlo, y el texto solo sale si dices que sí. Pedir los últimos mensajes de una persona vale igual, con el nombre de ese chat. Telegram, de momento, no lo he conseguido.

Aunque el móvil tenga ya la aplicación oficial de Grok, esta no puede usar esa sesión. No hay forma de enlazar la cuenta sin escribir nada. La clave de la API se pega una vez, desde [console.x.ai](https://console.x.ai), y se queda en el teléfono. Si el crédito se acaba, la voz dice que necesita más gasolina.

Android, a veces, cierra lo que lleva un rato escuchando. Si mata el proceso, el micrófono no vuelve solo: hay que abrir la aplicación y pulsar el botón. Si la aplicación sigue viva pero el micrófono ya no oye, avisa por voz unas pocas veces, con unos minutos de por medio, por si la persona no estaba cerca en el primer aviso. Cuántas veces, y cada cuánto, se cambia en ajustes.

La aplicación habla español, inglés, francés, alemán e italiano.

<p align="center">
  <img src="docs/img/home.png" width="280" alt="El botón Escuchar, en el centro">
</p>

## Qué teléfono hace falta

Un Android 9 o más nuevo, de los normales (ARM, 64 o 32 bits). Hace falta micrófono, y red la primera vez: baja un modelo pequeño de la frase, y cada charla con Grok sale a internet. La aplicación ocupa unos 45 MB, y el modelo, unos pocos más. No hace falta tener instalada la aplicación oficial de Grok. Sí hace falta una clave de [console.x.ai](https://console.x.ai).

## Cómo dejarla lista

1. Baja el APK de la [versión 1.0.0](https://github.com/antonio-castellon/Grok_Android/releases/tag/v1.0.0).
2. En el teléfono, permite instalar desde ese origen y abre el archivo.
3. Abre Grok voz y pulsa el icono pequeño de arriba a la izquierda.
4. Pega la clave. Si quieres, cambia el idioma o la frase. Viene en «hola grok».
5. Vuelve atrás y pulsa **Escuchar**. Acepta el micrófono y, si sale, las notificaciones.
6. Di «hola grok» y habla. Un «gracias» cierra la charla.
7. Si el teléfono acostumbra a cerrar las aplicaciones, desde ajustes se abre la excepción de batería. Alarga la escucha. No despierta un proceso que Android ya haya matado.
8. WhatsApp y las llamadas se encienden en esa misma pantalla, y solo entonces piden su permiso. Telegram todavía no está.

## Cómo compilarla

Hace falta JDK 17 y el Android SDK 35. El `java` de la máquina puede ser más viejo: Gradle tiene que usar el 17.

```
git clone https://github.com/antonio-castellon/Grok_Android.git
cd Grok_Android
.\gradlew.bat assembleDebug
```

El paquete queda en `app/build/outputs/apk/debug/app-debug.apk`. La primera compilación descarga las librerías nativas de sherpa-onnx. Con el teléfono en depuración USB:

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Colaboraciones

Se aceptan ideas, fallos y cambios. El botón grande y la privacidad de la frase son la pieza que no quiero perder. Antes de un cambio, mira [cómo colaborar](CONTRIBUTING.md). El [trato entre personas](CODE_OF_CONDUCT.md) es corto. Una clave o un fallo de seguridad no se escribe en un issue abierto: va por [SECURITY.md](SECURITY.md).

MIT. Lo de sherpa-onnx va con su propia licencia Apache-2.0, en [NOTICE](NOTICE). El resto está en [LICENSE](LICENSE).
