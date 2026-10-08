[English](README.EN.md) · [Français](README.FR.md) · [Deutsch](README.DE.md)

<p>
  <a href="https://github.com/antonio-castellon/Oigo/releases/latest"><img src="https://img.shields.io/github/v/release/antonio-castellon/Oigo?label=versi%C3%B3n" alt="versión"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/licencia-MIT-blue" alt="licencia MIT"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" alt="Android 9 o más">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

![Un señor mayor, a gusto en el sofá, charlando con el móvil de la mesita](docs/img/banner.jpg)

# Oigo

Un teléfono, aunque se le ponga la letra muy grande, sigue siendo una pantalla pequeña. Para una persona con la vista justa, encontrar el botón del micrófono, acertar con el dedo y no perderse por los menús hace que el aparato acabe siendo casi inservible en el día a día. Grok ya escucha si uno llega a pulsar ese botón. El problema es llegar.

Oigo sigue el camino de [Grok Assistant](https://github.com/antonio-castellon/Grok_Assistant), en el ordenador, y de [Grok Pi Assistance](https://github.com/antonio-castellon/Grok_Pi_Assistance), en la Raspberry. En mi caso la persona es mi padre. Quería que pudiera tener a Grok como compañero de conversación, y que para eso no tuviera que leer.

La pantalla, a propósito, casi no tiene nada. Un botón grande en el centro vuelve a abrir el micrófono. Preguntar, seguir hablando y terminar se hace con la voz, de la forma más natural que he podido dejar dentro de lo que Android permite. Los ajustes están en un icono pequeño, arriba a la izquierda y un poco separado de la barra, para no pulsarlo sin querer al bajar las notificaciones.

El micrófono procura quedarse abierto todo el tiempo que el sistema lo consiente. Lo que se oye en casa no sale del teléfono. Solo una frase dirigida a Grok viaja a la nube. Se abre con «hola grok» —o con «hey grok», si se prefiere esa—.

Ya en conversación, cada frase para el teléfono empieza por la palabra grok. «Grok, qué tiempo hace.» «Grok, cuéntame un cuento.» Detrás va lo que se le pide. Se cierra con «grok, gracias», o con un adiós corto que también empiece así. Si pasa un rato en silencio, se cierra sola.

Grok es la forma de dirigirse al móvil, al agente. No hay huella de voz que distinga a quien habla. El micrófono sigue oyendo la habitación: la televisión, otra persona y la voz del asistente mientras contesta. Solo lo que empieza por grok se entiende como dicho para él, y solo eso puede salir del teléfono. Lo demás se queda en el aparato y no corta el silencio con el que la charla se cierra.

La charla no queda guardada en xAI. Cada pregunta sale sola. El teléfono no vuelve a mandar las páginas que buscó. De lo hablado guarda un resumen corto del día y las últimas frases, y eso cubre unas doce horas, el día diurno. Se cambia en ajustes, en «Horas que la charla recuerda», de hora en hora, hasta veinticuatro. «Grok, gracias» y el silencio cierran la escucha, no ese día. «Grok, olvida» sí borra la charla.

Los recuerdos y las alarmas son otra caja, en el propio teléfono, y no se van con la charla. «Grok, recuerda que…» lo repite y lo apunta si dices «grok, sí». «Grok, avísame a las ocho» propone una alarma: dice la hora y el aviso, y solo la guarda con un «grok, sí». «Grok, qué recuerdas» los dice. «Grok, borra los recuerdos» y «grok, cancela las alarmas» los quitan.

Al abrir la conversación, la aplicación pone el altavoz en el volumen de la voz que está guardado en ajustes. Viene al 80 %. Así no hay que ir cada vez a la barra del teléfono a subirlo a mano. Se cambia en ajustes, de diez en diez, en «Volumen de la voz».

La primera vez no hay que grabar la voz ni dejar una huella. La frase ya está escrita, en fonemas, no es una grabación de la persona. Aun así hay que probarla con el micrófono de verdad: algunos recortan el principio de la palabra, y entonces la frase no llega a oírse.

Hay otras aplicaciones que escuchan, leen mensajes o llaman. Esta no intenta ocupar su sitio. Está hecha para que una persona mayor solo tenga que acertar un botón, y para que lo que quiera preguntar o contar pueda decirlo hablando. Si más adelante hace falta, en ajustes se puede encender la lectura de WhatsApp y también llamar a alguien de la agenda por la voz. Un mensaje nuevo no se lee solo. Aunque la conversación esté cerrada, pregunta de quién es. Cuando acaba la pregunta suena un tono. Después vale «sí», o «grok, sí», y no abre la conversación. Con escuchar encendido, «llamar a…» o «grok, llamar a…» marca, sin abrir la charla. Si se dice más tarde, hace falta «grok, sí», y lo oye la misma escucha que espera «hola grok». Si la charla ya está abierta, «grok, sí» también lo confirma, y la charla sigue. El texto solo sale con ese sí. Otra frase de la conversación no tira el aviso. Si el botón de escuchar está apagado, avisa igual, y la pregunta queda para cuando lo enciendas. Pedir los últimos mensajes de una persona vale igual, con el nombre de ese chat. Eso, y el aviso de un mensaje nuevo, sale de la notificación del teléfono. No se abre el chat de WhatsApp ni el de Telegram. Si el aviso no está, o se ha borrado, no se puede leer esa conversación. Enviar por Telegram, de momento, no lo he conseguido.

Aunque el móvil tenga ya la aplicación oficial de Grok, esta no puede usar esa sesión. No hay forma de enlazar la cuenta sin escribir nada. La clave de la API se pega una vez, desde [console.x.ai](https://console.x.ai), y se queda en el teléfono. Si el crédito se acaba, la voz dice que necesita más gasolina.

Android, a veces, cierra lo que lleva un rato escuchando. Si mata el proceso, el micrófono no vuelve solo: hay que abrir la aplicación y pulsar el botón. Si la aplicación sigue viva pero el micrófono ya no oye, avisa por voz unas pocas veces, con unos minutos de por medio, por si la persona no estaba cerca en el primer aviso. Cuántas veces, y cada cuánto, se cambia en ajustes.

La aplicación habla español, inglés, francés, alemán e italiano.

<p align="center">
  <img src="docs/img/home.png" width="280" alt="El botón Escuchar, en el centro">
</p>

## Cada frase empieza por grok

La conversación se abre con «hola grok». Desde ahí, cada cosa que se le dice al teléfono empieza por la palabra grok. Si no empieza así, el móvil lo oye y no contesta: puede ser la televisión, otra persona o su propia voz. Se cierra con «grok, gracias».

- **Persona.** Hola grok.
- **Grok.** Hola.
- **Persona.** Grok, qué tiempo hace en Barcelona.
- **Grok.** Hoy está nublado.
- **Persona.** Grok, cuéntame un cuento corto.
- **Grok.** Había una vez un faro que se apagaba al salir el sol.
- **Persona.** Grok, avísame a las ocho de la pastilla.
- **Grok.** A las 8:00, la pastilla. Si es eso, di grok, sí.
- **Persona.** Grok, sí.
- **Grok.** Te aviso a las 8:00.
- **Persona.** Grok, gracias.
- **Grok.** Hasta luego.

## Qué se le puede pedir

El tiempo y las noticias. «Grok, qué tiempo hace hoy en Barcelona.» «Grok, qué noticias hay.» Eso se busca en internet.

Un dato histórico. «Grok, quién fue Napoleón.» «Grok, en qué año cayó el muro de Berlín.» También se busca. Una fecha o un hecho no se inventan.

Un cuento o que se lo explique. «Grok, cuéntame un cuento.» «Grok, explícame qué es un arcoíris.» Eso no se busca: lo cuenta.

Una alarma o un aviso. «Grok, avísame a las ocho de llamar al médico.» «Grok, dentro de diez minutos, la pastilla.» «Grok, recuerda que las llaves están en el cajón.» Primero repite lo que ha entendido. Solo lo guarda si contestas «grok, sí». «Grok, no» lo deja sin guardar.

Llamar a alguien de la agenda. «Grok, llama a María.» En ajustes hay que encender Llamadas. El teléfono pide los contactos y el permiso de llamar. Sin eso, no marca.

## Qué teléfono hace falta

Un Android 9 o más nuevo, de los normales (ARM, 64 o 32 bits). Hace falta micrófono, y red la primera vez: baja un modelo pequeño de la frase, y cada charla con Grok sale a internet. La aplicación ocupa unos 45 MB, y el modelo, unos pocos más. No hace falta tener instalada la aplicación oficial de Grok. Sí hace falta una clave de [console.x.ai](https://console.x.ai).

## Cómo dejarla lista

1. Baja el APK de la [versión 1.0.35](https://github.com/antonio-castellon/Oigo/releases/tag/v1.0.35).
2. En el teléfono, permite instalar desde ese origen y abre el archivo.
3. Abre Oigo y pulsa el icono pequeño de arriba a la izquierda.
4. Pega la clave. Si quieres, cambia el idioma, la frase o el volumen de la voz. Viene en «hola grok», y el volumen al 80 %.
5. Vuelve atrás y pulsa **Escuchar**. Acepta el micrófono y, si sale, las notificaciones.
6. Di «hola grok». En la conversación, cada frase empieza por grok. «Grok, gracias» cierra la charla.
7. Los permisos de abajo no son un extra. Sin ellos el botón se pulsa y el teléfono cierra la escucha al apagar la pantalla.

## Permisos y restricciones

Para que oiga y siga oyendo:

- **Micrófono.** Se pide al pulsar Escuchar. Si se niega, no hay frase ni conversación.
- **Notificaciones.** Hacen falta para el aviso de que está escuchando y para el aviso hablado si el micrófono se para. En Xiaomi, Redmi y POCO deja las notificaciones de Oigo activas, no en silencio.
- **Batería sin restricciones.** En la ficha de la aplicación, el ahorro de batería tiene que quedar en sin restricciones. Desde los ajustes de Oigo también se abre esa pantalla. Eso alarga la escucha. No despierta un proceso que el sistema ya haya matado.
- **Inicio automático**, en Xiaomi, Redmi y POCO. Si está apagado, HyperOS cierra la escucha en cuanto se apaga la pantalla. Está en la ficha de la aplicación, o en Seguridad.
- **Fijar la aplicación** en las recientes, el candado, para que un barrido no la quite.
- **Alarmas y recordatorios**, si el teléfono lo pregunta. Sirven para repetir el aviso de que el micrófono ya no escucha. Sin ese permiso el aviso puede llegar tarde.

Para instalar el archivo a mano, y sobre todo en un POCO o un Xiaomi:

- Permitir **instalar aplicaciones desconocidas** desde donde abras el APK.
- Si la pasas por USB: **depuración USB**, **depuración USB (ajustes de seguridad)** e **instalar vía USB**. En HyperOS, sin instalar vía USB el teléfono cancela la instalación aunque el ordenador ya esté aceptado. A veces esa opción no se deja encender hasta iniciar la cuenta Mi.

Solo si enciendes las extras. Apagadas, no piden nada:

- **Llamadas:** contactos y teléfono. Sin los dos, no marca.
- **WhatsApp:** contactos, para abrir un borrador, y acceso a las notificaciones. Se lee la notificación, no el chat dentro de WhatsApp. Si el aviso no existe o se ha borrado, no se puede leer esa conversación. El texto no se dice hasta que contestes «grok, sí».
- **Telegram:** lo mismo. Solo la notificación, no el chat de la aplicación. Si no hay aviso, no hay nada que leer. Enviar un mensaje por Telegram, de momento, no está.

Nada de esto usa la sesión de la aplicación oficial de Grok.

## Cómo compilarla

Hace falta JDK 17 y el Android SDK 35. El `java` de la máquina puede ser más viejo: Gradle tiene que usar el 17.

```
git clone https://github.com/antonio-castellon/Oigo.git
cd Oigo
.\gradlew.bat assembleDebug
```

El paquete queda en `app/build/outputs/apk/debug/app-debug.apk`. La primera compilación descarga las librerías nativas de sherpa-onnx. Con el teléfono en depuración USB:

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Colaboraciones

Se aceptan ideas, fallos y cambios. El botón grande y la privacidad de la frase son la pieza que no quiero perder. Antes de un cambio, mira [cómo colaborar](CONTRIBUTING.md). El [trato entre personas](CODE_OF_CONDUCT.md) es corto. Una clave o un fallo de seguridad no se escribe en un issue abierto: va por [SECURITY.md](SECURITY.md).

MIT. Lo de sherpa-onnx va con su propia licencia Apache-2.0, en [NOTICE](NOTICE). El resto está en [LICENSE](LICENSE).
