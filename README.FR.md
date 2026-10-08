[Español](README.md) · [English](README.EN.md) · [Deutsch](README.DE.md)

![Le bouton Écouter, au milieu d'un écran presque vide](docs/img/home.png)

# Grok voix

Un téléphone, même avec de très gros caractères, reste un petit écran. Pour une personne qui voit mal, trouver le bouton du micro, l'atteindre du doigt et ne pas se perdre dans les menus suffit à rendre l'appareil presque inutilisable au quotidien. Grok écoute, si l'on arrive jusqu'à ce bouton. Y arriver est le plus difficile.

Grok voix poursuit [Grok Assistant](https://github.com/antonio-castellon/Grok_Assistant), sur l'ordinateur, et [Grok Pi Assistance](https://github.com/antonio-castellon/Grok_Pi_Assistance), sur le Raspberry Pi. Dans mon cas, la personne est mon père. Je voulais qu'il puisse avoir Grok comme compagnon de conversation, sans avoir à lire.

L'écran est presque vide, exprès. Un grand bouton au centre rouvre le micro. Demander, continuer, s'arrêter, cela se fait à la voix, aussi naturellement que j'ai pu le laisser dans ce qu'Android autorise. Les réglages sont dans une petite icône en haut à gauche, un peu sous la barre, pour ne pas la toucher en abaissant les notifications.

Le micro cherche à rester ouvert aussi longtemps que le système le permet. Ce qui s'entend dans la maison ne quitte pas le téléphone. Seule une phrase adressée à Grok part vers le nuage. On commence par « hola grok » — ou « hey grok », si l'on préfère — et on ferme quand on veut, avec un « gracias » ou un au revoir court. Un moment de silence ferme aussi la conversation.

La première ouverture n'enregistre pas la voix et ne prend pas d'empreinte. La phrase est déjà écrite, en phonèmes, ce n'est pas un enregistrement de la personne. Il faut tout de même l'essayer sur un vrai micro : certains coupent le début du mot, et la phrase n'est alors jamais entendue.

D'autres applications écoutent, lisent les messages ou téléphonent. Celle-ci n'essaie pas de prendre leur place. Elle est faite pour qu'une personne âgée n'ait qu'un bouton à atteindre, et pour qu'elle puisse demander ou raconter en parlant. Plus tard, si besoin, la lecture de WhatsApp et l'appel à quelqu'un du carnet d'adresses s'activent dans les réglages. Un message nouveau n'est jamais lu tout seul. La voix demande, et le texte n'est dit qu'après un oui. On peut aussi demander les derniers messages d'une personne, par le nom de cette conversation. Telegram, pour l'instant, je n'y suis pas arrivé.

L'application officielle de Grok, même déjà installée sur le téléphone, ne partage pas sa session. Il n'y a pas moyen de lier le compte sans rien écrire. La clé API se colle une fois, depuis [console.x.ai](https://console.x.ai), et elle reste sur le téléphone. Si le crédit est épuisé, la voix dit qu'il lui faut plus de carburant.

Android ferme parfois ce qui écoute depuis un moment. S'il tue le processus, le micro ne revient pas seul : il faut ouvrir l'application et appuyer sur le bouton. Si l'application est encore là mais que le micro n'entend plus, elle prévient à voix haute quelques fois, à quelques minutes d'intervalle, au cas où personne n'était près du téléphone la première fois. Le nombre de fois, et l'écart, se règlent dans les réglages.

L'application parle espagnol, anglais, français, allemand et italien.

L'APK pour l'essayer est dans la [version 1.0.0](https://github.com/antonio-castellon/Grok_Android/releases/tag/v1.0.0). Sur le téléphone, il faut autoriser l'installation depuis cette source, ouvrir le fichier et, la première fois que l'on appuie sur Écouter, accepter le micro.

Pour la compiler, il faut JDK 17 et le Android SDK 35. `.\gradlew.bat assembleDebug` laisse le paquet dans `app/build/outputs/apk/debug/`. La première compilation télécharge les bibliothèques sherpa-onnx, et la première écoute télécharge le modèle de la phrase.

MIT. Les parties sherpa-onnx gardent leur licence Apache-2.0. Voir `NOTICE`.
