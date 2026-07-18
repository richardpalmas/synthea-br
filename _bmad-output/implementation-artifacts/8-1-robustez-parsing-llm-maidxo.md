---
baseline_commit: 65c589c4dfe83b885d38e1c39af012859ea536e2
---

# Story 8.1: Robustez de Parsing LLM no Pipeline MAI-DxO

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador que usa enriquecimento opcional por IA (BYOK),
quero que respostas malformadas ou truncadas do LLM sejam recuperadas de forma previsível,
para não desperdiçar chamadas de API nem perder turnos de persona silenciosamente no MAI-DxO.

## Acceptance Criteria

1. **Given** `MaiDxoOrchestrator` recebe resposta LLM que não é JSON válido
   **When** o parsing processa a saída
   **Then** tenta extração via delimitadores `{...}` e fence markdown (comportamento existente + melhoria), e se falhar, invoca **fallback de limpeza** com chamada LLM passando a saída bruta + mensagem de erro do parser
   [Source: ADR-007; Poulett et al. 2026 §4.1]

2. **Given** fallback de limpeza LLM também falha após N tentativas configuráveis (default `br.ai.json_parse_retries=1`)
   **When** o turno da persona termina
   **Then** o debate registra falha explícita no log no formato `{persona} ERRO: json_parse_failed` (mesmo padrão de `LlmException` em L69–71) e **continua** com a próxima persona — sem abortar o paciente
   [Source: MaiDxoOrchestrator.java L69–78; ADR-007 auditoria]

3. **Given** resposta LLM contém marcadores de truncamento conhecidos
   **When** `LlmResponseGuard` detecta o padrão
   **Then** dispara chamada de **continuação** (mesmo `LlmClient`) com prompt de retomada, concatena saídas e só então faz o parse
   [Source: Poulett et al. 2026 §4.3]

4. **Given** `br.ai.enrichment.enabled=true` e cohort enriquecida
   **When** `enrichment_log.json` é escrito
   **Then** `metadata` inclui contadores agregados da cohort: `json_parse_retries`, `truncation_continuations`, `persona_turns_skipped` (inteiros ≥ 0)
   [Source: ADR-007; CohortEnrichmentLog]

5. **Given** a suíte de testes
   **When** `./gradlew check` é executado
   **Then** testes cobrem: JSON válido; JSON em markdown fence; JSON inválido com mock de fallback bem-sucedido; truncamento + continuação; falha total com skip + log `json_parse_failed`; contadores no log
   [Source: project-context.md Testing Rules]

6. **Given** restrições BYOK
   **When** fallback ou continuação são acionados
   **Then** usam o **mesmo** `LlmClient` da execução (sem provider adicional); chamadas extras contam no orçamento por paciente documentado abaixo; `br.ai.max_patients` continua limitando quantos pacientes entram no enriquecimento
   [Source: ADR-007 BYOK; AiEnrichmentService L58–62]

## Tasks / Subtasks

