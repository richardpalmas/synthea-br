---
baseline_commit: c1247106c03fa57ace54d269af98c7833f4006a6
---

# Story 4.1: Catálogo de Regras de Plausibilidade

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero um catálogo versionado de regras de coerência clínica com IDs estáveis e severidade,
para definir e evoluir critérios de qualidade de forma rastreável, independente de quando a cohort real estiver disponível.

## Acceptance Criteria

1. **Given** o repositório Synthea-br está configurado
   **When** o grupo adiciona regras de plausibilidade
   **Then** cada regra possui ID estável no formato `PLAUS-###` (ex.: `PLAUS-001`), severidade (`alta`/`média`/`baixa`) e descrição, definidos em um catálogo versionado em `src/main/resources/br/plausibility/catalog_breast_cancer.json`
   [Source: planning-artifacts/epics.md#Story-4.1; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-8; architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-5]

2. **Given** o catálogo piloto deve cobrir pelo menos 3 categorias de coerência clínica
   **When** as regras piloto são escritas para câncer de mama
   **Then** existem no mínimo: (a) **`PLAUS-001` (severidade alta)** — presença de tratamento/procedimento de câncer de mama sem diagnóstico (`ConditionOnset` SNOMED `254837009`) registrado **antes** do procedimento, espelhando o exemplo explícito do addendum ("mastectomia sem diagnóstico de câncer"); (b) **`PLAUS-002` (severidade média)** — sequência temporal improvável (ex.: procedimento de tratamento ocorrendo antes do exame diagnóstico/rastreamento que o originaria); (c) **`PLAUS-003` (severidade média ou baixa, a calibrar)** — compatibilidade de medicamentos (ex.: presença de medicamento de quimioterapia sem diagnóstico de câncer de mama associado no mesmo encontro/paciente)
   [Source: planning-artifacts/epics.md#Story-4.1 AC "sequência temporal de exames, compatibilidade de medicamentos, presença de diagnóstico"; planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Rigor-de-Plausibilidade — exemplo "mastectomia sem diagnóstico de câncer" como severidade Alta]

3. **Given** a validação deve ser **read-only** sobre `HealthRecord` (AD-2)
   **When** as regras são implementadas
   **Then** cada regra é uma classe Java que **apenas lê** `Person.record` (encounters, conditions, procedures, medications) e retorna violações estruturadas — nenhuma regra modifica o `HealthRecord` ou qualquer atributo do `Person`
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2]

4. **Given** o motor `HealthRecordEditor` já existente no Synthea upstream (`org.mitre.synthea.engine.HealthRecordEditor`/`HealthRecordEditors`) tem propósito **oposto** ao desta story
   **When** a arquitetura desta feature é decidida
   **Then** o catálogo de plausibilidade **não reutiliza** `HealthRecordEditor` — aquele mecanismo existe para **injetar** erros/ruído realista no registro (ex.: `GrowthDataErrorsEditor`), mutando o `HealthRecord` intencionalmente; esta story precisa do oposto (leitura pura, sem mutação) e deve usar uma interface nova e independente
   [Source: src/main/java/org/mitre/synthea/engine/HealthRecordEditor.java — Javadoc "HealthRecordEditors SHOULD NOT be used to simulate clinical interactions..."]

5. **Given** as regras devem ser testáveis **sem depender de uma cohort real gerada pelo Epic 2**
   **When** os testes desta story são escritos
   **Then** cada regra é testada com fixtures de `Person`/`HealthRecord` construídas manualmente em código de teste (adicionando `Encounter`/`Condition`/`Procedure`/`Medication` diretamente via a API existente de `HealthRecord`), cobrindo tanto o caso de violação quanto o caso conforme — **nenhum teste desta story depende da Epic 2 (módulo GMF, gate) estar implementada**
   [Source: instrução de sequenciamento desta tarefa — "testes unitários do catálogo de regras podem ser feitos isoladamente com fixtures de HealthRecord, sem esperar Epic 2 completo"]

6. **Given** a saída de cada regra deve ser estruturada e determinística (AD-5)
   **When** uma regra é avaliada
   **Then** retorna um objeto/registro com: `ruleId`, `severity`, `patientId` (ou referência), `description` da violação específica encontrada, e timestamp/contexto relevante (ex.: datas dos eventos em conflito) — formato reutilizável pela Story 4.2 (relatório)
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-5 "saída estruturada"]

7. **Given** esta story é fundação para a Story 4.2 (relatório) e para a revisão futura do ADR-001 (Story 1.1)
   **When** o catálogo é concluído
   **Then** o Dev Agent Record desta story inclui uma nota explícita sinalizando que, após a Story 4.2 também estar concluída e produzir métricas reais de SM-2, o **ADR-001** ("Spike de Viabilidade IA vs Regras Puras", Story 1.1) deve ser revisitado com dados reais, conforme já antecipado pelo próprio ADR-001
   [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md — Subtask 4.6 "ADR será revisitado com métricas reais após Epic 4"]

## Tasks / Subtasks

