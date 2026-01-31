# Chemin furtif (Mindustry Mod)

Calcule et dessine les chemins les plus sûrs / les moins dangereux en utilisant la portée des tourelles et unités ennemies.

## Utilisation

### Raccourcis

- `X` (`sp_path_turrets`) : tourelles seulement (maintenir = aperçu en direct)
- `Y` (`sp_path_all`) : tourelles + unités (maintenir = aperçu en direct)
- `K` (`sp_mode_cycle`) : appuyer pour changer le mode d'affichage
- `L` (`sp_threat_cycle`) : changer le filtre de menace (sol/air/les deux)

### Modes

Appuyez sur `K` pour faire défiler 3 modes :

1) Mode cible normal (choisi dans les paramètres)
2) Groupes de générateurs ennemis (dessine plusieurs chemins)
3) Joueur -> position de la souris

## Paramètres

Ouvrir : `Settings → Stealth Path`

- Secondes d'affichage du chemin
- Épaisseur de ligne
- Intervalle d'actualisation de l'aperçu (en maintenant X/Y)
- Mode générateurs : couleur, quantité, taille minimale, démarrer depuis le joueur
- Mode joueur->souris : couleur

## Multijoueur

Ce mod est uniquement côté client (overlay). `mod.json` utilise `"hidden": true` pour éviter les vérifications de différence de mods serveur/client.

## Compiler

Depuis la racine du dépôt `Mindustry-master` :

```powershell
./gradlew.bat stealth-path:jar
```

Sortie : `mods/stealth-path/build/libs/stealth-path.zip`
