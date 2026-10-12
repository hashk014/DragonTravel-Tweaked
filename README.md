# DragonTravel Reforged — hash014

Revival de [DragonTravel](https://github.com/Phiwa/DragonTravel), dans le fork [DragonTravel-Tweaked](https://github.com/hashk014/DragonTravel-Tweaked).

Plugin **serveur Java** pour Bukkit/Spigot/Paper. Aucun mod client ni pack de ressources requis : le joueur monte sur un véritable Ender Dragon. Un seul JAR compilé en Java 8 ; les serveurs récents utilisent leur propre Java récent. La matrice publiée dans `docs/reforged/compatibility-results.json` donne les versions et builds effectivement testés, jusqu'à **26.3**. Les versions non listées ne sont pas présentées comme testées. Folia n'est pas pris en charge.

## Installation

1. Arrêter le serveur et sauvegarder ses mondes et sa configuration.
2. Retirer l'ancien JAR DragonTravel : les deux plugins utilisent `/dt`.
3. Copier `DragonTravel-Reforged-2.0.0.jar` dans `plugins/`, puis démarrer le serveur.
4. Comme opérateur, se placer sur un sol solide et utiliser `/dt setstat village`.
5. Créer une seconde station, puis `/dt travel village` ou `/dt menu`.

`plugins/DragonTravelReforged/config.yml` règle vitesse, altitude, limites, cooldown, mondes, départ depuis une station et tarifs. La vitesse se mesure en **blocs par seconde**, pas en blocs par tick. L'arrivée nécessite deux blocs d'air au-dessus d'un sol solide. Si la destination est devenue dangereuse, le voyage revient au départ. `/dt stop` ou la touche pour descendre revient aussi au départ.

## Fonctionnalités

- Vol réel avec décollage, altitude de croisière, atterrissage et progression avec durée restante.
- Stations persistantes, domicile par UUID, voyage vers un joueur qui peut refuser via `/dt ptoggle`.
- Itinéraires panoramiques avec jusqu'à 64 étapes et édition en jeu.
- Menu paginé des stations et itinéraires, permissions par destination.
- Dragons décoratifs persistants ; chunks tenus chargés uniquement pendant leur utilisation.
- Panneaux `[DTR]`, `travel` ou `flight` en ligne 2 et nom en ligne 3.
- Gratuit, paiement en ressources ordinaires, ou intégration Vault facultative.
- Journal atomique du départ avant la montée : retour au départ après déconnexion ou interruption au prochain login ; retour au spawn si le sol du départ n'est plus sûr.
- Dragons sans IA destructive, protection des voyageurs pendant le vol et protection temporaire contre les dégâts de chute.
- API avec événement annulable `JourneyStartEvent` pour les protections tierces et `JourneyEndEvent`.

## Commandes

| Commande | Usage |
| --- | --- |
| `/dt menu` | Menu des stations et itinéraires |
| `/dt travel <station>` | Voyager vers une station |
| `/dt stations`, `/dt routes` | Lister les destinations |
| `/dt flight <route>` | Suivre les étapes d'un itinéraire |
| `/dt sethome`, `/dt home` | Enregistrer / rejoindre son domicile |
| `/dt ptravel <joueur>`, `/dt ptoggle` | Voyage vers un joueur / confidentialité |
| `/dt status`, `/dt stop` | Progression / retour au départ |
| `/dt setstat <nom>`, `/dt remstat <nom>` | Gérer les stations (admin) |
| `/dt createflight <nom>` | Ouvrir l'éditeur d'itinéraire (admin) |
| `/dt setwp`, `/dt remlastwp`, `/dt saveflight` | Ajouter, retirer, enregistrer les étapes (admin) |
| `/dt remflight <nom>` | Supprimer un itinéraire (admin) |
| `/dt showflight <nom>` | Nombre d'étapes |
| `/dt flight <route> <joueur>` | Envoyer un joueur, soumis à ses permissions (admin) |
| `/dt addstatdragon <nom> [nom-affiché]` | Dragon décoratif (admin) |
| `/dt remstatdragon <nom>` | Retirer un dragon décoratif (admin) |
| `/dt ctravel <x> <y> <z> [monde]` | Voyage par coordonnées (permission dédiée) |
| `/dt reload` | Recharger la configuration (admin) |
| `/dt importlegacy` | Importer stations et itinéraires historiques (admin) |

Les noms techniques acceptent 1 à 32 lettres ASCII, chiffres, `_` ou `-`. Pour un itinéraire, la dernière étape doit être un atterrissage sûr ; les étapes intermédiaires peuvent être dans les airs. Un itinéraire reste dans un seul monde. Les voyages intermondes sont désactivés par défaut ; lorsqu'activés, une transition explicite vers le spawn du monde cible précède le vol local.

## Permissions et tarifs

`dragontravel.use`, `dragontravel.home` et `dragontravel.player` sont autorisés par défaut. `dragontravel.admin`, `dragontravel.coords` et `dragontravel.bypasscost` sont réservés aux opérateurs. `dt.admin` est reconnu pour les commandes d'administration historiques.

Par défaut, `dragontravel.destination.*` et `dragontravel.route.*` autorisent toutes les destinations. Pour restreindre un joueur, retirer le wildcard dans le gestionnaire de permissions, puis accorder `dragontravel.destination.village` ou `dragontravel.route.tour`.

Le mode `ITEM` consomme uniquement les ressources sans nom/enchantement/métadonnées dans les 36 emplacements de stockage. Le mode `VAULT` refuse le voyage si le fournisseur d'économie manque ou refuse le paiement. Une interruption technique rembourse ; une annulation volontaire garde le tarif. Le journal protège la position du joueur après crash ; il ne constitue pas un journal bancaire et ne garantit pas un remboursement après coupure brutale du processus. L'intégration Vault demande un fournisseur installé et n'est pas équivalente aux tests en émeraudes de la matrice.

## Migration et différences avec l'ancien plugin

L'ancien code et son `pom.xml` sont conservés dans `legacy/`. L'ancien dossier `plugins/DragonTravel/databases/` est lu par `/dt importlegacy` sans être modifié. L'import reprend `Stations` et les `Flights` au format `x%y%z%world`, sans écraser les noms déjà présents. Les domiciles historiques par nom ne sont pas automatiquement attribués à des UUID : utiliser `/dt sethome`.

La reprise porte sur le voyage à dos de dragon, les stations, routes, domiciles, panneaux et dragons décoratifs. Les anciennes intégrations spécifiques Factions, Towny, Heroes et anticheat, la compatibilité binaire de l'ancienne API et toutes les anciennes options/permissions ne sont pas reproduites. Un addon peut utiliser l'événement de pré-vol et l'API Reforged. Ne pas installer deux JAR DragonTravel simultanément.

## Construction et tests reproductibles

Maven : `mvn verify` avec un JDK moderne ; bytecode Java 8. Aucune dépendance NMS liée à une version. ASM est inclus et isolé dans le JAR ; l'API serveur reste fournie par le serveur.

Sur Windows, les scripts Python utilisent Python 3.9+ et Node.js pour les clients :

```powershell
python tools/bootstrap.py
./tools/build.ps1
python tools/prepare_servers.py --all
python tools/cache_vanilla.py
New-Item -ItemType Directory -Force .tools/protocol
Copy-Item tools/protocol-package.json .tools/protocol/package.json
npm install --prefix .tools/protocol
python tools/run_matrix.py --workers 3 --crash
```

`bootstrap.py` installe les JDK 8, 16, 17, 21, 25 et Maven dans `.tools/`, avec vérification des sommes de contrôle. La matrice sélectionne automatiquement le Java approprié. `prepare_servers.py` télécharge les builds publiés par Paper et vérifie SHA-256. Les préversions avec suffixe sont exclues de `--all`; un build bêta portant une version sans suffixe reste identifié dans le rapport.

Les tests démarrent des serveurs temporaires **uniquement sur 127.0.0.1**, avec mondes de test propres, autorisent leur EULA pour cette exécution et arrêtent les processus après les contrôles. Ils ne touchent aucun serveur personnel. Le JAR de production désactive les commandes de test : celles-ci exigent une console et `-Ddragontravel.reforged.test=true`.

Les tests serveur contrôlent la vraie entité, la monture, la suppression de l'IA, le déplacement, les sauvegardes, des voyages avec un joueur connecté, les tarifs, annulations et remboursements, puis un redémarrage et une reconnexion. Avec `--crash`, le processus Java est tué pendant un quatrième vol réel, avant la vérification du journal au redémarrage. Le client réseau confirme aussi les paquets de création du dragon, de monture et de mouvement. Les commandes de voyage sont exercées via leur gestionnaire réel avec le joueur connecté ; le dispatch console vérifie l'enregistrement de `/dt`. Ce client ne rend pas de fenêtre Minecraft : la fluidité visuelle, le confort de caméra et les interactions avec les plugins d'un serveur réel restent à valider en jeu.

Les tests n'attestent pas d'une compatibilité universelle avec tous les plugins tiers, d'une charge de production ou des logiciels Spigot/CraftBukkit différents des builds Paper testés. Les diagnostics et builds exacts sont enregistrés, pas une extrapolation depuis le numéro de version.

## Licence et attribution

Licence BSD à deux clauses, conservée dans `LICENSE.md` et dans le JAR. DragonTravel original : Philipp « Phiwa » Vesper et Oliver Hayes (« Seyah »). Réimplémentation Reforged : hash014, 2026. Le source historique reste disponible dans ce fork.
