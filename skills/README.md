# Skills del equipo para Claude Code

Esta carpeta tiene **26 skills** para Claude Code y la lista de **4 plugins** que usamos en el equipo. Una skill es un conjunto de instrucciones que Claude carga cuando la tarea lo necesita: por ejemplo, cómo hacer pruebas, cómo revisar código o cómo planificar.

## Forma más fácil: pedírselo a Claude

1. Clona el repositorio (o actualízalo con `git pull`).
2. Abre Claude Code en la carpeta del repositorio.
3. Copia y pega este mensaje:

```text
Instala las skills de la carpeta skills/ de este repositorio: copia cada subcarpeta a mi carpeta personal de skills (~/.claude/skills/, en Windows C:\Users\<mi usuario>\.claude\skills\), sin borrar las skills que ya tengo. Después instala los plugins que aparecen en skills/README.md y dime cuáles quedaron instalados.
```

4. Cierra y vuelve a abrir Claude Code para que cargue las skills nuevas.

## Con comandos

Ejecútalos desde la carpeta raíz del repositorio.

**Windows (PowerShell):**

```powershell
powershell -ExecutionPolicy Bypass -File skills\instalar.ps1
```

**Mac o Linux (o Git Bash en Windows):**

```bash
bash skills/instalar.sh
```

Los dos scripts copian cada skill a `~/.claude/skills/`. Si ya tienes una skill con el mismo nombre, se actualiza con esta versión. Las demás skills que tengas no se tocan.

## Plugins

Los plugins no se copian como archivos: se instalan desde su catálogo (marketplace). En Claude Code, escribe estos comandos uno por uno:

```text
/plugin install superpowers@claude-plugins-official
/plugin install frontend-design@claude-plugins-official
/plugin install code-review@claude-plugins-official
/plugin marketplace add blader/humanizer
/plugin install humanizer@humanizer
```

Si Claude dice que no encuentra `claude-plugins-official`, añádelo primero:

```text
/plugin marketplace add anthropics/claude-plugins-official
```

En la app de escritorio de Claude también puedes instalarlos desde la sección de plugins.

| Plugin | Para qué sirve |
|---|---|
| `superpowers` | Flujo de trabajo: lluvia de ideas, planes, pruebas primero, depuración sistemática |
| `frontend-design` | Diseño de interfaces con personalidad, sin aspecto genérico |
| `code-review` | Revisión de código de un cambio o de un pull request |
| `humanizer` | Reescribe textos para que no suenen a IA |

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

## Comprobar que funcionó

Abre Claude Code y pregúntale: **"¿Qué skills tienes disponibles?"**. Deberían aparecer las de la tabla.
