# Story 2.2: Módulo GMF Câncer de Mama (Piloto)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero um módulo GMF que identifique de forma confiável pacientes com câncer de mama,
para que a geração direcionada (Story 2.3) consiga reter/excluir pacientes corretamente, sem recriar a trajetória clínica que já existe no Synthea upstream.

## Acceptance Criteria

1. **Given** o Synthea upstream **já inclui** um módulo de doença completo para câncer de mama (`src/main/resources/modules/breast_cancer.json`, GMF 2.0, ~2080 linhas, com onset baseado em risco por raça/idade, rastreamento, diagnóstico, estadiamento e tratamento, usando o código SNOMED `254837009` "Malignant neoplasm of breast (disorder)" no estado `ConditionOnset` chamado `"Breast Cancer"`)
   **When** esta story é escopada
   **Then** o módulo de doença existente **não é recriado nem duplicado** — esta story entrega apenas o **módulo de gate** ("keep module") que verifica a presença da condição no `HealthRecord` do paciente, reaproveitando 100% da modelagem clínica já presente e citada upstream
   [Source: src/main/resources/modules/breast_cancer.json; planning-artifacts/epics.md#Story-2.2 — reinterpretado à luz do mecanismo nativo descoberto em 2.1]

2. **Given** o padrão nativo de "keep module" já existe em `src/main/resources/keep_modules/` (ex.: `keep_diabetes.json`, usando `condition_type: "Active Condition"` com código SNOMED)
   **When** o módulo de gate piloto é criado
   **Then** é criado em `src/main/resources/keep_modules/br/breast_cancer.json`, seguindo exatamente o mesmo padrão: estado `Initial` com `conditional_transition` checando `"Active Condition"` (SNOMED `254837009`) → transição para estado terminal `"Keep"`; caso contrário → transição para estado terminal `"Terminal"` (descartar/reexecutar)
   [Source: src/main/resources/keep_modules/keep_diabetes.json; src/test/resources/keep_patients_module/keep.json]

3. **Given** a estrutura sugerida pela Architecture Spine cita `resources/modules/br/` para "módulos GMF de condição alvo"
   **When** o módulo de gate é posicionado em `keep_modules/br/` em vez de `modules/br/`
   **Then** essa escolha é documentada explicitamente nesta story (Dev Notes) como uma decisão pragmática: o mecanismo nativo `-k`/`keepPatientsModulePath` resolve módulos de gate especificamente dentro de `keep_modules/` (ver `App.java`); posicioná-lo em `modules/br/` o tornaria inacessível ao mecanismo nativo sem reescrever a resolução de caminho. Esta nota previne que um revisor futuro "corrija" o caminho para `modules/br/` e quebre a feature
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#Structural-Seed vs. src/main/java/App.java — resolução real de `-k`]

4. **Given** o módulo de gate é processado pelo motor GMF (`Module.process`)
   **When** a geração roda com `br.target_condition=breast_cancer` (Story 2.1) e seed fixo
   **Then** os estados do módulo de gate são clonados por paciente automaticamente pelo motor (comportamento padrão de `Module.process`, não exige código extra nesta story) — o módulo mestre (`keep_modules/br/breast_cancer.json` carregado em memória) nunca é mutado entre execuções de pacientes diferentes (AD-2)
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2; _bmad-output/project-context.md — "Módulos JSON são compartilhados entre pacientes — clonar State antes de executar; nunca mutar o master module"]

