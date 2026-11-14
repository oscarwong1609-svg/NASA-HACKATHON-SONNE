# Space Mission Design Game: plan de construcción en 24 horas

> Reto: **Space Mission Design Game**, NASA Space Apps Challenge 2026.
> Título provisional del juego: **Mission Lab**.

---

## 1. El juego

Diriges una pequeña agencia espacial. Recibes un **encargo científico**, construyes un satélite para cumplirlo en un **hangar pixel-art**, lo lanzas y ves **la foto real de NASA** que tu satélite habría tomado.

- Un buen diseño devuelve una foto **nítida**.
- Un mal diseño devuelve una foto **borrosa, cortada o inexistente**, y el juego explica qué decisión lo causó.

Es un juego **2D pixel-art para 1 a 4 jugadores en una sola pantalla**. Los jugadores caminan por el hangar, toman piezas de las estanterías y las llevan a un plano (blueprint). Cada pieza tiene **peso, consumo de energía, datos generados y precio**.

### Por qué destaca

- **El resultado es una imagen real de NASA.** El jugador ve la calidad de su ingeniería en lugar de leer una puntuación.
- **El trabajo en equipo es físico.** Las piezas pesadas las tienen que cargar dos jugadores, y los cuatro roles quieren cosas distintas de un mismo presupuesto.
- **Las etapas siguen el proceso real de NASA:** encargo → diseño → construcción y pruebas → lanzamiento → operación → resultado.

---

## 2. Ideas: qué entra, qué espera y qué se corta

8 ideas entran en las 24 horas, 3 esperan a que el núcleo funcione y 4 se cortan.

| Idea | Lo que vive el jugador | Decisión |
|---|---|---|
| Encargo científico | *"Unos científicos quieren saber cuánto hielo pierde Groenlandia. Tienes $500.000 y el cohete sale en 12 meses."* | ✅ Se construye |
| Hangar y plano | Caminar a una estantería, tomar una pieza y llevarla a un hueco del plano | ✅ Se construye |
| Cada pieza es distinta | Peso, energía, datos, precio y una descripción de una línea en cada pieza | ✅ Se construye |
| Piezas pesadas para dos | Un tanque grande o una cámara grande solo se mueven si los cargan dos jugadores | ✅ Se construye |
| Indicadores en vivo y báscula | Las barras de peso, energía, datos y dinero cambian al encajar cada pieza | ✅ Se construye |
| Problemas de construcción | *"Se soltó una antena en la prueba de vibración. ¿Pagas para arreglarla o te arriesgas?"* | ✅ Se construye |
| Lanzamiento por etapas | Despegue, órbita, primer contacto, pasada de la foto. El fallo aparece en la etapa que lo causó | ✅ Se construye |
| Foto real de NASA como resultado | La imagen guardada de NASA aparece nítida, borrosa, cortada o en negro, con el motivo | ✅ Se construye |
| Cuenta atrás del lanzamiento | Los meses avanzan mientras construyes; si no llegas, el cohete se va sin ti | ⏳ Si vamos adelantados en la hora 14 |
| Parche de misión y tarjeta de misión real | Un parche pixel-art y *"tu diseño se parece a esta misión real de NASA"* | ⏳ Si vamos adelantados en la hora 14 |
| Tercer encargo | Una selva nublada que una cámara normal no puede atravesar | ⏳ Si vamos adelantados en la hora 14 |
| Otros planetas | Destinos en Marte y Venus | ❌ Cortado: no hay foto equivalente y duplica el contenido |
| Minijuego de operación en vivo | Disparar la foto a mano sobre el objetivo | ❌ Cortado: caro y enseña poco |
| Encargos generados sin fin | Una misión aleatoria nueva cada vez | ❌ Cortado: los encargos hechos a mano son más fáciles de equilibrar |
| Juego en línea | Cada jugador en su propio ordenador | ❌ Cortado: una pantalla compartida es mucho más barata y mejor para la demo |

### Los dos encargos que se construyen primero

1. **Hielo de Groenlandia (fácil):** objetivo de día, cerca del polo. Enseña que **la órbita tiene que pasar sobre el objetivo**.
2. **Incendio nocturno en la Amazonía (medio):** de noche una cámara normal no ve nada; la cámara térmica sí funciona, pero pesa más y consume más. Enseña que **el instrumento depende de la pregunta científica**.

---

## 3. Una partida

Una partida dura **de 5 a 8 minutos**. Esto es lo que ve el jugador:

1. **Pantalla de título.** Se eligen de 1 a 4 jugadores. Cada jugador elige un rol y un personaje de color.
2. **Llega el encargo.** Un mensaje en la pantalla del hangar con la pregunta científica, el lugar, el presupuesto y la fecha de lanzamiento. Un mapa pequeño muestra el objetivo.
3. **Elegir la órbita.** En el mapa de pared, el jugador de lanzamiento elige una de tres órbitas. Se dibuja una línea sobre la Tierra para que todos vean si pasa sobre el objetivo.
4. **Construir en el hangar.** El plano tiene seis huecos: **instrumento, energía, batería, orientación, radio y estructura**. Los jugadores llevan piezas de las estanterías a los huecos. Al acercarse a una pieza se ve su tarjeta. Los indicadores se mueven con cada pieza colocada y pasan a ámbar y luego a rojo.
5. **Aparece un problema.** Una o dos veces por construcción falla una prueba o se retrasa un proveedor. El equipo elige: **pagar, perder tiempo o aceptar el riesgo**.
6. **Pesaje y cohete.** El satélite terminado va a la báscula. El jugador de lanzamiento elige un cohete capaz de llevar ese peso a esa órbita. El viaje compartido barato solo va a una órbita.
7. **Lanzamiento.** Todos mantienen pulsado su botón de acción para iniciar la cuenta atrás. El cohete despega con la pantalla temblando y sonido.
8. **La misión se desarrolla.** Cuatro etapas se iluminan por turnos: **despegue, órbita alcanzada, primer contacto por radio, pasada de la foto**. Un riesgo aceptado en el paso 5 puede aparecer aquí.
9. **La foto.** Aparece la imagen real de NASA del objetivo, con la calidad que se ganó el diseño.
10. **Informe final.** Puntuación, las tres decisiones que más pesaron, una sugerencia y un botón **"Intentar de nuevo"** que vuelve al hangar con el mismo diseño.

> **Ejemplo de partida fallida:** el equipo instala la mejor cámara y la radio pequeña. Todo despega bien. La foto llega como una franja estrecha, y el informe dice: *"Tu cámara grabó 12 GB al día, pero tu radio solo podía enviar 2 GB. La mayor parte de la imagen nunca llegó a la Tierra."*

---

## 4. Reglas del juego

Cada pieza tiene cinco números: **peso (kg), energía (W consumidos o producidos), datos (GB al día producidos o enviados), precio y fiabilidad**. Todos los números están simplificados para enseñar, y el juego lo dice en la pantalla de título.

### Piezas en las estanterías

| Hueco | Opciones | El compromiso |
|---|---|---|
| Instrumento | Cámara básica, cámara nítida, cámara térmica | Mejores fotos pesan más, consumen más energía y producen más datos |
| Energía | Paneles en el cuerpo, paneles desplegables | Los desplegables dan más energía, pero pueden atascarse al abrirse |
| Batería | Pequeña, grande | La grande aguanta el lado oscuro de la órbita, pero es pesada (dos jugadores) |
| Orientación | Imanes, ruedas de reacción | Las ruedas mantienen la cámara estable; los imanes son baratos e inestables |
| Radio | Lenta, media, rápida | Las radios rápidas envían más datos y consumen más energía |
| Estructura | Marco pequeño, mediano, grande | Un marco más grande carga más peso y cuesta más lanzarlo |
| Órbita (mapa de pared) | Baja ecuatorial, baja polar, alta polar | Las polares llegan a Groenlandia; las altas duran más pero hacen la foto más borrosa |
| Cohete (plataforma) | Viaje compartido, cohete pequeño propio | El compartido es barato, pero va a una sola órbita en una sola fecha |
| Estación terrena (escritorio) | Una estación, tres estaciones | Más estaciones dan más minutos de contacto, con un costo anual |

### Cómo el diseño se convierte en foto

| Lo que ve el jugador | Causa | Lección |
|---|---|---|
| Foto nítida y completa | Todo encaja | Un diseño equilibrado gana a la mejor pieza suelta |
| Foto borrosa | Orientación inestable, cámara básica u órbita demasiado alta | Las piezas dependen unas de otras |
| Foto cortada en una franja | La radio o las estaciones no pueden enviar todos los datos | Un gran sensor no sirve sin un buen enlace |
| Foto en negro | Cámara normal de noche, o la batería murió en la oscuridad | El instrumento depende de la pregunta; los eclipses importan |
| Foto del lugar equivocado | La órbita nunca pasa sobre el objetivo | La órbita depende del objetivo |
| Sin foto, el cohete cae | Demasiado peso para el cohete y la órbita elegidos | La capacidad de lanzamiento depende de la órbita |
| Sin foto, silencio | Se cumplió un riesgo aceptado, como la antena suelta | Las pruebas existen por algo |
| No hay lanzamiento | Por encima del presupuesto, o sin terminar a la fecha | El dinero y el tiempo también son recursos |