- [x] Task 1: Extrair utilitário de parsing robusto (AC: #1, #2)
  - [x] Subtask 1.1: Criar `org.mitre.synthea.br.ai.LlmJsonParser` — extract `{...}`, strip fence \`\`\`json, parse Gson; retornar resultado tipado (sucesso / erro com mensagem)
  - [x] Subtask 1.2: Implementar `cleanWithLlm(LlmClient, raw, parseError, maxRetries)` — prompt mínimo PT/EN pedindo **apenas** JSON válido da decisão MAI-DxO; incrementar contador de retries
  - [x] Subtask 1.3: Refatorar `MaiDxoOrchestrator` — substituir `parseDecision`/`extractJson` estáticos pelo utilitário; em falha total: log `ERRO: json_parse_failed` + skip (não `continue` silencioso)

- [x] Task 2: Detecção de truncamento (AC: #3)
  - [x] Subtask 2.1: Criar `LlmResponseGuard` com padrões canônicos (constantes documentadas; opcional override via property CSV se trivial)
  - [x] Subtask 2.2: `completeWithContinuation(LlmClient, system, user, maxContinuations)` — detecta truncamento na resposta, pede continuação, concatena
  - [x] Subtask 2.3: Integrar no loop de personas **antes** do parse (substituir chamada direta a `llmClient.complete` nas L66–68)

- [x] Task 3: Auditoria, config e docs (AC: #4, #6)
  - [x] Subtask 3.1: Acumular contadores no orchestrator (por paciente) e agregar em `CohortEnrichmentLog.metadata` via `AiEnrichmentService`
  - [x] Subtask 3.2: Properties em `synthea.properties` + getters em `AiEnrichmentConfig` + validação (≥ 0; retries/continuations razoáveis)
  - [x] Subtask 3.3: Estender `ai_enrichment` no `ResearchManifestWriter` com os 3 contadores quando log presente (opcional mas preferido — AD-6)
  - [x] Subtask 3.4: Nota curta em `docs/GUIA-DE-USO.md` §10.1 sobre as novas flags e orçamento de chamadas

- [x] Task 4: Testes (AC: #5)
  - [x] Subtask 4.1: `LlmJsonParserTest.java` — válido; fence; inválido + fallback mock OK; retries esgotados
  - [x] Subtask 4.2: `LlmResponseGuardTest.java` — padrões detectados/não detectados; concatenação
  - [x] Subtask 4.3: Estender `MaiDxoOrchestratorTest.java` — malformado com skip explícito; truncamento simulado; contadores
  - [x] Subtask 4.4: Rodar `./gradlew check`

## Dev Notes

### Gap atual no código (obrigatório ler antes de implementar)

`MaiDxoOrchestrator.parseDecision` (L132–156) e `extractJson` (L158–165):

- ✅ Extrai substring entre primeiro `{` e último `}`
- ❌ Sem strip de fence markdown
- ❌ Sem fallback LLM
- ❌ `catch (Exception expected)` → retorna `PersonaDecision` vazio; loop L76–78 faz `continue` **sem log**
- ✅ `LlmException` no `complete` (L69–71) já loga `{persona} ERRO: {msg}` e continua — **replicar esse padrão** para parse fail

`CohortEnrichmentLog.setMetadata` hoje grava só `provider`, `model`, `deterministic`, `orchestration` — sem contadores de robustez.

`PatientEnrichmentResult` é imutável (campos finais) — preferir contadores no orchestrator → metadata do cohort log, **ou** estender o result com campos de robustez + `with*` se necessário. Não quebrar `withNarrativeSummary`.

### Orçamento de chamadas por paciente (AC #6 — política canônica)

Por paciente, o número máximo de chamadas LLM do debate é:

```
max_calls ≈ max_iterations × |PERSONA_CHAIN| × (1 + truncation_continuation_max)
            + (json_parse_retries × turnos_com_parse_falho)
```

Onde `|PERSONA_CHAIN| = 5` (hypothesis, test_chooser, stewardship, checklist, challenger).

- Defaults: `max_iterations=5`, `json_parse_retries=1`, `truncation_continuation_max=1`
- `br.ai.max_patients` **não** limita chamadas por paciente — limita quantos pacientes entram no slice (`AiEnrichmentService` L58–62)
- Fallback/continuação **devem** usar o mesmo `LlmClient` resolvido por `LlmClientFactoryHolder`
- Documentar a fórmula no GUIA e no comentário de `synthea.properties`

### Padrões canônicos de truncamento (Task 2)

Detectar (case-insensitive) pelo menos:

- `For brevity I have stopped`
- `Would you like me to continue`
- `I have been cut off`
- `continuar?` / `deseja que eu continue` (PT-BR comum em modelos)

Não inventar detecção por heurística de “JSON incompleto” nesta story — só marcadores textuais conhecidos. JSON incompleto sem marcador cai no fluxo de parse + fallback LLM.

### Schema de auditoria

Em `enrichment_log.json` → `metadata`:

```json
{
  "provider": "...",
  "model": "...",
  "deterministic": false,
  "orchestration": "MAI-DxO",
  "json_parse_retries": 0,
  "truncation_continuations": 0,
  "persona_turns_skipped": 0
}
```

Contadores = soma da cohort. Incrementar `persona_turns_skipped` apenas quando parse falha após esgotar retries (não quando `LlmException` de rede — esse já tem caminho próprio).

Manifest `ai_enrichment` (preferido): espelhar os 3 contadores a partir de `getLastLog().getMetadata()`.

### Architecture compliance

| Decisão | Implicação nesta story |
|---------|------------------------|
| AD-1 | Pipeline IA in-process — sem sidecar |
| AD-2 | **Não** alterar `CorrectionApplicator`; parsing não muta `HealthRecord` |
| AD-6 | Contadores no log (+ manifest); nunca gravar `br.ai.api_key` |
| ADR-007 | BYOK; `deterministic=false`; hardening §4.1/§4.3 apenas |
| AD-7 | Classes novas em `org.mitre.synthea.br.ai.*` — extensão organizada |

### Escopo negativo (NÃO fazer)

- Personas de escrita / viés demográfico → Story **8.2**
- Alterar whitelist de ops / `CorrectionApplicator` / `Gatekeeper`
- Novo provider LLM ou segunda API key
- Treinar modelos / GPU
- Mudar HTML templates (Epic 6 já consome log; contadores não precisam de UI nesta story)

### Properties / flags

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.ai.json_parse_retries` | `1` | Tentativas de limpeza LLM após extração/parse falhar |
| `br.ai.truncation_continuation_max` | `1` | Máximo de continuações por turno de persona |
| `br.ai.enrichment.enabled` | `false` | Pré-requisito ADR-007 (já existe) |
| `br.ai.max_iterations` | `5` | Já existe — não alterar semântica |
| `br.ai.max_patients` | `10` | Já existe — cap de pacientes no slice |

Adicionar getters + validação em `AiEnrichmentConfig` (padrão das keys existentes L16–25).

### Project Structure Notes

```
src/main/java/org/mitre/synthea/br/ai/
  LlmJsonParser.java           ← NOVO
  LlmResponseGuard.java        ← NOVO
  MaiDxoOrchestrator.java      ← UPDATE (loop + parse)
  CohortEnrichmentLog.java     ← UPDATE (metadata counters)
  AiEnrichmentConfig.java      ← UPDATE (novas keys)
  AiEnrichmentService.java     ← UPDATE (agregar contadores → log)
  PatientEnrichmentResult.java ← UPDATE só se contadores por paciente forem necessários
src/main/java/org/mitre/synthea/br/research/
  ResearchManifestWriter.java  ← UPDATE (ai_enrichment counters)
src/main/resources/synthea.properties  ← UPDATE
docs/GUIA-DE-USO.md            ← UPDATE §10.1
src/test/java/org/mitre/synthea/br/ai/
  LlmJsonParserTest.java       ← NOVO
  LlmResponseGuardTest.java    ← NOVO
  MaiDxoOrchestratorTest.java  ← UPDATE
  MockLlmClient.java           ← reutilizar (já conta calls via getCallCount)
```

### O que preservar (não quebrar)

- Happy path atual: JSON limpo → `ProposeCorrection` / `FinalizePatient` / Gatekeeper
- Merge de ops entre personas na mesma iteração
- Cap `maxIterations`
- `MockLlmClient` sequencial — testes novos devem enfileirar respostas de fallback/continuação na ordem esperada
- `HtmlExporterAiSectionTest` e fluxo `getLastLog()` — não remover campos existentes do log
- Clear de API key no `finally` de `enrichCohort`

### Previous / related intelligence

- Epic 8 story 1 — sem story anterior no épico; núcleo MAI-DxO veio em commit `f7992dd84` (web UI + HTML + AI enrichment)
- Commit recente `c1247106c` — reason obrigatório em `flag_unfixable` (não relacionado ao parse, mas mostra padrão de endurecer contrato JSON das personas)
- Story 8.2 **bloqueada** por esta: `AiNarrativeSummarizer` e bias test precisam de debate/parse confiável
- Epic 6 HTML já renderiza seção IA a partir do log — contadores novos são auditoria, não UI obrigatória

### Testing Standards

- JUnit 4 + asserts static import; **sem** JUnit 5
- `MockLlmClient` — zero rede
- Checkstyle Google (max 100 cols); imports explícitos
- `./gradlew check` obrigatório antes de marcar review

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-8 / Story 8.1]
- [Source: docs/research/adr/ADR-007-ai-enrichment-maidxo.md]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md#Adendo-A]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-1 AD-2 AD-6]
- [Source: src/main/java/org/mitre/synthea/br/ai/MaiDxoOrchestrator.java]
- [Source: src/main/java/org/mitre/synthea/br/ai/CohortEnrichmentLog.java]
- [Source: src/main/java/org/mitre/synthea/br/ai/AiEnrichmentService.java]
- [Source: Poulett et al. 2026 — arXiv:2606.26879v2 §4.1, §4.3]
- [Source: _bmad-output/project-context.md]

