# Mission Lab

Juego 2D pixel art para el reto Space Mission Design Game. El equipo recibe un encargo científico, monta un satélite en un hangar, pasa la revisión de vuelo, lo lanza, lo opera desde Mission Control y ve la foto que consiguió. De 1 a 4 jugadores en una sola pantalla.

## Arrancar

Requisitos: Java JDK 17 o más nuevo. Gradle no hace falta instalarlo; el proyecto lo descarga solo la primera vez (necesita internet).

| Para | Comando (en Windows usar `gradlew.bat` en vez de `./gradlew`) |
| --- | --- |
| Jugar | `./gradlew lwjgl3:run` |
| Correr las pruebas | `./gradlew :core:test` |
| Revisar el balance | `./gradlew :core:balance` |
| Crear el juego en un solo archivo | `./gradlew lwjgl3:jar` y luego `java -jar lwjgl3/build/libs/MissionLab.jar` |

En Mac, el archivo empaquetado se abre con `java -XstartOnFirstThread -jar lwjgl3/build/libs/MissionLab.jar`. Con `lwjgl3:run` no hace falta.

En IntelliJ: File > Open, elegir esta carpeta y aceptar Gradle.

## Controles

| Jugador | Papel (con 2+ jugadores) | Moverse | Acción |
| --- | --- | --- | --- |
| 1 | Scientist | W A S D | E |
| 2 | Engineer | Flechas | Enter |
| 3 | Launch lead | I J K L | O |
| 4 | Ground control | Teclado numérico 8 4 5 6 | Teclado numérico 0 |

- En una consola (mapa de órbitas, consola del cohete, mesa de estaciones): izquierda/derecha cambian la opción; acción o Esc la cierran.
- En Mission Control: izquierda = IMAGE (Scientist), derecha = DOWNLINK (Ground control), mantener acción = avanzar el tiempo más rápido, Esc = terminar.
- Esc dos veces en el hangar vuelve al inicio.

## Cómo se juega: las cinco fases

Una barra arriba de cada pantalla (en el hangar, abajo) dice en qué fase está el equipo.

1. **Briefing** (dos páginas). La pregunta científica, la zona en un mapa real, el presupuesto, la fecha del cohete, datos reales de NASA y, en la segunda página, **cómo se gana**: éxito mínimo, completo y ejemplar con números concretos.
2. **Build & review** (hangar). Llevar piezas al plano, votar los problemas de las pruebas y usar las tres consolas. La barra de abajo dice siempre el **siguiente paso** y el estado de la **revisión de vuelo**. La plataforma solo deja lanzar cuando ningún sistema está en NO-GO.
3. **Launch**. Cuenta atrás, despegue, cada tramo de la ruta, paneles y primer contacto, con la hora de la misión.
4. **Mission Control**. Un día de operaciones en órbita: mandar IMAGE para la pasada sobre el objetivo y DOWNLINK para cada contacto con una estación, vigilando la batería.
5. **Results**. La foto, la medalla, los criterios de éxito con los números conseguidos y qué mejorar. La pantalla de inicio guarda la mejor medalla de cada misión.

## Los dos encargos

Tienen exactamente la misma estructura; solo cambian los números. La Mission 1 es la de aprender; la Mission 2 obliga a equilibrar más.

| | Mission 1: Amazon Fire Alarm | Mission 2: Greenland Ice Watch |
| --- | --- | --- |
| Objetivo | Incendios en la Amazonía, **de noche** | Frentes de glaciares en Groenlandia, **de día** |
| Presupuesto | $700k | $1.1M |
| Cohete sale en | 14 meses | 12 meses |
| Vida necesaria | 1 año | 3 años |
| Detalle pedido | píxeles de 100 m o menos | píxeles de 15 m o menos |
| Lección principal | de noche hace falta infrarrojo térmico, y la cámara gasta batería en la sombra | el detalle fino pide telescopio, puntería precisa, un 6U y mucho enlace de datos |

## Niveles de éxito (como en NASA)

| Nivel | Medalla | Condición |
| --- | --- | --- |
| Minimum success | Bronze | Foto del objetivo con la luz correcta, al menos 50% recibida, píxeles como mucho el doble de lo pedido |
| Full success | Silver | Píxeles iguales o mejores que lo pedido, al menos 90% recibida, el satélite dura toda la misión |
| Exemplary | Gold | Full success y nota de 90 o más |

## Las cuentas (`sim/Budgets.java`)

Todas salen del mismo sitio: barras del hangar, revisión de vuelo, simulador y pantalla final.

