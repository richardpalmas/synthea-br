# Story 9.4: HTML Narrativo por Fase — Modo Orientador (Abordagem C)

Status: backlog

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

- [ ] Task 1: Config `exporter.html.pathway_mode` (AC: #1, #2, #3)
  - [ ] Subtask 1.1: Adicionar property em `synthea.properties` com valores documentados
  - [ ] Subtask 1.2: Implementar default condicional: `orientador` quando `br.pathway.focus=true`, senão `full`
  - [ ] Subtask 1.3: Parsing com erro claro para valores inválidos

- [ ] Task 2: Modelo de dados para template (AC: #1, #5)
  - [ ] Subtask 2.1: Estender preparação de modelo em `HtmlExporter` — agrupar eventos por `phase_id` via `PathwayCatalog`
  - [ ] Subtask 2.2: Classificar cada evento exportável em fase ou "out_of_pathway"
  - [ ] Subtask 2.3: Ordenar fases pela ordem canônica do catálogo; eventos por timestamp dentro da fase

- [ ] Task 3: Templates FreeMarker (AC: #1, #2, #4, #6)
  - [ ] Subtask 3.1: Criar partial `sections/pathway-phases.ftl` — timeline por fase
  - [ ] Subtask 3.2: Modo pesquisador: seção `<details>` "Fora da trajetória"
  - [ ] Subtask 3.3: Destaque visual condição-alvo (CSS/badge)
  - [ ] Subtask 3.4: Labels PT-BR das fases a partir do catálogo

- [ ] Task 4: Integração com export focus (AC: #1, #7)
  - [ ] Subtask 4.1: Reutilizar `PathwayExportFilter` (9.3) ou view equivalente antes de render
  - [ ] Subtask 4.2: Modo `full` bypassa filtro pathway (mantém Epic 6)

- [ ] Task 5: Testes (AC: #8)
  - [ ] Subtask 5.1: `HtmlExporterPathwayTest.java` — assert agrupamento por fase no HTML
  - [ ] Subtask 5.2: Modo orientador — substring/ausência de evento ruído conhecido
  - [ ] Subtask 5.3: Modo pesquisador — presença seção colapsável
  - [ ] Subtask 5.4: Rodar `./gradlew check`

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
src/main/java/org/mitre/synthea/br/pathway/
  PathwayHtmlModelBuilder.java              <- opcional, se lógica crescer
src/main/resources/templates/html/
  sections/pathway-phases.ftl               <- novo partial
src/test/java/org/mitre/synthea/export/
  HtmlExporterPathwayTest.java
```

### Testing Standards Summary

JUnit 4. Reutilizar padrão de assert substring/DOM leve de `HtmlExporterTest`. `./gradlew check` obrigatório.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.4]
- [Source: _bmad-output/planning-artifacts/epics.md#FR21]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/6-1-cohort-narrative-viewer-export-html-mvp.md, 9-3-export-focado-trajetoria-abordagem-c.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
