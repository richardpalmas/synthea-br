# Story 2.3: Gate de Cohort com Condição Garantida

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero que 100% (ou um percentual configurável) da cohort apresente a condição alvo ao final da simulação,
para usar os dados em estudos estatísticos sem filtragem manual.

## Acceptance Criteria

1. **Given** `br.target_condition=breast_cancer` está ativo (Story 2.1) e o módulo de gate correspondente existe (Story 2.2)
   **When** o pesquisador gera uma cohort com seed fixo (ex.: n=50 em teste, n=500 em validação de release) usando o modo padrão (`retry`, ver AC #4)
   **Then** 100% dos pacientes exportados apresentam o código SNOMED `254837009` ("Malignant neoplasm of breast (disorder)") verificável no `HealthRecord` (SM-1)
   [Source: planning-artifacts/epics.md#Story-2.3; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-2, #SM-1]

2. **Given** o mecanismo nativo `Generator` já reexecuta a geração de um paciente até satisfazer o módulo de gate ou exceder `generate.max_attempts_to_keep_patient` (default 1000; já existente upstream)
   **When** o número de tentativas é excedido (situação rara para câncer de mama: risco basal upstream ~10-13% em mulheres, média esperada de ~8 tentativas por paciente — bem abaixo do limite default)
   **Then** a execução falha com a mensagem de erro **já existente** do `Generator` ("Failed to produce a matching patient after N attempts...") — esta story documenta esse comportamento como o "modo falha" exigido pelo FR-2, sem precisar reimplementá-lo
   [Source: src/main/java/org/mitre/synthea/engine/Generator.java#generatePerson — mensagem de erro nativa já existente]

3. **Given** o FR-2 menciona "pacientes não conformes são excluíveis via flag de exportação" como alternativa ao modo de retry/falha
   **When** o pesquisador define `br.target_condition.gate_mode=exclude` (novo, default `retry`)
   **Then** a geração roda **sem** o loop de reexecução nativo (não usa `keepPatientsModulePath` diretamente) — gera a população solicitada normalmente e, antes de cada exportação individual, avalia o módulo de gate; pacientes não conformes **não são exportados** (resultando em uma cohort exportada potencialmente menor que `-p` solicitado), e a contagem de excluídos é logada de forma clara ao final da execução
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-2 "Pacientes fora da condição excluíveis via flag de exportação"]

4. **Given** dois modos de gate precisam coexistir sem ambiguidade
   **When** a property `br.target_condition.gate_mode` é documentada
   **Then** os valores aceitos são exatamente `retry` (default — usa o mecanismo nativo `-k`/`keepPatientsModulePath`, garante 100% por reexecução ou falha) e `exclude` (gera sem retry, filtra na exportação, cohort final pode ser menor que solicitado) — qualquer outro valor produz erro de configuração claro
   [Source: derivado de FR-2 + mecanismo nativo documentado na Story 2.1/2.2]

5. **Given** a reprodutibilidade é um requisito transversal (NFR1)
   **When** a mesma seed e a mesma configuração (incluindo `gate_mode`) são usadas em duas execuções
   **Then** o percentual de pacientes conformes (100% no modo `retry`; percentual observado e estável no modo `exclude`) e a contagem final de pacientes exportados são **idênticos** entre as execuções
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#NFR1; planning-artifacts/epics.md#Story-2.3 AC "mesma seed + config produz percentual idêntico"]

6. **Given** AD-2 e AD-4 exigem que o gate não mute exportadores e que apenas engine/módulos escrevam no `HealthRecord`
   **When** o modo `exclude` é implementado
   **Then** a filtragem ocorre **antes** da chamada a `Exporter.export(...)` (pulando a chamada para pacientes não conformes), nunca removendo/alterando dados já escritos no `HealthRecord` por um exportador — os exportadores permanecem 100% read-only
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-4]

7. **Given** esta story depende fisicamente dos artefatos das Stories 2.1 e 2.2
   **When** esta story é iniciada
   **Then** o desenvolvedor confirma que ambas as stories anteriores estão implementadas e seus testes passam **antes** de escrever testes de integração ponta a ponta desta story — testes unitários da lógica de `gate_mode` em si (ex.: roteamento de configuração) podem usar fixtures/mocks independentes das Stories 2.1/2.2, mas o teste de cohort real (AC #1) exige ambas implementadas
   [Source: instrução de sequenciamento desta tarefa — Epic 2 é a cadeia de dependências mais rígida do roadmap]

8. **Given** o percentual configurável (`<100%`) é mencionado no FR-2 como "percentual configurável (default 100%)"
   **When** o escopo desta story é avaliado
   **Then** a implementação de um percentual configurável **menor que 100%** (ex.: 80% da cohort com a condição, 20% sem) está **fora do escopo do MVP** desta story — o mecanismo nativo de retry é binário por natureza (paciente satisfaz ou não); suportar fração arbitrária exigiria lógica adicional de seleção probabilística por índice, não coberta pelos ACs acima. Esta lacuna deve ser registrada como nota de acompanhamento (idealmente um ADR ou item de backlog futuro), não implementada "de qualquer jeito" para fechar a AC literal do FR-2
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§9 SM-1 "100% pacientes... com condição verificável" — meta MVP é 100%, não fração arbitrária]

## Tasks / Subtasks

- [x] Task 1: Confirmar pré-requisitos (AC: #7)
  - [x] Subtask 1.1: Verificar que `org.mitre.synthea.br.condition.TargetConditionConfig` (Story 2.1) e `keep_modules/br/breast_cancer.json` (Story 2.2) estão implementados e com testes passando
  - [x] Subtask 1.2: Se alguma das duas estiver pendente, implementar a lógica de roteamento de `gate_mode` desta story usando fixtures locais (não bloquear todo o trabalho), mas marcar o teste de integração ponta a ponta (Task 4) como bloqueado até a dependência ser resolvida

- [x] Task 2: Implementar `br.target_condition.gate_mode` (AC: #3, #4)
  - [x] Subtask 2.1: Adicionar property em `synthea.properties` com comentário documentando os dois valores aceitos e o default `retry`
  - [x] Subtask 2.2: Criar `org.mitre.synthea.br.condition.GateMode` (enum: `RETRY`, `EXCLUDE`) com parsing e erro claro para valores inválidos
  - [x] Subtask 2.3: No modo `RETRY` (default): delegar para `TargetConditionConfig.applyToOptions` (Story 2.1), que popula `options.keepPatientsModulePath` — comportamento nativo já cobre o restante
  - [x] Subtask 2.4: No modo `EXCLUDE`: **não** popular `keepPatientsModulePath`; em vez disso, adicionar um hook que avalia o módulo de gate (reutilizando a mesma lógica de carregamento/processamento do módulo já usada no modo `RETRY`, extraída para um método compartilhado) imediatamente antes da chamada de exportação por paciente, pulando a exportação se não conforme

- [x] Task 3: Implementar contagem/log de exclusão (AC: #3, #6)
  - [x] Subtask 3.1: Adicionar contador de pacientes excluídos no modo `EXCLUDE`
  - [x] Subtask 3.2: Logar ao final da execução (ex.: via log estruturado, conforme convenção de "logs estruturados para anexar em docs/research/experiments" da Architecture Spine): total solicitado, total exportado, total excluído, percentual conforme

- [x] Task 4: Testes de integração ponta a ponta (AC: #1, #2, #5) — depende de 2.1 e 2.2 completas
  - [x] Subtask 4.1: Teste com seed fixo, `gate_mode=retry`, n pequeno (ex.: 20-50 para velocidade de CI) — assertar 100% de conformidade
  - [x] Subtask 4.2: Teste de reprodutibilidade — duas execuções com mesma seed/config produzem mesma contagem final e mesmo percentual
  - [x] Subtask 4.3: Teste de modo `exclude` — assertar que pacientes não conformes não aparecem no output exportado, e que a contagem exportada pode ser menor que a solicitada
  - [x] Subtask 4.4: Teste de configuração inválida (`gate_mode=invalido`) — erro claro

- [x] Task 5: Documentar a limitação de percentual fixo em 100%/binário (AC: #8)
  - [x] Subtask 5.1: Adicionar nota em `synthea.properties` e sugerir, no comentário do código, abertura de um ADR futuro caso percentuais fracionários arbitrários se tornem um requisito real (pós-MVP)
  - [x] Subtask 5.2: Rodar `./gradlew check`

## Dev Notes

### Esta é a story de maior risco de sequenciamento do Epic 2

Diferente de 2.1 e 2.2 (parcialmente paralelizáveis com fixtures), esta story **não pode ser validada de ponta a ponta** sem que 2.1 (config) e 2.2 (módulo de gate físico) estejam ambas implementadas e corretas. Implementar 2.3 antes de 2.1/2.2 estarem prontas é a definição de "disaster de sequenciamento" citada nas instruções desta tarefa — o desenvolvedor pode escrever a lógica de roteamento de `gate_mode` isoladamente (Task 2, com testes unitários simples), mas **não deve** declarar a story concluída sem o teste de integração real (Task 4), que é o único que prova SM-1.

### Reaproveitamento do mecanismo nativo — resumo (ver Stories 2.1/2.2 para detalhes completos)

O modo `retry` desta story **é** o mecanismo nativo `Generator.GeneratorOptions.keepPatientsModulePath` já presente no upstream, apenas acionado através da configuração BR (Story 2.1) em vez de diretamente via `-k`. Não há lógica de retry nova a ser escrita nesta story para o modo `retry` — apenas o roteamento de configuração e o modo alternativo `exclude`.

### Modo `exclude` exige extração de lógica compartilhada — atenção a duplicação

Para implementar `gate_mode=exclude` sem duplicar a lógica de avaliação de "o módulo de gate termina em Keep?", extrair a checagem (hoje embutida em `Generator.checkCriteria`) para um método reutilizável, idealmente em `org.mitre.synthea.br.condition` (ex.: `GateEvaluator.matchesCondition(Person, Module gateModule, long finishTime)`), chamado tanto pelo caminho nativo (se decidir reaproveitar) quanto pelo novo caminho de exclusão. Evitar copiar/colar a lógica de `Generator.checkCriteria` — isso violaria a regra de manutenibilidade (NFR3) e criaria dois comportamentos que podem divergir silenciosamente em rebases futuros do core `Generator.java`.

### Risco de escopo — percentual fracionário não é MVP

Ver AC #8. Não implementar geração com percentual arbitrário (`br.target_condition.percentage=80`) "para cumprir a letra" do FR-2 — isso adicionaria complexidade significativa (seleção probabilística por índice de paciente) sem necessidade real no MVP (SM-1 exige 100%). Documentar a lacuna e seguir adiante.

### Conexão com Epic 4 (plausibilidade) — não testar aqui

Esta story garante presença da condição (gate estrutural), não plausibilidade clínica fina (sequência temporal, comorbidades). Isso é escopo do Epic 4 (Stories 4.1/4.2). Não expandir o escopo desta story para incluir checagens de plausibilidade.

### Project Structure Notes

```
src/main/resources/
  synthea.properties                          <- adicionar br.target_condition.gate_mode
src/main/java/org/mitre/synthea/br/condition/
  GateMode.java                                <- novo (enum)
  GateEvaluator.java                           <- novo (lógica compartilhada de avaliação)
src/test/java/org/mitre/synthea/br/condition/
  GateModeIntegrationTest.java                 <- novo
```

### Testing Standards Summary

JUnit 4. Testes de integração (Task 4) podem ser mais lentos — seguir o padrão já usado em `GeneratorTest` para população pequena em testes de CI. `./gradlew check` deve passar antes de finalizar.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-2, #NFR1, #SM-1]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-4]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-2, #Story-2.3]
- [Source: src/main/java/org/mitre/synthea/engine/Generator.java#generatePerson, #checkCriteria]
- [Source: _bmad-output/implementation-artifacts/2-1-configuracao-de-condicao-clinica-alvo.md, 2-2-modulo-gmf-cancer-de-mama-piloto.md]

## Dev Agent Record

### Agent Model Used

claude-opus-4-6

### Debug Log References

Modo `retry` reutiliza mecanismo nativo `keepPatientsModulePath`. Modo `exclude` usa `GateEvaluator` antes da exportação.

### Completion Notes List

- `GateMode` enum com `retry` (default) e `exclude`
- `GateEvaluator.matchesCondition()` extraído para lógica compartilhada
- Modo exclude: skip export + log `Synthea-br gate (exclude): requested=... exported=... excluded=...`
- Percentual fracionário documentado como pós-MVP em `synthea.properties`
- 4 testes de integração em `GateModeIntegrationTest`

### File List

- src/main/java/org/mitre/synthea/br/condition/GateMode.java
- src/main/java/org/mitre/synthea/br/condition/GateEvaluator.java
- src/main/java/org/mitre/synthea/br/condition/TargetConditionIntegration.java
- src/main/java/org/mitre/synthea/engine/Generator.java
- src/main/resources/synthea.properties
- src/test/java/org/mitre/synthea/br/condition/GateModeIntegrationTest.java

## Change Log

- 2026-06-30: Story 2.3 implementada — gate de cohort com modos retry/exclude

### Review Findings

- [x] [Review][Decision] `GateEvaluator` não compartilhado com `checkCriteria` — **Corrigido:** `Generator.checkCriteria` agora chama `GateEvaluator.matchesCondition()` para o caminho nativo retry.
- [x] [Review][Decision] Gate considera apenas condição ativa — **ADR-004 criado (Proposto):** decisão formal pendente do grupo entre ativa/resolvida/configurável.
- [x] [Review][Patch] `GateModeIntegrationTest` sem `@After` — **Corrigido:** tearDown reseta `br.target_condition`, `br.target_condition.gate_mode`, `generate.max_attempts_to_keep_patient`.
- [x] [Review][Patch] `logSummary` com `singlePersonSeed` — **Corrigido:** `effectiveRequestedPopulation` em `Generator.run()` usa 1 quando `singlePersonSeed` está definido.
- [x] [Review][Defer→Patch] Aviso sobre custo do modo retry — **Corrigido:** WARNING adicionado em `synthea.properties` sobre restrições demográficas.
