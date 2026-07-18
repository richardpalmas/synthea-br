---
baseline_commit: 65c589c4dfe83b885d38e1c39af012859ea536e2
origin: investigacao-ui-vs-epics-4-5-8-9-2026-07-10
---

# Story 7.2: Interface Web — Trajetória Focada e Artefatos Pós-Geração

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador ou estudante usando a interface web do Synthea-br,
quero configurar trajetória clínica focada (Epic 9) e acessar artefatos de validação pós-geração (Epic 4),
para que a geração pela UI produza o mesmo resultado enriquecido que a receita CLI documentada — sem depender de flags manuais ou edição de `synthea.properties`.

## Contexto / Motivação

**Problema observado (2026-07-10):** após implementação dos Epics 4, 5, 8 e 9, cohorts geradas pela Web UI com preset “câncer de mama” produzem HTML no estilo Epic 6 (timeline plana, vida inteira, comorbidades) — indistinguíveis do MVP original.

**Causa raiz confirmada:** Story 7.1 entregou formulário MVP (Epic 7) **antes** do Epic 9. `GenerationService.applyRequestToConfig()` não propaga flags de trajetória; defaults em `synthea.properties` mantêm `br.pathway.focus=false`, `module_profile=full`, `trajectory_mode=lifespan`. Epic 4 gera `plausibility_report.json` (default `br.plausibility.report.enabled=true`) mas a UI não linka o arquivo. Epic 8 já tem checkbox; Epic 5 manifest já é detectado mas sem campos Epic 9 no manifest quando flags não são setadas.

Esta story **fecha a lacuna de produto** entre backend/CLI e Web UI — não reimplementa lógica clínica (AD-1, AD-2).

## Acceptance Criteria

1. **Given** a página web carregada (`GET /api/config/options`)
   **When** o usuário expande a seção **“Trajetória clínica focada”**
   **Then** os controles abaixo estão disponíveis com labels e help text em PT-BR:
   - **Foco na trajetória** (`br.pathway.focus`) — checkbox, default **desligado** (compatibilidade retroativa)
   - **Modo HTML** (`exporter.html.pathway_mode`) — select: `auto` (default), `orientador`, `pesquisador`, `full`
     - Help: `auto` = `orientador` quando focus ligado, senão `full` (mesma semântica de `PathwayHtmlModeConfig.resolveMode()`)
   - **Perfil de módulos** (`br.generation.module_profile`) — select: `full` (default), `pathway_minimal`
   - **Modo de trajetória** (`br.generation.trajectory_mode`) — select: `lifespan` (default), `episodic`
   - **Janela de simulação** (`br.generation.simulation_window`) — select: `full_lifespan` (default), `pre_onset_years:10` (MVP piloto; valor fixo ou numérico 5–15 documentado)
   **And** a seção fica **visível/relevante** quando condição alvo está selecionada (mesmo padrão do gate mode)
   [Source: docs/GUIA-DE-USO.md §8 Epic 9; synthea.properties L392–418]

2. **Given** parâmetros válidos submetidos via `POST /api/generate`
   **When** `GenerationService.applyRequestToConfig()` executa
   **Then** cada campo de trajetória é mapeado para `Config.set(...)` ou `Config.remove(...)` quando desligado/default — **sem estado órfão** entre jobs (lição Story 7.1: gate_mode)
   **And** mapeamento exato:

   | Campo UI | Property | Comportamento quando off/default |
   | --- | --- | --- |
   | `pathwayFocus=false` | `br.pathway.focus` | `Config.set("br.pathway.focus", "false")` ou remove → default false |
   | `htmlPathwayMode=auto` | `exporter.html.pathway_mode` | `Config.remove` → resolver usa focus |
   | `htmlPathwayMode=orientador\|pesquisador\|full` | `exporter.html.pathway_mode` | `Config.set` explícito |
   | `moduleProfile=full` | `br.generation.module_profile` | remove ou `full` |
   | `moduleProfile=pathway_minimal` | `br.generation.module_profile` | `Config.set` |
   | `trajectoryMode=lifespan` | `br.generation.trajectory_mode` | remove ou `lifespan` |
   | `trajectoryMode=episodic` | `br.generation.trajectory_mode` | `Config.set` |
   | `simulationWindow=full_lifespan` | `br.generation.simulation_window` | remove |
   | `simulationWindow=pre_onset_years:N` | `br.generation.simulation_window` | `Config.set` |

   **And** a geração usa o **mesmo** `Generator` + pipeline de export — zero reimplementação clínica
   [Source: GenerationService.java; Story 7.1 AD-1]