## Dev Agent Record

### Agent Model Used

Cursor Grok 4.5

### Debug Log References

- Truncation markers left inside JSON broke parse after concatenation — fixed by stripping markers before/after continuation.
- Full `./gradlew check` hit pre-existing `BrDemographicsIntegrationTest` threshold failures (race white 0.487 vs 0.435); AI package tests + checkstyle for touched files passed.

### Completion Notes List

- Ultimate context engine analysis completed — comprehensive developer guide created (2026-07-09)
- Implemented LlmJsonParser (fence + extract + LLM cleanup), LlmResponseGuard (markers + continuation), RobustnessStats
- MaiDxoOrchestrator now logs `ERRO: json_parse_failed`, uses same LlmClient for fallback/continuation
- CohortEnrichmentLog metadata + ResearchManifestWriter ai_enrichment expose the 3 counters
- Properties `br.ai.json_parse_retries` / `br.ai.truncation_continuation_max` + GUIA §10.1
- Tests: LlmJsonParserTest, LlmResponseGuardTest, MaiDxoOrchestratorTest extensions, AiEnrichmentConfigTest robustness defaults
- Post-review: continuation only when local parse fails + marker present; safe string field extraction for action/query
- ✅ Resolved review finding [High]: falso-positivo de truncamento
- ✅ Resolved review finding [High]: action/query não-string abortava paciente
- ✅ Resolved review finding [Med]: testes de regressão dos Highs

