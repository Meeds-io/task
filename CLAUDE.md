# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **Meeds/eXo Task Management add-on** (`io.meeds.task`): projects, tasks, statuses (kanban), comments, labels, and assignments inside the Meeds/eXo social platform. It builds a `task-management-services` JAR + a `task-management-webapps` WAR + a `task-management-packaging` ZIP, deployed into a running eXo/Meeds server.

## Build & test

Maven multi-module build (`services` → `webapps` → `packaging`). Inherits from `io.meeds.addons:addons-parent-pom`.

```bash
mvn install                         # full build (compiles Java, runs frontend build + JS lint, packages WAR/ZIP)
mvn install -pl services            # backend module only
mvn test -pl services               # run all backend tests
mvn test -pl services -Dtest=TestPermission        # single test class
mvn test -pl services -Dtest=TestPermission#method # single test method
```

Frontend (in `webapps/`, driven by `frontend-maven-plugin` during the Maven build, or run directly):

```bash
cd webapps
npm run build      # webpack production build → src/main/webapp/js/*.bundle.js
npm run watch      # webpack dev watch
npm run lint       # eslint --fix over vue-app/ (also runs at build time via eslint-webpack-plugin)
```

i18n: edit only the `_en` source bundles under `*/src/main/resources/locale/`; Crowdin syncs the rest (see `crowdin.yml`).

## Architecture

### Two-generation backend (important — DI differs by package)

- **Legacy core `org.exoplatform.task.*`** — wired via **eXo Kernel `configuration.xml`**, NOT Spring. Services use constructor injection of kernel components and `CommonsUtils.getService(...)` / `PortalContainer.getComponentInstanceOfType(...)` for lookups. Component bindings live in `services/src/main/resources/conf/portal/configuration.xml` (`<component><key>interface</key><type>impl</type></component>`). Layering:
  - `service/` (interfaces) + `service/impl/` — business logic (`TaskService`, `ProjectService`, `StatusService`, `CommentService`, `LabelService`, `UserService`).
  - `storage/` + `storage/impl/` — maps domain entities ↔ DTOs, sits between services and DAOs.
  - `dao/` + `dao/jpa/` — JPA handlers (`DAOHandler` aggregates `TaskHandler`, `ProjectHandler`, etc.). `TaskQuery`/`ProjectQuery` + `dao/condition/` build dynamic queries.
  - `domain/` — JPA `@Entity` classes (`Task`, `Project`, `Status`, `Comment`, `Label`); `dto/` — transport objects; `rest/` — JAX-RS endpoints (`TaskRestService`, `ProjectRestService`, `StatusRestService`).
- **Newer integrations `io.meeds.task.*`** — use **Spring** (`@Service`/`@Component`/`@Autowired`). These cover platform plugins (`plugin/` — ACL, content-link, permanent-link), `listener/` (content-link + indexing), `search/TaskSearchConnector`, and `mcp/TaskMcpTool`. They `@Autowired` the legacy kernel services across the bridge.

This is the reverse of the platform-wide default (where core is Spring); here the legacy core is still Kernel-wired. When editing, follow the convention of the package you're in.

### DB migrations

Liquibase changelogs in `services/src/main/resources/db/changelog/task.db.changelog-*.xml`, applied at startup. Add a new versioned changelog file for schema changes (e.g. the recent `task.db.changelog-7.2.0.xml`); never edit shipped ones.

### MCP / AI integration

`io.meeds.task.mcp.TaskMcpTool implements io.meeds.mcp.server.plugin.McpToolPlugin`. Public methods become snake_case MCP tools (`listProjects` → `list_projects`). **Every tool method REQUIRES a matching entry in `services/src/main/resources/ai-tool-definitions.json`** (root `{"tools":[…]}`, `name` = method snake_case) or it is silently dropped. Named-arg binding depends on the `-parameters` javac flag (configured via the parent pom). The tool acts as the calling user, so platform ACLs apply.

### Frontend (Vue 2)

Per-feature apps under `webapps/src/main/webapp/vue-app/<app>/main.js`, each a webpack entry in `webapps/webpack.common.js` → `js/<name>.bundle.js` (AMD, `libraryTarget: 'amd'`). Entries include `tasks`, `taskDrawer`, `tasksManagement`, `taskCommentsDrawer`, `taskSearch`, plus extension bundles (`engagementCenterExtensions`, `connectorEventExtensions`, `notificationExtension`, `taskQuickAction`, `taskContentLinkExtension`, `restrictedProject`). Modules + load-groups are declared in `webapps/src/main/webapp/WEB-INF/gatein-resources.xml`. WAR packaging excludes raw `vue-app/**`, `*.vue`, `*.less` (`packagingExcludes`).

### Platform wiring (webapp side)

`external-component-plugins` and integration config live in `webapps/src/main/webapp/WEB-INF/conf/task-addon/*.xml`: `task-service-configuration.xml`, `acl-configuration.xml`, `search-configuration.xml`, `indexing-configuration.xml`, `social-configuration.xml`, `notification-configuration.xml`, `analytics-configuration.xml`, `gamification-integration-configuration.xml`, `ckeditor-configuration.xml`, `dynamic-container-configuration.xml`.

## Tests

Backend tests use eXo Kernel container bootstrap, not Spring. Extend `org.exoplatform.task.AbstractTest`, which manages the JPA `RequestLifeCycle` (begin/end per test, `restartTransaction()` between DAO ops) and provides `deleteAll()` for cleanup. Resolve components via `PortalContainer.getInstance().getComponentInstanceOfType(...)`.
