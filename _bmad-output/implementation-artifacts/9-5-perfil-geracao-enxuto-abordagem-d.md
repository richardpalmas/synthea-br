# Story 9.5: Perfil de Geração Enxuto — Abordagem D

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do Synthea-br,
quero `br.generation.module_profile=pathway_minimal` quando uso condição-alvo,
para reduzir comorbidades e módulos paralelos **durante** a simulação, não só no export.

## Acceptance Criteria

1. **Given** `br.target_condition=breast_cancer` e `br.generation.module_profile=pathway_minimal`
   **When** a geração executa
   **Then** apenas módulos do perfil curado são carregados — mínimo documentado: lifecycle, insurance, encounters, wellness reduzido, `breast_cancer` + submódulos
   [Source: planning-artifacts/epics.md#Story-9.5; planning-artifacts/epics.md#FR22]

2. **Given** perfil `pathway_minimal` ativo
   **When** lista de módulos é resolvida
   **Then** módulos de **baixa relevância oncológica piloto** estão excluídos (ex.: dental, veteran, etc.) conforme `pathway_minimal.json`
   [Source: planning-artifacts/epics.md#Story-9.5 AC excluídos]

3. **Given** `br.generation.module_profile` ausente ou `full`
   **When** a geração executa
   **Then** comportamento upstream permanece **inalterado** — todos os módulos padrão carregados
   [Source: planning-artifacts/epics.md#Story-9.5 AC perfil full default]

4. **Given** AD-3 (data packs em resources)
   **When** perfil minimal é definido
   **Then** lista allow/deny versionada reside em `src/main/resources/br/generation/module_profiles/pathway_minimal.json` — não hardcoded no Java
   [Source: planning-artifacts/epics.md#Story-9.5 AC JSON; ARCHITECTURE-SPINE.md#AD-3]

5. **Given** Epic 2 gate ativo (SM-1)
   **When** cohort minimal é gerada com seed fixo
   **Then** 100% dos pacientes exportados ainda apresentam condição-alvo verificável (`254837009`) — perfil minimal **não** quebra o gate
   [Source: planning-artifacts/epics.md#Story-9.5 AC gate; Epic 2 SM-1]

6. **Given** NFR1 (reprodutibilidade)
   **When** mesma seed + config + perfil são usados
   **Then** cohort gerada é idêntica entre execuções (mesmas condições, mesmos eventos relevantes)
   [Source: planning-artifacts/epics.md#Story-9.5 AC NFR1]

7. **Given** flag upstream `-m` (módulos isolados) existe
   **When** pesquisador configura geração enxuta
   **Then** implementação **não** usa `-m` isolado sem perfil — perfil é conjunto **testado e documentado** que substitui uso ad hoc de `-m`
   [Source: planning-artifacts/epics.md#Story-9.5 AC "não usa flag -m isolada"]

8. **Given** ADR-008 documenta abordagem D
   **When** perfil minimal é implementado
   **Then** decisões de inclusão/exclusão de módulos são rastreáveis ao ADR-008 (comentário no JSON ou referência cruzada)
   [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]

9. **Given** a suíte de testes
   **When** `./gradlew check` é executado
   **Then** inclui teste de integração: cohort `pathway_minimal` vs `full` com contagem de condições distintas **inferior** no minimal (mesma seed)
   [Source: planning-artifacts/epics.md#Story-9.5 AC teste integração]

## Tasks / Subtasks

- [ ] Task 1: Data pack `pathway_minimal.json` (AC: #1, #2, #4, #8)
  - [ ] Subtask 1.1: Criar `src/main/resources/br/generation/module_profiles/pathway_minimal.json`
  - [ ] Subtask 1.2: Documentar allowlist (lifecycle, insurance, encounters, wellness reduzido, breast_cancer+subs)
  - [ ] Subtask 1.3: Documentar denylist (dental, veteran, etc.) com justificativa oncológica
  - [ ] Subtask 1.4: Adicionar `profile_version` para manifest/rastreabilidade

- [ ] Task 2: Loader e resolução de perfil (AC: #3, #4, #7)
  - [ ] Subtask 2.1: Criar `org.mitre.synthea.br.pathway.generation.ModuleProfileConfig` (ou `org.mitre.synthea.br.generation.*`)
  - [ ] Subtask 2.2: Parsing de `br.generation.module_profile` — valores `full`, `pathway_minimal`; erro para inválidos
  - [ ] Subtask 2.3: Hook no carregamento de módulos do `Generator` — filtrar lista antes de simulação

- [ ] Task 3: Integração com condição-alvo (AC: #1, #5)
  - [ ] Subtask 3.1: Garantir módulo `breast_cancer` e gate (Epic 2) sempre incluídos no minimal
  - [ ] Subtask 3.2: Validar interação com `TargetConditionConfig` — perfil sem target_condition: comportamento documentado

- [ ] Task 4: Config e documentação (AC: #3, #7)
  - [ ] Subtask 4.1: Adicionar `br.generation.module_profile = full` (default) em `synthea.properties` com comentário
  - [ ] Subtask 4.2: Documentar diferença perfil vs `-m` no guia de uso (quando 9.7 concluir trade-offs)

- [ ] Task 5: Testes (AC: #5, #6, #9)
  - [ ] Subtask 5.1: Integração seed fixo — gate 100% no minimal
  - [ ] Subtask 5.2: Comparar contagem condições distintas minimal vs full
  - [ ] Subtask 5.3: Reprodutibilidade — duas execuções, assert equivalência
  - [ ] Subtask 5.4: Rodar `./gradlew check`

## Dev Notes

### Abordagem D — geração enxuta na origem

Diferente da abordagem C (filtro pós-geração), D **reduz ruído durante a simulação** suprimindo módulos irrelevantes. O `HealthRecord` resultante já contém menos comorbidades — export full (sem focus) também fica mais limpo.

### ADR-008 — janela temporal (Story 9.6) documentada separadamente

Perfil minimal trata **quais módulos** rodam; Story 9.6 trata **quando** a simulação começa. Combináveis: `module_profile=pathway_minimal` + `simulation_window=pre_onset_years:N`.

### Dependências

- **Depende de:** Story 9.1 (ADR-008), Epic 2 (gate)
- **Paralelizável com:** Story 9.3 após 9.2
- **Bloqueia:** Story 9.6 (janela temporal)
- **Recomendado operacional para:** Story 9.7 (GMF episódico)

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.generation.module_profile` | `full` | Perfil de módulos (`pathway_minimal`) |
| `br.target_condition` | — | Condiciona uso típico do perfil minimal |

### Project Structure Notes

```
src/main/resources/br/generation/module_profiles/
  pathway_minimal.json
src/main/java/org/mitre/synthea/br/pathway/generation/
  ModuleProfileConfig.java
  ModuleProfileLoader.java
src/main/java/org/mitre/synthea/engine/
  Generator.java                              <- hook mínimo de filtro de módulos (AD-7)
src/test/java/org/mitre/synthea/br/pathway/generation/
  ModuleProfileIntegrationTest.java
```

Preferir pacote `org.mitre.synthea.br.pathway.generation` ou `org.mitre.synthea.br.generation` — manter consistência com AD-7; documentar escolha no Dev Agent Record.

### Testing Standards Summary

JUnit 4. Teste de integração pode ser mais lento — n pequeno (10-20). `./gradlew check` obrigatório.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.5]
- [Source: _bmad-output/planning-artifacts/epics.md#FR22]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/9-1-spike-referencias-trajetoria-longitudinal-adr-008.md, 2-3-gate-de-cohort-com-condicao-garantida.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
