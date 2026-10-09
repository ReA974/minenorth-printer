# MineNorth Printer (Forge 1.20.1)

Imprimante à billets avec interface graphique, réservoir d'encre, batterie et améliorations.
Les billets sortent avec les items de **minenorth_eurobank** (plus de Bubustein).

## Compiler
```
gradlew build
```
Le jar est dans `build/libs/`. À mettre côté **serveur et client** (l'interface est côté client).

## En jeu
- Clic droit sur l'imprimante : ouvre l'interface (jauges encre/énergie, progression en direct, temps restant, gains, total imprimé).
- Slot cartouche / slot batterie : la recharge se vide progressivement dans l'imprimante (barre de durabilité visible sur l'item).
- 4 slots d'amélioration (un par type) : empiler les items pour monter de niveau (max configurable, 3 par défaut).
  - Vitesse : −20 % de temps / niveau
  - Réservoir d'encre : +1000 de capacité / niveau
  - Rendement : +25 € par impression / niveau
  - Efficacité : −20 % d'énergie / niveau
- 6 slots de sortie pour les billets. Bac plein = l'imprimante se met en pause.
- Bouton Marche/Arrêt. Statuts : Impression, Arrêt, Plus d'encre, Batterie vide, Bac plein, Erreur config.

## Items
`minenorth_printer:printer`, `ink_cartridge`, `ink_cartridge_large`, `battery`, `battery_large`,
`upgrade_speed`, `upgrade_ink`, `upgrade_yield`, `upgrade_efficiency`.
Pas de recette : à vendre en boutique / donner en commande (`/give @p minenorth_printer:ink_cartridge`).

## Config
`world/serverconfig/minenorth_printer-server.toml` (généré au 1er lancement, synchronisé aux clients).
Durée d'impression, consommations, capacités, contenance des recharges, effet des améliorations,
`onlyOwnerCanOpen`, son, et la liste des billets (`money.denominations`, format `modid:item=valeur`).

## Son
Quand l'imprimante tourne, elle émet un bruit de fonctionnement en boucle (moteur + tête d'impression),
audible à ~16 blocs, avec fondu à l'allumage/arrêt. Réglable dans la config : `runningSound`, `runningSoundVolume`.
Pour mettre ton propre son : remplace `assets/minenorth_printer/sounds/printer_running.ogg` (OGG mono, boucle propre).

## Licence

**Tous droits réservés - MineNorthRP.** Réutilisation, copie, modification, décompilation / ingénierie
inverse (y compris par outils d'intelligence artificielle) et utilisation pour entraîner une IA sont
**interdites** sans autorisation écrite. Voir [LICENSE](LICENSE).
