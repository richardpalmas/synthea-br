# Story 9.7: Módulo GMF de Trajetória Episódica — Abordagem E

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do Synthea-br,
quero um módulo GMF BR (`modules/br/breast_cancer_trajectory.json`) que modele a jornada oncológica como **trajetória principal**,
para coerência clínica na origem em vez de depender só de filtragem pós-geração.

## Acceptance Criteria

1. **Given** catálogo de fases 9.2 e gate Epic 2 ativos
   **When** `br.generation.trajectory_mode=episodic` e condição `breast_cancer` configurada
   **Then** pacientes passam por fluxo GMF episódico alinhado às fases do catálogo (transições documentadas; compatível GMF 2.0)
   [Source: planning-artifacts/epics.md#Story-9.7; planning-artifacts/epics.md#FR24]

2. **Given** módulo episódico implementado
   **When** inspecionado no repositório
   **Then** reside em `src/main/resources/modules/br/breast_cancer_trajectory.json` (ou path documentado equivalente) e integra-se ao gate existente — SNOMED `254837009` verificável ao final
   [Source: planning-artifacts/epics.md#Story-9.7 AC path e gate]

3. **Given** AD-2 (master module imutável)
   **When** módulo GMF executa para múltiplos pacientes
   **Then** states são clonados por paciente — master module **nunca** mutado entre execuções
   [Source: planning-artifacts/epics.md#Story-9.7 AC AD-2; ARCHITECTURE-SPINE.md#AD-2; _bmad-output/implementation-artifacts/2-2-modulo-gmf-cancer-de-mama-piloto.md]

4. **Given** `br.generation.trajectory_mode=lifespan` ou property ausente (default)
   **When** a geração executa
   **Then** simulação upstream atual (vida inteira + módulo breast_cancer existente) permanece inalterada
   [Source: planning-artifacts/epics.md#Story-9.7 AC modo lifespan default]

5. **Given** transições entre fases do catálogo 9.2
   **When** módulo episódico é desenhado
   **Then** states/transitions mapeiam explicitamente para `screening`, `diagnosis`, `staging`, `treatment`, `follow_up` — documentado em comentários JSON ou doc adjacente
   [Source: Story 9.2 fases; Story 9.7 alinhamento catálogo]

6. **Given** testes com seed fixo
   **When** cohort episódica é gerada
   **Then** provam sequência clínica piloto: diagnóstico antes de procedimentos de tratamento principais (alinhado PLAUS-001/002 quando Epic 4 existir)
   [Source: planning-artifacts/epics.md#Story-9.7 AC testes sequência]

7. **Given** AD-4 (apenas engine/módulos escrevem no HealthRecord)
   **When** modo episódico está ativo
   **Then** eventos clínicos são produzidos exclusivamente via execução GMF — exportadores permanecem read-only
   [Source: ARCHITECTURE-SPINE.md#AD-2, #AD-4]

8. **Given** trade-off episódico vs vida inteira
   **When** documentação é atualizada
   **Then** guia de uso (`docs/GUIA-DE-USO.md` ou equivalente) descreve quando usar `trajectory_mode=episodic` vs `lifespan`, interação com abordagens C e D
   [Source: planning-artifacts/epics.md#Story-9.7 AC documentação]

9. **Given** Story 9.8 depende deste módulo
   **When** módulo episódico define transições entre fases
   **Then** transições usam placeholders ou distribuições default configuráveis — preparados para substituição por priors JSON (9.8) sem reestruturar o GMF
   [Source: planning-artifacts/epics.md#Story-9.8 dependência 9.7]

## Tasks / Subtasks

- [ ] Task 1: Desenhar GMF `breast_cancer_trajectory.json` (AC: #1, #2, #5, #9)
  - [ ] Subtask 1.1: Mapear fases do `PathwayCatalog` para states GMF
  - [ ] Subtask 1.2: Definir transições screening→diagnosis→staging→treatment→follow_up
  - [ ] Subtask 1.3: Integrar condição `254837009` e compatibilidade gate Epic 2
  - [ ] Subtask 1.4: Usar distribuições temporais placeholder (substituíveis por 9.8)

- [ ] Task 2: Config `br.generation.trajectory_mode` (AC: #1, #4)
  - [ ] Subtask 2.1: Adicionar property — valores `lifespan` (default), `episodic`
  - [ ] Subtask 2.2: Roteamento em `TargetConditionConfig` ou `ModuleProfileConfig` — carregar módulo episódico vs upstream
  - [ ] Subtask 2.3: Validar combinação com `module_profile=pathway_minimal` (recomendado)

- [ ] Task 3: Integração com Generator e gate (AC: #2, #3, #7)
  - [ ] Subtask 3.1: Registrar módulo episódico no pipeline de geração quando mode=episodic
  - [ ] Subtask 3.2: Confirmar gate `keep_modules/br/breast_cancer.json` ou equivalente ainda satisfaz SM-1
  - [ ] Subtask 3.3: Teste master module imutável — múltiplos pacientes, mesmo JSON source

- [ ] Task 4: Testes de sequência clínica (AC: #6)
  - [ ] Subtask 4.1: Integração seed fixo — assert ordem diagnóstico < tratamento
  - [ ] Subtask 4.2: (Quando Epic 4 disponível) correlacionar com PLAUS-001/002

- [ ] Task 5: Documentação (AC: #8)
  - [ ] Subtask 5.1: Seção no guia de uso — trade-offs C/D/E
  - [ ] Subtask 5.2: Rodar `./gradlew check`

## Dev Notes

### Abordagem E — trajetória na origem (GMF episódico)

Abordagem E **muta** `HealthRecord` via GMF (AD-2/AD-4) — diferente de C (read-only export). Objetivo: coerência clínica **antes** do export, reduzindo dependência de filtro pós-geração.

### Relação C + D + E

| Abordagem | Papel com E ativo |
|-----------|-------------------|
| **E** | Simula trajetória oncológica estruturada |
| **D** | Suprime módulos ruído + encurta janela |
| **C** | Ainda útil para export/HTML enxuto se lifespan residual existir |

Combinação recomendada para cohort piloto: `episodic` + `pathway_minimal` + `br.pathway.focus=true`.

### Dependências

- **Depende de:** Story 9.2, Story 9.1 (ADR-008), Epic 2 (gate)
- **Recomendado:** Story 9.5 (perfil minimal operacional)
- **Alimenta:** Story 9.8 (calibração timings)

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.generation.trajectory_mode` | `lifespan` | `episodic` ativa GMF trajetória |
| `br.target_condition` | — | `breast_cancer` piloto |
| `br.generation.module_profile` | `full` | `pathway_minimal` recomendado com episodic |

### Project Structure Notes

```
src/main/resources/modules/br/
  breast_cancer_trajectory.json             <- módulo GMF episódico (E)
src/main/resources/keep_modules/br/
  breast_cancer.json                        <- gate Epic 2 (existente)
src/main/java/org/mitre/synthea/br/pathway/
  TrajectoryModeConfig.java                 <- parsing trajectory_mode
src/test/java/org/mitre/synthea/br/pathway/
  EpisodicTrajectoryIntegrationTest.java
```

### Testing Standards Summary

JUnit 4. Testes de integração com seed fixo e n pequeno. `./gradlew check` obrigatório.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.7]
- [Source: _bmad-output/planning-artifacts/epics.md#FR24]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/9-2-catalogo-fases-trajetoria-clinica.md, 2-2-modulo-gmf-cancer-de-mama-piloto.md, 9-5-perfil-geracao-enxuto-abordagem-d.md]

## Dev Agent Record

### Agent Model Used

Amelia (CR adversarial 2026-07-10)

### Completion Notes List

- MVP explícito: módulo E = **marcador de fase** (`pathway_phase`) em paralelo ao `breast_cancer` upstream (não substitui eventos clínicos).
- CR: `lifespan` exclui `breast_cancer_trajectory_br`; `episodic` força include + fail-fast de modo inválido.

### Senior Developer Review (AI)

**Date:** 2026-07-10 · **Outcome:** approve → done (MVP marcador)

| Finding | Action |
|---------|--------|
| vazamento em lifespan+minimal | patch ✓ |
| escopo marcador vs trajetória clínica | decision A — MVP documentado |
| fail-fast modo inválido | patch ✓ |

### File List

- `TrajectoryModeConfig.java`, `Generator.java`, `pathway_minimal.json`, testes
