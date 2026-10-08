[Español](README.md) · [English](README.EN.md) · [Deutsch](README.DE.md)

<p>
  <a href="https://github.com/antonio-castellon/Grok_Android/releases/latest"><img src="https://img.shields.io/github/v/release/antonio-castellon/Grok_Android?label=version" alt="version"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-blue" alt="licence MIT"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" alt="Android 9 ou plus">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

![Un monsieur âgé, à l'aise sur le canapé, qui parle avec le téléphone de la petite table](docs/img/banner.jpg)

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

<p align="center">
  <img src="docs/img/home.png" width="280" alt="Le bouton Écouter, au centre">
</p>

## Quel téléphone

Android 9 ou plus récent, un téléphone ARM ordinaire (64 ou 32 bits). Il faut un micro, et le réseau la première fois : un petit modèle de la phrase se télécharge, et chaque conversation avec Grok sort sur internet. L'application pèse environ 45 Mo, le modèle un peu plus. L'application officielle de Grok n'est pas nécessaire. Une clé de [console.x.ai](https://console.x.ai) l'est.

## Comment la préparer

1. Téléchargez l'APK de la [version 1.0.0](https://github.com/antonio-castellon/Grok_Android/releases/tag/v1.0.0).
2. Sur le téléphone, autorisez l'installation depuis cette source et ouvrez le fichier.
3. Ouvrez Grok voix et touchez la petite icône en haut à gauche.
4. Collez la clé. Changez la langue ou la phrase si vous voulez. Elle est sur « hola grok ».
5. Revenez et appuyez sur **Écouter**. Acceptez le micro et, si on le demande, les notifications.
6. Dites « hola grok » et parlez. Un « gracias » ferme la conversation.
7. Les permissions ci-dessous ne sont pas un supplément. Sans elles, le bouton est pressé et le téléphone ferme l'écoute dès que l'écran s'éteint.

## Permissions et restrictions

Pour qu'elle entende, et continue d'entendre :

- **Micro.** Demandé en appuyant sur Écouter. S'il est refusé, il n'y a ni phrase ni conversation.
- **Notifications.** Nécessaires pour l'avis « en écoute » et pour l'avertissement parlé si le micro s'arrête. Sur Xiaomi, Redmi et POCO, laissez les notifications de Grok voix actives, pas en silence.
- **Batterie sans restriction.** Sur la fiche de l'application, l'économie de batterie doit être sans restrictions. Les réglages de Grok voix ouvrent aussi cet écran. Cela prolonge l'écoute. Cela ne réveille pas un processus que le système a déjà tué.
- **Démarrage automatique**, sur Xiaomi, Redmi et POCO. S'il est éteint, HyperOS ferme l'écoute dès que l'écran s'éteint. C'est sur la fiche de l'application, ou dans Sécurité.
- **Épingler l'application** dans les récentes, le cadenas, pour qu'un balayage ne l'enlève pas.
- **Alarmes et rappels**, si le téléphone le demande. Ils répètent l'avertissement que le micro n'écoute plus. Sans cette permission, l'avertissement peut arriver tard.

Pour installer le fichier à la main, et surtout sur un POCO ou un Xiaomi :

- Autoriser **l'installation d'applications inconnues** depuis l'endroit où vous ouvrez l'APK.
- Si vous le passez par USB : **débogage USB**, **débogage USB (paramètres de sécurité)** et **installer via USB**. Sous HyperOS, sans installer via USB, le téléphone annule l'installation même si l'ordinateur est déjà accepté. Cette option reste parfois bloquée tant qu'un compte Mi n'est pas ouvert.

Seulement si vous activez les extras. Éteints, ils ne demandent rien :

- **Appels :** contacts et téléphone. Sans les deux, ça ne compose pas.
- **WhatsApp :** contacts, pour ouvrir un brouillon, et accès aux notifications, pour voir un nouvel avis. Le texte n'est lu que si vous dites oui. Pas l'historique d'avant : seulement ce qui arrive avec l'option active.
- Telegram n'est pas là. Aucune permission à lui donner.

Rien de tout cela n'utilise la session de l'application officielle de Grok.

## Comment compiler

Il faut JDK 17 et le Android SDK 35. Le `java` de la machine peut être plus ancien : Gradle doit utiliser le 17.

```
git clone https://github.com/antonio-castellon/Grok_Android.git
cd Grok_Android
.\gradlew.bat assembleDebug
```

Le paquet se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. La première compilation télécharge les bibliothèques natives de sherpa-onnx. Avec le téléphone en débogage USB :

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Collaboration

Les idées, les défauts et les changements sont les bienvenus. Le grand bouton et la vie privée de la phrase sont la partie que je ne veux pas perdre. Avant un changement, lisez [comment contribuer](CONTRIBUTING.md). La [façon de se traiter](CODE_OF_CONDUCT.md) est courte. Une clé ou un problème de sécurité ne s'écrit pas dans un issue ouvert : passez par [SECURITY.md](SECURITY.md).

MIT. Les parties sherpa-onnx gardent leur licence Apache-2.0, dans [NOTICE](NOTICE). Le reste est dans [LICENSE](LICENSE).
