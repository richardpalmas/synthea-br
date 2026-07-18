---
baseline_commit: 65c589c4dfe83b885d38e1c39af012859ea536e2
---

# Story 9.8: Calibração de Timings a Partir de Referências Externas

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do Synthea-br,
quero importar **priors temporais** (intervalos entre fases) derivados de referências documentadas no ADR-008,
para que transições GMF e ordem narrativa reflitam linhas SUS/DATASUS e literatura oncológica — sem incorporar PHI.

## Acceptance Criteria

1. **Given** Story 9.1 define formato de importação e Story 9.7 implementa módulo episódico
   **When** data pack `src/main/resources/br/pathways/breast_cancer_timing_priors.json` está presente
   **Then** transições entre fases usam distribuições documentadas (min/max/median ou buckets) configuráveis via JSON
   [Source: planning-artifacts/epics.md#Story-9.8; planning-artifacts/epics.md#FR25; docs/research/adr/ADR-008-trajetoria-clinica-focada.md]

2. **Given** fontes citadas no data pack
   **When** priors são inspecionados
   **Then** metadados referenciam agregados SUS/DATASUS públicos e parâmetros inspirados em OncoSynth/Coogee **como metadado bibliográfico** — **não** runtime ML
   [Source: planning-artifacts/epics.md#Story-9.8 AC fontes]

3. **Given** NFR5 (privacidade — proibido PHI no repositório)
   **When** repositório é auditado
   **Then** nenhum arquivo contém PHI ou microdados de paciente real — apenas estatísticas agregadas e citações
   [Source: planning-artifacts/epics.md#Story-9.8 AC NFR5; planning-artifacts/epics.md#NFR5]

4. **Given** NFR1 (determinismo)
   **When** data pack de timings é alterado mas seed + demais config permanecem
   **Then** timings de transição alteram de forma **determinística** e reprodutível
   [Source: planning-artifacts/epics.md#Story-9.8 AC NFR1]

5. **Given** manifest de rastreabilidade (Story 1.4)
   **When** geração com priors calibrados completa
   **Then** `manifest.json` registra `pathway_timing_priors_version` e `pathway_reference_notes` (links/citações)
   [Source: planning-artifacts/epics.md#Story-9.8 AC manifest; ARCHITECTURE-SPINE.md#AD-6]

6. **Given** módulo GMF episódico (9.7) com transições placeholder
   **When** priors JSON é carregado
   **Then** `org.mitre.synthea.br.pathway.PathwayTimingLoader` (ou equivalente) injeta parâmetros nas transições GMF **sem** reestruturar o grafo de states — substituição de distribuições apenas
   [Source: Story 9.7 AC #9; AD-3 data packs]

7. **Given** Epic 4 disponível (SM-2, PLAUS-002)
   **When** experimento piloto é conduzido
   **Then** documento em `docs/research/experiments/` compara cohort episódica **calibrada** vs **não calibrada** — métricas: % eventos fora de ordem, SM-2 quando Epic 4 disponível
   [Source: planning-artifacts/epics.md#Story-9.8 AC experimento]

8. **Given** ADR-008 fecha loop de calibração
   **When** SM-2 real estiver disponível pós-Epic 4
   **Then** nota no experimento ou ADR registra necessidade de revisão ADR-001/ADR-008 se timings calibrados não atingirem SM-2 média
   [Source: planning-artifacts/epics.md#Story-9.8 Dependências "Fecha loop com Epic 4"]

9. **Given** catálogo de fases 9.2
   **When** priors JSON referencia transições
   **Then** chaves de transição usam `phase_id` estáveis do catálogo (ex.: `screening→diagnosis`) — não IDs ad hoc
   [Source: Story 9.2 phase_id; AD-3 consistência]

10. **Given** a suíte de testes
    **When** `./gradlew check` é executado
    **Then** testes unitários validam parsing do JSON, determinismo com seed fixa, e ausência de campos proibidos (PHI patterns)
    [Source: derivado de ACs #3, #4]

## Tasks / Subtasks

- [x] Task 1: Criar data pack `breast_cancer_timing_priors.json` (AC: #1, #2, #3, #9)
  - [x] Subtask 1.1: Seguir schema definido no ADR-008 (Story 9.1)
  - [x] Subtask 1.2: Preencher priors por transição de fase com min/max/median ou buckets
  - [x] Subtask 1.3: Adicionar `priors_version`, `reference_notes`, links SUS/DATASUS/OncoSynth/Coogee
  - [x] Subtask 1.4: Revisão de privacidade — zero PHI/microdados

- [x] Task 2: Implementar loader de timings (AC: #1, #4, #6)
  - [x] Subtask 2.1: Criar `org.mitre.synthea.br.pathway.PathwayTimingLoader`
  - [x] Subtask 2.2: Resolver transições por `phase_id` source→target
  - [x] Subtask 2.3: Integrar com módulo GMF episódico — injeção JSON pré-`Module` (Delay states)
  - [x] Subtask 2.4: Adicionar Delay placeholders em `breast_cancer_trajectory_br.json` (gap 9.7)

- [x] Task 3: Manifest e rastreabilidade (AC: #5)
  - [x] Subtask 3.1: Estender `ResearchManifestWriter` com `pathway_timing_priors_version`, `pathway_reference_notes` (só se episodic)
  - [x] Subtask 3.2: Testes de serialização / unitários loader

- [x] Task 4: Experimento piloto (AC: #7, #8)
  - [x] Subtask 4.1: Protocolo calibrada vs baseline (mesma seed, priors on/off)
  - [x] Subtask 4.2: Métricas CI (JSON Delay) + SM-2 pendente Epic 4
  - [x] Subtask 4.3: Publicar `docs/research/experiments/2026-07-09-timing-calibration/exp-9.8-timing-calibration.md`

- [x] Task 5: Testes (AC: #4, #10)
  - [x] Subtask 5.1: `PathwayTimingLoaderTest` — parsing, version, phase keys, PHI positivo
  - [x] Subtask 5.2: Determinismo — transformação JSON idempotente
  - [x] Subtask 5.3: Rodar testes 9.8 + episódico + manifest

## Dev Notes

### Semântica

- Property: `br.pathway.timing_priors` = `default` | `off` | classpath
- Injeção em `Module.loadFile` via `PathwayTimingLoader.maybeApplyPriors` (só `breast_cancer_trajectory_br.json`)
- Delay states estáveis: `delay_screening_to_diagnosis`, etc.
- Aproximação: ADR `median` → GMF TRIANGULAR `mode` (documentado)
- Limitação: calibra marcadores `pathway_phase`, não timings internos de `breast_cancer.json`

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.pathway.timing_priors` | `default` | Pack de priors / `off` |
| `br.generation.trajectory_mode` | `lifespan` | Precisa `episodic` para efeito clínico + manifest |

### References

- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/implementation-artifacts/9-7-modulo-gmf-trajetoria-episodica-abordagem-e.md]

## Dev Agent Record

### Agent Model Used

Cursor Grok 4.5 (implement) + Claude Sonnet (adversarial code-review)

### Debug Log References

- Review BLOCKED: manifest gravava priors em qualquer run default (proveniência falsa)
- Fix: campos de timing só quando `trajectory_mode=episodic`
- Documentado median→mode; teste PHI positivo

### Completion Notes List

- Data pack v1.0.0 + Delay states no módulo episódico
- `PathwayTimingLoader` + hook `Module.loadFile`
- Manifest condicionado a episodic
- Experimento piloto + GUIA + ADR checkbox
- Limitação fase-marker vs upstream documentada

### File List

- src/main/resources/br/pathways/breast_cancer_timing_priors.json
- src/main/resources/br/pathways/README.md
- src/main/resources/modules/breast_cancer_trajectory_br.json
- src/main/java/org/mitre/synthea/br/pathway/PathwayTimingLoader.java
- src/main/java/org/mitre/synthea/engine/Module.java
- src/main/java/org/mitre/synthea/br/research/ResearchManifestWriter.java
- src/main/resources/synthea.properties
- src/test/java/org/mitre/synthea/br/pathway/PathwayTimingLoaderTest.java
- docs/research/experiments/2026-07-09-timing-calibration/exp-9.8-timing-calibration.md
- docs/research/adr/ADR-008-trajetoria-clinica-focada.md
- docs/GUIA-DE-USO.md
- _bmad-output/implementation-artifacts/9-8-calibracao-timings-referencias-externas.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-07-09: Story 9.8 implemented — timing priors pack, loader, Delay states, experiment
- 2026-07-09: Code-review High fixed — episodic-gated manifest provenance
- 2026-07-09: Status → done

## Senior Developer Review (AI)

**Outcome:** BLOCKED → High addressed → PASS_WITH_NOTES  
**Review date:** 2026-07-09  
**Reviewer model:** Claude Sonnet (adversarial; separate from implementer)

### Action Items

- [x] [High] Manifest timing fields only when episodic
- [x] [Med] Document median→mode approximation
- [x] [Med] Use `loadForConfiguredCondition` in manifest
- [x] [Med] Positive PHI detection test
- [ ] [Med] Full n=10 dual-arm wall-clock experiment outputs — deferred (protocol + CI asserts)
- [ ] [Low] Stronger seed-level delay observation test — deferred

### Severity breakdown

- High: 1 (resolvido)
- Med: 4 (3 resolvidos; 1 deferred protocolo)
- Low: 3 (débito)

## Tasks / Subtasks → Review Follow-ups (AI)

- [x] [AI-Review][High] Episodic-gated manifest
- [x] [AI-Review][Med] median→mode docs
- [x] [AI-Review][Med] PHI positive test