| Variable | Cómo se calcula |
| --- | --- |
| Peso | piezas + estructura (+ vela de frenado si la órbita duraría más que misión + 5 años). Límite: el menor entre la estructura (2 kg por U) y el vehículo más pequeño de la ruta |
| Volumen | piezas + 0.5U del ordenador; tiene que caber en la estructura (2U, 3U o 6U) |
| Energía | consumo medio = ordenador 2.5 W + orientación + cámara × 20% del tiempo + radio × parte del día en contacto. Generación = paneles × parte de cada vuelta al sol |
| Batería | en la sombra: (ordenador + orientación) × minutos de sombra; de noche, además, la cámara. Solo se usa el 25% de la batería en cada vuelta (profundidad de descarga) |
| Datos | foto del día (GB) frente a Mbps × minutos de contacto × 60 / 8000 |
| Presupuesto | piezas + $150k de ordenador y pruebas + ruta + estación × años + arreglos |
| Tiempo | 3 meses de montaje y pruebas + la pieza que más tarda (se compran a la vez) + retrasos. Hay que entregarlo antes del lanzamiento lo que pida la ruta (el viaje gratis de NASA, 4 meses) |
| Resolución | altura (km) × IFOV de la cámara (µrad) / 1000 = metros por píxel |

**Revisión de vuelo** (`sim/Readiness.java`): 13 preguntas GO / WARN / NO-GO. Bloquean el lanzamiento: presupuesto, calendario, estructura (peso o volumen) y vehículo. Lo demás avisa pero deja volar.

**Nota sobre 100** (`sim/Score.java`): ciencia 50 + márgenes 20 (5 por cada margen de al menos 10% en peso, energía y batería, y un mes de holgura) + presupuesto 15 (completo con 10% de reserva) + fiabilidad 15 (menos 3 por cada riesgo aceptado).

## Física de órbitas y Mission Control

`sim/OrbitMechanics.java` calcula con física real simplificada: periodo (Kepler), velocidad, traza sobre la Tierra que gira, eclipses con el ángulo beta de la estación del año, círculo de cobertura de cada antena (10° sobre el horizonte) y contactos al día. Por eso Svalbard oye a un satélite polar casi en cada vuelta y nunca a uno en la órbita de la ISS.

`sim/MissionOps.java` juega un día en órbita sin gráficos. Las órdenes entran en una cola y se ejecutan cuando llega su momento. Para puntuar con justicia, un piloto automático perfecto juega el mismo día con el mismo satélite: la nota de las operaciones es lo que bajó el equipo comparado con él.

## Las consolas del hangar

- **Orbit map**: mapa real de la Tierra con la zona del encargo, la noche, la traza de un día y de la vuelta siguiente, el satélite moviéndose, la cobertura de las antenas elegidas y la telemetría (altura, inclinación, periodo, velocidad, sombra, pasadas útiles al día, vida, resolución con la cámara montada).
- **Rocket console**: el grafo de vuelo dibujado con el precio de cada arista para el peso actual, la ruta elegida, la más barata (Dijkstra) y las que no pueden con el peso; la lista de rutas con su fecha de entrega, y la revisión de vuelo completa con el veredicto LAUNCH: GO / NO-GO.
- **Ground station desk**: las antenas en el mapa con los tramos del día en que hay contacto, cada red con su precio, pasadas y minutos al día, el presupuesto de datos con la fórmula, y la batería a lo largo de dos vueltas con el límite seguro.

## Estructura del código

| Carpeta | Qué hay | Depende de libGDX |
| --- | --- | --- |
| `core/.../ds` | Las dos estructuras de datos: grafo dirigido ponderado y cola | No |
| `core/.../model` | Piezas, órbitas, vehículos y grafo de vuelo, rutas, estaciones, encargos, problemas, diseño | No |
| `core/.../data/Catalog.java` | **Todo el contenido y los números del juego** | No |
| `core/.../data/EarthMask.java` | Mapa de tierra y mar a 1° (generado, no hace falta tocarlo) | No |
| `core/.../sim` | Cuentas, revisión de vuelo, física de órbitas, simulación, Mission Control, puntuación, balance | No |
| `core/.../hangar` | Reglas del hangar: moverse, cargar, instalar, votar, consolas, lanzar | No |
| `core/.../game` | Paleta, teclas, carga de archivos, mapa del mundo, fotos provisionales | Sí |
| `core/.../screens` | Las seis pantallas y las tres consolas | Sí |
| `lwjgl3` | Arranque en escritorio | Sí |
| `assets` | Sprites, fotos, sonidos, fuente | Archivos |

`ds`, `model`, `sim` y `hangar` no dibujan nada y tienen pruebas en `core/src/test`. Si se cambia una regla, correr `./gradlew :core:test`.

## Estructuras de datos

El juego se apoya en dos estructuras hechas a mano, en `core/.../ds`. Las dos mueven reglas del juego; no están de adorno.

### Grafo dirigido y ponderado: el mapa de vuelo (`DirectedWeightedGraph`)

- **Vértices** (`Waypoint`): la base de lanzamiento y cada órbita.
- **Aristas** (`Transfer`): un vehículo que lleva de un sitio a otro (viaje gratis de NASA a la ISS, despliegue pagado desde la ISS, viaje compartido a 550 km, viaje compartido a 800 km, remolcador de 550 a 800 km). Son dirigidas porque los viajes solo suben.
- **Pesos**: dinero fijo + dinero por kilo, peso máximo, horas de viaje y meses de entrega.
- **Camino** (`Route`): la ruta que elige el Launch lead, por ejemplo `Launch site > [Rideshare] > 550 km > [Space tug] > 800 km`.