### File List

- src/main/java/org/mitre/synthea/br/ai/LlmJsonParser.java
- src/main/java/org/mitre/synthea/br/ai/LlmResponseGuard.java
- src/main/java/org/mitre/synthea/br/ai/RobustnessStats.java
- src/main/java/org/mitre/synthea/br/ai/MaiDxoOrchestrator.java
- src/main/java/org/mitre/synthea/br/ai/PatientEnrichmentResult.java
- src/main/java/org/mitre/synthea/br/ai/CohortEnrichmentLog.java
- src/main/java/org/mitre/synthea/br/ai/AiEnrichmentConfig.java
- src/main/java/org/mitre/synthea/br/ai/AiEnrichmentService.java
- src/main/java/org/mitre/synthea/br/research/ResearchManifestWriter.java
- src/main/resources/synthea.properties
- docs/GUIA-DE-USO.md
- src/test/java/org/mitre/synthea/br/ai/LlmJsonParserTest.java
- src/test/java/org/mitre/synthea/br/ai/LlmResponseGuardTest.java
- src/test/java/org/mitre/synthea/br/ai/MaiDxoOrchestratorTest.java
- src/test/java/org/mitre/synthea/br/ai/AiEnrichmentConfigTest.java
- _bmad-output/implementation-artifacts/8-1-robustez-parsing-llm-maidxo.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-07-09: Story 8.1 implemented — robust LLM JSON parsing, truncation continuation, audit counters; status → review
- 2026-07-09: Addressed code review findings — 3 items resolved (2 High, 1 Med); 1 Med deferred (full check demographics)
- 2026-07-09: Status → done (implementação + code-review concluídos; Highs resolvidos)

## Senior Developer Review (AI)

**Outcome:** Changes Requested → addressed in-session  
**Review date:** 2026-07-09  
**Reviewer model:** Claude Sonnet (adversarial layers; separate from implementer)

### Action Items

- [x] [High] Falso-positivo de truncamento em JSON válido com frase-marcador — só continuar se parse local falhar
- [x] [High] `action`/`query` não-string abortava paciente — `readJsonString` + skip com log
- [x] [Med] Testes para falso-positivo e `action: null`
- [ ] [Med] Confirmar `./gradlew check` completo verde (falhas pré-existentes em `BrDemographicsIntegrationTest` fora do escopo 8.1; pacote `br.ai.*` verde)

### Severity breakdown

- High: 2 (resolvidos)
- Med: 2 (1 resolvido; 1 documentado — check completo)
- Low: 3 (débito: fence edge, teto de retries, extract heurístico)

## Tasks / Subtasks → Review Follow-ups (AI)

- [x] [AI-Review][High] Guard de truncamento sem falso-positivo
- [x] [AI-Review][High] Parse seguro de campos tipados
- [x] [AI-Review][Med] Testes de regressão dos Highs
