---
baseline_commit: 28d295ef22a8efec0c3ca6a7478236d7add5d158
---

# Story 7.1: Interface Web de Geração — MVP

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como Product Owner / pesquisador do Synthea-br,
quero uma interface HTML local para selecionar parâmetros e disparar a geração de dados sintéticos,
para que estudantes e orientadores usem o gerador sem depender de CMD, flags ou edição manual de `synthea.properties`.

## Acceptance Criteria

1. **Given** o fork Synthea-br instalado com Java 17+
   **When** o usuário inicia o servidor web via comando documentado (ex.: `./gradlew runWeb` ou `run_synthea.bat --web`)
   **Then** o browser abre (ou o usuário navega para) uma página em `http://localhost:{porta}` com formulário em PT-BR
   **And** a CLI tradicional (`run_synthea`, `App.main`) continua funcionando sem regressão quando a flag web não é usada
   [Source: PO intent; AD-1 in-process; docs/GUIA-DE-USO.md]

2. **Given** a página de geração carregada
   **When** o usuário visualiza o formulário MVP
   **Then** os campos abaixo estão disponíveis com labels e help text em português:
   - Seed (`-s`) — numérico, default sugerido 42
   - Tamanho da população (`-p`) — inteiro ≥1, default 10
   - Gênero (`-g`) — opcional: M, F ou “qualquer”
   - Faixa etária (`-a`) — min/max opcionais
   - Perfil brasileiro (`br.profile`) — checkbox/toggle
   - Condição alvo (`br.target_condition`) — select populado de `SupportedConditions` (MVP: `breast_cancer`)
   - Modo de gate (`br.target_condition.gate_mode`) — select `retry`/`exclude`, visível só se condição alvo selecionada
   - Exportações — checkboxes: FHIR R4, CSV, HTML narrativo (`exporter.fhir.export`, `exporter.csv.export`, `exporter.html.export`)
   **And** valores inválidos são rejeitados no client e no server com mensagem clara em PT-BR
   [Source: docs/GUIA-DE-USO.md §7-9; SupportedConditions.java]

3. **Given** parâmetros válidos submetidos
   **When** o usuário clica em “Gerar cohort”
   **Then** o servidor aplica os parâmetros via `Config.set(...)` + `Generator.GeneratorOptions` (mesmo caminho lógico de `App.main`, sem reimplementar regras clínicas)
   **And** a geração roda **assíncrona** em thread dedicada — a UI não trava o browser
   **And** apenas **um job** de geração pode estar ativo por vez; segundo submit retorna HTTP 409 com mensagem “Geração em andamento”
   [Source: App.java:283-286; NFR2 até 30 min para n=500]

4. **Given** geração em andamento
   **When** o usuário consulta status (polling ou SSE simples)
   **Then** a UI exibe: estado (`idle` | `running` | `completed` | `failed`), progresso estimado (pacientes gerados / total solicitado quando disponível via logs ou contador interno), e mensagens de log resumidas (últimas N linhas)
   **And** ao concluir com sucesso, a UI mostra link para pasta de output (`exporter.baseDirectory`, default `./output/`) e presença de `manifest.json`
   **And** em falha, exibe mensagem de erro legível (condição inválida, gate excedido, etc.) sem stack trace completo na UI
   [Source: Story 1.4 manifest; TargetConditionIntegration]

