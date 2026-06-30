---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 2.1: Configuração de Condição Clínica Alvo

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero declarar a condição clínica alvo via `synthea.properties` ou flag CLI,
para direcionar a geração de cohort sem editar código Java.

## Acceptance Criteria

1. **Given** o Synthea-br está instalado e a propriedade `br.target_condition` está documentada em `synthea.properties`
   **When** o pesquisador define `br.target_condition=breast_cancer` via `synthea.properties`, `-c <arquivo>` ou `--br.target_condition=breast_cancer` no CLI
   **Then** o sistema carrega a condição e valida que existe um módulo GMF correspondente registrado em `Module` (para `breast_cancer`, o módulo `Breast_cancer` já existe upstream em `src/main/resources/modules/breast_cancer.json` — esta validação passa hoje, sem depender de nenhuma story futura)
   [Source: planning-artifacts/epics.md#Story-2.1; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-1; src/main/resources/modules/breast_cancer.json]

2. **Given** o pesquisador tenta usar uma condição não suportada (ex.: `br.target_condition=diabetes_tipo_x`)
   **When** a configuração é carregada
   **Then** a execução falha com mensagem de erro clara e identificável (ex.: `"Condição clínica alvo desconhecida: 'diabetes_tipo_x'. Condições suportadas: breast_cancer"`), sem stack trace genérico ou exceção não tratada
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-1 "Consequences (testable): Sistema rejeita condição inexistente com mensagem identificável"]

3. **Given** a documentação do projeto deve listar as condições do MVP
   **When** o pesquisador consulta `synthea.properties` ou a documentação
   **Then** a lista de condições suportadas no MVP (mínimo: `breast_cancer`) está documentada em comentário inline na property e referenciada na documentação (Story 1.5, quando existir)
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-1 "Documentação lista condições do MVP"]

4. **Given** a resolução de condição precisa, eventualmente, acionar o mecanismo de gate de cohort (Story 2.3) e o módulo de gate piloto (Story 2.2) ainda não existe neste momento de implementação
   **When** `br.target_condition=breast_cancer` é resolvido com sucesso (AC #1) mas o módulo de "keep" correspondente (`keep_modules/br/breast_cancer.json`, criado pela Story 2.2) ainda não existe no repositório
   **Then** o sistema falha com erro **distinto e claro** ("módulo de gate para a condição 'breast_cancer' ainda não disponível — ver Story 2.2"), nunca um `FileNotFoundException` genérico — permitindo que a Story 2.1 seja implementada e testada de forma isolada, em paralelo à Story 2.2, usando um módulo de gate de teste/fixture
   [Source: derivado do mecanismo nativo `Generator.GeneratorOptions.keepPatientsModulePath`; ver Dev Notes]

5. **Given** a implementação reside em `org.mitre.synthea.br.*` (AD-7)
   **When** o código é revisado
   **Then** nenhuma classe nova de produção é criada fora de `org.mitre.synthea.br.*`, e o(s) ponto(s) de integração com código core (`App.java` e/ou `Generator.java`) é(são) limitados a no máximo uma chamada delegando para a classe BR — sem lógica de resolução de condição implementada inline no core
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-7]

