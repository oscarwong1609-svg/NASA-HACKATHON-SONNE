# Mission Lab v4.0

A 2D pixel-art game for the Space Mission Design Game challenge. The team receives a science mission, builds a satellite in a hangar, passes the flight readiness review, launches it, operates it from Mission Control and sees the image it captured. 1 to 4 players on a single screen.

## Version

**4.0**: English codebase, full-window scaling on every display (including macOS Retina), F11 fullscreen.

## Getting started

Requirements: Java JDK 17 or newer. Gradle does not need to be installed; the project downloads it automatically the first time (internet access required).

| To | Command (on Windows use `gradlew.bat` instead of `./gradlew`) |
| --- | --- |
| Play | `./gradlew lwjgl3:run` |
| Run the tests | `./gradlew :core:test` |
| Check the balance | `./gradlew :core:balance` |
| Build the game as a single file | `./gradlew lwjgl3:jar`, then `java -jar lwjgl3/build/libs/MissionLab.jar` |

On macOS, the packaged file is launched with `java -XstartOnFirstThread -jar lwjgl3/build/libs/MissionLab.jar`. This is not needed with `lwjgl3:run`.

In IntelliJ: File > Open, select this folder and accept the Gradle import.

## Controls

| Player | Role (with 2+ players) | Move | Action |
| --- | --- | --- | --- |
| 1 | Scientist | W A S D | E |
| 2 | Engineer | Arrow keys | Enter |
| 3 | Launch lead | I J K L | O |
| 4 | Ground control | Numpad 8 4 5 6 | Numpad 0 |

- In a console (orbit map, rocket console, ground station desk): left/right change the option; action or Esc closes it.
- In Mission Control: left = IMAGE (Scientist), right = DOWNLINK (Ground control), hold action = fast-forward time, Esc = end.
- Pressing Esc twice in the hangar returns to the title screen.
- F11 toggles fullscreen (on macOS the green window button also works). The window can be resized or maximised freely; the game always fills it.

## How to play: the five phases

A bar at the top of every screen (at the bottom in the hangar) shows which phase the team is in.

1. **Briefing** (two pages). The science question, the area on a real map, the budget, the launch date, real NASA facts and, on the second page, **how to win**: minimum, full and exemplary success with concrete numbers.
2. **Build & review** (hangar). Carry parts to the blueprint, vote on test problems and use the three consoles. The bottom bar always shows the **next step** and the status of the **flight readiness review**. The pad only allows launch when no system is NO-GO.
3. **Launch**. Countdown, liftoff, each leg of the route, panel deployment and first contact, with mission elapsed time.
4. **Mission Control**. One day of on-orbit operations: send IMAGE for the pass over the target and DOWNLINK for each ground station contact, while watching the battery.
5. **Results**. The image, the medal, the success criteria with the numbers achieved and what to improve. The title screen keeps the best medal for each mission.

## The two missions

They have exactly the same structure; only the numbers change. Mission 1 is for learning; Mission 2 demands more careful trade-offs.

| | Mission 1: Amazon Fire Alarm | Mission 2: Greenland Ice Watch |
| --- | --- | --- |
| Target | Fires in the Amazon, **at night** | Glacier fronts in Greenland, **in daylight** |
| Budget | $700k | $1.1M |
| Launch in | 14 months | 12 months |
| Required lifetime | 1 year | 3 years |
| Required detail | pixels of 100 m or smaller | pixels of 15 m or smaller |
| Main lesson | at night thermal infrared is required, and the camera drains the battery in eclipse | fine detail requires a telescope, precise pointing, a 6U frame and a high-capacity data link |

## Success tiers (as at NASA)

| Tier | Medal | Condition |
| --- | --- | --- |
| Minimum success | Bronze | Image of the target with the right lighting, at least 50% received, pixels at most twice the required size |
| Full success | Silver | Pixels equal to or better than required, at least 90% received, the satellite lasts the whole mission |
| Exemplary | Gold | Full success and a score of 90 or more |

## The budgets (`sim/Budgets.java`)

They all come from the same place: hangar bars, flight readiness review, simulator and final screen.

