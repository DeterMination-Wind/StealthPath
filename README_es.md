# Ruta Sigilosa (Mindustry Mod)

Calcula y dibuja rutas más seguras / de menor daño usando el alcance de torretas y unidades enemigas.

## Uso

### Teclas

- `X` (`sp_path_turrets`): solo torretas (mantener = vista previa en vivo)
- `Y` (`sp_path_all`): torretas + unidades (mantener = vista previa en vivo)
- `K` (`sp_mode_cycle`): tocar para alternar el modo de visualización
- `L` (`sp_threat_cycle`): alternar filtro de amenaza (tierra/aire/ambos)

### Modos

Toca `K` para alternar entre 3 modos:

1) Modo de objetivo normal (se elige en ajustes)
2) Grupos de generadores enemigos (dibuja varias rutas)
3) Jugador -> posición del ratón

## Ajustes

Abrir: `Settings → Stealth Path`

- Segundos de visualización de la ruta
- Grosor de línea
- Intervalo de actualización de vista previa (al mantener X/Y)
- Modo de generadores: color, cantidad, tamaño mínimo, iniciar desde el jugador
- Modo jugador->ratón: color

## Multijugador

Este mod es solo de cliente (overlay). `mod.json` usa `"hidden": true` para evitar comprobaciones de mods distintos entre servidor/cliente.

## Compilar

Desde la raíz del repo `Mindustry-master`:

```powershell
./gradlew.bat stealth-path:jar
```

Salida: `mods/stealth-path/build/libs/stealth-path.zip`
