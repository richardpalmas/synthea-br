# Story 9.6: Janela Temporal de Simulação — Abordagem D

Status: backlog

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

- [ ] Task 1: Especificar semântica no ADR-008 ou doc complementar (AC: #1, #7)
  - [ ] Subtask 1.1: Confirmar fórmula `target_age - N` vs alternativas (onset module-specific)
  - [ ] Subtask 1.2: Documentar impacto em seed/demografia/comorbidades pré-janela
  - [ ] Subtask 1.3: Definir limites válidos de N para câncer de mama piloto (5–15)

- [ ] Task 2: Config `br.generation.simulation_window` (AC: #1, #2, #6)
  - [ ] Subtask 2.1: Adicionar property em `synthea.properties` — formato `pre_onset_years:N` ou `full_lifespan`
  - [ ] Subtask 2.2: Parser com validação de N e compatibilidade com `-a`/idade alvo
  - [ ] Subtask 2.3: Mensagens de erro claras para combinações inválidas

- [ ] Task 3: Implementar `SimulationWindowConfig` (AC: #1, #5, #7)
  - [ ] Subtask 3.1: Criar `org.mitre.synthea.br.pathway.generation.SimulationWindowConfig`
  - [ ] Subtask 3.2: Calcular `birthTime`/`startTime` efetivo para `Person`
  - [ ] Subtask 3.3: Hook em `Generator` — aplicar start time antes do loop de módulos
  - [ ] Subtask 3.4: Preservar demografia IBGE/Epic 3 coerente com idade efetiva

- [ ] Task 4: Integração com perfil minimal (AC: #3, #8)
  - [ ] Subtask 4.1: Medir duração geração baseline vs minimal+window (n pequeno CI, n=500 manual)
  - [ ] Subtask 4.2: Logar duração no manifest

- [ ] Task 5: Testes (AC: #4, #5, #6)
  - [ ] Subtask 5.1: Integração gate SM-1 com janela ativa
  - [ ] Subtask 5.2: Reprodutibilidade seed fixa
  - [ ] Subtask 5.3: Testes de config inválida — assert fail-fast
  - [ ] Subtask 5.4: Rodar `./gradlew check`

## Dev Notes

### Abordagem D — complemento ao perfil minimal (9.5)

| Mecanismo | O que reduz |
|-----------|-------------|
| `module_profile=pathway_minimal` | Módulos paralelos (comorbidades) |
| `simulation_window=pre_onset_years:N` | Anos simulados antes do onset |

Usar ambos para máxima redução de ruído e tempo (SM-9.4).

### Risco arquitetural — alteração no `Generator`

Janela temporal pode exigir ajuste de `birthTime` ou ponto de entrada dos módulos. **Obrigatório:** ADR-008 deve ter documentado impacto; qualquer patch em `Generator.java` além de hook delegado exige justificativa explícita (AD-7, upstream-first).

### Dependências

- **Depende de:** Story 9.5, Story 9.1 (ADR-008)
- **Recomendado antes de produção:** Epic 4 (plausibilidade no recorte temporal)

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.generation.simulation_window` | `full_lifespan` | Janela temporal (`pre_onset_years:N`) |
| `br.generation.module_profile` | `full` | Combinável com window |
| `br.target_condition` | — | Contexto oncológico piloto |

### Project Structure Notes

```
src/main/java/org/mitre/synthea/br/pathway/generation/
  SimulationWindowConfig.java
  SimulationWindowParser.java
src/main/java/org/mitre/synthea/engine/
  Generator.java                              <- hook start time (mínimo)
src/main/java/org/mitre/synthea/br/research/
  ResearchManifestWriter.java                 <- simulation_window, duration_ms
src/test/java/org/mitre/synthea/br/pathway/generation/
  SimulationWindowIntegrationTest.java
```

### Testing Standards Summary

JUnit 4. Testes de integração com n pequeno. Benchmark n=500 opcional manual. `./gradlew check` obrigatório.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.6]
- [Source: _bmad-output/planning-artifacts/epics.md#FR23]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/9-5-perfil-geracao-enxuto-abordagem-d.md, 2-3-gate-de-cohort-com-condicao-garantida.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
