---
baseline_commit: 65c589c4dfe83b885d38e1c39af012859ea536e2
---

# Story 9.4: HTML Narrativo por Fase — Modo Orientador (Abordagem C)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como estudante apresentando cohort ao orientador,
quero timeline agrupada por **fase clínica** e modo "orientador" que oculta ruído residual,
para que a narrativa pareça seguir rastreio → diagnóstico → tratamento → seguimento.

## Acceptance Criteria

1. **Given** Story 9.3 implementada e `exporter.html.export=true`
   **When** `exporter.html.pathway_mode=orientador` (default quando `br.pathway.focus=true`)
   **Then** cada paciente exibe timeline **agrupada por fase** na ordem canônica do catálogo 9.2, com eventos ordenados cronologicamente dentro de cada fase
   [Source: planning-artifacts/epics.md#Story-9.4; planning-artifacts/epics.md#FR21]

2. **Given** três modos de visualização são suportados
   **When** o pesquisador configura `exporter.html.pathway_mode`
   **Then** valores aceitos são: `orientador`, `pesquisador`, `full` — com semântica documentada:
   - `orientador`: apenas eventos da trajetória (+ demografia)
   - `pesquisador`: trajetória + seção colapsável "Fora da trajetória"
   - `full`: equivalente ao Epic 6 (export HTML integral)
   [Source: planning-artifacts/epics.md#Story-9.4 AC modos]

3. **Given** `br.pathway.focus=true` e `exporter.html.pathway_mode` ausente
   **When** HTML é gerado
   **Then** default é `orientador` (não `full`)
   [Source: planning-artifacts/epics.md#Story-9.4 AC default orientador]

4. **Given** condição-alvo `breast_cancer` ativa
   **When** timeline é renderizada
   **Then** condição-alvo possui destaque visual ao longo da timeline (complemento v1.1 do brainstorm Epic 6 — badge, cor ou ícone consistente)
   [Source: planning-artifacts/epics.md#Story-9.4 AC destaque visual; _bmad-output/implementation-artifacts/6-1-cohort-narrative-viewer-export-html-mvp.md non-goals v1.1]

5. **Given** `br.profile=br` ativo
   **When** labels e fases são renderizados
   **Then** títulos de fase e rótulos UI estão em português do Brasil, usando títulos do catálogo 9.2
   [Source: planning-artifacts/epics.md#Story-9.4 AC PT-BR; Epic 3]

6. **Given** AD-2 (export read-only)
   **When** templates HTML são processados
   **Then** implementação read-only em `HtmlExporter` / templates FreeMarker — dados lidos de `Person`/`HealthRecord` filtrado (9.3) ou view derivada, sem mutação clínica permanente
   [Source: planning-artifacts/epics.md#Story-9.4; ARCHITECTURE-SPINE.md#AD-2]

7. **Given** modo `orientador` ativo
   **When** HTML de um paciente é inspecionado
   **Then** eventos fora da allowlist do catálogo **não aparecem** na timeline principal (SM-9.1: ≥ 80% eventos classificados como trajetória no export focus)
   [Source: planning-artifacts/epics.md#Epic-9 métricas SM-9.1]

8. **Given** a suíte de testes
   **When** `./gradlew check` é executado
   **Then** testes cobrem agrupamento por fase, ordem canônica, ausência de eventos irrelevantes no modo orientador, e presença da seção colapsável no modo pesquisador
   [Source: planning-artifacts/epics.md#Story-9.4 AC testes]

9. **Given** Epic 4 (plausibilidade) recomendado antes de produção
   **When** PLAUS-002 (sequência temporal) estiver disponível
   **Then** violações podem ser exibidas como aviso não-bloqueante no HTML (opcional nesta story; não bloquear MVP se Epic 4 pendente)
   [Source: planning-artifacts/epics.md#Story-9.4 Dependências "Recomendado: Epic 4"]

## Tasks / Subtasks

- [x] Task 1: Config `exporter.html.pathway_mode` (AC: #1, #2, #3)
  - [x] Subtask 1.1: Adicionar property em `synthea.properties` com valores documentados
  - [x] Subtask 1.2: Implementar default condicional: `orientador` quando `br.pathway.focus=true`, senão `full`
  - [x] Subtask 1.3: Parsing com erro claro para valores inválidos

- [x] Task 2: Modelo de dados para template (AC: #1, #5)
  - [x] Subtask 2.1: Estender preparação de modelo em `HtmlExporter` — agrupar eventos por `phase_id` via `PathwayCatalog`
  - [x] Subtask 2.2: Classificar cada evento exportável em fase ou "out_of_pathway"
  - [x] Subtask 2.3: Ordenar fases pela ordem canônica do catálogo; eventos por timestamp dentro da fase

- [x] Task 3: Templates FreeMarker (AC: #1, #2, #4, #6)
  - [x] Subtask 3.1: Criar partial `sections/pathway-phases.ftl` — timeline por fase
  - [x] Subtask 3.2: Modo pesquisador: seção `<details>` "Fora da trajetória"
  - [x] Subtask 3.3: Destaque visual condição-alvo (CSS/badge)
  - [x] Subtask 3.4: Labels PT-BR das fases a partir do catálogo

- [x] Task 4: Integração com export focus (AC: #1, #7)
  - [x] Subtask 4.1: Reutilizar `PathwayExportFilter` (9.3) ou view equivalente antes de render
  - [x] Subtask 4.2: Modo `full` bypassa filtro pathway (mantém Epic 6)
  - [x] Subtask 4.3: Snapshot pré-filtro via ThreadLocal (não poluir `Person.attributes`) para pesquisador+focus

- [x] Task 5: Testes (AC: #8)
  - [x] Subtask 5.1: `HtmlExporterPathwayTest.java` — assert agrupamento por fase no HTML
  - [x] Subtask 5.2: Modo orientador — substring/ausência de evento ruído conhecido
  - [x] Subtask 5.3: Modo pesquisador — presença seção colapsável
  - [x] Subtask 5.4: Rodar testes 9.4 (`HtmlExporterPathwayTest` + `PathwayHtmlModeConfigTest`) — verdes; `./gradlew check` global ainda falha em teste pré-existente Epic 6 (insurance)

## Dev Notes

### Abordagem C — camada de apresentação sobre filtro 9.3

Esta story estende o Cohort Narrative Viewer (Epic 6) com **semântica de fase clínica**. A lógica de classificação de eventos deve reutilizar `PathwayCatalog` — não duplicar allowlists nos templates FTL.

### Dependências

- **Depende de:** Story 9.3, Epic 6 (Story 6.1 — `HtmlExporter` base)
- **Recomendado:** Epic 4 (PLAUS-002 sequência temporal) antes de uso em produção/apresentação formal

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `exporter.html.export` | `false` | Pré-requisito Epic 6 |
| `exporter.html.pathway_mode` | `orientador` se focus; senão `full` | Modo de narrativa por fase |
| `br.pathway.focus` | `false` | Influencia default do pathway_mode |
| `br.profile` | — | PT-BR quando `br` |

### Project Structure Notes

```
src/main/java/org/mitre/synthea/export/
  HtmlExporter.java                         <- agrupamento por fase
  Exporter.java                             <- ThreadLocal HTML_SOURCE_PERSON
src/main/java/org/mitre/synthea/br/pathway/
  PathwayHtmlModeConfig.java
  PathwayHtmlModelBuilder.java
src/main/resources/templates/html/
  sections/pathway-phases.ftl               <- partial por fase
src/test/java/org/mitre/synthea/export/
  HtmlExporterPathwayTest.java
src/test/java/org/mitre/synthea/br/pathway/
  PathwayHtmlModeConfigTest.java
```

### Testing Standards Summary

JUnit 4. Reutilizar padrão de assert substring/DOM leve de `HtmlExporterTest`. Testes específicos 9.4 obrigatórios; `./gradlew check` global tem falha pré-existente fora do escopo.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.4]
- [Source: _bmad-output/planning-artifacts/epics.md#FR21]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/6-1-cohort-narrative-viewer-export-html-mvp.md, 9-3-export-focado-trajetoria-abordagem-c.md]

## Dev Agent Record

### Agent Model Used

Cursor Grok 4.5 (implement) + Claude Sonnet (adversarial code-review)

### Debug Log References

- Code-review blocked initially: stash via `Person.attributes` → StackOverflow risk with JSONExporter
- Fix: `ThreadLocal<Person> HTML_SOURCE_PERSON` in `Exporter`

### Completion Notes List

- Modos `orientador` / `pesquisador` / `full` via `PathwayHtmlModeConfig`
- Default `orientador` quando `br.pathway.focus=true`
- Timeline por fase + seção "Fora da trajetória" (pesquisador)
- Orientador oculta seções clínicas fora da trajetória (mantém demografia + timeline)
- HTML usa snapshot pré-filtro (ThreadLocal) para pesquisador+focus; CSV/FHIR continuam filtrados
- Sync de `record` no loop `hasMultipleRecords` entre person filtrado e snapshot HTML
- Acentuação PT-BR corrigida em títulos/descrições do catálogo `breast_cancer_phases.json`
- Documentado em `docs/GUIA-DE-USO.md`

### File List

- src/main/java/org/mitre/synthea/export/Exporter.java
- src/main/java/org/mitre/synthea/export/HtmlExporter.java
- src/main/java/org/mitre/synthea/br/pathway/PathwayHtmlModeConfig.java
- src/main/java/org/mitre/synthea/br/pathway/PathwayHtmlModelBuilder.java
- src/main/resources/templates/html/patient-accordion.ftl
- src/main/resources/templates/html/sections/pathway-phases.ftl
- src/main/resources/br/pathways/breast_cancer_phases.json
- src/main/resources/synthea.properties
- docs/GUIA-DE-USO.md
- src/test/java/org/mitre/synthea/export/HtmlExporterPathwayTest.java
- src/test/java/org/mitre/synthea/br/pathway/PathwayHtmlModeConfigTest.java
- _bmad-output/implementation-artifacts/9-4-html-narrativo-por-fase-modo-orientador.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-07-09: Story 9.4 implemented — pathway HTML modes, phase timeline, orientador hide noise
- 2026-07-09: Code-review Highs fixed — ThreadLocal stash (no Person.attributes pollution)
- 2026-07-09: Review Med — multi-record HTML record sync; PT-BR accents in catalog
- 2026-07-09: Status → done

## Senior Developer Review (AI)

**Outcome:** PASS_WITH_NOTES → Highs addressed; Meds partially addressed in-session  
**Review date:** 2026-07-09  
**Reviewer model:** Claude Sonnet (adversarial; separate from implementer)

### Action Items

- [x] [High] Stash via Person.attributes → ThreadLocal (StackOverflow / JSONExporter)
- [x] [High] orientador hides non-pathway clinical sections
- [x] [High] pesquisador+focus uses pre-filter person for out-of-pathway
- [x] [Med] hasMultipleRecords + focus: sync htmlSourcePerson.record per key
- [x] [Med] PT-BR accents in breast_cancer_phases.json titles/descriptions
- [ ] [Med] Warn when pathway_mode set without target_condition (silent degrade to flat) — deferred
- [ ] [Med] ./gradlew check global — HtmlExporterTest insurance failure pré-existente (fora de escopo)
- [ ] [Low] Indicate active pathway mode in HTML UI
- [ ] [Low] Concurrent ThreadLocal isolation test

### Severity breakdown

- High: 3 (resolvidos — 0 novos no re-review)
- Med: 5 (2 resolvidos in-session; 2 deferred; 1 pré-existente)
- Low: 6 (débito)

## Tasks / Subtasks → Review Follow-ups (AI)

- [x] [AI-Review][High] ThreadLocal HTML source person
- [x] [AI-Review][High] Hide clinical sections in orientador
- [x] [AI-Review][High] Pre-filter snapshot for pesquisador
- [x] [AI-Review][Med] Multi-record record sync
- [x] [AI-Review][Med] Catalog PT-BR accents
