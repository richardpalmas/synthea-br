# Story 8.1: Robustez de Parsing LLM no Pipeline MAI-DxO

Status: backlog

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador que usa enriquecimento opcional por IA (BYOK),
quero que respostas malformadas ou truncadas do LLM sejam recuperadas de forma previsível,
para não desperdiçar chamadas de API nem perder turnos de persona silenciosamente no MAI-DxO.

## Acceptance Criteria

1. **Given** `MaiDxoOrchestrator` recebe resposta LLM que não é JSON válido
   **When** `parseDecision` processa a saída
   **Then** tenta extração via regex/delimitadores `{...}` (comportamento existente) e, se falhar, invoca **fallback de limpeza** com segunda chamada LLM passando a saída bruta + mensagem de erro do parser
   [Source: docs/research/adr/ADR-007-ai-enrichment-maidxo.md; Poulett et al. 2026 §4.1 — padrão NHS England]

2. **Given** fallback de limpeza LLM também falha após N tentativas configuráveis (default 1)
   **When** o turno da persona termina
   **Then** o debate registra falha explícita no log (`persona ERRO: json_parse_failed`) e **continua** com a próxima persona — sem abortar o paciente inteiro
   [Source: MaiDxoOrchestrator.java — padrão atual de `LlmException`; ADR-007 auditoria]

3. **Given** resposta LLM contém marcadores de truncamento conhecidos (ex.: "For brevity I have stopped", "Would you like me to continue")
   **When** `LlmResponseGuard` (ou equivalente) detecta o padrão
   **Then** dispara chamada de **continuação** com prompt de retomada e concatena saídas antes do parse
   [Source: Poulett et al. 2026 §4.3 — jornadas longas truncadas pelo modelo]

4. **Given** `br.ai.enrichment.enabled=true` e cohort gerada
   **When** enriquecimento completa
   **Then** `enrichment_log.json` inclui contadores agregados: `json_parse_retries`, `truncation_continuations`, `persona_turns_skipped`
   [Source: ADR-007 — trilha de auditoria; ResearchManifestWriter extensível]

5. **Given** a suíte de testes
   **When** `./gradlew check` é executado
   **Then** testes unitários cobrem: JSON válido; JSON em markdown fence; JSON inválido com mock de fallback bem-sucedido; truncamento detectado e continuação simulada; falha total com skip de turno
   [Source: project-context.md#Testing-Rules]

6. **Given** restrições BYOK do laboratório PUCPR
   **When** fallback LLM é acionado
   **Then** usa o **mesmo** `LlmClient` da execução (sem provider adicional) e respeita `br.ai.max_patients` — nenhuma chamada extra além do cap documentado por paciente
   [Source: ADR-007 BYOK; NFR5]

## Tasks / Subtasks

- [ ] Task 1: Extrair utilitário de parsing robusto (AC: #1, #2)
  - [ ] Subtask 1.1: Criar `org.mitre.synthea.br.ai.LlmJsonParser` — extract `{...}`, regex fence, delegação ao Gson
  - [ ] Subtask 1.2: Implementar `cleanWithLlm(LlmClient, raw, parseError)` — prompt mínimo de correção JSON
  - [ ] Subtask 1.3: Refatorar `MaiDxoOrchestrator.parseDecision` para usar o utilitário

- [ ] Task 2: Detecção de truncamento (AC: #3)
  - [ ] Subtask 2.1: Criar `LlmResponseGuard` com lista configurável de padrões (properties ou constantes documentadas)
  - [ ] Subtask 2.2: Método `completeWithContinuation(LlmClient, system, user, maxContinuations=1)`
  - [ ] Subtask 2.3: Integrar no loop de personas do orchestrator

- [ ] Task 3: Auditoria e config (AC: #4, #6)
  - [ ] Subtask 3.1: Estender `PatientEnrichmentResult` / log cohort com contadores de robustez
  - [ ] Subtask 3.2: Documentar properties em `synthea.properties` (`br.ai.json_parse_retries`, etc.)
  - [ ] Subtask 3.3: Atualizar seção `ai_enrichment` do manifest se aplicável

- [ ] Task 4: Testes (AC: #5)
  - [ ] Subtask 4.1: `LlmJsonParserTest.java` — casos felizes e edge
  - [ ] Subtask 4.2: `MaiDxoOrchestratorTest.java` — mock client com respostas malformadas/truncadas
  - [ ] Subtask 4.3: Rodar `./gradlew check`

## Dev Notes

### Origem — insight do paper NHS England (arXiv 2606.26879v2)

Poulett et al. (NHS England, jun/2026) documentam dois padrões operacionais que o fork **ainda não implementa** de forma completa:

**1. Fallback de parsing JSON em duas etapas** (§4.1):

> Outputs are expected to be JSONs. When invalid, the pipeline first attempts regex extraction; if that fails, an LLM is prompted to clean the output given the raw text + Python JSON decode error.

Estado atual no fork (`MaiDxoOrchestrator.parseDecision`):

- ✅ Extração `{...}` via `indexOf`/`lastIndexOf`
- ❌ Sem fallback LLM de limpeza
- ❌ Falha silenciosa (`catch Exception` → turno ignorado sem retry)

**2. Detecção de truncamento em gerações longas** (§4.3):

> For longer journeys, the LLM often cuts-off before completing. Another LLM prompt detects early termination and asks to continue.

Relevante para respostas de personas com `operations` extensas ou debate multi-turno.

### Escopo — hardening apenas; não expandir personas

Esta story **não** adiciona personas narrativas (Story 8.2) nem altera `CorrectionApplicator`. Foco exclusivo em confiabilidade do contrato JSON MAI-DxO.

### Dependências

- **Depende de:** ADR-007 aceito; `MaiDxoOrchestrator`, `LlmClient`, `AiEnrichmentService` existentes
- **Bloqueia:** Story 8.2 (personas narrativas dependem de parsing confiável)
- **Não depende de:** Epic 9

### Properties / flags (preview)

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.ai.json_parse_retries` | `1` | Tentativas de limpeza LLM após regex falhar |
| `br.ai.truncation_continuation_max` | `1` | Máximo de continuações por turno |
| `br.ai.enrichment.enabled` | `false` | Pré-requisito ADR-007 |

### Project Structure Notes

```
src/main/java/org/mitre/synthea/br/ai/
  LlmJsonParser.java              <- novo utilitário
  LlmResponseGuard.java           <- truncamento + continuação
  MaiDxoOrchestrator.java         <- refatorar parseDecision
src/test/java/org/mitre/synthea/br/ai/
  LlmJsonParserTest.java
  MaiDxoOrchestratorTest.java     <- estender
```

### Testing Standards Summary

JUnit 4 + `MockLlmClient` existente. Sem chamadas de rede reais. `./gradlew check` obrigatório.

### References

- [Source: docs/research/adr/ADR-007-ai-enrichment-maidxo.md]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md#Adendo-A]
- [Source: Poulett et al. 2026 — arXiv:2606.26879v2 §4.1, §4.3]
- [Source: src/main/java/org/mitre/synthea/br/ai/MaiDxoOrchestrator.java]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
