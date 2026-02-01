# Chemin furtif (Mindustry Mod)

Calcule et dessine les chemins les plus sûrs / les moins dangereux en utilisant la portée des tourelles et unités ennemies.

Version actuelle : `2.0.6`

## Utilisation

### Raccourcis

- `X` (`sp_path_turrets`) : tourelles seulement (maintenir = aperçu en direct)
- `Y` (`sp_path_all`) : tourelles + unités (maintenir = aperçu en direct)
- `N` (`sp_auto_mouse`) : mode auto (groupe → souris)
- `M` (`sp_auto_attack`) : mode auto (groupe → <Attack>(x,y))
- `K` (`sp_mode_cycle`) : appuyer pour changer le mode d'affichage
- `L` (`sp_threat_cycle`) : changer le filtre de menace (sol/air/les deux)

### Modes automatiques

- Utilise le centre du groupe d’unités (unités sélectionnées si présentes ; sinon votre unité) et détecte air/sol/mixte pour calculer un chemin avec le moins de dégâts (mode Y : tourelles + unités).
- Si les unités sélectionnées sont séparées en plusieurs groupes avec un écart supérieur au seuil configuré (par défaut 5 cases), il calcule et dessine un chemin pour chaque groupe séparément.
- Pendant l’exécution d’un déplacement automatique, le mod met à jour les points RTS quand le meilleur chemin change (par exemple si une tourelle commence à tirer).
- Cible via chat : envoyez `"<Attack>(x,y)"` dans le chat (x,y = coordonnées de case). Seul votre propre message est analysé (côté client).

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
- Mode Pro : déverrouille le menu de paramètres avancés
- Paramètres avancés (Mode Pro) : réglages de recherche de chemin/déplacement auto/cache/groupes de générateurs
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
