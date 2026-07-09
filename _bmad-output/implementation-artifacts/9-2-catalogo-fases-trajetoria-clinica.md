# Story 9.2: Catálogo de Fases de Trajetória Clínica (Data Pack)

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do Synthea-br,
quero um catálogo versionado de fases da trajetória de câncer de mama com allowlists de códigos clínicos,
para que filtros de export, narrativa HTML e regras de plausibilidade compartilhem a mesma ontologia de "evento relevante".

## Acceptance Criteria

1. **Given** ADR-008 aceito (Story 9.1) e módulo upstream `breast_cancer.json` disponível
   **When** o data pack é carregado de `src/main/resources/br/pathways/breast_cancer_phases.json`
   **Then** o parsing completa sem erro e expõe todas as fases definidas no JSON
   [Source: planning-artifacts/epics.md#Story-9.2; docs/research/adr/ADR-008-trajetoria-clinica-focada.md]

2. **Given** o catálogo está carregado
   **When** cada fase é inspecionada
   **Then** possui: `phase_id` estável, título PT-BR, ordem canônica, descrição, `code_allowlist` (subset SNOMED/LOINC/RxNorm/CPT extraído do módulo piloto) e `encounter_types` opcionais
   [Source: planning-artifacts/epics.md#Story-9.2 AC estrutura de fase]

3. **Given** câncer de mama piloto
   **When** o catálogo é validado
   **Then** fases mínimas cobrem: `screening`, `diagnosis`, `staging`, `treatment`, `follow_up` (nomes finais documentados no JSON)
   [Source: planning-artifacts/epics.md#Story-9.2 AC fases mínimas]

4. **Given** demografia e metadados de cohort são sempre relevantes independente da fase
   **When** o catálogo define entradas `always_include`
   **Then** idade, sexo, município BR e metadados de cohort equivalentes estão marcados como `always_include` e documentados no JSON
   [Source: planning-artifacts/epics.md#Story-9.2 AC always_include]

5. **Given** AD-3 exige dados em resources, não hardcoded no Java
   **When** consumidores precisam resolver fases por condição-alvo
   **Then** API Java `org.mitre.synthea.br.pathway.PathwayCatalog` expõe resolução por `br.target_condition` (ex.: `breast_cancer`) **sem** hardcode de códigos clínicos no Java
   [Source: planning-artifacts/epics.md#Story-9.2 AC PathwayCatalog; ARCHITECTURE-SPINE.md#AD-3]

6. **Given** o catálogo é versionado para rastreabilidade (manifest, export focus)
   **When** `PathwayCatalog` é instanciado
   **Then** expõe `catalog_version` (campo no JSON ou derivado de hash) consumível por Stories 9.3+ para `manifest.json`
   [Source: planning-artifacts/epics.md#Story-9.3 AC manifest pathway_catalog_version]

7. **Given** SNOMED `254837009` é código crítico da condição-alvo (Epic 2)
   **When** testes unitários validam o catálogo
   **Then** assertam parsing correto, ordem canônica das fases e presença de códigos críticos incluindo `254837009` na allowlist apropriada
   [Source: planning-artifacts/epics.md#Story-9.2 AC testes; Epic 2 SM-1]

8. **Given** AD-7 orienta extensões em `org.mitre.synthea.br.*`
   **When** a implementação é concluída
   **Then** loaders e modelos residem em `org.mitre.synthea.br.pathway.*` sem alterar classes core upstream além de hooks mínimos documentados
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-7]

## Tasks / Subtasks

- [ ] Task 1: Criar data pack `breast_cancer_phases.json` (AC: #1, #2, #3, #4)
  - [ ] Subtask 1.1: Extrair códigos SNOMED/LOINC/RxNorm/CPT do módulo `breast_cancer.json` e submódulos piloto
  - [ ] Subtask 1.2: Definir fases `screening`, `diagnosis`, `staging`, `treatment`, `follow_up` com títulos/descrições PT-BR
  - [ ] Subtask 1.3: Configurar seção `always_include` (demografia, metadados cohort BR)
  - [ ] Subtask 1.4: Adicionar `catalog_version` e metadados de condição (`breast_cancer`)

- [ ] Task 2: Implementar `PathwayCatalog` e modelos (AC: #5, #6, #8)
  - [ ] Subtask 2.1: Criar `PathwayPhase`, `CodeAllowlistEntry` (ou equivalente) em `org.mitre.synthea.br.pathway`
  - [ ] Subtask 2.2: Implementar `PathwayCatalog.loadForCondition(String targetCondition)` lendo de `resources/br/pathways/`
  - [ ] Subtask 2.3: Expor ordem canônica, lookup por `phase_id`, resolução de allowlist unificada
  - [ ] Subtask 2.4: Integrar leitura de `br.target_condition` via `TargetConditionConfig` (Story 2.1) quando aplicável

- [ ] Task 3: Testes unitários (AC: #7)
  - [ ] Subtask 3.1: Criar `PathwayCatalogTest.java` — parsing, ordem de fases, versão
  - [ ] Subtask 3.2: Assertar presença de `254837009` e outros códigos críticos piloto
  - [ ] Subtask 3.3: Teste de condição desconhecida — erro claro ou catálogo vazio documentado

- [ ] Task 4: Documentação e validação (AC: #1, #8)
  - [ ] Subtask 4.1: Comentar schema JSON inline ou em README curto em `resources/br/pathways/`
  - [ ] Subtask 4.2: Rodar `./gradlew check`

## Dev Notes

### Ontologia compartilhada — hub do Epic 9

`PathwayCatalog` é consumido por:
- **9.3** — `PathwayExportFilter` (allowlist de export)
- **9.4** — agrupamento HTML por fase
- **9.7** — alinhamento de transições GMF episódico
- **9.8** — mapeamento fase→timing priors

Evitar duplicar allowlists em Java ou templates — **single source of truth** no JSON (AD-3).

### Abordagem — pré-requisito transversal (base para C, D, E)

Esta story não implementa filtro (C), geração enxuta (D) nem GMF episódico (E). Fornece o **data pack e API** usados por todas as abordagens.

### Dependências

- **Depende de:** Story 9.1 (ADR-008 aceito)
- **Bloqueia:** Stories 9.3, 9.4, 9.7, 9.8
- **Recomendado:** módulo `breast_cancer.json` (Epic 2 / Story 2.2) disponível para extração de códigos

### Properties / flags — leitura apenas

| Property | Uso nesta story |
|----------|-----------------|
| `br.target_condition` | Chave de resolução do catálogo (`breast_cancer`) |

Flags `br.pathway.focus`, `br.generation.*` são **consumidas** em stories posteriores, não introduzidas aqui.

### Project Structure Notes

```
src/main/resources/br/pathways/
  breast_cancer_phases.json              <- data pack principal
src/main/java/org/mitre/synthea/br/pathway/
  PathwayCatalog.java                    <- loader + API pública
  PathwayPhase.java                      <- modelo de fase
  PathwayCatalogLoader.java              <- parsing JSON (opcional, se separar)
src/test/java/org/mitre/synthea/br/pathway/
  PathwayCatalogTest.java
```

### Testing Standards Summary

JUnit 4. Testes rápidos, sem geração de cohort. `./gradlew check` deve passar.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.2]
- [Source: _bmad-output/planning-artifacts/epics.md#FR19]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/2-2-modulo-gmf-cancer-de-mama-piloto.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