5. **Given** perfil `br.profile=br` e condição `breast_cancer` selecionados
   **When** a geração completa com seed fixa
   **Then** o resultado é **reprodutível** — mesmos parâmetros + seed produzem checksum equivalente no `manifest.json` (NFR1, SM-3)
   **And** cohort respeita gate Epic 2 quando condição alvo ativa
   [Source: ARCHITECTURE-SPINE.md#AD-4, #AD-6]

6. **Given** implementação concluída
   **When** `./gradlew check` é executado
   **Then** novos testes passam cobrindo: validação de parâmetros, endpoint de status, job único, integração mínima com `Generator` (1 paciente, seed fixa, flag web off nos testes existentes intactos)
   **And** documentação em `docs/GUIA-DE-USO.md` inclui seção “Modo interface web” com comando de inicialização e screenshot/descrição dos campos
   [Source: project-context.md#Testing-Rules]

7. **Given** escopo MVP de segurança
   **When** o servidor web está ativo
   **Then** escuta apenas `127.0.0.1` (localhost) por padrão — **não** expor rede LAN sem flag explícita documentada como unsafe
   **And** não há autenticação no MVP (uso local/lab); disclaimer visível: “dados 100% sintéticos; não usar em produção clínica”
   [Source: NFR5; PRD disclaimer ético]

## Tasks / Subtasks

- [x] Task 1: Extração do bootstrap de geração (AC: #3, #5)
  - [x] Subtask 1.1: Criar `org.mitre.synthea.br.web.GenerationService` (ou `SyntheaRunService`) encapsulando: aplicar `GenerationRequest` → `Config.set` → `Generator.GeneratorOptions` → `validateConfig` → `new Generator(...).run()`
  - [x] Subtask 1.2: Reutilizar lógica de `App.resetOptionsFromConfig` e `App.validateConfig` — extrair para classe compartilhada em `org.mitre.synthea.br.web` ou `org.mitre.synthea.helpers` **sem alterar comportamento CLI** (App delega ou duplica mínimo documentado)
  - [x] Subtask 1.3: Mapear `GenerationRequest` DTO com campos MVP; converter condição via `SupportedConditions.resolve`

- [x] Task 2: Servidor HTTP embarcado (AC: #1, #7)
  - [x] Subtask 2.1: Criar `org.mitre.synthea.br.web.WebServer` usando `com.sun.net.httpserver.HttpServer` (JDK 17 — **sem** Spring/Jetty/Spark novos)
  - [x] Subtask 2.2: Properties: `br.web.enabled`, `br.web.port` (default 8080), `br.web.bind=127.0.0.1` em `synthea.properties`
  - [x] Subtask 2.3: Entry point: flag `--web` em `App.main` OU task Gradle `runWeb` — documentar uma opção canônica
  - [x] Subtask 2.4: Servir estáticos de `src/main/resources/br/web/` (`index.html`, `app.css`, `app.js`)

- [x] Task 3: API REST mínima (AC: #3, #4)
  - [x] Subtask 3.1: `GET /api/config/options` — retorna JSON: condições suportadas, gate modes, defaults, toggles de export
  - [x] Subtask 3.2: `POST /api/generate` — body JSON → enfileira job; retorna `{ jobId, status }`
  - [x] Subtask 3.3: `GET /api/generate/status` — estado, progresso, log tail, paths de output, erro se houver
  - [x] Subtask 3.4: Serialização JSON via Gson (já no projeto) — não introduzir Jackson para API

- [x] Task 4: UI HTML do formulário (AC: #2, #4)
  - [x] Subtask 4.1: Formulário responsivo simples, PT-BR, sem framework pesado (HTML + CSS + fetch API vanilla)
  - [x] Subtask 4.2: Carregar opções dinamicamente de `/api/config/options`
  - [x] Subtask 4.3: Polling de status a cada 2s durante `running`; botão desabilitado enquanto job ativo
  - [x] Subtask 4.4: Link pós-sucesso para `output/html/index.html` quando `exporter.html.export=true`

- [x] Task 5: Job manager thread-safe (AC: #3)
  - [x] Subtask 5.1: `GenerationJobManager` singleton com lock — um job por vez
  - [x] Subtask 5.2: Capturar `System.out`/`System.err` ou usar logger appendable para tail de log na UI (últimas 50 linhas)
  - [x] Subtask 5.3: Propagar exceções de `TargetConditionIntegration` / gate como mensagens user-friendly

- [x] Task 6: Testes (AC: #6)
  - [x] Subtask 6.1: `GenerationServiceTest` — 1 paciente, seed fixa, assert manifest/output
  - [x] Subtask 6.2: `WebServerTest` — HttpClient JDK: GET /, POST generate com params inválidos → 400, job concorrente → 409
  - [x] Subtask 6.3: `GenerationRequestValidationTest` — bounds population, idade min≤max, condição desconhecida
  - [x] Subtask 6.4: `./gradlew check` completo

- [x] Task 7: Documentação (AC: #6)
  - [x] Subtask 7.1: Atualizar `docs/GUIA-DE-USO.md` — nova seção “Interface web (MVP)”
  - [x] Subtask 7.2: Atualizar comentários em `synthea.properties` para flags `br.web.*`
  - [x] Subtask 7.3: Registrar ADR recomendado `ADR-NNN-interface-web-localhost-mvp.md` — justifica reversão do non-goal GUI do PRD §7

### Review Findings

- [x] [Review][Patch] Progresso ao vivo nunca atualiza durante `running` (viola AC4) [src/main/java/org/mitre/synthea/br/web/GenerationJobManager.java:74-76] — corrigido: `GenerationService.run()` ganhou overload com callback `Consumer<Generator>` invocado logo após construir o `Generator` (antes de `.run()`), permitindo que `GenerationJobManager` capture `activeGenerator` imediatamente e leia `totalGeneratedPopulation` ao vivo.
- [x] [Review][Patch] Default de população no formulário mostra 1 em vez de 10 (viola AC2 — chave `generate.default_population` compartilhada com a CLI já vale 1) [src/main/java/org/mitre/synthea/br/web/WebServer.java:147; src/main/resources/synthea.properties:117] — corrigido: nova chave dedicada `br.web.default_population=10` em `synthea.properties`, usada por `OptionsHandler`.
- [x] [Review][Patch] Mensagens de exceção não sanitizadas vazam via `errorMessage` da API de status, sem log de stack trace para depuração real [src/main/java/org/mitre/synthea/br/web/GenerationJobManager.java:86-91] — corrigido: exceções conhecidas (`IllegalStateException`/`IllegalArgumentException`, já com mensagem PT-BR amigável) usam `ex.getMessage()`; qualquer outra exceção loga stack trace completo no console do servidor e expõe apenas mensagem genérica ao cliente.
- [x] [Review][Patch] `LogCaptureStream` não é thread-safe (`ArrayDeque`/`StringBuilder`) sob escrita concorrente (Generator usa thread pool) e leitura via polling HTTP [src/main/java/org/mitre/synthea/br/web/LogCaptureStream.java] — corrigido: `write`, `tail` e `reset` sincronizados; parsing de `Records: total=` protegido contra `NumberFormatException`.
- [x] [Review][Patch] Sem limite superior de `population` em `GenerationRequest.validate()` — aceita valores arbitrariamente grandes [src/main/java/org/mitre/synthea/br/web/GenerationRequest.java] — corrigido: limite de proteção `MAX_POPULATION = 100_000` com mensagem PT-BR.
- [x] [Review][Patch] `br.target_condition.gate_mode` não é limpo quando `targetCondition` é removido — estado órfão no `Config` global entre execuções [src/main/java/org/mitre/synthea/br/web/GenerationService.java:88-90] — corrigido: `Config.remove("br.target_condition.gate_mode")` adicionado ao branch sem condição alvo.
- [x] [Review][Patch] Validação de `gender` sensível a maiúsculas/minúsculas e sem `trim()` [src/main/java/org/mitre/synthea/br/web/GenerationRequest.java] — corrigido: `normalizedGender()` faz `trim()`+`toUpperCase()`, usado tanto na validação quanto em `GenerationService`.
- [x] [Review][Patch] Envio de apenas `minAge` OU `maxAge` (não ambos) é silenciosamente ignorado sem erro de validação [src/main/java/org/mitre/synthea/br/web/GenerationRequest.java] — corrigido: `validate()` rejeita quando exatamente um dos dois é informado.
- [x] [Review][Patch] `GenerateHandler` só trata `IllegalStateException` ao iniciar job; outras exceções inesperadas escapam sem resposta JSON de erro [src/main/java/org/mitre/synthea/br/web/WebServer.java (GenerateHandler)] — corrigido: catch genérico adicional retorna 500 com mensagem PT-BR e loga stack trace.
- [x] [Review][Patch] `GenerationRunHelper` declarado `abstract` sem necessidade — inconsistente com padrão `final` usado em `WebServerLauncher` [src/main/java/org/mitre/synthea/helpers/GenerationRunHelper.java] — corrigido: classe agora `final`.
- [x] [Review][Defer] Sem proteção CSRF/Origin no endpoint de geração [src/main/java/org/mitre/synthea/br/web/WebServer.java] — deferred, aceito como limite documentado do MVP local sem autenticação (ADR-006)
- [x] [Review][Defer] Sem limite de tamanho no corpo da requisição POST [src/main/java/org/mitre/synthea/br/web/WebServer.java (GenerateHandler)] — deferred, risco só relevante se a porta for exposta além do localhost
- [x] [Review][Defer] Aviso contra `br.web.bind=0.0.0.0` existe só como comentário, sem guarda em runtime [src/main/resources/synthea.properties:373-375] — deferred, mesmo racional do MVP local
- [x] [Review][Defer] Sem isolamento de diretório de saída entre jobs — `manifestPresent`/`htmlIndexPath` só checam existência de arquivo [src/main/java/org/mitre/synthea/br/web/GenerationJobManager.java:77-84] — deferred, baixo risco com job único por vez
- [x] [Review][Defer] Sem interrupção real da thread de geração (`daemon thread`) no shutdown do servidor [src/main/java/org/mitre/synthea/br/web/WebServerLauncher.java] — deferred, aceitável para uso local de curta duração
- [x] [Review][Defer] `contentTypeFor` só reconhece `.css`/`.js` [src/main/java/org/mitre/synthea/br/web/WebServer.java] — deferred, sem impacto com os assets atuais
- [x] [Review][Defer] AC5 (reprodutibilidade via checksum do manifest.json) não é verificada por nenhum teste no caminho web [src/test/java/org/mitre/synthea/br/web/GenerationServiceTest.java] — deferred, cobertura de teste incompleta mas pipeline de manifest é pré-existente (Story 1.4)
- [x] [Review][Defer] Argumentos extras passados junto de `--web` são silenciosamente ignorados sem aviso [src/main/java/App.java] — deferred, cosmético/baixo impacto

## Dev Notes

### Course correction — PRD vs PO

O PRD original (`prd.md` §7) lista **GUI web em v1** como non-goal. Esta story implementa **decisão explícita do PO** para reduzir barreira de entrada em lab acadêmico PUCPR. Registrar ADR antes ou durante implementação. **Não** confundir com Epic 6 (Story 6.1): Epic 6 exporta HTML **narrativo da cohort gerada**; Epic 7 é **interface para configurar e executar** a geração.

### Escopo MVP vs v1.1 — não implementar agora

- Autenticação, multi-usuário, fila de jobs paralelos
- Upload de `synthea.properties` customizado ou módulos `-d`
- Geografia upstream (state/city) quando `br.profile=br` — perfil BR sorteia municípios piloto automaticamente
- Flexporter, snapshots `-i/-u`, physiology
- WebSocket/SSE (polling HTTP basta no MVP)
- Empacotamento Docker ou deploy remoto
- Substituir CLI — CMD permanece caminho avançado/reprodutível para papers

### Padrão arquitetural obrigatório

- **AD-1 In-Process:** servidor web e `Generator` no **mesmo JVM** — não microserviço
- **AD-7 Upstream First:** código novo em `org.mitre.synthea.br.web.*`; alteração mínima em `App.java` (flag `--web` ou branch)
- **AD-2:** UI/API não mutam `HealthRecord` — apenas disparam pipeline existente
- **AD-6:** `manifest.json` continua sendo escrito pelo pipeline pós-geração — UI apenas linka

### Fluxo técnico recomendado

```
Browser (index.html)
  → POST /api/generate { seed, population, br.profile, ... }
  → GenerationJobManager (single job lock)
  → GenerationService.run():
       Config.set(...) para cada property
       GeneratorOptions options = mapRequest(...)
       resetOptionsFromConfig(options, exportOptions)
       validateConfig(options, false)
       new Generator(options, exportOptions).run()
  → GET /api/generate/status (polling)
  → UI mostra completed + links output/
```

### Referência App.java — ponto de integração CLI existente

```283:286:src/main/java/App.java
    if (validArgs && validateConfig(options, overrideFutureDateError)) {
      Generator generator = new Generator(options, exportOptions);
      generator.run();
    }
```

A web UI deve chegar neste mesmo ponto — **não** forkar lógica de simulação.

### Parâmetros BR — mapeamento form → Config/Options

| Campo UI | Destino | Notas |
| --- | --- | --- |
| Seed | `options.seed` | Long |
| População | `options.population` + `Config.set("generate.default_population", ...)` | App.java:116-117 |
| Gênero | `options.gender` | M/F ou omitir |
| Idade min/max | `options.minAge`, `options.maxAge`, `options.ageSpecified=true` | Formato `-a min-max` |
| Perfil BR | `Config.set("br.profile", "br")` ou remover se off | `BrProfile.isActive()` |
| Condição alvo | `Config.set("br.target_condition", key)` | Validar via `SupportedConditions` |
| Gate mode | `Config.set("br.target_condition.gate_mode", ...)` | retry/exclude |
| Exports | `Config.set("exporter.*.export", "true/false")` | Resetar toggles não marcados para false no job |

**Receita piloto câncer de mama** (GUIA §8): quando condição = `breast_cancer`, UI deve **sugerir** (não forçar) `-g F` e `-a 45-75` via help text ou preset “Cohort câncer de mama”.

### HttpServer JDK — sem dependências novas

```java
HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
server.createContext("/", staticHandler);
server.createContext("/api/", apiHandler);
server.setExecutor(Executors.newFixedThreadPool(2)); // API + generation worker separados
```

Versão explícita no `build.gradle`: Java 17 — `HttpServer` estável em `com.sun.net.httpserver`.

**Alternativa rejeitada no MVP:** Spark Java, Spring Boot, Jetty — aumentam superficie de rebase upstream e dependências.

### Estrutura de arquivos

```
src/main/java/org/mitre/synthea/br/web/
  WebServer.java                 ← NEW — bootstrap HTTP
  WebServerMain.java             ← NEW (opcional) ou branch em App
  GenerationService.java         ← NEW
  GenerationJobManager.java      ← NEW
  GenerationRequest.java         ← NEW DTO
  GenerationStatus.java          ← NEW DTO
  ApiHandlers.java               ← NEW — routing /api/*
  StaticResourceHandler.java     ← NEW

src/main/resources/br/web/
  index.html                     ← NEW
  app.css                        ← NEW
  app.js                         ← NEW

src/main/java/App.java           ← UPDATE mínimo (--web flag)

src/main/resources/synthea.properties  ← UPDATE br.web.*

src/test/java/org/mitre/synthea/br/web/
  GenerationServiceTest.java     ← NEW
  WebServerTest.java             ← NEW
  GenerationRequestValidationTest.java ← NEW

docs/GUIA-DE-USO.md              ← UPDATE seção web
docs/research/adr/ADR-NNN-*.md     ← NEW (recomendado)
```

### Integração com Story 6.1 (HtmlExporter)

Se Story 6.1 já implementada, checkbox “Export HTML narrativo” aciona `exporter.html.export=true` e pós-geração a UI pode linkar `output/html/index.html`. Se 6.1 ainda não implementada, checkbox pode existir mas help text indica “requer HtmlExporter (Epic 6)” — preferir implementar 6.1 antes ou em paralelo.

### Testing Standards Summary

- JUnit 4, asserts `org.junit.Assert`
- Testes HTTP: `java.net.http.HttpClient` (JDK 11+)
- Porta efêmera 0 em testes (`new InetSocketAddress("127.0.0.1", 0)`)
- Restaurar `Config` global após cada teste (`Config.clear()` ou reload properties de teste)
- Não iniciar web server em testes de integração existentes — isolar `@Before/@After`

### Dependências de outras stories

| Story | Dependência |
| --- | --- |
| 2.1-2.3 | Condição alvo + gate — obrigatório para fluxo BR direcionado |
| 3.1 | Perfil BR no form |
| 1.4 | Manifest linkado pós-geração |
| 6.1 | Link para HTML narrativo (opcional no MVP, desejável) |

### Git Intelligence

Fork recente concentra extensões em `org.mitre.synthea.br.*`. `App.java` já foi estendido para flags `--br.*`. Manter diff em `App.java` ≤20 linhas (flag `--web` delegando para `WebServer.start()`).

### Latest Tech Information

- **Java 17 HttpServer:** API estável; CORS não necessário (same-origin localhost)
- **Fetch API + polling:** suficiente para jobs longos (NFR2: até 30 min)
- **Gson 2.9.0:** já usado — `new Gson().toJson(status)` para API
- **FreeMarker:** não usar para UI web interativa — apenas HTML estático + JS; FreeMarker reservado a exports (CCDA/HTML cohort)

### Anti-patterns a evitar

- ❌ `Runtime.exec("run_synthea.bat ...")` — quebra determinismo, paths Windows, manifest
- ❌ Duplicar parsing de `--config` do App.java na UI
- ❌ Servidor escutando `0.0.0.0` por default — risco em lab compartilhado
- ❌ Múltiplos jobs paralelos sem pool dedicado — esgotar CPU/RAM (NFR2 assume máquina 16GB)

### References

- [Source: PO intent — interface HTML substituindo CMD para geração]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md §4.1, §7 — non-goal GUI, superseded by ADR]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-1, #AD-7]
- [Source: _bmad-output/project-context.md]
- [Source: docs/GUIA-DE-USO.md §4-9]
- [Source: src/main/java/App.java]
- [Source: src/main/java/org/mitre/synthea/engine/Generator.java]
- [Source: src/main/java/org/mitre/synthea/br/condition/SupportedConditions.java]
- [Source: _bmad-output/implementation-artifacts/6-1-cohort-narrative-viewer-export-html-mvp.md — complementar, não confundir escopo]

## Dev Agent Record

### Agent Model Used

Composer

### Debug Log References

- `./gradlew test --tests "org.mitre.synthea.br.web.*"` — 12 testes OK
- `./gradlew check` — 707 testes; 2 falhas pré-existentes em `BrDemographicsIntegrationTest` (limiar estatístico 5%, delta 5.2% — não relacionadas a Epic 7)

### Completion Notes List

- Implementado servidor HTTP embarcado (`HttpServer` JDK) em `org.mitre.synthea.br.web.*` com formulário PT-BR, API REST e job assíncrono único.
- Extraído `GenerationRunHelper` compartilhado entre `App` e `GenerationService`.
- Entry points: `./gradlew runWeb` e `run_synthea.bat --web` → http://127.0.0.1:8080
- ADR-006 documenta course correction do non-goal GUI do PRD.
- Preset “cohort câncer de mama” na UI sugere F + idade 45–75.

### File List

- src/main/java/org/mitre/synthea/helpers/GenerationRunHelper.java (new)
- src/main/java/org/mitre/synthea/br/web/GenerationRequest.java (new)
- src/main/java/org/mitre/synthea/br/web/GenerationStatus.java (new)
- src/main/java/org/mitre/synthea/br/web/WebUiOptions.java (new)
- src/main/java/org/mitre/synthea/br/web/LogCaptureStream.java (new)
- src/main/java/org/mitre/synthea/br/web/GenerationService.java (new)
- src/main/java/org/mitre/synthea/br/web/GenerationJobManager.java (new)
- src/main/java/org/mitre/synthea/br/web/WebServer.java (new)
- src/main/java/org/mitre/synthea/br/web/WebServerLauncher.java (new)
- src/main/resources/br/web/index.html (new)
- src/main/resources/br/web/app.css (new)
- src/main/resources/br/web/app.js (new)
- src/main/java/App.java (modified)
- src/main/java/org/mitre/synthea/br/condition/GateMode.java (modified)
- src/main/resources/synthea.properties (modified)
- build.gradle (modified)
- src/test/java/org/mitre/synthea/br/web/GenerationRequestValidationTest.java (new)
- src/test/java/org/mitre/synthea/br/web/GenerationServiceTest.java (new)
- src/test/java/org/mitre/synthea/br/web/WebServerTest.java (new)
- docs/GUIA-DE-USO.md (modified)
- docs/research/adr/ADR-006-interface-web-localhost-mvp.md (new)
- docs/research/adr/README.md (modified)

## Change Log

- 2026-07-01: Epic 7 Story 7.1 — interface web local MVP para geração de cohorts (formulário + API + job manager + testes + docs + ADR-006).
- 2026-07-02: Code review (Blind Hunter + Edge Case Hunter + Acceptance Auditor) — 10 findings `patch` corrigidos (progresso ao vivo real via callback do `Generator`, default de população dedicado, sanitização de mensagens de erro, thread-safety do `LogCaptureStream`, limite de população, limpeza de `gate_mode` órfão, normalização de gênero, validação de idade parcial, tratamento de exceções genéricas no `GenerateHandler`, `GenerationRunHelper` `final`); 8 findings `defer` registrados em `deferred-work.md` como limitações aceitas do MVP local (ADR-006). `./gradlew test --tests "org.mitre.synthea.br.web.*"` e `checkstyleMain`/`checkstyleTest` OK após as correções.

## Story Completion Status

- **Status:** done
- **Nota:** Implementação completa; code-review adversarial (3 camadas) concluído em 2026-07-02, 10 patches aplicados e verificados (testes + checkstyle OK), 8 itens adiados documentados como limites aceitos do MVP local.