- [x] Task 1: Criar o catálogo versionado (AC: #1, #2)
  - [x] Subtask 1.1: Criar `src/main/resources/br/plausibility/catalog_breast_cancer.json` com metadados de `PLAUS-001`, `PLAUS-002`, `PLAUS-003` (id, severidade, título, descrição, versão)
  - [x] Subtask 1.2: Documentar no cabeçalho/README do diretório a convenção de numeração sequencial (futuras regras: `PLAUS-004`, etc.) e a estrutura de severidade (alta/média/baixa, ver definição operacional no addendum)

- [x] Task 2: Definir a interface de regra (AC: #3, #4, #6)
  - [x] Subtask 2.1: Criar `org.mitre.synthea.br.plausibility.PlausibilityRule` (interface): método `List<Violation> evaluate(Person person)`, somente leitura
  - [x] Subtask 2.2: Criar `org.mitre.synthea.br.plausibility.Violation` (classe imutável/record): `ruleId`, `severity`, `patientId`, `description`, `eventTimestamps`
  - [x] Subtask 2.3: Javadoc explícito reforçando a proibição de mutação do `HealthRecord` dentro de implementações de `PlausibilityRule`

- [x] Task 3: Implementar as 3 regras piloto (AC: #2)
  - [x] Subtask 3.1: `PLAUS001_TreatmentWithoutDiagnosis` — percorre `person.record.encounters`, verifica se existe `Procedure`/`Medication` de tratamento de câncer de mama (catálogo de códigos a definir, reaproveitando os códigos do módulo `breast_cancer.json` — ex. cirurgia, quimioterapia) sem `Condition` com código `254837009` registrada em data anterior
  - [x] Subtask 3.2: `PLAUS002_TreatmentBeforeDiagnosticExam` — verifica se procedimento de tratamento ocorre antes do procedimento diagnóstico (ex.: mamografia, código `241055006`, ou ultrassonografia, código `1571000087109` — ambos já usados no módulo upstream)
  - [x] Subtask 3.3: `PLAUS003_MedicationDiagnosisCompatibility` — verifica presença de medicação de quimioterapia sem diagnóstico de câncer de mama associado no registro do paciente
  - [x] Subtask 3.4: Cada regra referencia seu `ruleId` do catálogo (Task 1) de forma centralizada (constante ou leitura do próprio catálogo JSON), evitando hardcoding duplicado do ID

- [x] Task 4: Criar registro/catálogo executável (AC: #1, #6)
  - [x] Subtask 4.1: Criar `org.mitre.synthea.br.plausibility.PlausibilityCatalog` — carrega metadados do JSON e mantém a lista de implementações de `PlausibilityRule` registradas, expondo um método `evaluateAll(Person person)` que roda todas as regras e agrega violações

- [x] Task 5: Testes com fixtures (AC: #5)
  - [x] Subtask 5.1: Criar `src/test/java/org/mitre/synthea/br/plausibility/Plaus001Test.java` (e equivalentes para 002/003) — construir `Person`/`HealthRecord` manualmente via API existente (`person.record.encounterStart(...)`, `encounter.conditions.add(...)`, etc.) para os casos de violação e conformidade
  - [x] Subtask 5.2: Teste de `PlausibilityCatalog.evaluateAll` agregando múltiplas violações de um mesmo paciente fixture
  - [x] Subtask 5.3: Rodar `./gradlew check`

## Dev Notes

### Esta story é deliberadamente desacoplada do Epic 2 — não esperar pela cohort real

Por design explícito (AC #5), todos os testes desta story usam fixtures construídas manualmente, não uma simulação real via `Generator`. Isso permite que o Epic 4 comece a ser desenvolvido em paralelo ao Epic 2/3, desde que os códigos clínicos de referência (SNOMED do módulo `breast_cancer.json`) sejam usados como base — eles já existem hoje, independente de qualquer story do Epic 2 estar implementada.

### Não confundir com `HealthRecordEditor` — propósito oposto

Ver AC #4. Esta é uma confusão fácil de cometer por similaridade de nome — `HealthRecordEditor` é uma ferramenta de **injeção de ruído realista** (mutação intencional), não de validação. O catálogo de plausibilidade precisa de uma interface nova, estritamente read-only.

### `PLAUS-001` é o exemplo "âncora" do addendum — não trivializar

O addendum cita literalmente "mastectomia sem diagnóstico de câncer" como exemplo de violação de severidade **Alta** (meta MVP: 0% dos pacientes). Esta regra (`PLAUS-001`) é a mais importante do catálogo piloto e deve ser implementada com atenção máxima a falsos negativos (deixar passar uma violação real) — calibrar a lista de "procedimentos de tratamento" cuidadosamente a partir do módulo `breast_cancer.json` real (ler o módulo por completo para extrair todos os procedimentos de tratamento relevantes, não assumir uma lista incompleta).

### Conexão com ADR-001 (Story 1.1) — sinalizar, não revisar agora

Ver AC #7. Esta story (e a 4.2) são exatamente o que o ADR-001 identificou como necessário para uma análise quantitativa real de SM-2. Não é responsabilidade desta story reabrir/editar o ADR-001 — apenas registrar a sinalização no Dev Agent Record para que, ao concluir a Story 4.2, alguém (humano ou agente) volte ao ADR-001 com os dados reais.

### Formato do catálogo — JSON, não YAML

O FR-8 menciona "JSON/YAML" como opções; esta story padroniza em **JSON** por consistência com os demais data packs BR já definidos nas Stories anteriores (ex.: `snomed_to_cid10_breast_cancer.json` da Story 3.3) e com o uso já estabelecido de Gson no projeto (`project-context.md` — "Gson para módulos JSON"). Não introduzir SnakeYAML para este catálogo especificamente, mesmo estando disponível no projeto.

### Project Structure Notes

```
src/main/resources/br/plausibility/
  catalog_breast_cancer.json              <- novo (metadados das regras)
src/main/java/org/mitre/synthea/br/plausibility/
  PlausibilityRule.java                   <- novo (interface)
  Violation.java                          <- novo
  PlausibilityCatalog.java                <- novo
  rules/
    Plaus001TreatmentWithoutDiagnosis.java
    Plaus002TreatmentBeforeDiagnosticExam.java
    Plaus003MedicationDiagnosisCompatibility.java
src/test/java/org/mitre/synthea/br/plausibility/
  Plaus001Test.java, Plaus002Test.java, Plaus003Test.java, PlausibilityCatalogTest.java
```

### Testing Standards Summary

JUnit 4, fixtures manuais de `Person`/`HealthRecord` (sem `Generator` completo). `./gradlew check` deve passar. Cobertura deve incluir caso de violação E caso conforme para cada regra (evitar testes que só verificam o "caminho feliz").

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-8]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Rigor-de-Plausibilidade]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-5]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-4, #Story-4.1]
- [Source: src/main/java/org/mitre/synthea/engine/HealthRecordEditor.java, HealthRecordEditors.java — mecanismo distinto, não reutilizar]
- [Source: src/main/java/org/mitre/synthea/world/concepts/HealthRecord.java — estrutura de Encounter/Condition/Procedure/Medication]
- [Source: src/main/resources/modules/breast_cancer.json — códigos SNOMED de referência para as regras piloto]
- [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md — nota de revisão futura do ADR-001]

## Dev Agent Record

### Agent Model Used

claude-4.6-opus-high-thinking

### Debug Log References

- Testes unitários passaram via `./gradlew test --tests "org.mitre.synthea.br.plausibility.*"` após correção de diretório binário de resultados no Windows (`build.gradle`).

### Completion Notes List

- Catálogo JSON v1.0.0 com PLAUS-001/002/003, códigos clínicos extraídos de `breast_cancer.json` e submódulos (`surgery_therapy_breast`, `chemotherapy_breast`).
- Interface `PlausibilityRule` read-only (distinta de `HealthRecordEditor`); `Violation` imutável com `eventTimestamps`.
- Três regras piloto implementadas referenciando metadados do catálogo via `PlausibilityCatalogLoader`.
- Testes com fixtures manuais cobrindo violação e conformidade para cada regra + agregação em `PlausibilityCatalog`.
- **Sinalização ADR-001:** após Story 4.2 produzir métricas SM-2 reais, o ADR-001 deve ser revisitado com dados quantitativos (conforme AC #7).

### File List

- src/main/resources/br/plausibility/catalog_breast_cancer.json
- src/main/resources/br/plausibility/README.md
- src/main/java/org/mitre/synthea/br/plausibility/PlausibilityRule.java
- src/main/java/org/mitre/synthea/br/plausibility/Violation.java
- src/main/java/org/mitre/synthea/br/plausibility/RuleMetadata.java
- src/main/java/org/mitre/synthea/br/plausibility/ClinicalCode.java
- src/main/java/org/mitre/synthea/br/plausibility/BreastCancerCodeSets.java
- src/main/java/org/mitre/synthea/br/plausibility/PlausibilityCatalogLoader.java
- src/main/java/org/mitre/synthea/br/plausibility/HealthRecordScan.java
- src/main/java/org/mitre/synthea/br/plausibility/PlausibilityCatalog.java
- src/main/java/org/mitre/synthea/br/plausibility/rules/Plaus001TreatmentWithoutDiagnosis.java
- src/main/java/org/mitre/synthea/br/plausibility/rules/Plaus002TreatmentBeforeDiagnosticExam.java
- src/main/java/org/mitre/synthea/br/plausibility/rules/Plaus003MedicationDiagnosisCompatibility.java
- src/test/java/org/mitre/synthea/br/plausibility/Plaus001Test.java
- src/test/java/org/mitre/synthea/br/plausibility/Plaus002Test.java
- src/test/java/org/mitre/synthea/br/plausibility/Plaus003Test.java
- src/test/java/org/mitre/synthea/br/plausibility/PlausibilityCatalogTest.java

### Change Log

- 2026-07-08: Story 4.1 implementada — catálogo versionado, interface read-only, 3 regras piloto e testes com fixtures.
