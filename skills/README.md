# Skills y plugins del equipo para Claude Code

Esta carpeta tiene **26 skills** y **4 plugins** para Claude Code.

- Una **skill** es un conjunto de instrucciones que Claude carga cuando la tarea lo necesita: por ejemplo, cómo hacer pruebas, cómo revisar código o cómo planificar.
- Un **plugin** es un paquete más grande: puede traer varias skills, comandos y automatismos. Aquí van dentro de `plugins/`, junto con un catálogo del equipo (`equipo-mission-lab`) para instalarlos desde el repositorio.

## Forma más fácil: pedírselo a Claude

1. Clona el repositorio (o actualízalo con `git pull`).
2. Abre Claude Code en la carpeta del repositorio.
3. Copia y pega este mensaje:

```text
Instala lo que hay en la carpeta skills/ de este repositorio siguiendo skills/README.md:
1) Copia cada subcarpeta de skills/ que tenga un SKILL.md a mi carpeta personal de skills (~/.claude/skills/, en Windows C:\Users\<mi usuario>\.claude\skills\), sin borrar las skills que ya tengo.
2) Añade el catálogo de plugins del equipo con "claude plugin marketplace add" usando la ruta completa de la carpeta skills/ de este repositorio, e instala desde él superpowers, frontend-design, code-review y humanizer (equipo-mission-lab). Si ya tengo alguno de esos plugins instalado desde otro catálogo, no lo instales otra vez y avísame.
Al terminar, dime qué skills y qué plugins quedaron instalados.
```

4. Cierra y vuelve a abrir Claude Code para que cargue todo.

## Con comandos

Todos se ejecutan desde la **carpeta raíz del repositorio**, en una terminal.

### 1. Skills

**Windows (PowerShell):**

```powershell
powershell -ExecutionPolicy Bypass -File skills\instalar.ps1
```

**Mac o Linux (o Git Bash en Windows):**

```bash
bash skills/instalar.sh
```

Los scripts copian cada skill a `~/.claude/skills/`. Si ya tienes una skill con el mismo nombre, se actualiza con esta versión. Las demás skills que tengas no se tocan.

### 2. Plugins

```bash
claude plugin marketplace add ./skills
claude plugin install superpowers@equipo-mission-lab
claude plugin install frontend-design@equipo-mission-lab
claude plugin install code-review@equipo-mission-lab
claude plugin install humanizer@equipo-mission-lab
```

También se puede hacer dentro del chat de Claude Code con `/plugin marketplace add ./skills` y `/plugin install superpowers@equipo-mission-lab` (y así con cada uno).

Si ya tienes alguno de estos plugins instalado desde su catálogo original, no hace falta instalarlo otra vez.

> **Importante:** los plugins se leen desde la carpeta del repositorio. **No muevas ni borres la carpeta del repositorio** después de instalarlos. Si la mueves, vuelve a ejecutar `claude plugin marketplace add` con la ruta nueva.

### 3. Comprobar que funcionó

```bash
claude plugin list
```

Y en Claude Code pregunta: **"¿Qué skills tienes disponibles?"**.

## Actualizar

Cuando alguien suba versiones nuevas al repositorio:

```bash
git pull
claude plugin marketplace update equipo-mission-lab
```

Para las skills, vuelve a ejecutar el script de instalación.

## Qué plugins hay

| Plugin | Versión | Para qué sirve | Autor y licencia |
|---|---|---|---|
| `superpowers` | 7.0.2 | Flujo de trabajo: lluvia de ideas, planes, pruebas primero, depuración sistemática | Jesse Vincent · MIT |
| `frontend-design` | 1.1.1 | Diseño de interfaces con personalidad, sin aspecto genérico | Anthropic · Apache 2.0 |
| `code-review` | 1.0.1 | Revisión de código de un cambio o de un pull request | Anthropic · Apache 2.0 |
| `humanizer` | 3.1.0 | Reescribe textos para que no suenen a IA | blader (Siqi Chen) · MIT |

Son copias sin cambios de las versiones originales, cada una con su archivo `LICENSE`. También se pueden instalar desde sus catálogos originales (`claude-plugins-official` y `blader/humanizer`), que se actualizan solos.

## Qué skills hay

| Skill | Para qué sirve |
|---|---|
| `api-and-interface-design` | Diseñar APIs e interfaces entre módulos |
| `browser-testing-with-devtools` | Probar en un navegador real con Chrome DevTools |
| `ci-cd-and-automation` | Montar integración y despliegue continuos |
| `code-review-and-quality` | Revisar código en varios aspectos antes de juntarlo |
| `code-simplification` | Simplificar código sin cambiar lo que hace |
| `constraint-driven-development` | Dejar por escrito el nivel de calidad y vigilar que no baje |
| `context-engineering` | Preparar el contexto de Claude al empezar o cambiar de tarea |
| `debugging-and-error-recovery` | Encontrar la causa real de un error |
| `deprecation-and-migration` | Retirar o migrar sistemas y APIs |
| `documentation-and-adrs` | Documentar decisiones de arquitectura |
| `doubt-driven-development` | Revisar cada decisión importante con una mirada crítica |
| `frontend-ui-engineering` | Interfaces accesibles, adaptables y con calidad de producción |
| `git-workflow-and-versioning` | Ramas, commits, versiones y pull requests |
| `graphify` | Convertir una carpeta de código o documentos en un grafo de conocimiento (ver nota abajo) |
| `idea-refine` | Pulir una idea vaga hasta que sea concreta |
| `incremental-implementation` | Construir en pasos pequeños y comprobables |
| `interview-me` | Que Claude te entreviste para entender qué quieres de verdad |
| `observability-and-instrumentation` | Logs, métricas y alertas |
| `performance-optimization` | Mejorar el rendimiento |
| `planning-and-task-breakdown` | Dividir el trabajo en tareas ordenadas |
| `security-and-hardening` | Revisar seguridad y vulnerabilidades |
| `shipping-and-launch` | Preparar un lanzamiento a producción |
| `source-driven-development` | Basar cada decisión en la documentación oficial |
| `spec-driven-development` | Escribir la especificación antes del código |
| `test-driven-development` | Escribir primero la prueba, luego el código |
| `using-agent-skills` | Elegir qué skill usar en cada momento |

### Nota sobre graphify

`graphify` necesita además su programa en Python. Si vas a usarlo, instálalo con:

```bash
pip install graphifyy
```

Después se usa escribiendo `/graphify` en Claude Code.