6. **Given** o perfil `br` não está ativo (`br.target_condition` não definido)
   **When** o Synthea-br é executado normalmente
   **Then** o comportamento upstream permanece **inalterado** — nenhum gate é aplicado, nenhum erro novo é introduzido para usuários que não usam a feature
   [Source: planning-artifacts/epics.md#Story-3.1 AC "perfil desativado mantém comportamento upstream inalterado" — princípio aplicado também aqui por analogia/AD-1]

7. **Given** a forma de passar a condição via CLI precisa ser tecnicamente correta
   **When** a documentação é escrita
   **Then** o mecanismo CLI documentado é `--br.target_condition=breast_cancer` (convenção genérica `--config.chave=valor` já existente em `App.java`), **não** `-Dbr.target_condition=...` (flag de propriedade JVM, que `Config` não lê) — o texto do épico citava `-D` de forma imprecisa; esta story corrige a documentação para o mecanismo real
   [Source: src/main/java/App.java — parsing de `--config.*=value`; src/main/java/org/mitre/synthea/helpers/Config.java — não há leitura de `System.getProperty`]

## Tasks / Subtasks

- [x] Task 1: Criar catálogo de condições suportadas (AC: #1, #2, #3)
  - [x] Subtask 1.1: Criar `org.mitre.synthea.br.condition.SupportedConditions` (ou nome equivalente) com um registro estático mapeando chave de condição (`"breast_cancer"`) para: nome do módulo GMF de doença (`"Breast_cancer"`) e caminho do módulo de gate (`"br/breast_cancer.json"`, relativo a `keep_modules/`)
  - [x] Subtask 1.2: Javadoc documentando que o catálogo é o único lugar a editar ao adicionar novas condições no futuro (pós-MVP, FR-3)

- [x] Task 2: Implementar resolução e validação de `br.target_condition` (AC: #1, #2, #4, #5)
  - [x] Subtask 2.1: Criar `org.mitre.synthea.br.condition.TargetConditionConfig` com método estático `resolve(String conditionKey)` retornando um objeto/registro com módulo de doença + caminho do módulo de gate, ou lançando exceção clara
  - [x] Subtask 2.2: Validar existência do módulo de doença via `Module` (registro já carregado por `Module.loadModules()`/`Module.getModuleByPath`) — para `breast_cancer`, deve passar usando o módulo upstream já existente
  - [x] Subtask 2.3: Validar existência do recurso do módulo de gate (`keep_modules/br/breast_cancer.json`) via `Class.getResourceAsStream` ou checagem de arquivo; se ausente, lançar exceção com mensagem citando a Story 2.2 (AC #4)
  - [x] Subtask 2.4: Adicionar método `applyToOptions(Generator.GeneratorOptions options)` que, se a condição resolver com sucesso (módulo de gate presente), define `options.keepPatientsModulePath` apontando para o módulo de gate resolvido

- [x] Task 3: Integrar ao ponto de entrada (AC: #5, #6)
  - [x] Subtask 3.1: Decidir e documentar o ponto único de integração — recomendado: dentro de `Generator.init()` (não em `App.java`), pois cobre tanto uso via CLI quanto uso programático/testes do `Generator` como biblioteca (ver Dev Notes)
  - [x] Subtask 3.2: Adicionar a chamada única: se `Config.get("br.target_condition")` não for nulo/vazio, delegar para `TargetConditionConfig.resolve(...)` e `applyToOptions(this.options)`
  - [x] Subtask 3.3: Garantir que, com a property não definida, nenhum código novo é executado (early return)

- [x] Task 4: Documentar a property (AC: #3, #7)
  - [x] Subtask 4.1: Adicionar `br.target_condition` em `synthea.properties` com comentário listando condições suportadas no MVP e o mecanismo CLI correto (`--br.target_condition=valor`)

- [x] Task 5: Testes (AC: #1, #2, #4, #6)
  - [x] Subtask 5.1: Criar `src/test/java/org/mitre/synthea/br/condition/TargetConditionConfigTest.java`
  - [x] Subtask 5.2: Teste: `breast_cancer` resolve o módulo de doença com sucesso (independente da Story 2.2)
  - [x] Subtask 5.3: Teste: condição desconhecida lança exceção com mensagem clara contendo a lista de condições suportadas
  - [x] Subtask 5.4: Teste: módulo de gate ausente (usar uma condição de teste fictícia registrada apenas no teste, apontando para um caminho de gate inexistente) lança exceção distinta mencionando a Story 2.2 — **não** depende do artefato real da Story 2.2 para este teste passar
  - [x] Subtask 5.5: Teste: sem `br.target_condition` definido, `Generator` se comporta identicamente ao upstream (nenhuma mudança em `options.keepPatientsModulePath`)
  - [x] Subtask 5.6: Rodar `./gradlew check` antes de finalizar

## Dev Notes

### Descoberta crítica — NÃO reinventar o mecanismo de gate; ele já existe no Synthea upstream

O Synthea upstream **já implementa** um mecanismo completo de "manter apenas pacientes que satisfazem uma condição": `Generator.GeneratorOptions.keepPatientsModulePath` (+ `Config: generate.max_attempts_to_keep_patient`, default 1000) + lógica em `Generator.checkCriteria()`/`generatePerson()` que reexecuta a simulação com seed rotacionada até um módulo de "keep" terminar no estado `"Keep"`. CLI: flag `-k <caminho-ou-nome>` (ver `App.java`, resolve por caminho absoluto ou por nome dentro de `src/main/resources/keep_modules/`). Existem módulos de exemplo prontos em `src/main/resources/keep_modules/` (ex.: `keep_diabetes.json`, usando `condition_type: "Active Condition"` com código SNOMED) e um teste de referência: `GeneratorTest.testKeepPatientsModule()` + fixture `src/test/resources/keep_patients_module/keep.json`.

**Implicação direta para esta story:** `br.target_condition` **não precisa implementar um gate do zero** — seu papel é resolver o nome da condição para (a) o módulo de doença GMF correspondente (validação de existência) e (b) o caminho do módulo de "keep" a ser usado, e então **delegar** para o mecanismo nativo já existente, populando `options.keepPatientsModulePath`. A Story 2.3 cuidará da semântica completa do gate (percentual configurável, modo de exclusão vs. falha).

### Descoberta crítica — o módulo de doença "breast_cancer" já existe upstream

`src/main/resources/modules/breast_cancer.json` (~2080 linhas) já modela a trajetória clínica completa de câncer de mama (rastreamento, diagnóstico, estadiamento, tratamento, óbito), com onset baseado em risco por raça/idade (não garantido — ~10-13% das mulheres). O estado `"Breast Cancer"` (`ConditionOnset`) usa o código SNOMED `254837009` ("Malignant neoplasm of breast (disorder)"). Este módulo já roda para **todo** paciente gerado (não é opcional/gated) porque está na pasta padrão de módulos. **Não recriar este módulo na Story 2.2** — ver Dev Notes daquela story.

### Correção em relação ao texto do épico — mecanismo CLI real

O `epics.md` cita `-Dbr.target_condition=...` como exemplo de CLI. Isso está **tecnicamente incorreto** para este codebase: `Config` é um wrapper simples de `Properties`, sem leitura de `System.getProperty`. O mecanismo real e já existente em `App.java` é `--config.chave=valor` (qualquer propriedade de `synthea.properties` pode ser sobrescrita via `--chave=valor` no CLI). Use `--br.target_condition=breast_cancer`. Documentar isso corretamente evita que o desenvolvedor implemente um parser de `-D` que não terá efeito.

### Ponto de integração recomendado — `Generator.init()`, não `App.java`

`Generator` é documentado como usável como biblioteca standalone (Javadoc: "When Synthea is used as a standalone library"), e há testes que constroem `Generator` diretamente (`GeneratorTest`), sem passar por `App.java`. Se a resolução de `br.target_condition` for implementada apenas em `App.java`, qualquer uso programático/de teste do `Generator` não se beneficiaria da feature. Recomenda-se integrar dentro de `Generator.init()` (ou imediatamente após, antes do primeiro `generatePerson`), garantindo cobertura uniforme. Esta é uma decisão de implementação, não uma mudança de AC — se o time preferir `App.java` por algum motivo (ex.: isolar 100% o core), documentar a troca via nota no Dev Agent Record desta story.

### Dependência crítica de sequenciamento com Story 2.2 (evita bloqueio)

Por design (AC #4, Task 5.4), esta story **não bloqueia** na ausência do artefato real da Story 2.2 — usa um módulo de gate fictício apenas nos próprios testes. Isso permite que 2.1 e 2.2 sejam desenvolvidas em paralelo, conforme indicado no plano de sequenciamento do sprint. Quando a Story 2.2 entregar `keep_modules/br/breast_cancer.json`, o catálogo desta story (Task 1.1) já estará apontando para o caminho correto — nenhuma mudança de código deve ser necessária em 2.1, apenas a presença do arquivo.

### Project Structure Notes

```
src/main/java/org/mitre/synthea/br/condition/
  SupportedConditions.java          <- novo
  TargetConditionConfig.java        <- novo
src/main/resources/
  synthea.properties                <- adicionar br.target_condition (comentado)
src/test/java/org/mitre/synthea/br/condition/
  TargetConditionConfigTest.java    <- novo
```
Primeira story de Epic 2 — define o padrão de subpacote `org.mitre.synthea.br.condition.*` que a Story 2.3 (gate) deverá seguir.

### Testing Standards Summary

JUnit 4, `org.junit.Assert` estático, localização espelhando pacote. `./gradlew check` deve passar (Checkstyle, JaCoCo). Testes não devem depender de artefatos de outras stories ainda não implementadas (usar fixtures locais quando necessário, como já é o padrão upstream em `GeneratorTest`).

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-1, #FR-3]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-1, #AD-4, #AD-7]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-2, #Story-2.1, #Story-2.2, #Story-2.3]
- [Source: _bmad-output/project-context.md — convenções de pacote, properties, testes]
- [Source: src/main/java/org/mitre/synthea/engine/Generator.java — `GeneratorOptions.keepPatientsModulePath`, `checkCriteria`, `maxAttemptsToKeepPatient`]
- [Source: src/main/java/App.java — flag `-k`, parsing `--config.*=value`]
- [Source: src/main/resources/keep_modules/keep_diabetes.json — padrão de módulo de gate via "Active Condition"]
- [Source: src/test/java/org/mitre/synthea/engine/GeneratorTest.java#testKeepPatientsModule; src/test/resources/keep_patients_module/keep.json]
- [Source: src/main/resources/modules/breast_cancer.json — módulo de doença já existente, código SNOMED 254837009]
- [Source: src/main/java/org/mitre/synthea/helpers/Config.java]

## Dev Agent Record

### Agent Model Used

claude-opus-4-6

### Debug Log References

Integração via `TargetConditionIntegration.tryInitialize()` em `Generator.init()` — ponto único de delegação.

### Completion Notes List

- Catálogo `SupportedConditions` com `breast_cancer` → módulo upstream + gate `keep_modules/br/breast_cancer.json`
- `TargetConditionConfig.resolve()` valida módulo GMF e gate; exceções `UnknownTargetConditionException` e `GateModuleNotAvailableException`
- Property documentada em `synthea.properties` com CLI `--br.target_condition=valor`
- 5 testes em `TargetConditionConfigTest` — todos passando

### File List

- src/main/java/org/mitre/synthea/br/condition/SupportedConditions.java
- src/main/java/org/mitre/synthea/br/condition/TargetConditionConfig.java
- src/main/java/org/mitre/synthea/br/condition/KeepModulePaths.java
- src/main/java/org/mitre/synthea/br/condition/UnknownTargetConditionException.java
- src/main/java/org/mitre/synthea/br/condition/GateModuleNotAvailableException.java
- src/main/java/org/mitre/synthea/br/condition/TargetConditionIntegration.java
- src/main/java/org/mitre/synthea/engine/Generator.java
- src/main/resources/synthea.properties
- src/test/java/org/mitre/synthea/br/condition/TargetConditionConfigTest.java

## Change Log

- 2026-06-30: Story 2.1 implementada — resolução de `br.target_condition` com delegação ao mecanismo nativo `keepPatientsModulePath`

### Review Findings

- [x] [Review][Decision] AC#5 — múltiplos touchpoints em `Generator.java` — **Aceito como exceção documentada:** AC#5 foi escrito antes da Story 2.3; os touchpoints adicionais (exclude mode, logSummary, getter) são necessários para o gate de cohort.
- [x] [Review][Patch] Sobrescrita silenciosa de `keepPatientsModulePath` — **Corrigido:** `TargetConditionConfig.applyToOptions()` lança `IllegalStateException` se o path já estiver definido.
- [x] [Review][Patch] `tryInitialize()` sem try/catch em `Generator.init()` — **Corrigido:** envolvido em try/catch com `ExceptionInInitializerError`, consistente com bloco irmão.