### Puntuación sobre 100

| Parte | Puntos | Se basa en |
|---|---|---|
| Foto | 60 | Nitidez, completitud, lugar y momento correctos |
| Márgenes | 15 | Entre 10% y 30% de peso y energía de sobra. Quedarse sin margen o pasarse mucho restan puntos |
| Dinero | 10 | Presupuesto sobrante, con tope para que construir de menos nunca gane |
| Fiabilidad | 15 | Riesgos arreglados en vez de aceptados; terminar antes de la fecha de lanzamiento |

**Objetivo de equilibrio:** en cada encargo, al menos **tres diseños distintos** superan los 80 puntos, y **ninguno llega a 100**.

---

## 5. Juego en equipo

Hasta cuatro jugadores comparten una pantalla y un teclado (con mandos si los hay). Cada rol es dueño de una parte del hangar y tiene un objetivo personal que choca con el de los demás.

| Rol | Es dueño de | Objetivo personal |
|---|---|---|
| Científico | Estantería de instrumentos | La foto más nítida posible |
| Ingeniero | Estanterías de energía, batería, orientación y estructura | Peso y energía de sobra |
| Jefe de lanzamiento | Mapa de pared y plataforma de lanzamiento | Llegar al objetivo a tiempo con el viaje más barato |
| Control en tierra | Estantería de radio y escritorio de estaciones | Que cada byte llegue a la Tierra, sin pasarse del presupuesto |

### Reglas que obligan a cooperar

- Solo el dueño puede sacar piezas de su estantería, pero cualquiera puede ayudar a cargarlas.
- Las piezas pesadas necesitan dos jugadores.
- Los problemas de construcción se deciden **votando**: los jugadores caminan al lado "Pagar" o "Arriesgar" del hangar.
- El lanzamiento solo empieza cuando **todos** mantienen pulsado su botón.

**Con menos de cuatro jugadores**, las estanterías sin dueño quedan abiertas para todos. **El modo de un jugador** es el mismo juego con todas las estanterías abiertas y sin piezas para dos personas.

El informe final muestra qué objetivos personales se cumplieron, para que el equipo tenga algo que discutir antes de la siguiente partida.

---

## 6. Plan de desarrollo

Metas: **un encargo jugable de principio a foto en la hora 10, juego en equipo en la hora 14, congelación de funciones en la hora 18.** Las horas cuentan desde que se empieza.

### Responsables

Asignar nombres según las fortalezas de cada uno. **D no necesita programar.**

| Responsable | Línea de trabajo | Entrega |
|---|---|---|
| **A** | Reglas del juego | Datos de piezas y misiones, simulación, puntuación, problemas de construcción, tests |
| **B** | Jugabilidad del hangar | Movimiento, tomar y cargar, huecos del plano, carga entre dos, indicadores |
| **C** | Pantallas y sensación | Título, encargo, mapa de órbitas, secuencia de lanzamiento, foto final, informe, sonido |
| **D** | Arte, contenido y pitch | Sprites, paleta, tarjetas de piezas, textos de problemas, imágenes de NASA guardadas, pruebas con jugadores, video, entrega |

### Calendario

| Horas | A: Reglas | B: Hangar | C: Pantallas | D: Arte y contenido |
|---|---|---|---|---|
| **0–1** | *Los cuatro juntos:* acordar este plan, la forma de los datos, la paleta y el tamaño de pantalla. Crear el repositorio. | ← | ← | ← |
| **1–4** | Datos de piezas y misiones; comprobaciones de peso, energía y presupuesto | Un jugador camina por un hangar vacío con cajas provisionales | Cambio entre pantallas; pantallas de título y de encargo | Elegir paleta y pack de assets; suelo del hangar, estanterías, sprite del jugador |
| **4–8** | Reglas de datos, órbita y calidad de foto; puntuación | Tomar, cargar y colocar en el plano; indicadores conectados a las reglas | Mapa de órbitas; secuencia de lanzamiento por etapas | Sprites y tarjetas de piezas; guardar imágenes de NASA para los dos encargos |
| **8–10** | *A, B y C:* unir todo, una partida completa en una máquina | ← | ← | Primera prueba con jugadores; anotar cada confusión |
| **10–14** | Problemas de construcción; segundo encargo | Jugadores 2 a 4; carga entre dos; estanterías por rol | Foto final con efectos de borroso, franja y negro; informe | Textos de problemas; explicaciones del informe; efectos de sonido |
| **14–18** | Equilibrar según las pruebas | Pulir movimiento y sensación al tomar piezas | Cuenta atrás o parche de misión, si vamos adelantados | Prueba completa con cuatro jugadores; guion de la demo |
| **18–21** | Solo arreglos | Solo arreglos | Solo arreglos | Grabar el video de la demo |
| **21–23** | README: cómo funcionan las reglas | Empaquetar el juego para que funcione en cualquier ordenador | Capturas de pantalla | Texto de la entrega; créditos de arte e imágenes de NASA |
| **23–24** | *Entregar, con una hora de margen* | ← | ← | ← |