Quién lo usa: la consola del cohete (Dijkstra propone la más barata; las aristas que no pueden con el peso cuentan como infinitas, así que la mejor ruta cambia mientras se montan piezas), `Budgets` (coste y límite de peso), `Readiness` (vehículo y fecha de entrega), `Simulator` (recorre el camino tramo a tramo y avisa si había una ruta más barata) y `Balance` (prueba todas las rutas). Para añadir un viaje, basta con añadir una línea a `FLIGHTS` en `Catalog.java`.

### Cola FIFO: lo que pasa en orden (`LinkedQueue`)

Una cola hecha con nodos enlazados: `enqueue` añade al final y `dequeue` saca del principio, las dos en O(1). Se usa tres veces:

- **Campaña de pruebas del hangar** (`Session`): las pruebas salen en el orden programado; si una necesita una pieza que todavía no está, vuelve al final de la cola.
- **Línea de tiempo del lanzamiento** (`Simulator` → `LaunchScreen`): el simulador encola cada suceso con su hora de misión y la pantalla los saca uno a uno.
- **Secuencia de órdenes a bordo** (`MissionOps`): IMAGE y DOWNLINK esperan su turno, como en un satélite real. Si DOWNLINK va primero, la foto espera aunque pase el objetivo: el orden importa.

Por qué una cola y no otra estructura lineal: todo esto se produce en orden y se consume en ese mismo orden, sin saltarse nada ni mirar en medio. Una pila lo daría al revés, un array obligaría a llevar índices a mano y una lista enlazada sin más dejaría insertar en cualquier sitio.

## Cambiar números o textos

Todo está en `Catalog.java`. Después de cambiar algo, correr `./gradlew :core:balance`. Debe terminar con `BALANCE OK`, que comprueba en cada encargo:

1. Al menos un 25% de los diseños pasa la revisión de vuelo.
2. El diseño con la pieza más barata de cada hueco se puede lanzar.
3. Al menos 10 diseños llegan al éxito completo, con al menos 4 combinaciones distintas de órbita, radio y estación.
4. El éxito mínimo se consigue con menos del 70% del presupuesto, y el completo con menos del 95%.
5. Ningún diseño llega a 100.
6. Cada pieza sirve para algo: en algún encargo hay un diseño de 60 o más que la usa.

## Añadir arte, fotos y sonidos

El juego funciona sin ningún archivo: dibuja cajas de colores y fotos provisionales. Cada archivo que se añade sustituye al provisional automáticamente, sin tocar código.

- `assets/sprites/<id>.png`: una pieza (por ejemplo `cam_sharp.png`; los ids están en `Catalog.java`). Se dibuja a 12 × 12 px. Personajes: `player_0.png` a `player_3.png`, 8 × 10 px.
- `assets/photos/greenland.png`, `amazon_fire.png` y `off_target.png` (océano): las fotos reales de NASA. Al cambiarlas, poner su crédito en `photoCredit` dentro de `Catalog.java`.
- `assets/sounds/`: `pickup`, `place`, `click`, `deny`, `alert`, `launch`, `success`, `fail` (`.wav`, `.ogg` o `.mp3`).
- `assets/fonts/pixel.fnt`: fuente pixel en formato BMFont (se puede generar con Hiero, la herramienta de libGDX).

La paleta de colores está en `game/Palette.java`. Usar solo esos colores en los sprites.

## Fuentes y créditos

- Las fases siguen el proceso de NASA: diseño, construcción y pruebas, revisión de preparación para el vuelo, lanzamiento y operación.
- Los problemas de construcción vienen de las pruebas descritas en *CubeSat 101* (NASA CubeSat Launch Initiative).
- Tamaños y pesos de estructura: CubeSat Design Specification rev. 14.1 (Cal Poly): hasta 2 kg por U.
- Viaje gratis desde la ISS: NASA CubeSat Launch Initiative (CSLI). Precio por kilo de los viajes compartidos: del orden de los Transporter de SpaceX.
- Vela de frenado: regla de la FCC (2022) de bajar los satélites de órbita baja como mucho 5 años después de terminar la misión.
- Incendios: VIIRS (Suomi NPP, NOAA-20), píxeles de 375 m, pasadas hacia la 1:30. Glaciares: Landsat 8 y 9 (15 a 30 m); retroceso de Jakobshavn de NASA Earth Observatory.
- Estaciones: Svalbard (KSAT) ve a los satélites polares en casi todas las vueltas.
- Mapa de tierra y mar: máscara NOAA/GLOBE a través del paquete `global-land-mask`.
- Fotos: buscar en NASA Worldview o NASA Earth Observatory. Revisar las condiciones de uso de cada imagen y poner su crédito.
- Si se usa un paquete de arte gratuito, revisar su licencia y citarlo.
- Los números del juego están simplificados para enseñar; no son especificaciones reales.
