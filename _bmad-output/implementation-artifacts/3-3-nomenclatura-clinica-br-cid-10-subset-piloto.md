---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 3.3: Nomenclatura Clínica BR (CID-10 Subset Piloto)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero que a condição piloto (câncer de mama) seja exportada também com código CID-10 brasileiro no FHIR,
para interoperabilidade com terminologia clínica nacional, sem perder a codificação SNOMED original já usada internamente.

## Acceptance Criteria

1. **Given** o módulo de doença upstream (`breast_cancer.json`, ver Story 2.2) usa o código SNOMED `254837009` ("Malignant neoplasm of breast (disorder)") para a condição de câncer de mama
   **When** o mapeamento EU→BR é criado
   **Then** existe um arquivo documentado em `src/main/resources/br/coding/` mapeando `254837009` (SNOMED-CT) para um código CID-10 piloto (proposta: `C50.9` — "Neoplasia maligna da mama, não especificada", a ser confirmada/ajustada pelo grupo), incluindo a fonte do subset CID-10 usado
   [Source: planning-artifacts/epics.md#Story-3.3; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-6; src/main/resources/modules/breast_cancer.json — código SNOMED 254837009]

2. **Given** a fonte oficial de CID-10 BR (DATASUS/TabCID, WHO ICD-10 release, ou subset curado manualmente) **não foi decidida** no PRD/addendum (Open Question #1)
   **When** esta story é implementada
   **Then** o subset piloto usa uma curadoria manual documentada (aceitável para MVP conforme o PRD), e um ADR explícito é redigido (seguindo a convenção da Story 1.3) registrando essa decisão como **provisória**, com nota clara de que qualquer expansão de FR-6 além do piloto de câncer de mama **requer** revisão/confirmação da fonte oficial antes de prosseguir
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§10 Open-Questions item-1; planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#Deferred]

3. **Given** o export FHIR R4 hoje serializa a condição com `Condition.code` contendo apenas a `Coding` SNOMED original (ver `FhirR4.java`, método `condition(...)`, linha ~1647)
   **When** o perfil `br` está ativo (Story 3.1) e a condição piloto está presente
   **Then** o `Condition.code` (`CodeableConcept`) recebe uma **segunda `Coding` adicional** com o sistema/código CID-10 documentado, **sem remover ou substituir** a `Coding` SNOMED original — abordagem aditiva, não destrutiva
   [Source: src/main/java/org/mitre/synthea/export/FhirR4.java#condition — análise desta tarefa; planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2 "validação/export read-only sobre HealthRecord"]

4. **Given** a abordagem aditiva (AC #3) precisa permanecer compatível com a suíte de validação HAPI existente (AD-8)
   **When** `./gradlew check` (incluindo testes de exportação FHIR) é executado
   **Then** todos os testes existentes de exportação R4 continuam passando — um `CodeableConcept` com múltiplas `Coding`s é válido em FHIR R4 e não quebra a validação HAPI
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-8; _bmad-output/project-context.md#Testing-Rules "Exportadores FHIR: validar output com HAPI ValidationResult"]

5. **Given** o projeto já possui um padrão de mapeamento de códigos ponderado em JSON (`org.mitre.synthea.export.rif.CodeMapper`, usado pelo exportador BFD/RIF — schema `{ "codigo_origem": [{ "code": "...", "weight": ... }] }`)
   **When** o mapeamento EU→BR é implementado
   **Then** reaproveita o **mesmo formato JSON simples** (sem necessariamente reaproveitar a classe `CodeMapper` em si, que está acoplada a configs `exporter.bfd.*`) — criar um carregador próprio e mais simples em `org.mitre.synthea.br.coding.*`, evitando inventar um terceiro formato de mapeamento de código no projeto
   [Source: src/main/java/org/mitre/synthea/export/rif/CodeMapper.java — padrão de schema já existente no projeto]

6. **Given** TUSS/SUS billing está explicitamente fora do escopo do MVP
   **When** o mapeamento é documentado
   **Then** nenhuma referência a códigos TUSS ou tabelas de faturamento SUS é incluída — apenas nomenclatura clínica (CID-10) para fins de interoperabilidade de pesquisa
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§8.2 "TUSS completo — v2"]

7. **Given** esta story depende do código SNOMED da condição piloto (Story 2.2) e do flag de perfil (Story 3.1)
   **When** a implementação é planejada
   **Then** o mapeamento de código em si (Tasks 1-2) pode ser desenvolvido de forma independente (o código SNOMED `254837009` já existe upstream hoje, sem depender de nenhuma story ainda não implementada); apenas a **ativação condicional no export** (Task 3, AC #3) depende de `BrProfile.isActive()` (Story 3.1) estar disponível
   [Source: derivado da análise de dependências desta tarefa]

## Tasks / Subtasks

- [x] Task 1: Pesquisar e documentar o subset CID-10 piloto (AC: #1, #2)
  - [x] Subtask 1.1: Levantar candidatos a código CID-10 para câncer de mama (família `C50`, subcategorias `C50.0`-`C50.9`) e propor `C50.9` como código piloto padrão (não especificado), documentando a justificativa
  - [x] Subtask 1.2: Documentar explicitamente que a fonte (DATASUS/TabCID vs. WHO ICD-10 vs. curadoria manual) é uma decisão pendente — registrar como ADR (ver Task 4)

- [x] Task 2: Criar o mapeamento EU→BR (AC: #1, #5, #6)
  - [x] Subtask 2.1: Criar `src/main/resources/br/coding/snomed_to_cid10_breast_cancer.json` (ou nome equivalente) com schema `{ "254837009": [{ "code": "C50.9", "system": "CID-10", "display": "Neoplasia maligna da mama, não especificada" }] }`
  - [x] Subtask 2.2: Criar `org.mitre.synthea.br.coding.BrCodeMapper` (carregador simples, sem dependência de `exporter.bfd.*`) para ler o JSON
  - [x] Subtask 2.3: Documentar no cabeçalho do JSON a fonte e a natureza piloto/provisória do mapeamento

- [x] Task 3: Integrar ao export FHIR R4 (AC: #3, #4, #7)
  - [x] Subtask 3.1: Ler `FhirR4.java` por completo na vizinhança do método `condition(...)` (linha ~1647) e do método compartilhado de construção de `CodeableConcept` a partir de `HealthRecord.Code`, antes de qualquer alteração
  - [x] Subtask 3.2: Adicionar branch condicional: se `BrProfile.isActive()` e existe mapeamento BR para o código SNOMED da condição, adicionar `Coding` extra ao `CodeableConcept` retornado
  - [x] Subtask 3.3: Garantir que, sem perfil `br` ou sem mapeamento disponível para o código, o comportamento é idêntico ao upstream (apenas SNOMED)

- [x] Task 4: Redigir ADR sobre fonte de CID-10 (AC: #2)
  - [x] Subtask 4.1: Seguindo a convenção da Story 1.3, criar `docs/research/adr/ADR-00N-fonte-cid10-br.md` (número sequencial após os ADRs de Epic 1) documentando a decisão provisória de curadoria manual e a condição de revisão antes de expandir FR-6
  - [x] Subtask 4.2: Registrar no índice de ADRs (`docs/research/adr/README.md`)

- [x] Task 5: Testes (AC: #3, #4)
  - [x] Subtask 5.1: Criar `src/test/java/org/mitre/synthea/br/coding/BrCodeMapperTest.java` — validar carregamento do JSON e resolução do código SNOMED piloto
  - [x] Subtask 5.2: Teste de integração de export — gerar um paciente com a condição piloto, perfil `br` ativo, exportar FHIR R4, e assertar que o `Condition.code` contém **ambas** as `Coding`s (SNOMED e CID-10), validando com HAPI `ValidationResult`
  - [x] Subtask 5.3: Teste de regressão — sem perfil `br`, o `Condition.code` contém apenas a `Coding` SNOMED (comportamento upstream)
  - [x] Subtask 5.4: Rodar `./gradlew check` — aceito pelo Boss; gate local pendente JDK 17 (CI)

### Review Findings

- [x] [Review][Decision] Suprimir `addTranslation("ICD10-CM")` quando `BrProfile.isActive()`? — **Resolvido:** guard em `FhirR4.condition()`; teste asserta ausência de ICD-10-CM com perfil BR.
- [x] [Review][Patch] Teste de integração hardcoded `assertEquals(2, codings)` [`BrCid10ExportIntegrationTest.java:89`] — **Resolvido:** asserts de presença + ausência ICD-10-CM.
- [x] [Review][Patch] Guard null em entry JSON malformado [`BrCodeMapper.java:97`] — **Resolvido.**
- [x] [Review][Patch] Capturar `JsonParseException` além de `IOException` em `readMapping` [`BrCodeMapper.java:152`] — **Resolvido.**
- [x] [Review][Defer] `./gradlew check` não verificado nesta sessão (JVM 8 no ambiente local; requer JDK 17) — deferred, pre-existing env
- [x] [Review][Defer] `condition.codes.get(0)` sem guard `isEmpty()` [`FhirR4.java:1703`] — deferred, pre-existing upstream
- [x] [Review][Defer] Round-trip `getSystemFromURI` perde identidade `CID-10` (retorna `ICD10`) [`ExportHelper.java:274`] — deferred, sem consumer atual
- [x] [Review][Defer] `lookup()` usa sempre `entries.get(0)` e ignora `weight` — deferred, piloto single-entry documentado no ADR-005

## Dev Notes

### Ambiguidade real do PRD — não resolver silenciosamente

O PRD (§10, Open Question #1) e a Architecture Spine (§Deferred) deixam **explicitamente aberta** a fonte oficial de CID-10 BR. Esta story **não deve fingir que a questão está resolvida** — a abordagem correta (AC #2, Task 4) é entregar um subset piloto funcional (suficiente para câncer de mama) **e** formalizar a natureza provisória via ADR, deixando claro para o grupo que uma decisão de fonte oficial é necessária antes de expandir a cobertura de condições/códigos.

### Abordagem aditiva, não substitutiva — preserva AD-2 e AD-8

A decisão mais importante desta story é **não substituir** a codificação SNOMED existente no export, apenas **adicionar** uma `Coding` CID-10. Isso: (a) preserva qualquer integração/teste existente que dependa do código SNOMED no export, (b) evita qualquer necessidade de alterar o `HealthRecord` interno ou os módulos GMF (que continuam usando SNOMED conforme padrão upstream), e (c) mantém conformidade com AD-2 (mutação clínica apenas no engine/módulos — esta story não toca neles) e AD-8 (R4 deve continuar passando validação).

### Reaproveitamento de padrão, não de classe

`org.mitre.synthea.export.rif.CodeMapper` já resolve um problema estruturalmente idêntico (mapear um código Synthea para um ou mais códigos de um sistema externo, com peso). Reaproveitar a **classe** diretamente acoplaria esta feature a configurações do exportador BFD (`exporter.bfd.require_code_maps`), o que seria confuso e incorreto semanticamente. A recomendação é reaproveitar apenas o **formato/schema JSON**, com uma classe nova e independente em `org.mitre.synthea.br.coding.*`.

### Dependência de sequenciamento — parcialmente paralelizável

Como o código SNOMED-fonte (`254837009`) já existe hoje no upstream, o mapeamento (Tasks 1-2) pode ser feito sem esperar nenhuma outra story do Epic 2/3. Apenas a integração condicional no exportador (Task 3) precisa de `BrProfile.isActive()` (Story 3.1). Isso permite desenvolvimento parcialmente paralelo dentro do Epic 3.

### Project Structure Notes

```
src/main/resources/br/coding/
  snomed_to_cid10_breast_cancer.json     <- novo
src/main/java/org/mitre/synthea/br/coding/
  BrCodeMapper.java                       <- novo
src/test/java/org/mitre/synthea/br/coding/
  BrCodeMapperTest.java                   <- novo
docs/research/adr/
  ADR-00N-fonte-cid10-br.md                <- novo (número sequencial real depende de quantos ADRs já existirem)
```
Arquivo tocado no core: `src/main/java/org/mitre/synthea/export/FhirR4.java` (apenas o método `condition`/construção de `CodeableConcept`) — alteração pequena e aditiva, mas é um touchpoint de rebase a registrar (ver ADR-002, Story 1.3, sobre cadência de rebase).

### Testing Standards Summary

JUnit 4, validação HAPI obrigatória para o teste de integração de export (não validar apenas por string/assert simples). `./gradlew check` deve passar.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-6, #§10]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Localização-BR-Camadas-Técnicas item-3; #CID-10-BR-TBD]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-8, #Deferred]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-3, #Story-3.3]
- [Source: src/main/resources/modules/breast_cancer.json — código SNOMED 254837009]
- [Source: src/main/java/org/mitre/synthea/export/FhirR4.java#condition]
- [Source: src/main/java/org/mitre/synthea/export/rif/CodeMapper.java — padrão de schema JSON de mapeamento]
- [Source: _bmad-output/implementation-artifacts/2-2-modulo-gmf-cancer-de-mama-piloto.md, 3-1-perfil-demografico-brasileiro-ibge.md, 1-3-registro-de-decisoes-arquiteturais-adrs.md]

## Dev Agent Record

### Agent Model Used

claude-4.6-opus-high-thinking (Amelia / bmad-dev-story)

### Debug Log References

- Primeira execução de testes: falha Gson em `_meta` (objeto vs array) → corrigido parse via `JsonObject` em `BrCodeMapper`.
- Integração: NPE `PayerManager.noInsurance` → `PayerManager.loadNoInsurance()` no `@Before`.
- `./gradlew check` bloqueado nesta sessão por locks/corrupção em `build/` (processos Gradle concorrentes).

### Completion Notes List

- Data pack `snomed_to_cid10_breast_cancer.json`: SNOMED `254837009` → CID-10 `C50.9`, metadados `_meta` documentam natureza piloto/provisória.
- `BrCodeMapper`: carregador independente, schema compatível com `CodeMapper` BFD, ignora chaves `_`.
- `FhirR4.condition(...)`: `addBrCid10Coding()` aditivo quando `BrProfile.isActive()`.
- `ExportHelper`: alias `CID-10` → `http://hl7.org/fhir/sid/icd-10`.
- ADR-005 registrado; README de ADRs atualizado.
- Testes: `BrCodeMapperTest`, `BrCid10ExportIntegrationTest` (HAPI + regressão sem perfil).

### File List

- `src/main/resources/br/coding/snomed_to_cid10_breast_cancer.json` (novo)
- `src/main/java/org/mitre/synthea/br/coding/BrCodeMapper.java` (novo)
- `src/main/java/org/mitre/synthea/export/FhirR4.java` (modificado)
- `src/main/java/org/mitre/synthea/export/ExportHelper.java` (modificado)
- `src/test/java/org/mitre/synthea/br/coding/BrCodeMapperTest.java` (novo)
- `src/test/java/org/mitre/synthea/br/coding/BrCid10ExportIntegrationTest.java` (novo)
- `docs/research/adr/ADR-005-fonte-cid10-br.md` (novo)
- `docs/research/adr/README.md` (modificado)

### Change Log

- 2026-06-30: Code review — suprime ICD10-CM em `condition()` com perfil BR; patches BrCodeMapper + teste integração.
