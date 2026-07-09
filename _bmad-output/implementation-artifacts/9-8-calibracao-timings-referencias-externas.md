# Story 9.8: Calibração de Timings a Partir de Referências Externas

Status: backlog

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

- [ ] Task 1: Criar data pack `breast_cancer_timing_priors.json` (AC: #1, #2, #3, #9)
  - [ ] Subtask 1.1: Seguir schema definido no ADR-008 (Story 9.1)
  - [ ] Subtask 1.2: Preencher priors por transição de fase com min/max/median ou buckets
  - [ ] Subtask 1.3: Adicionar `priors_version`, `reference_notes`, links SUS/DATASUS/OncoSynth/Coogee
  - [ ] Subtask 1.4: Revisão de privacidade — zero PHI/microdados

- [ ] Task 2: Implementar loader de timings (AC: #1, #4, #6)
  - [ ] Subtask 2.1: Criar `org.mitre.synthea.br.pathway.PathwayTimingLoader`
  - [ ] Subtask 2.2: Resolver transições por `phase_id` source→target
  - [ ] Subtask 2.3: Integrar com módulo GMF episódico — injeção de distribuições em runtime de simulação

- [ ] Task 3: Manifest e rastreabilidade (AC: #5)
  - [ ] Subtask 3.1: Estender `ResearchManifestWriter` com `pathway_timing_priors_version`, `pathway_reference_notes`
  - [ ] Subtask 3.2: Testes de serialização

- [ ] Task 4: Experimento piloto (AC: #7, #8)
  - [ ] Subtask 4.1: Gerar cohort n=10–50 calibrada vs baseline (mesma seed, priors on/off)
  - [ ] Subtask 4.2: Medir % eventos fora de ordem (script ou relatório plausibilidade Epic 4)
  - [ ] Subtask 4.3: Publicar `docs/research/experiments/exp-9.8-timing-calibration.md` (ou nome convencional)

- [ ] Task 5: Testes (AC: #4, #10)
  - [ ] Subtask 5.1: `PathwayTimingLoaderTest` — parsing, version, phase keys
  - [ ] Subtask 5.2: Determinismo — duas execuções, mesmos intervalos observados
  - [ ] Subtask 5.3: Rodar `./gradlew check`

## Dev Notes

### Fechamento do loop Epic 9 — calibração pós-E (GMF episódico)

Story 9.8 conecta referências bibliográficas (9.1/ADR-008) ao motor de simulação (9.7) via data pack determinístico. **Não** integra OncoSynth/Coogee como runtime — apenas estatísticas importadas.

### Abordagem E + data pack temporal (AD-3)

Timings calibram **Abordagem E**; abordagens C e D beneficiam indiretamente (menos violações temporais no export/narrativa).

### Dependências

- **Depende de:** Story 9.1 (formato ADR-008), Story 9.7 (módulo episódico)
- **Recomendado:** Story 9.2 (phase_id), Epic 4 (SM-2, PLAUS-002 para experimento)
- **Fecha:** loop de calibração com Epic 4 (revisão ADR-001 pós-SM-2 real)

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.pathway.timing_priors` | `default` ou path | Seleciona versão/arquivo de priors (TBD na implementação) |
| `br.generation.trajectory_mode` | `lifespan` | Deve ser `episodic` para priors terem efeito |
| `br.target_condition` | — | `breast_cancer` piloto |

Definir nome final da property durante implementação — documentar no Dev Agent Record.

### Project Structure Notes

```
src/main/resources/br/pathways/
  breast_cancer_timing_priors.json          <- priors temporais
src/main/java/org/mitre/synthea/br/pathway/
  PathwayTimingLoader.java
  PathwayTimingPrior.java                   <- modelo transição
src/main/resources/modules/br/
  breast_cancer_trajectory.json             <- consome priors (9.7)
src/main/java/org/mitre/synthea/br/research/
  ResearchManifestWriter.java               <- pathway_timing_priors_version
docs/research/experiments/
  exp-9.8-timing-calibration.md             <- experimento piloto
src/test/java/org/mitre/synthea/br/pathway/
  PathwayTimingLoaderTest.java
```

### Testing Standards Summary

JUnit 4. Testes unitários rápidos + experimento documental. Integração Epic 4 opcional se pendente. `./gradlew check` obrigatório.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.8]
- [Source: _bmad-output/planning-artifacts/epics.md#FR25]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/9-1-spike-referencias-trajetoria-longitudinal-adr-008.md, 9-7-modulo-gmf-trajetoria-episodica-abordagem-e.md]
- [Source: docs/research/adr/ADR-001-spike-ia-vs-regras.md — revisão pós-SM-2]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