### Puntos de control

- [ ] **Hora 1:** forma de los datos y paleta acordadas; el proyecto vacío funciona en todos los portátiles.
- [ ] **Hora 4:** un personaje camina por el hangar.
- [ ] **Hora 10:** un jugador completa el encargo de Groenlandia y ve una foto.
- [ ] **Hora 14:** cuatro jugadores construyen y lanzan juntos.
- [ ] **Hora 18:** congelación de funciones.
- [ ] **Hora 21:** video de la demo grabado.
- [ ] **Hora 23:** entregado.

### Dos reglas de trabajo

1. **Nadie empieza una idea "si vamos adelantados" hasta pasar el punto de control de la hora 14.**
2. **Todos trabajan con cuadrados provisionales hasta la hora 8**, para que el arte nunca bloquee al código.

---

## 7. Construcción en Java (libGDX)

Se construye en **Java con libGDX**, un framework 2D gratuito que gestiona la ventana, los sprites, la entrada y el sonido. Esta sección da por hecho que el equipo programa en Java; si no es así, hay que reescribirla para el lenguaje que usen.

### Preparación (hora 0 a 1)

1. Una persona genera el proyecto con el generador de libGDX (**gdx-liftoff**), solo plataforma de escritorio, y lo sube a GitHub.
2. Todos lo clonan y ejecutan el lanzador de escritorio. **Todos los portátiles tienen que mostrar la ventana vacía antes de que nadie escriba código del juego.**
3. Configurar el juego con una vista de **320 × 180 píxeles** escalada a la ventana, con el filtrado de texturas en **"nearest"** para que los píxeles se vean nítidos.

### Organización del código

| Paquete | Clases | Responsable | ¿Depende de libGDX? |
|---|---|---|---|
| `model` | `Part`, `Mission`, `Design`, `Orbit`, `Rocket`, `BuildEvent` | A | No |
| `sim` | `Simulator`, `SimResult`, `PhotoQuality`, `Score` | A | No |
| `data` | `parts.json`, `missions.json`, `events.json`, cargador | A y D | Solo el cargador |
| `hangar` | `Player`, `Shelf`, `BlueprintSlot`, `CarriedPart`, `Gauges` | B | Sí |
| `screens` | `TitleScreen`, `CommissionScreen`, `HangarScreen`, `LaunchScreen`, `ResultScreen` | C | Sí |
| `assets` | Sprites, fuente, sonidos, imágenes de NASA | D | Solo archivos |

Los paquetes `model` y `sim` son **Java puro, sin gráficos**. A puede probarlos con tests unitarios normales desde la primera hora, sin esperar al hangar.

### El único contrato contra el que programa todo el mundo

```java
SimResult result = Simulator.run(design, mission, seed);
// result.launched, result.stageReached,
// result.photo.sharpness (0..1), result.photo.completeness (0..1),
// result.photo.lit, result.onTarget,
// result.score, result.reasons (lista de causas en lenguaje sencillo)
```

Estos campos se acuerdan en la hora 0. **Hasta que exista el simulador real, B y C usan uno falso que devuelve valores fijos.**

### Orden de construcción

1. `Part` y `Design` con totales de peso, energía, datos y precio. *Test: añadir una pieza cambia los totales.*
2. `HangarScreen` con un `Player` que se mueve con las flechas, dibujado como un cuadrado de color.
3. `Shelf` y `BlueprintSlot`: pulsar la tecla de acción cerca de una estantería para tomar, cerca de un hueco para colocar. El hueco añade la pieza al `Design`.
4. `Gauges` lee el `Design` en cada fotograma y dibuja cuatro barras.
5. `Simulator.run` con las comprobaciones de la sección de reglas, una a una, cada una con su test.
6. `LaunchScreen` reproduce las etapas a partir de `SimResult.stageReached`.
7. `ResultScreen` dibuja la imagen de NASA:
   - **Borroso:** dibujarla en un búfer pequeño fuera de pantalla y escalarla; cuanto menor la nitidez, más pequeño el búfer.
   - **Franja:** dibujar solo la parte superior de la imagen, en la proporción de `completeness`.
   - **Negro:** oscurecerla.
