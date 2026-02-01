# Ruta Sigilosa (Mindustry Mod)

Calcula y dibuja rutas más seguras / de menor daño usando el alcance de torretas y unidades enemigas.

Versión actual: `2.0.7`

## Uso

### Teclas

- `X` (`sp_path_turrets`): solo torretas (mantener = vista previa en vivo)
- `Y` (`sp_path_all`): torretas + unidades (mantener = vista previa en vivo)
- `N` (`sp_auto_mouse`): modo automático (grupo → ratón)
- `M` (`sp_auto_attack`): modo automático (grupo → <Attack>(x,y))
- `K` (`sp_mode_cycle`): tocar para alternar el modo de visualización
- `L` (`sp_threat_cycle`): alternar filtro de amenaza (tierra/aire/ambos)

### Modos automáticos

- Usa el centro del grupo de unidades (las unidades seleccionadas si existen; si no, tu unidad actual) y detecta aire/tierra/mixto para calcular una ruta de menor daño (modo Y: torretas + unidades).
- Si las unidades seleccionadas están separadas en varios grupos con una distancia mayor que el umbral configurado (por defecto 5 casillas), calcula y dibuja una ruta para cada grupo por separado.
- Mientras sigue una orden de movimiento automático, el mod actualiza los puntos RTS cuando cambia la mejor ruta (por ejemplo, si una torreta empieza a disparar).
- Objetivo por chat: envía `"<Attack>(x,y)"` en el chat (x,y son coordenadas de casilla). Solo se analiza tu propio mensaje (cliente).

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
- Modo Pro: desbloquea el menú de ajustes avanzados
- Ajustes avanzados (Modo Pro): parámetros de búsqueda de ruta/movimiento automático/caché/grupos de generadores
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