5. **Given** o módulo de gate precisa ser testável de forma determinística
   **When** testes automatizados rodam com seed fixo
   **Then** existe teste JUnit 4 que: (a) gera um paciente cujo `HealthRecord` contém a condição (forçando via fixture/atributo de teste ou usando seeds conhecidas que produzem a condição no módulo de doença upstream) e confirma que o módulo de gate termina em `"Keep"`; (b) gera um paciente sem a condição e confirma terminação em `"Terminal"`
   [Source: planning-artifacts/epics.md#Story-2.2 AC3 "testes com seed fixo passam em ./gradlew check"]

6. **Given** o catálogo de condições suportadas foi criado na Story 2.1 apontando para `keep_modules/br/breast_cancer.json`
   **When** esta story entrega o arquivo
   **Then** o caminho do arquivo entregue corresponde **exatamente** ao caminho já registrado no catálogo da Story 2.1 (`org.mitre.synthea.br.condition.SupportedConditions`) — nenhuma divergência de nome/caminho entre as duas stories
   [Source: _bmad-output/implementation-artifacts/2-1-configuracao-de-condicao-clinica-alvo.md — Task 1.1]

7. **Given** o módulo de gate deve cobrir tanto pacientes femininos quanto o raro caso masculino já modelado no módulo de doença upstream
   **When** o gate é avaliado
   **Then** a checagem de "Active Condition" não depende do gênero do paciente — usa apenas a presença do código SNOMED `254837009` no registro, válido tanto para a ramificação `"Female"` quanto `"Male"` do módulo de doença upstream
   [Source: src/main/resources/modules/breast_cancer.json — ramificações `Female`/`Male` no estado `Initial`]

## Tasks / Subtasks

- [x] Task 1: Confirmar o código SNOMED de referência (AC: #1, #2)
  - [x] Subtask 1.1: Reconfirmar em `breast_cancer.json` que o estado `"Breast Cancer"` usa `ConditionOnset` com código `254837009` / display `"Malignant neoplasm of breast (disorder)"` (já verificado nesta análise; revalidar se o módulo upstream mudar em rebase futuro)

- [x] Task 2: Criar o módulo de gate (AC: #2, #3, #7)
  - [x] Subtask 2.1: Criar `src/main/resources/keep_modules/br/breast_cancer.json` seguindo o padrão de `keep_diabetes.json`: `Initial` → `conditional_transition` com `condition_type: "Active Condition"`, `codes: [{system: "SNOMED-CT", code: "254837009", display: "Malignant neoplasm of breast (disorder)"}]` → `"Keep"`; fallback → `"Terminal"`
  - [x] Subtask 2.2: Adicionar `gmf_version: 2` e `remarks` citando que este módulo depende do módulo de doença upstream `breast_cancer.json` (referência cruzada para manutenção futura)
  - [x] Subtask 2.3: Validar o módulo com `./gradlew graphviz` (ferramenta já disponível no projeto) se aplicável, ou validação manual de schema GMF

- [x] Task 3: Validar consistência com a Story 2.1 (AC: #6)
  - [x] Subtask 3.1: Confirmar que `SupportedConditions` (Story 2.1) aponta exatamente para `keep_modules/br/breast_cancer.json`
  - [x] Subtask 3.2: Se a Story 2.1 ainda não estiver implementada, deixar nota explícita no PR desta story sobre o caminho exato entregue, para o desenvolvedor da 2.1 (ou quem revisar a integração) confirmar o match

- [x] Task 4: Testes (AC: #4, #5)
  - [x] Subtask 4.1: Criar `src/test/java/org/mitre/synthea/br/condition/BreastCancerKeepModuleTest.java` (ou localização equivalente)
  - [x] Subtask 4.2: Teste positivo — usar `Generator.GeneratorOptions.keepPatientsModulePath` apontando para o novo arquivo, com seed(s) conhecida(s) que produzem a condição no módulo de doença upstream dentro de um número razoável de tentativas (`generate.max_attempts_to_keep_patient`), e assertar que o paciente final tem o código `254837009` ativo no `HealthRecord`
  - [x] Subtask 4.3: Teste negativo/estrutural — carregar o módulo de gate isoladamente (sem rodar uma simulação completa) e usar um `Person` fixture com condição manualmente adicionada/ausente ao `record`, processando o módulo de gate diretamente e verificando o estado terminal (`Keep` vs `Terminal`) — abordagem mais rápida e determinística que depender de probabilidade de onset do módulo de doença
  - [x] Subtask 4.4: Rodar `./gradlew check`

## Dev Notes

### Reframing crítico desta story — não reinventar a roda

A leitura literal do `epics.md` ("quero um módulo GMF de câncer de mama em `resources/modules/br/`... para que a simulação produza trajetória clínica compatível") sugeria recriar toda a modelagem clínica. **Isso seria um erro grave de reinvenção**: o Synthea upstream já inclui um módulo de doença de câncer de mama extremamente completo e bem fundamentado (6 fontes citadas no próprio arquivo, incluindo Susan G. Komen, American Cancer Society, NCCN, CDC, AJCC Cancer Staging Manual). Esta story foi reescopada, à luz dessa descoberta, para entregar **apenas** o artefato realmente novo necessário: o módulo de **gate** que verifica a condição já produzida pelo módulo upstream, usando o mecanismo nativo `-k`/`keepPatientsModulePath` descoberto e documentado na Story 2.1.

### Inteligência da Story 2.1 (anterior no mesmo épico)

A Story 2.1 cria `org.mitre.synthea.br.condition.SupportedConditions`, que registra, para a chave `"breast_cancer"`: o nome do módulo de doença (`"Breast_cancer"`) e o caminho do módulo de gate (`keep_modules/br/breast_cancer.json`). Esta story (2.2) **entrega o arquivo físico** que a 2.1 já espera encontrar nesse caminho. Se 2.1 e 2.2 forem desenvolvidas em paralelo (cenário documentado e permitido pela própria Story 2.1, que usa fixtures de teste para não depender deste artefato), garantir alinhamento de caminho ao final via revisão cruzada antes de considerar o Epic 2 "integrável ponta a ponta".

### Dependência de sequenciamento com Story 2.3

A Story 2.3 (gate de cohort com condição garantida) depende desta story (módulo de gate físico) **e** da Story 2.1 (resolução de configuração) estarem ambas implementadas para ser testável de ponta a ponta com uma cohort real. Esta story, isoladamente, só entrega e testa o módulo de gate — não implementa o comportamento de "100% da cohort" nem o modo configurável de exclusão/falha (isso é explicitamente escopo da Story 2.3).

### Nota sobre nomenclatura de arquivo na Architecture Spine

Ver AC #3 — a Architecture Spine usa o caminho `resources/modules/br/` de forma genérica para "módulos GMF de condição alvo" na seção "Capability → Architecture Map", mas o mecanismo de implementação real (resolução do `-k`) exige que módulos de gate fiquem em `keep_modules/`. Esta story documenta essa nuance para qualquer revisor futuro — não é uma divergência da arquitetura, é uma interpretação correta do AD-7 (extensão organizada, sem modificar o core) aplicada ao mecanismo nativo já existente.

### Project Structure Notes

```
src/main/resources/
  modules/
    breast_cancer.json              <- já existe upstream; NÃO tocar
  keep_modules/
    keep_diabetes.json              <- já existe upstream; padrão de referência
    br/
      breast_cancer.json            <- criar nesta story
src/test/java/org/mitre/synthea/br/condition/
  BreastCancerKeepModuleTest.java    <- criar nesta story
```

### Testing Standards Summary

JUnit 4, `org.junit.Assert`. Preferir o Subtask 4.3 (processamento direto do módulo com fixture de `Person`/`HealthRecord`) como teste primário por ser determinístico e rápido; o Subtask 4.2 (simulação completa) serve como teste de integração complementar, aceitando custo de execução maior. `./gradlew check` deve passar.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-2]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-7, #Structural-Seed]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-2, #Story-2.2]
- [Source: _bmad-output/project-context.md — "Módulos JSON são compartilhados entre pacientes — clonar State antes de executar"]
- [Source: src/main/resources/modules/breast_cancer.json — módulo de doença upstream, código SNOMED 254837009]
- [Source: src/main/resources/keep_modules/keep_diabetes.json — padrão de módulo de gate]
- [Source: src/test/resources/keep_patients_module/keep.json; src/test/java/org/mitre/synthea/engine/GeneratorTest.java#testKeepPatientsModule]
- [Source: _bmad-output/implementation-artifacts/2-1-configuracao-de-condicao-clinica-alvo.md — catálogo de condições e caminho esperado do módulo de gate]

## Dev Agent Record

### Agent Model Used

claude-opus-4-6

### Debug Log References

SNOMED 254837009 confirmado no módulo upstream `breast_cancer.json`.

### Completion Notes List

- Gate module em `keep_modules/br/breast_cancer.json` — padrão `keep_diabetes.json`, Active Condition SNOMED 254837009
- Caminho alinhado com `SupportedConditions` da Story 2.1
- 4 testes em `BreastCancerKeepModuleTest` (estrutural + integração Generator)

### File List

- src/main/resources/keep_modules/br/breast_cancer.json
- src/test/java/org/mitre/synthea/br/condition/BreastCancerKeepModuleTest.java

## Change Log

- 2026-06-30: Story 2.2 implementada — módulo de gate piloto para câncer de mama

### Review Findings

- [x] [Review][Patch] SNOMED `254837009` duplicado sem teste de sincronização — **Corrigido:** `BreastCancerKeepModuleTest.testSnomedCodeMatchesJsonKeepModule()` valida JSON vs constante Java.
- [x] [Review][Defer] `KeepModulePaths` resolve caminho de dev relativo ao diretório de trabalho atual (não ao classpath), e o fallback via classloader nunca é exercitado pelos testes (todos rodam a partir da árvore fonte) — deferred, frágil para cenário de JAR empacotado; precisa de teste de integração específico no futuro.
