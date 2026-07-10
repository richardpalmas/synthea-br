# Story 9.3: Export Focado em Trajetória — Abordagem C

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do Synthea-br,
quero ativar `br.pathway.focus` para exportar apenas eventos da trajetória clínica alvo (+ demografia),
para eliminar poluição visual em CSV/FHIR sem perder o prontuário completo na simulação.

## Acceptance Criteria

1. **Given** `br.target_condition=breast_cancer` e `br.pathway.focus=true` (CLI `-Dbr.pathway.focus=true` ou property)
   **When** a cohort é gerada e exportada (CSV e FHIR R4)
   **Then** recursos exportados restringem-se a entradas cujo código/tipo está no catálogo 9.2 ou em `always_include`
   [Source: planning-artifacts/epics.md#Story-9.3; planning-artifacts/epics.md#FR20]

2. **Given** `br.pathway.focus=false` ou property ausente (default)
   **When** a cohort é exportada
   **Then** comportamento permanece idêntico ao export integral upstream — nenhuma regressão para usuários sem trajetória focada
   [Source: planning-artifacts/epics.md#Story-9.3 AC default false]

3. **Given** AD-2 exige export read-only sobre `HealthRecord`
   **When** `PathwayExportFilter` é aplicado
   **Then** implementação estende o padrão `Exporter.filterForExport` em `org.mitre.synthea.br.pathway.PathwayExportFilter` — filtra **cópia/views para export**, nunca muta o `HealthRecord` original do paciente
   [Source: planning-artifacts/epics.md#Story-9.3; ARCHITECTURE-SPINE.md#AD-2; src/main/java/org/mitre/synthea/export/Exporter.java#filterForExport]

4. **Given** rastreabilidade acadêmica (manifest Story 1.4)
   **When** export com focus ativo completa
   **Then** `manifest.json` inclui campos: `pathway_focus` (boolean), `pathway_catalog_version`, `pathway_condition`
   [Source: planning-artifacts/epics.md#Story-9.3 AC manifest; ARCHITECTURE-SPINE.md#AD-6]

5. **Given** NFR1 (reprodutibilidade)
   **When** mesma seed + config + versão do catálogo são usadas em duas execuções
   **Then** output filtrado (CSV/FHIR) é equivalente entre execuções
   [Source: planning-artifacts/epics.md#Story-9.3 AC NFR1; planning-artifacts/epics.md#NFR1]

6. **Given** Epic 2 garante condição-alvo (SM-1)
   **When** focus exporta cohort de câncer de mama
   **Then** eventos da condição-alvo (`254837009` e correlatos no catálogo) permanecem presentes no export filtrado — focus não remove a trajetória oncológica central
   [Source: Epic 2 SM-1; Story 9.2 allowlist]

7. **Given** a suíte de testes
   **When** `./gradlew check` é executado
   **Then** inclui testes com fixture `Person` manual contendo eventos **in** e **out** da allowlist — assertando inclusão/exclusão correta sem gerar cohort completa
   [Source: planning-artifacts/epics.md#Story-9.3 AC testes fixture]

8. **Given** métrica SM-9.2 do Epic 9
   **When** comparado export full vs focus na mesma cohort piloto
   **Then** redução de linhas CSV ≥ 50% é documentada em nota de experimento ou Dev Agent Record (validação manual ou teste de integração opcional)
   [Source: planning-artifacts/epics.md#Epic-9 métricas SM-9.2]

## Tasks / Subtasks

- [ ] Task 1: Config e flag `br.pathway.focus` (AC: #1, #2)
  - [ ] Subtask 1.1: Adicionar `br.pathway.focus = false` com comentário em `synthea.properties`
  - [ ] Subtask 1.2: Suportar override CLI `-Dbr.pathway.focus=true`
  - [ ] Subtask 1.3: Validar interação com `br.target_condition` — focus sem condição-alvo: erro claro ou no-op documentado

- [ ] Task 2: Implementar `PathwayExportFilter` (AC: #1, #3, #6)
  - [ ] Subtask 2.1: Criar `org.mitre.synthea.br.pathway.PathwayExportFilter` espelhando contrato de `Exporter.filterForExport`
  - [ ] Subtask 2.2: Carregar allowlist unificada via `PathwayCatalog` (Story 9.2)
  - [ ] Subtask 2.3: Filtrar conditions, procedures, observations, medications, encounters conforme mapeamento código/tipo
  - [ ] Subtask 2.4: Preservar entradas `always_include` (demografia, metadados)

- [ ] Task 3: Integração em `Exporter.java` (AC: #1, #2, #3)
  - [ ] Subtask 3.1: Hook após `filterForExport` existente (years of history) quando `br.pathway.focus=true`
  - [ ] Subtask 3.2: Garantir CSV e FHIR R4 passam pelo filtro; CCDA/HTML fora de escopo desta story (9.4)
  - [ ] Subtask 3.3: Confirmar `HealthRecord` original intacto pós-export

- [ ] Task 4: Manifest e metadados (AC: #4)
  - [ ] Subtask 4.1: Estender `ResearchManifestWriter` com `pathway_focus`, `pathway_catalog_version`, `pathway_condition`
  - [ ] Subtask 4.2: Testes de serialização manifest com focus on/off

- [ ] Task 5: Testes (AC: #5, #7, #8)
  - [ ] Subtask 5.1: `PathwayExportFilterTest` — fixture Person manual in/out allowlist
  - [ ] Subtask 5.2: Teste reprodutibilidade — duas execuções, mesmo hash de output filtrado (ou assert estrutural)
  - [ ] Subtask 5.3: (Opcional) integração n pequeno — documentar redução CSV vs full
  - [ ] Subtask 5.4: Rodar `./gradlew check`

## Dev Notes

### Abordagem C — filtro de export (read-only)

A simulação continua gerando prontuário de **vida inteira** completo no `HealthRecord`. Apenas a **camada de export** aplica filtro baseado no catálogo 9.2. Isso preserva compatibilidade upstream e permite comparar full vs focus na mesma execução.

### Padrão `filterForExport` — reutilizar, não reinventar

```698:727:src/main/java/org/mitre/synthea/export/Exporter.java
  public static Person filterForExport(Person original, int yearsToKeep, long endTime) {
    // ... filtro temporal existente ...
  }
```

`PathwayExportFilter` deve compor com (não substituir) filtro temporal quando ambos ativos — ordem documentada: temporal → pathway focus.

### Dependências

- **Depende de:** Story 9.2 (`PathwayCatalog`), Epic 2 (condição garantida), Epic 5 Story 5.1 recomendado (FHIR estável)
- **Bloqueia:** Story 9.4 (HTML por fase)
- **Paralelizável com:** Story 9.5 após 9.2

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.pathway.focus` | `false` | Ativa export focado na trajetória |
| `br.target_condition` | — | Deve estar ativo (ex.: `breast_cancer`) |

### Project Structure Notes

```
src/main/java/org/mitre/synthea/br/pathway/
  PathwayExportFilter.java
src/main/java/org/mitre/synthea/export/
  Exporter.java                             <- hook de integração
src/main/java/org/mitre/synthea/br/research/
  ResearchManifestWriter.java               <- campos pathway_*
src/test/java/org/mitre/synthea/br/pathway/
  PathwayExportFilterTest.java
```

### Testing Standards Summary

JUnit 4. Preferir fixtures `Person` manuais para velocidade. Integração lenta opcional com tag ou n pequeno. `./gradlew check` obrigatório.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.3]
- [Source: _bmad-output/planning-artifacts/epics.md#FR20]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/9-2-catalogo-fases-trajetoria-clinica.md]
- [Source: src/main/java/org/mitre/synthea/export/Exporter.java#filterForExport]

## Dev Agent Record

### Agent Model Used

Amelia (CR adversarial 2026-07-10)

### Completion Notes List

- CR: filtro passa a remover `imagingStudies`/`devices`/`supplies`; SM-9.2 documentado em `docs/research/experiments/2026-07-10-pathway-focus-sm92/`.

### Senior Developer Review (AI)

**Date:** 2026-07-10 · **Outcome:** approve → done

| Finding | Action |
|---------|--------|
| imaging/devices/supplies não filtrados | patch ✓ |
| SM-9.2 sem evidência | patch ✓ (experimento) |
| Claims órfãos pós-filtro | defer |

### File List

- `PathwayExportFilter.java`, `PathwayExportFilterTest.java`
- `docs/research/experiments/2026-07-10-pathway-focus-sm92/experiment.md`
