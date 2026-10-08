[English](README.EN.md) · [Français](README.FR.md) · [Deutsch](README.DE.md)

![El botón Escuchar, en el centro de una pantalla casi vacía](docs/img/home.png)

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

El APK para probarla está en la [versión 1.0.0](https://github.com/antonio-castellon/Grok_Android/releases/tag/v1.0.0). En el teléfono hay que permitir instalar desde ese origen, abrir el archivo y, la primera vez que se pulsa Escuchar, aceptar el micrófono.

Para compilarla hacen falta JDK 17 y el Android SDK 35. Con `.\gradlew.bat assembleDebug` el paquete queda en `app/build/outputs/apk/debug/`. La primera compilación descarga las librerías de sherpa-onnx, y la primera escucha descarga el modelo de la frase.

MIT. Lo de sherpa-onnx va con su propia licencia Apache-2.0. Está en `NOTICE`.
