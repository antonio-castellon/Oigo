[Español](README.md) · [English](README.EN.md) · [Deutsch](README.DE.md)

<p>
  <a href="https://github.com/antonio-castellon/Oigo/releases/latest"><img src="https://img.shields.io/github/v/release/antonio-castellon/Oigo?label=version" alt="version"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-blue" alt="licence MIT"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white" alt="Android 9 ou plus">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
</p>

![Un monsieur âgé, à l'aise sur le canapé, qui parle avec le téléphone de la petite table](docs/img/banner.jpg)

# Oigo

J'ai essayé l'application en espagnol. Je n'ai pas essayé ce français avec une personne dont c'est la langue, ni dans la voix ni sur cette page. Si le français est votre langue, je serai heureux de recevoir vos retours, des améliorations et les défauts que vous trouverez.

Un téléphone, même avec de très gros caractères, reste un petit écran. Pour une personne qui voit mal, trouver le bouton du micro, l'atteindre du doigt et ne pas se perdre dans les menus suffit à rendre l'appareil presque inutilisable au quotidien. Grok écoute, si l'on arrive jusqu'à ce bouton. Y arriver est le plus difficile.

Oigo poursuit [Grok Assistant](https://github.com/antonio-castellon/Grok_Assistant), sur l'ordinateur, et [Grok Pi Assistance](https://github.com/antonio-castellon/Grok_Pi_Assistance), sur le Raspberry Pi. Dans mon cas, la personne est mon père. Je voulais qu'il puisse avoir Grok comme compagnon de conversation, sans avoir à lire.

D'autres applications écoutent, lisent les messages ou téléphonent. Celle-ci n'essaie pas de prendre leur place. Elle est faite pour qu'une personne âgée n'ait qu'un bouton à atteindre, et pour qu'elle puisse demander ou raconter en parlant.

## Écran
L'écran est presque vide, exprès. Un grand bouton au centre rouvre le micro. Demander, continuer, s'arrêter, cela se fait à la voix, aussi naturellement que j'ai pu le laisser dans ce qu'Android autorise. Les réglages sont dans une petite icône en haut à gauche, un peu sous la barre, pour ne pas la toucher en abaissant les notifications.

<p align="center">
  <img src="docs/img/home.png" width="250" alt="Le bouton Écouter, au centre">
</p>

## Microphone
Le micro cherche à rester ouvert aussi longtemps que le système le permet. Ce qui s'entend dans la maison ne quitte pas le téléphone. Seule une phrase adressée à Grok part vers le nuage. On l'ouvre avec « hola grok » — ou « hey grok », si l'on préfère.

À l'ouverture de la conversation, l'application règle le haut-parleur sur le volume de la voix enregistré dans les réglages. Il est à 80 % au départ. Personne n'a à aller à chaque fois sur la barre du téléphone pour le monter à la main. On le change dans les réglages, de dix en dix, sous « Volume de la voix ».

Une fois la conversation ouverte, chaque phrase pour le téléphone commence par le mot grok. « Grok, quel temps fait-il. » « Grok, raconte-moi une histoire. » Ce qui suit est la demande. On ferme avec « grok, gracias », ou avec un au revoir court qui commence de la même façon. La conversation se ferme seule après un moment de silence (1 minute par défaut, et cela se change).

Grok est la façon de s'adresser au téléphone, à l'agent. Il n'y a pas d'empreinte vocale pour distinguer qui parle. Le micro continue d'entendre la pièce : la télévision, une autre personne, et la voix de l'assistant pendant qu'il répond. Seule une phrase qui commence par grok est prise comme dite pour lui, et seule celle-là peut quitter le téléphone. Le reste reste dans l'appareil.

La conversation n'est pas gardée chez xAI. De ce qui a été dit il garde un court résumé de la journée et les dernières phrases, en local, comme contexte de la conversation, et cela couvre environ douze heures, le jour. On le change dans les réglages, sous « Heures dont la conversation se souvient », heure par heure, jusqu'à vingt-quatre. « Grok, gracias » et le silence ferment l'écoute, pas le contexte du jour. « Grok, oublie » efface la conversation et son contexte local.

## Alarmes / notifications
Les notes et les alarmes sont gardées localement sur le téléphone, et elles ne partent pas dans le nuage comme les questions d'une conversation. « Grok, souviens-toi que… » le répète et le note si tu dis « grok, oui ». « Grok, rappelle-moi à huit heures » propose une alarme : il dit l'heure et le rappel, et ne la garde qu'avec un « grok, oui ». « Grok, de quoi tu te souviens » les dit. « Grok, oublie les notes » et « grok, annule les alarmes » les enlèvent.

Dans les réglages on peut activer la lecture de WhatsApp et aussi appeler quelqu'un du carnet d'adresses à la voix. Les messages nouveaux ne sont pas lus tout seuls. Elle explique qu'un message est arrivé et, après un signal, demande une confirmation parlée avant de lire. Alors tu peux dire « oui » ou « grok, oui », et sans ouvrir de conversation elle lit le contenu du message depuis la notification. Si les notifications sont effacées, plus rien ne peut être lu, parce que le système n'ouvre ni le chat de WhatsApp ni celui de Telegram. Je n'ai pas encore réussi à faire fonctionner la même chose avec Telegram. Cela reste une tâche pour une prochaine version.

La commande « appelle … » ou « grok, appelle … » compose, et elle n'ouvre pas non plus la conversation.

Android ferme parfois ce qui écoute depuis un moment. S'il tue le processus de l'application, par malheur pour l'usage quotidien, le micro ne revient pas seul : il faut ouvrir l'application et appuyer sur le bouton. Si l'application est encore là mais que le micro n'est plus en mode écoute, elle prévient à voix haute quelques fois, à quelques minutes d'intervalle, au cas où personne n'était près du téléphone la première fois. Le nombre de fois, et l'écart, se règlent dans les réglages.

## Réglages
L'application officielle de Grok, même déjà installée sur le téléphone, ne partage pas sa session. Il faut une clé API de [console.x.ai](https://console.x.ai), gardée chiffrée sur le téléphone. Si le crédit est épuisé, la voix dit qu'il lui faut plus de carburant, à chaque échange.

L'application parle espagnol, anglais, français, allemand et italien.

## Chaque phrase commence par grok

La conversation s'ouvre avec « hola grok ». Ensuite, tout ce qu'on dit au téléphone commence par le mot grok. Sinon, le téléphone l'entend et ne répond pas : cela peut être la télévision, une autre personne, ou sa propre voix. On ferme avec « grok, gracias ».

- **Personne.** Hola grok.
- **Grok.** Bonjour.
- **Personne.** Grok, quel temps fait-il à Barcelone.
- **Grok.** Aujourd'hui c'est nuageux.
- **Personne.** Grok, raconte-moi une courte histoire.
- **Grok.** Il était une fois un phare qui s'éteignait au lever du soleil.
- **Personne.** Grok, rappelle-moi à huit heures pour le médicament.
- **Grok.** À 8:00, le médicament. Si c'est bien ça, dis grok, oui.
- **Personne.** Grok, oui.
- **Grok.** Je te préviens à 8:00.
- **Personne.** Grok, gracias.
- **Grok.** À plus tard.

## Ce qu'on peut demander

Le temps et les nouvelles. « Grok, quel temps fait-il aujourd'hui à Barcelone. » « Grok, quelles sont les nouvelles. » Cela se cherche sur internet.

Un fait historique. « Grok, qui était Napoléon. » « Grok, en quelle année le mur de Berlin est-il tombé. » Cela se cherche aussi. Une date ou un fait ne s'invente pas.

Une histoire, ou une explication. « Grok, raconte-moi une histoire. » « Grok, explique-moi ce qu'est un arc-en-ciel. » Cela ne se cherche pas. Il le raconte.

Une alarme ou un rappel. « Grok, rappelle-moi à huit heures d'appeler le médecin. » « Grok, dans dix minutes, le médicament. » « Grok, souviens-toi que les clés sont dans le tiroir. » Il répète d'abord ce qu'il a compris. Il ne le garde que si tu réponds « grok, oui ». « Grok, non » ne le garde pas.

Appeler quelqu'un du carnet d'adresses. « Grok, appelle Marie. » Il faut allumer Appels dans les réglages. Le téléphone demande les contacts et l'autorisation d'appeler. Sans cela, il ne compose pas.

## Quel téléphone

Android 9 ou plus récent, un téléphone ARM ordinaire (64 ou 32 bits). Il faut un micro, et le réseau la première fois : un petit modèle de la phrase se télécharge, et chaque conversation avec Grok sort sur internet. L'application pèse environ 45 Mo, le modèle un peu plus. L'application officielle de Grok n'est pas nécessaire. Une clé de [console.x.ai](https://console.x.ai) l'est.

## Comment la préparer

1. Téléchargez l'APK de la [version 1.0.37](https://github.com/antonio-castellon/Oigo/releases/tag/v1.0.37).
2. Sur le téléphone, autorisez l'installation depuis cette source et ouvrez le fichier.
3. Ouvrez Oigo et touchez la petite icône en haut à gauche.
4. Collez la clé. Changez la langue, la phrase ou le volume de la voix si vous voulez. Elle est sur « hola grok », et le volume à 80 %.
5. Revenez et appuyez sur **Écouter**. Acceptez le micro et, si on le demande, les notifications.
6. Dites « hola grok ». Dans la conversation, chaque phrase commence par grok. « Grok, gracias » la ferme.
7. Les permissions ci-dessous ne sont pas un supplément. Sans elles, le bouton est pressé et le téléphone ferme l'écoute dès que l'écran s'éteint.

## Permissions et restrictions

Pour qu'elle entende, et continue d'entendre :

- **Micro.** Demandé en appuyant sur Écouter. S'il est refusé, il n'y a ni phrase ni conversation.
- **Notifications.** Nécessaires pour l'avis « en écoute » et pour l'avertissement parlé si le micro s'arrête. Sur Xiaomi, Redmi et POCO, laissez les notifications d'Oigo actives, pas en silence.
- **Batterie sans restriction.** Sur la fiche de l'application, l'économie de batterie doit être sans restrictions. Les réglages d'Oigo ouvrent aussi cet écran. Cela prolonge l'écoute. Cela ne réveille pas un processus que le système a déjà tué.
- **Démarrage automatique**, sur Xiaomi, Redmi et POCO. S'il est éteint, HyperOS ferme l'écoute dès que l'écran s'éteint. C'est sur la fiche de l'application, ou dans Sécurité.
- **Épingler l'application** dans les récentes, le cadenas, pour qu'un balayage ne l'enlève pas.
- **Alarmes et rappels**, si le téléphone le demande. Ils répètent l'avertissement que le micro n'écoute plus. Sans cette permission, l'avertissement peut arriver tard.

Pour installer le fichier à la main, et surtout sur un POCO ou un Xiaomi :

- Autoriser **l'installation d'applications inconnues** depuis l'endroit où vous ouvrez l'APK.
- Si vous le passez par USB : **débogage USB**, **débogage USB (paramètres de sécurité)** et **installer via USB**. Sous HyperOS, sans installer via USB, le téléphone annule l'installation même si l'ordinateur est déjà accepté. Cette option reste parfois bloquée tant qu'un compte Mi n'est pas ouvert.

Seulement si vous activez les extras. Éteints, ils ne demandent rien :

- **Appels :** contacts et téléphone. Sans les deux, ça ne compose pas.
- **WhatsApp :** contacts, pour ouvrir un brouillon, et accès aux notifications. On lit la notification, pas le chat dans WhatsApp. Si l'avis n'existe pas ou s'il a été effacé, cette conversation ne peut pas être lue. Le texte n'est dit qu'après « grok, oui ».
- **Telegram :** la même chose. Seulement la notification, pas le chat de l'application. Sans avis, il n'y a rien à lire. Envoyer un message par Telegram, pour l'instant, n'est pas là.

Rien de tout cela n'utilise la session de l'application officielle de Grok.

## Comment compiler

Il faut JDK 17 et le Android SDK 35. Le `java` de la machine peut être plus ancien : Gradle doit utiliser le 17.

```
git clone https://github.com/antonio-castellon/Oigo.git
cd Oigo
.\gradlew.bat assembleDebug
```

Le paquet se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. La première compilation télécharge les bibliothèques natives de sherpa-onnx. Avec le téléphone en débogage USB :

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Collaboration

Les idées, les défauts et les changements sont les bienvenus. Le grand bouton et la vie privée de la phrase sont la partie que je ne veux pas perdre. Avant un changement, lisez [comment contribuer](CONTRIBUTING.md). La [façon de se traiter](CODE_OF_CONDUCT.md) est courte. Une clé ou un problème de sécurité ne s'écrit pas dans un issue ouvert : passez par [SECURITY.md](SECURITY.md).

MIT. Les parties sherpa-onnx gardent leur licence Apache-2.0, dans [NOTICE](NOTICE). Le reste est dans [LICENSE](LICENSE).