3. **Given** validação server-side em `GenerationRequest.validate()`
   **When** combinações inválidas são submetidas
   **Then** erros claros em PT-BR **antes** de iniciar o job:
   - `pathwayFocus=true` ou `moduleProfile=pathway_minimal` ou `trajectoryMode=episodic` **sem** `targetCondition` → rejeitar
   - `trajectoryMode=episodic` com condição ≠ `breast_cancer` → rejeitar (MVP 9.7)
   - `simulationWindow=pre_onset_years:N` sem **ambos** `minAge` e `maxAge` → rejeitar (fail-fast `SimulationWindowConfig`)
   - `simulationWindow=pre_onset_years:N` com `N >= minAge` → rejeitar
   - `simulationWindow=pre_onset_years:N` para breast_cancer com N fora de 5–15 → rejeitar
   - `htmlPathwayMode` valor desconhecido → rejeitar
   [Source: TrajectoryModeConfig.validateEpisodicPrerequisites; SimulationWindowConfig.validateForGeneration]

4. **Given** o botão **“Aplicar preset: cohort câncer de mama”**
   **When** o usuário clica
   **Then** além dos campos atuais (F, 45–75, perfil BR, `breast_cancer`), o preset também ativa a **receita trajetória focada** documentada em GUIA receita H:
   - `pathwayFocus=true`
   - `htmlPathwayMode=auto` (→ orientador)
   - `moduleProfile=pathway_minimal`
   - `trajectoryMode=episodic`
   - `simulationWindow=pre_onset_years:10`
   **And** help text do preset explica que isso equivale à receita CLI §11 receita H
   [Source: docs/GUIA-DE-USO.md §11 receita H; investigação UI 2026-07-10]

5. **Given** geração concluída com sucesso
   **When** o usuário consulta status (`GET /api/generate/status`)
   **Then** além dos links existentes (output, manifest, HTML), a UI exibe quando aplicável:
   - Link para `plausibility_report.json` quando o arquivo existir no diretório de output (`PlausibilityReportWriter.REPORT_FILENAME`)
   - Indicador textual dos modos ativos no job (ex.: “Trajetória focada: sim · Perfil: pathway_minimal · HTML: orientador”) derivado do request ou manifest
   **And** mensagens em PT-BR; paths como `file:///` no mesmo padrão do link HTML existente
   [Source: GenerationJobManager; PlausibilityReportWriter.java]

6. **Given** `GET /api/config/options` estendido (`WebUiOptions`)
   **When** o formulário carrega
   **Then** JSON inclui metadados para dropdowns: valores permitidos, defaults, help texts, e objeto `focusedTrajectoryPreset` espelhando receita H (para preset e documentação inline)
   **And** compatibilidade retroativa: clientes antigos ignoram campos novos
   [Source: WebUiOptions.java; app.js loadOptions]

7. **Given** cohort breast_cancer gerada pela web com preset trajetória focada + seed fixa + export HTML
   **When** `output/html/index.html` é inspecionado
   **Then** timeline agrupada por fase clínica está presente (classes `.pathway-phase` ou títulos do catálogo 9.2)
   **And** `manifest.json` contém `pathway_focus: true`, `module_profile: "pathway_minimal"`, `simulation_window` e campos de trajetória Epic 9
   **And** `plausibility_report.json` existe (default Epic 4 ligado)
   [Source: Story 9.4 AC; ResearchManifestWriter.java; SM-9.1]