8. Jugadores 2 a 4: una lista de objetos `Player`, cada uno con sus teclas. Carga entre dos: una pieza pesada solo se mueve mientras la sujetan dos jugadores.
9. Ventanas emergentes de `BuildEvent` y la votación.
10. Sustituir los cuadrados por sprites; añadir la fuente pixel y los sonidos.

### Controles

| Jugador | Moverse | Acción |
|---|---|---|
| 1 | `W A S D` | `E` |
| 2 | Flechas | `Enter` |
| 3 | `I J K L` | `O` |
| 4 | Mando, o teclado numérico `8 4 5 6` | Botón del mando, o `0` del teclado numérico |

Cuatro jugadores en un solo teclado van apretados. Conviene llevar **dos mandos USB**; libGDX los soporta con su extensión de controladores.

### Entrega del ejecutable

Un juego en Java no funciona desde un enlace web. Hay que **empaquetarlo como un único archivo ejecutable** con la tarea de build de escritorio del proyecto, **probarlo en un portátil que nunca haya tenido el código** y hacer que **el video de la demo sea la forma principal en que el jurado vea el juego**.

---

## 8. Demo y entrega

La demo muestra **una misión que falla, el juego explicando por qué, y el diseño corregido devolviendo una foto nítida**. Esa secuencia demuestra en menos de dos minutos que el juego enseña.

1. **El problema en una frase:** los estudiantes aprenden órbitas y naves por separado, nunca los compromisos entre ellas.
2. Llega el encargo. Cuatro jugadores eligen rol.
3. El científico toma la cámara más nítida. Dos jugadores cargan la batería grande. Los indicadores se ponen en rojo; el equipo lanza de todos modos.
4. La foto llega como una franja estrecha y borrosa. El informe nombra la causa.
5. Segunda partida: radio más rápida, batería más pequeña, órbita polar. La foto real de NASA de Groenlandia aparece nítida. **Puntuación: 84.**
6. **Frase de cierre:** cada etapa del juego sigue el proceso de misiones de NASA, y los problemas de construcción vienen de CubeSat 101.

En la entrega escrita hay que **dar crédito** a los recursos de NASA que dieron forma al diseño (el paper de Concept Maturity Levels, CubeSat 101, el juego de telescopios), indicar **la fuente y la fecha de cada imagen de NASA** y **la licencia de cualquier pack de arte**. Dejar claro que **los números están simplificados para enseñar**.

> **Pregunta abierta:** ¿qué exige vuestro evento (duración del video, diapositivas, enlace al repositorio, versión jugable, zona horaria de la fecha límite)? Comprobarlo en la primera hora.

---

## 9. Riesgos y planes B

| Riesgo | Señal | Plan B |
|---|---|---|
| Caminar y cargar se come el calendario | En la hora 6 todavía no se puede colocar ninguna pieza | Quitar el caminar: clic en la estantería, clic en el hueco. Las reglas, los indicadores y el final con foto se mantienen |
| El arte no está listo | Todavía hay cuadrados en la hora 14 | Usar un pack de assets gratuito tal cual y dibujar solo las piezas del satélite |
| La carga entre dos falla | Las piezas se atascan o se teletransportan | Hacer que las piezas pesadas vayan lentas con un solo jugador |
| Las imágenes de NASA se ven mal al difuminarlas | Nítida y borrosa se ven igual a tamaño pixel | Mostrar la foto final a tamaño de ventana completa, fuera de la vista pixel |
| Un diseño siempre gana | La misma construcción queda primera en todas las pruebas | Subir su precio o sus datos generados; reducir la capacidad de la radio |
| Demasiado difícil para principiantes | Los jugadores de prueba fallan tres veces sin mejorar | Añadir una línea de "prueba esto" a cada causa del informe |
| El juego no funciona en el ordenador del jurado | Solo funciona en el portátil del desarrollador | Empaquetarlo todo en un archivo; probarlo en un portátil limpio en la hora 18; apoyarse en el video |
| Se amplía el alcance | Alguien empieza la cuenta atrás o el parche antes de la hora 14 | El responsable del tiempo (D) hace cumplir la regla del punto de control |
