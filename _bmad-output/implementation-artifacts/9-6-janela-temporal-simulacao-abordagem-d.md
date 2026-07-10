---
baseline_commit: 65c589c4dfe83b885d38e1c39af012859ea536e2
---

# Story 9.6: Janela Temporal de Simulação — Abordagem D

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do Synthea-br,
quero restringir a simulação a uma janela temporal relevante (`br.generation.simulation_window`),
para não simular décadas de vida irrelevantes antes do risco/onset oncológico.

## Acceptance Criteria

1. **Given** ADR-008 documenta impacto de janela temporal em demografia, seed e comorbidades
   **When** `br.generation.simulation_window=pre_onset_years:N` está configurado (N documentado; ex.: 5–15 para mama)
   **Then** a simulação inicia em `target_age - N` (ou equivalente documentado no ADR) em vez de nascimento, preservando atributos demográficos coerentes com `-a`/IBGE
   [Source: planning-artifacts/epics.md#Story-9.6; planning-artifacts/epics.md#FR23; docs/research/adr/ADR-008-trajetoria-clinica-focada.md]

2. **Given** janela temporal desabilitada (property ausente ou valor `full_lifespan`)
   **When** a geração executa
   **Then** comportamento upstream (simulação desde nascimento) permanece inalterado
   [Source: planning-artifacts/epics.md#Story-9.6 — default implícito]

3. **Given** combinação `simulation_window` + `module_profile=pathway_minimal` (Story 9.5)
   **When** cohort piloto é gerada
   **Then** tempo de geração é mensuravelmente menor vs baseline full lifespan + full profile — duração logada no manifest ou metadata de execução
   [Source: planning-artifacts/epics.md#Story-9.6 AC performance; planning-artifacts/epics.md#Epic-9 SM-9.4]

4. **Given** Epic 2 gate e Epic 4 plausibilidade
   **When** janela temporal está ativa
   **Then** gate continua garantindo 100% condição-alvo (SM-1) e regras de plausibilidade permanecem aplicáveis ao recorte exportado
   [Source: planning-artifacts/epics.md#Story-9.6 AC gate e plausibilidade]

5. **Given** NFR1 (reprodutibilidade)
   **When** mesma seed + config + janela são usadas
   **Then** resultado da simulação é idêntico entre execuções
   [Source: planning-artifacts/epics.md#Story-9.6 AC NFR1]

6. **Given** combinações inválidas de configuração
   **When** janela é incompatible com `-a` (idade alvo) ou N inválido (negativo, zero, acima de limite documentado)
   **Then** inicialização falha com **erro claro** antes de gerar pacientes — não falha silenciosa mid-cohort
   [Source: planning-artifacts/epics.md#Story-9.6 AC combinações inválidas]

7. **Given** AD-7 (extensão organizada, mínima alteração no core)
   **When** janela temporal é implementada
   **Then** lógica de cálculo de start time reside preferencialmente em `org.mitre.synthea.br.pathway.generation.*` com hook mínimo documentado em `Generator` — ADR-008 deve justificar qualquer alteração core além de hook
   [Source: ARCHITECTURE-SPINE.md#AD-7; docs/research/adr/ADR-008-trajetoria-clinica-focada.md]

8. **Given** NFR2 (performance n=500)
   **When** combinação minimal + window é usada em n=500
   **Then** tempo total não regride além do baseline documentado — SM-9.4 exige redução documentada sem violar NFR2
   [Source: planning-artifacts/epics.md#Epic-9 SM-9.4; planning-artifacts/epics.md#NFR2]

9. **Given** manifest de rastreabilidade
   **When** janela temporal está configurada
   **Then** `manifest.json` registra `simulation_window` efetivo e duração de geração (ms ou segundos)
   [Source: Story 9.6 AC log manifest; ARCHITECTURE-SPINE.md#AD-6]

## Tasks / Subtasks

- [x] Task 1: Especificar semântica no ADR-008 ou doc complementar (AC: #1, #7)
  - [x] Subtask 1.1: Confirmar fórmula `target_age - N` vs alternativas (onset module-specific)
  - [x] Subtask 1.2: Documentar impacto em seed/demografia/comorbidades pré-janela
  - [x] Subtask 1.3: Definir limites válidos de N para câncer de mama piloto (5–15)

- [x] Task 2: Config `br.generation.simulation_window` (AC: #1, #2, #6)
  - [x] Subtask 2.1: Adicionar property em `synthea.properties` — formato `pre_onset_years:N` ou `full_lifespan`
  - [x] Subtask 2.2: Parser com validação de N e compatibilidade com `-a`/idade alvo
  - [x] Subtask 2.3: Mensagens de erro claras para combinações inválidas

- [x] Task 3: Implementar `SimulationWindowConfig` (AC: #1, #5, #7)
  - [x] Subtask 3.1: Criar `org.mitre.synthea.br.pathway.generation.SimulationWindowConfig`
  - [x] Subtask 3.2: Calcular start time efetivo para `Person` (`lastUpdated`)
  - [x] Subtask 3.3: Hook em `Generator` — validate + bootstrap lifecycle + defer insurance até window start
  - [x] Subtask 3.4: Preservar demografia IBGE/Epic 3 coerente com idade efetiva

- [x] Task 4: Integração com perfil minimal (AC: #3, #8)
  - [x] Subtask 4.1: Instrumentar `generation_duration_ms`; smoke CI minimal+window vs full; n=500 manual (SM-9.4)
  - [x] Subtask 4.2: Logar duração no manifest

- [x] Task 5: Testes (AC: #4, #5, #6)
  - [x] Subtask 5.1: Integração gate SM-1 com janela ativa
  - [x] Subtask 5.2: Reprodutibilidade seed fixa
  - [x] Subtask 5.3: Testes de config inválida — assert fail-fast (incl. `-a` obrigatório)
  - [x] Subtask 5.4: Teste BR demografia + cobertura pós-janela; testes unitários/integração 9.6

## Dev Notes

### Semântica

```
start = birthdate + (target_age - N) * 365 days
```

- `BIRTHDATE` / demografia (`-a`/IBGE) preservados
- `-a` **obrigatório** quando janela ativa
- Piloto mama: N ∈ [5, 15]
- Insurance: `deferEnrollmentUntil(windowStart)` — cobertura retoma no início da janela

### Dependências

- **Depende de:** Story 9.5, Story 9.1 (ADR-008)
- **Recomendado antes de produção:** Epic 4

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.generation.simulation_window` | `full_lifespan` | Janela temporal (`pre_onset_years:N`) |
| `br.generation.module_profile` | `full` | Combinável com window |

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.6]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/implementation-artifacts/9-5-perfil-geracao-enxuto-abordagem-d.md]

## Dev Agent Record

### Agent Model Used

Cursor Grok 4.5 (implement) + Claude Sonnet (adversarial code-review)

### Debug Log References

- Review BLOCKED: insurance deferred until `stop` (never resumes); window without `-a` could yield `lastUpdated < birthdate`
- Fixes: defer until `windowStart`; require `-a`; guard `windowStart >= birthdate`

### Completion Notes List

- `SimulationWindowConfig` + hooks `Generator` (validate, lastUpdated, lifecycle bootstrap, insurance)
- Manifest: `simulation_window`, `generation_duration_ms`, `module_profile`
- ADR-008 + GUIA documentam fórmula, fail-fast, insurance e SM-9.4 (n=500 manual)
- Testes: gate, reprodutibilidade, BR demografia, insurance pós-janela, `-a` obrigatório, manifest

### File List

- src/main/java/org/mitre/synthea/br/pathway/generation/SimulationWindowConfig.java
- src/main/java/org/mitre/synthea/engine/Generator.java
- src/main/java/org/mitre/synthea/br/research/ResearchManifestWriter.java
- src/main/java/org/mitre/synthea/world/concepts/healthinsurance/CoverageRecord.java
- src/main/resources/synthea.properties
- docs/research/adr/ADR-008-trajetoria-clinica-focada.md
- docs/GUIA-DE-USO.md
- src/test/java/org/mitre/synthea/br/pathway/generation/SimulationWindowConfigTest.java
- src/test/java/org/mitre/synthea/br/pathway/generation/SimulationWindowIntegrationTest.java
- _bmad-output/implementation-artifacts/9-6-janela-temporal-simulacao-abordagem-d.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-07-09: Story 9.6 closed — docs + BR tests + review Highs (insurance + `-a` required)
- 2026-07-09: Status → done

## Senior Developer Review (AI)

**Outcome:** BLOCKED → Highs addressed in-session → PASS  
**Review date:** 2026-07-09  
**Reviewer model:** Claude Sonnet (adversarial; separate from implementer)

### Action Items

- [x] [High] Insurance deferred until `stop` → defer until window start
- [x] [High] Window without `-a` → require `-a` + runtime guard
- [x] [Med] Document `CoverageRecord.deferEnrollmentUntil` in ADR-008
- [ ] [Low] Stronger NFR1 assertion (deep record compare) — deferred
- [ ] [Low] Accent polish on PT-BR error messages — deferred
- [ ] [Med] Bound N for non-breast_cancer conditions — deferred (piloto mama only)

### Severity breakdown

- High: 2 (resolvidos)
- Med: 2 (1 resolvido via ADR; 1 deferred)
- Low: 4 (débito)

## Tasks / Subtasks → Review Follow-ups (AI)

- [x] [AI-Review][High] Insurance resume at window start
- [x] [AI-Review][High] Require `-a` with simulation_window
- [x] [AI-Review][Med] ADR documents CoverageRecord hook