8. **Given** implementação concluída
   **When** `./gradlew check` é executado
   **Then** novos testes passam cobrindo:
   - Mapeamento Config em `GenerationService` para flags Epic 9 (incl. cleanup/remove)
   - Validação `GenerationRequest` para combinações inválidas (AC #3)
   - `WebServerTest`: POST com trajetória focada → 200; combinação inválida → 400
   - Teste de integração: 1 paciente web path com `pathwayFocus=true` → manifest contém `pathway_focus`
   **And** testes existentes `org.mitre.synthea.br.web.*` continuam verdes
   [Source: project-context.md Testing Rules; Story 7.1 test patterns]

9. **Given** documentação
   **When** `docs/GUIA-DE-USO.md` §10 é revisada
   **Then** tabela de campos inclui trajetória focada e artefatos pós-geração
   **And** receita G (web) é atualizada para mencionar preset trajetória vs defaults Epic 6
   **And** nota explícita: **até Story 7.2**, web ≠ CLI receita H; **após 7.2**, preset equivale
   [Source: GUIA §10, §11 receitas G e H]

10. **Given** escopo e regressão
    **When** flags de trajetória **não** são alteradas pelo usuário (defaults)
    **Then** comportamento permanece idêntico ao Story 7.1 (HTML Epic 6 flat, simulação lifespan)
    **And** CLI (`App.main`, `run_synthea.bat`) inalterada salvo código compartilhado em `GenerationService`/`GenerationRequest` (sem impacto quando `--web` não usado)

## Tasks / Subtasks

- [x] Task 1: Estender DTOs API (AC: #2, #3, #6)
  - [x] Subtask 1.1: Adicionar campos em `GenerationRequest` com defaults seguros (backward compatible JSON)
  - [x] Subtask 1.2: Estender `validate()` com regras AC #3 — reutilizar mensagens de `TrajectoryModeConfig` / `SimulationWindowConfig` quando possível
  - [x] Subtask 1.3: Estender `WebUiOptions` — `TrajectoryOptions` nested class com enums/labels PT-BR
  - [x] Subtask 1.4: Estender `GenerationStatus` — `plausibilityReportPresent`, `plausibilityReportPath`, `trajectorySummary` (ou campos individuais)

- [x] Task 2: Mapeamento Config (AC: #2, #10)
  - [x] Subtask 2.1: Implementar `applyTrajectoryToConfig(GenerationRequest)` em `GenerationService`
  - [x] Subtask 2.2: Garantir `Config.remove` para properties não setadas (evitar bleed entre jobs)
  - [x] Subtask 2.3: Não setar `br.pathway.timing_priors` na UI (permanece `default` de properties quando episodic)

- [x] Task 3: UI formulário (AC: #1, #4, #5)
  - [x] Subtask 3.1: Nova `<section>` em `index.html` — “Trajetória clínica focada”
  - [x] Subtask 3.2: `app.js` — visibilidade condicional (condição alvo), `buildPayload()`, preset estendido
  - [x] Subtask 3.3: `app.js` — pós-geração: link plausibility + resumo trajetória no painel status
  - [x] Subtask 3.4: CSS mínimo em `app.css` se necessário (agrupamento visual da seção)

- [x] Task 4: Job manager / status (AC: #5)
  - [x] Subtask 4.1: `GenerationJobManager.runJob` — detectar `plausibility_report.json` após run
  - [x] Subtask 4.2: Propagar resumo de config trajetória do request para status snapshot

- [x] Task 5: Testes (AC: #7, #8)
  - [x] Subtask 5.1: `GenerationRequestValidationTest` — casos AC #3
  - [x] Subtask 5.2: `GenerationServiceTest` — pathway focus + manifest fields
  - [x] Subtask 5.3: `WebServerTest` — payload trajetória focada
  - [x] Subtask 5.4: `./gradlew test --tests "org.mitre.synthea.br.web.*"`

- [x] Task 6: Documentação (AC: #9)
  - [x] Subtask 6.1: Atualizar `docs/GUIA-DE-USO.md` §10 e receita G
  - [x] Subtask 6.2: Comentários em `synthea.properties` referenciando campos web (opcional, 1–2 linhas)

### Review Findings

- [x] [Review][Defer] Preset mama deve marcar export HTML? — deferred: preset só trajetória; usuário marca exports manualmente; GUIA não deve afirmar equivalência total à receita H (exports)
- [x] [Review][Patch] Relatório de plausibilidade pode ser stale (`exists()` sem frescor) [GenerationJobManager.java]
- [x] [Review][Patch] Normalizar `simulationWindow` para lower-case antes de `Config.set` [TrajectoryWebConstants.java]
- [x] [Review][Patch] Guardar `NumberFormatException` em `pre_onset_years` com N enorme [TrajectoryWebConstants.java:validateSimulationWindow]
- [x] [Review][Patch] Client: não tratar `simulationWindow` vazio como janela ativa antes de `loadOptions` [app.js:validateClient]
- [x] [Review][Patch] `encodeURI` no path `file:///` do link de plausibilidade [app.js:pollStatus]
- [x] [Review][Patch] Nota histórica AC #9 + clarificar que preset ≠ exports da receita H [docs/GUIA-DE-USO.md §10]
- [x] [Review][Patch] Testes AC #3 faltando: episodic≠breast_cancer; N fora 5–15; pathway_minimal sem target [GenerationRequestValidationTest.java]
- [x] [Review][Defer] Snapshot Config só restaura chaves de trajetória — deferred, padrão pré-existente 7.1 para exports/AI
- [x] [Review][Defer] `file://` bloqueado a partir de origem HTTP — deferred, mesmo contrato UX do link HTML 7.1
- [x] [Review][Defer] AC #7 HTML por fase sem assert no path web — deferred, Dev Notes permitem smoke via HtmlExporterPathwayTest
- [x] [Review][Defer] `./gradlew check` global com falhas estatísticas — deferred, BrDemographicsIntegrationTest pré-existente
- [x] [Review][Defer] Creep em synthea.properties / PlausibilityReportWriter além do mínimo 7.2 — deferred, working tree misturado com Epics 4/8/9

## Dev Notes

### Escopo explícito — o que NÃO implementar

- Toggle `br.plausibility.report.enabled` na UI (permanece default `true`; apenas **link** ao artefato)
- Exibir proveniência Epic 5 inline no HTML (permanece em manifest/FHIR)
- `br.pathway.timing_priors` customizado, upload de properties, geografia upstream
- Avisos PLAUS-### inline no HTML (Epic 4 / 9.4 defer — fora de escopo)
- WebSocket, jobs paralelos, autenticação
- Alterar defaults globais de `synthea.properties` (UI opt-in, CLI defaults intactos)

### Padrão arquitetural obrigatório

- **AD-1 In-Process:** UI continua disparando `Generator` existente
- **AD-2 Read-only:** UI não muta `HealthRecord` — apenas `Config.set`
- **AD-7 Upstream First:** alterações em `org.mitre.synthea.br.web.*`; diff mínimo elsewhere
- **Reutilizar** classes Epic 9 existentes — **não** duplicar validação de pathway/catalog

### Estado atual dos arquivos (baseline — ler antes de editar)

**`GenerationRequest.java`** — campos atuais: seed, population, gender, ages, brProfile, targetCondition, gateMode, exports, aiEnrichment. **Sem** pathway.

**`GenerationService.applyRequestToConfig()`** — seta br.profile, target_condition, gate, exports, ai. **Não** toca pathway/generation flags.

**`WebUiOptions.java`** — breastCancerPreset só tem gender/minAge/maxAge/helpText.

**`app.js` preset** — preenche demografia + condição; **não** trajetória.

**Defaults `synthea.properties`:**
```
br.pathway.focus = false
br.generation.module_profile = full
br.generation.trajectory_mode = lifespan
br.generation.simulation_window = full_lifespan
br.plausibility.report.enabled = true
exporter.html.pathway_mode → (unset; resolve via PathwayHtmlModeConfig)
```

### Fluxo técnico alvo

```
Browser buildPayload()
  → POST /api/generate { ..., pathwayFocus, moduleProfile, ... }
  → GenerationRequest.validate()  // fail-fast AC #3
  → GenerationService.applyRequestToConfig()
       applyTrajectoryToConfig()   // NEW
  → Generator.run() → export → manifest + plausibility_report.json
  → GET /api/generate/status
       links: output/, html/, plausibility_report.json
```

### Validação — preferir delegar ao pipeline quando possível

Client-side (`app.js`) deve espelhar regras óbvias (episodic sem condição, window sem idade) para UX rápida. Server-side é **autoritativo** — espelhar mensagens PT-BR de:
- `TrajectoryModeConfig.validateEpisodicPrerequisites()` → IllegalStateException text
- `SimulationWindowConfig.validateForGeneration()` → IllegalArgumentException text
- `PathwayFocusConfig.validateFocusPrerequisites()` → quando focus=true sem target

Converter exceções do `Generator` em mensagens user-friendly em `GenerationService.toUserMessage()` se novos padrões aparecerem.

### Preset “cohort câncer de mama” — decisão de produto

Story 7.1: preset = demografia + condição (reduzir tentativas gate).
Story 7.2: preset = **receita H completa** (trajetória focada) porque usuários esperam “melhorias Epic 9” ao usar preset oncológico.

Usuários que queiram cohort direcionada **sem** trajetória focada desmarcam manualmente os toggles ou não usam preset.

### `htmlPathwayMode=auto` — implementação sugerida

- Request JSON: `"htmlPathwayMode": "auto"` ou omitido
- `GenerationService`: `Config.remove("exporter.html.pathway_mode")` → `PathwayHtmlModeConfig.resolveMode()` aplica default condicional
- Valores explícitos: setar property diretamente

### Detecção pós-geração

```java
// GenerationJobManager.runJob — após generator.run()
File plausibility = new File(outputDir, "plausibility_report.json");
plausibilityReportPresent = plausibility.exists();
plausibilityReportPath = plausibilityReportPresent ? plausibility.getPath() : null;
```

### Estrutura de arquivos

```
src/main/java/org/mitre/synthea/br/web/
  GenerationRequest.java          UPDATE
  GenerationService.java          UPDATE
  GenerationStatus.java           UPDATE
  GenerationJobManager.java       UPDATE
  WebUiOptions.java               UPDATE

src/main/resources/br/web/
  index.html                      UPDATE
  app.js                          UPDATE
  app.css                         UPDATE (opcional)

src/test/java/org/mitre/synthea/br/web/
  GenerationRequestValidationTest.java  UPDATE
  GenerationServiceTest.java            UPDATE
  WebServerTest.java                    UPDATE

docs/GUIA-DE-USO.md               UPDATE §10, receita G
```

### Testing Standards Summary

- JUnit 4; restaurar `Config` em `@After` (padrão `GenerationServiceTest`)
- Porta efêmera em `WebServerTest`
- Teste integração pathway: `exportHtml=true`, `pathwayFocus=true`, `targetCondition=breast_cancer`, n=1 — assert manifest JSON contém `"pathway_focus":true` (parse Gson leve)
- Não exigir assert HTML completo por fase na web test (coberto por `HtmlExporterPathwayTest` Story 9.4) — smoke suficiente

### Dependências

| Story | Relação |
| --- | --- |
| 7.1 | Base web UI — **estender**, não reescrever |
| 9.3–9.7 | Backend flags — **consumir** via Config |
| 9.4 | HTML por fase — resultado visível quando AC #7 |
| 4.2 | plausibility_report.json — linkar, não reimplementar |
| 1.4 | manifest.json — campos pathway já implementados em `ResearchManifestWriter` |

### Anti-patterns a evitar

- ❌ Duplicar allowlists de fases do `PathwayCatalog` na UI
- ❌ Forçar `pathwayFocus=true` globalmente para todos os runs web (quebra regressão AC #10)
- ❌ Esquecer `Config.remove` → job anterior contamina job seguinte
- ❌ `Runtime.exec` CLI a partir da UI
- ❌ Introduzir Jackson — manter Gson

### References

- [Source: investigação UI vs Epics 4/5/8/9 — conversa 2026-07-10]
- [Source: _bmad-output/implementation-artifacts/7-1-interface-web-geracao-sintetica-mvp.md]
- [Source: docs/GUIA-DE-USO.md §8 Epic 9, §10 Web, §11 receitas G e H]
- [Source: docs/research/adr/ADR-006-interface-web-localhost-mvp.md]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: src/main/java/org/mitre/synthea/br/web/GenerationService.java]
- [Source: src/main/java/org/mitre/synthea/br/pathway/PathwayFocusConfig.java]
- [Source: src/main/java/org/mitre/synthea/br/pathway/TrajectoryModeConfig.java]
- [Source: src/main/java/org/mitre/synthea/br/pathway/generation/ModuleProfileConfig.java]
- [Source: src/main/java/org/mitre/synthea/br/pathway/generation/SimulationWindowConfig.java]
- [Source: src/main/java/org/mitre/synthea/br/pathway/PathwayHtmlModeConfig.java]
- [Source: src/main/java/org/mitre/synthea/br/plausibility/PlausibilityReportWriter.java]
- [Source: src/main/java/org/mitre/synthea/br/research/ResearchManifestWriter.java]

## Dev Agent Record

### Agent Model Used

Composer

### Debug Log References

- `./gradlew test --tests "org.mitre.synthea.br.web.*"` — 28 testes OK
- Corrigido vazamento de `Config` global após jobs web assíncronos via `TrajectoryConfigSnapshot` em `GenerationService.run()`

### Completion Notes List

- Seção **Trajetória clínica focada** no formulário web com mapeamento completo para flags Epic 9.
- Preset câncer de mama aplica flags de trajetória da receita H (focus + pathway_minimal + episodic + pre_onset_years:10); exportações seguem manuais.
- Status pós-geração exibe resumo de trajetória e link para `plausibility_report.json`.
- `TrajectoryWebConstants` centraliza validação PT-BR e metadados de opções.
- `PlausibilityReportWriter.getReportFilename()` exposto para detecção de artefato.
- Documentação GUIA §10 e receita G atualizadas.

### File List

- src/main/java/org/mitre/synthea/br/web/TrajectoryWebConstants.java (new)
- src/main/java/org/mitre/synthea/br/web/GenerationRequest.java (modified)
- src/main/java/org/mitre/synthea/br/web/GenerationService.java (modified)
- src/main/java/org/mitre/synthea/br/web/GenerationStatus.java (modified)
- src/main/java/org/mitre/synthea/br/web/GenerationJobManager.java (modified)
- src/main/java/org/mitre/synthea/br/web/WebUiOptions.java (modified)
- src/main/java/org/mitre/synthea/br/plausibility/PlausibilityReportWriter.java (modified)
- src/main/resources/br/web/index.html (modified)
- src/main/resources/br/web/app.js (modified)
- src/main/resources/synthea.properties (modified)
- src/test/java/org/mitre/synthea/br/web/GenerationRequestValidationTest.java (modified)
- src/test/java/org/mitre/synthea/br/web/GenerationServiceTest.java (modified)
- src/test/java/org/mitre/synthea/br/web/GenerationServiceTrajectoryConfigTest.java (new)
- src/test/java/org/mitre/synthea/br/web/WebServerTest.java (modified)
- docs/GUIA-DE-USO.md (modified)

## Change Log

- 2026-07-10: Story 7.2 — extensão web UI para trajetória focada Epic 9, artefatos pós-geração, preset receita H, testes e docs.
- 2026-07-10: Code review — 7 patches aplicados; 6 itens deferidos documentados em `deferred-work.md`.

## Story Completion Status

- **Status:** done
- **Nota:** Implementação completa; 28 testes `org.mitre.synthea.br.web.*` verdes. `./gradlew check` global pode falhar em testes estatísticos pré-existentes (`BrDemographicsIntegrationTest`).