| Variable | How it is computed |
| --- | --- |
| Mass | parts + structure (+ drag sail if the orbit would last longer than the mission + 5 years). Limit: the lower of the structure (2 kg per U) and the smallest vehicle on the route |
| Volume | parts + 0.5U for the flight computer; must fit in the structure (2U, 3U or 6U) |
| Power | average draw = flight computer 2.5 W + attitude control + camera × 20% of the time + radio × fraction of the day in contact. Generation = panels × fraction of each orbit in sunlight |
| Battery | in eclipse: (flight computer + attitude control) × eclipse minutes; at night, the camera as well. Only 25% of the battery is used per orbit (depth of discharge) |
| Data | daily image (GB) versus Mbps × contact minutes × 60 / 8000 |
| Budget | parts + $150k for the flight computer and testing + route + station × years + fixes |
| Schedule | 3 months of integration and testing + the slowest part (all are ordered together) + delays. The satellite must be delivered as far ahead of launch as the route requires (NASA's free ride: 4 months) |
| Resolution | altitude (km) × camera IFOV (µrad) / 1000 = metres per pixel |

**Flight readiness review** (`sim/Readiness.java`): 13 GO / WARN / NO-GO checks. These block the launch: budget, schedule, structure (mass or volume) and launch vehicle. The rest warn but still allow flight.

**Score out of 100** (`sim/Score.java`): science 50 + margins 20 (5 for each margin of at least 10% in mass, power and battery, and one month of schedule slack) + budget 15 (full with a 10% reserve) + reliability 15 (minus 3 per accepted risk).

## Orbital physics and Mission Control

`sim/OrbitMechanics.java` uses simplified but real physics: period (Kepler), velocity, ground track over the rotating Earth, eclipses using the seasonal beta angle, each antenna's coverage circle (10° above the horizon) and daily contacts. That is why Svalbard hears a polar satellite on almost every orbit and never one in the ISS orbit.

`sim/MissionOps.java` plays one day in orbit without graphics. Commands go into a queue and are executed when their time comes. To score fairly, a perfect autopilot plays the same day with the same satellite: the operations score is what the team brought down compared with it.

## The hangar consoles

- **Orbit map**: a real map of the Earth with the mission area, night, the ground track for one day and for the next orbit, the moving satellite, the coverage of the chosen antennas and telemetry (altitude, inclination, period, velocity, eclipse, useful passes per day, lifetime, resolution with the installed camera).
- **Rocket console**: the flight graph drawn with each edge's price for the current mass, the chosen route, the cheapest one (Dijkstra) and those that cannot carry the mass; the list of routes with their delivery dates, and the complete flight readiness review with the LAUNCH: GO / NO-GO verdict.
- **Ground station desk**: the antennas on the map with the parts of the day when there is contact, each network with its price, passes and minutes per day, the data budget with its formula, and the battery over two orbits with the safe limit.

## Code structure

| Folder | Contents | Depends on libGDX |
| --- | --- | --- |
| `core/.../ds` | The two data structures: directed weighted graph and queue | No |
| `core/.../model` | Parts, orbits, vehicles and flight graph, routes, stations, missions, problems, design | No |
| `core/.../data/Catalog.java` | **All of the game's content and numbers** | No |
| `core/.../data/EarthMask.java` | Land/sea mask at 1° (generated, no need to edit) | No |
| `core/.../sim` | Budgets, flight readiness review, orbital physics, simulation, Mission Control, scoring, balance | No |
| `core/.../hangar` | Hangar rules: move, carry, install, vote, consoles, launch | No |
| `core/.../game` | Palette, keys, asset loading, pixel fonts, sprites, pixel-art Earth, world map, placeholder photos | Yes |
| `core/.../screens` | The six screens and the three consoles | Yes |
| `lwjgl3` | Desktop launcher | Yes |
| `assets` | Optional photos, sounds and sprites that replace the built-in ones | Files |
| `tools` | Generators for the pixel art (`art.py`) and the fonts (`fontgen.py`) | Python |

`ds`, `model`, `sim` and `hangar` draw nothing and have tests in `core/src/test`. If a rule changes, run `./gradlew :core:test`.

## Data structures

The game relies on two hand-written structures in `core/.../ds`. Both drive game rules; neither is decorative.

### Directed weighted graph: the flight map (`DirectedWeightedGraph`)

- **Vertices** (`Waypoint`): the launch site and each orbit.
- **Edges** (`Transfer`): a vehicle that goes from one place to another (NASA's free ride to the ISS, paid deployment from the ISS, rideshare to 550 km, rideshare to 800 km, space tug from 550 to 800 km). They are directed because trips only go up.
- **Weights**: fixed price + price per kilogram, maximum mass, travel time and delivery months.
- **Path** (`Route`): the route chosen by the Launch lead, for example `Launch site > [Rideshare] > 550 km > [Space tug] > 800 km`.

Who uses it: the rocket console (Dijkstra proposes the cheapest route; edges that cannot carry the mass count as infinite, so the best route changes as parts are installed), `Budgets` (cost and mass limit), `Readiness` (vehicle and delivery date), `Simulator` (walks the path leg by leg and reports if there was a cheaper route) and `Balance` (tries every route). To add a trip, just add a line to `FLIGHTS` in `Catalog.java`.

### FIFO queue: what happens in order (`LinkedQueue`)

A queue built from linked nodes: `enqueue` adds to the back and `dequeue` removes from the front, both in O(1). It is used three times:

- **Hangar test campaign** (`Session`): tests come up in the scheduled order; if one needs a part that is not installed yet, it goes back to the end of the queue.
- **Launch timeline** (`Simulator` → `LaunchScreen`): the simulator enqueues each event with its mission time and the screen dequeues them one by one.
- **On-board command sequence** (`MissionOps`): IMAGE and DOWNLINK wait their turn, as on a real satellite. If DOWNLINK is first, the image waits even when the target passes: order matters.

Why a queue and not another linear structure: all of this is produced in order and consumed in that same order, without skipping anything or looking in the middle. A stack would return it reversed, an array would require manual index bookkeeping and a plain linked list would allow insertion anywhere.

## Changing numbers or text

Everything is in `Catalog.java`. After any change, run `./gradlew :core:balance`. It must end with `BALANCE OK`, which checks for each mission that:

1. At least 25% of designs pass the flight readiness review.
2. The design with the cheapest part in every slot can be launched.
3. At least 10 designs reach full success, with at least 4 distinct combinations of orbit, radio and station.
4. Minimum success is reachable with less than 70% of the budget, and full success with less than 95%.
5. No design reaches 100.
6. Every part is useful: in some mission there is a design scoring 60 or more that uses it.

## Pixel art and text

All art is built into the code: the game needs no image or font files.

- **Sharp text**: `game/PixelFont.java` draws pixel fonts glyph by glyph, without anti-aliasing, at integer scale and integer coordinates. There are four: `small` (Tiny5, labels and long text), `font` (Departure Mono, body text), `label` (Silkscreen, uppercase labels) and `big`/`logo` (Press Start 2P at 2x and 4x, titles). The glyphs are in `game/FontData.java`, generated with `tools/fontgen.py`.
- **Sharp full screen, any window size**: `game/ScreenCanvas.java` draws the game into an intermediate 640×360 image multiplied by the next integer at or above the window size (x2, x3, x5 on a Retina display...) and fits it to the window with a slight downscale. It fills the window at any resolution without blurring; if the display is not 16:9, the leftover bands are painted in the space colour. Sizes are read from the back buffer in physical pixels every frame, so maximising, resizing, fullscreen and macOS Retina displays all render at full resolution.
- **Sprites**: `game/Sprites.java` packs the drawings from `game/SpriteData.java` into one texture: astronauts (recoloured with each player's colour), the 16 parts (each tier looks more complex: lenses with gold foil, solar wings with more panels, battery cells, reaction wheel with star tracker, 2U/3U/6U frames, parabolic antennas), rockets, CubeSat, icons, medals and mission patches. To change a drawing: edit `tools/art.py`, preview it with `python3 tools/art.py preview sheet.png` and regenerate with `python3 tools/art.py java core/src/main/java/com/missionlab/game/SpriteData.java`.
- **The Earth**: `game/Globe.java` paints it in code with the real continents from `EarthMask`, clouds, deserts, ice, a night side with city lights and an atmosphere rim.
- **Terminal panels**: `Draw.terminal` (title bar, LEDs, scanlines and corner brackets), `Draw.panel`, `Draw.heading`, `Draw.chip` and `Draw.led` give every screen the same control-room look.

## Adding photos, sounds and custom art

Each file added replaces the placeholder automatically, without touching code.

- `assets/photos/greenland.png`, `amazon_fire.png` and `off_target.png` (ocean): the real NASA photos. When replacing them, put their credit line in `photoCredit` inside `Catalog.java`.
- `assets/sounds/`: `pickup`, `place`, `click`, `deny`, `alert`, `launch`, `success`, `fail` (`.wav`, `.ogg` or `.mp3`).
- `assets/sprites/<id>.png` (optional): replaces a part's built-in drawing (for example `cam_sharp.png`; the ids are in `Catalog.java`), at 12 × 12 px. `player_0.png` to `player_3.png` replace the astronauts.

The colour palette is in `game/Palette.java`. Use only those colours in sprites.

## Sources and credits

- The phases follow NASA's process: design, build and test, flight readiness review, launch and operations.
- The build problems come from the tests described in *CubeSat 101* (NASA CubeSat Launch Initiative).
- Structure sizes and masses: CubeSat Design Specification rev. 14.1 (Cal Poly): up to 2 kg per U.
- Free ride from the ISS: NASA CubeSat Launch Initiative (CSLI). Rideshare price per kilogram: on the order of SpaceX Transporter missions.
- Drag sail: the FCC (2022) rule requiring low-Earth-orbit satellites to deorbit no later than 5 years after the end of the mission.
- Fires: VIIRS (Suomi NPP, NOAA-20), 375 m pixels, passes around 1:30. Glaciers: Landsat 8 and 9 (15 to 30 m); Jakobshavn retreat from NASA Earth Observatory.
- Ground stations: Svalbard (KSAT) sees polar satellites on almost every orbit.
- Land/sea mask: NOAA/GLOBE mask via the `global-land-mask` package.
- Photos: search NASA Worldview or NASA Earth Observatory. Check each image's terms of use and credit it.
- Pixel fonts (SIL Open Font License 1.1, licences in `licenses/`): Tiny5 (The Tiny5 Project Authors), Departure Mono (Helena Zhang), Press Start 2P (CodeMan38) and Silkscreen (Jason Kottke). The game ships the glyphs pre-rendered as bitmaps.
- The pixel art (astronauts, parts, rockets, medals, patches, icons and the Earth) was made for this game.
- The game's numbers are simplified for teaching; they are not real specifications.
