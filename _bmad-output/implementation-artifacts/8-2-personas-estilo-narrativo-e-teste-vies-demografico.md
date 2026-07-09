# Story 8.2: Personas de Estilo Narrativo e Teste de Viés Demográfico

Status: backlog

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador apresentando cohort enriquecida por IA,
quero variação realista de **estilo de escrita** nas narrativas HTML e um teste automatizado de **viés demográfico**,
para que resumos clínicos sintéticos reflitam heterogeneidade de prontuário real sem amplificar preconceitos de gênero, raça ou região.

## Acceptance Criteria

1. **Given** ADR-007 ativo (`br.ai.enrichment.enabled=true`) e Story 8.1 concluída (parsing robusto)
   **When** `AiNarrativeSummarizer` gera resumo de paciente ou cohort
   **Then** aceita parâmetro opcional `NarrativeWritingPersona` (enum ou config) que modifica o **estilo** da narrativa sem alterar fatos clínicos do `HealthRecord`
   [Source: Poulett et al. 2026 §4.4.2 — Clinical Personas; ADR-007]

2. **Given** catálogo de personas de escrita definido em resources
   **When** persona é selecionada
   **Then** suporta no mínimo: `concise`, `narrative`, `bullet_points`, `clinical_shorthand`, `abcde` — textos de system prompt em PT-BR em `src/main/resources/br/ai/prompts/writing_personas/`
   [Source: Poulett et al. 2026 §4.4.2 — adaptação BR]

3. **Given** export HTML com seção de enriquecimento IA (Epic 6 / integração existente)
   **When** `br.ai.narrative.persona_mode=deterministic` (default)
   **Then** cada paciente recebe persona derivada **deterministicamente** de `hash(patientId + seed)` — mesma seed → mesma persona por paciente; persona estável entre reexecuções de export
   [Source: NFR1 parcial — seed governa atribuição; camada IA permanece `deterministic=false` no conteúdo LLM]

4. **Given** `br.ai.narrative.persona_mode=random`
   **When** narrativa é gerada
   **Then** persona amostrada por paciente com seed do `Generator` — reprodutível por seed global, documentado como não-determinístico no conteúdo textual
   [Source: ADR-007 trade-off determinismo]

5. **Given** cohort enriquecida com ≥2 pacientes de perfis demográficos distintos
   **When** teste de viés (`DemographicBiasSwapTest` ou notebook documentado) executa
   **Then** para cada paciente piloto, gera par de narrativas: baseline vs. **swap** de atributo protegido (sexo; extensão BR: raça/cor IBGE e UF quando presentes)
   **And** compara métricas configuráveis: comprimento, termos clínicos-chave, tom (heurística ou LLM-as-judge opcional BYOK)
   [Source: Poulett et al. 2026 §6.1 — Rickman gender swap; Epic 3 demografia BR]

6. **Given** swap detecta divergência acima de limiar documentado (ex.: >20% diferença de comprimento ou termo estigmatizante)
   **When** relatório de viés é emitido
   **Then** salva em `output/br/ai/bias_report.json` (ou path documentado) **sem PHI** — apenas ids sintéticos, atributo trocado, métricas agregadas
   [Source: NFR5; ADR-007 auditoria]

7. **Given** `br.ai.bias_test.enabled=false` (default)
   **When** geração/enriquecimento roda
   **Then** nenhum swap nem relatório de viés é produzido — zero overhead
   [Source: BYOK / opt-in]

8. **Given** a suíte de testes
   **When** `./gradlew check` é executado
   **Then** testes unitários cobrem: atribuição determinística de persona; carregamento de prompts; swap de sexo em atributos de prompt (mock LLM); relatório JSON schema; flag off não gera relatório
   [Source: project-context.md#Testing-Rules]

## Tasks / Subtasks

- [ ] Task 1: Modelo e catálogo de personas de escrita (AC: #1, #2)
  - [ ] Subtask 1.1: Criar `NarrativeWritingPersona` enum + loader de prompts FTL/txt
  - [ ] Subtask 1.2: Traduzir/adaptar personas NHS para PT-BR clínico (concise, narrative, bullet_points, clinical_shorthand, abcde)
  - [ ] Subtask 1.3: Estender `AiNarrativeSummarizer.summarizePatient/summarizeCohort` com parâmetro persona

- [ ] Task 2: Atribuição determinística por paciente (AC: #3, #4)
  - [ ] Subtask 2.1: Criar `NarrativePersonaAssigner` — hash(patientId + seed) ou random com seed
  - [ ] Subtask 2.2: Properties `br.ai.narrative.persona_mode`, documentar em `synthea.properties`
  - [ ] Subtask 2.3: Integrar em `AiEnrichmentService` antes de chamar summarizer

- [ ] Task 3: Teste de viés demográfico (AC: #5, #6, #7)
  - [ ] Subtask 3.1: Criar `DemographicBiasSwapper` — troca sexo/raça/UF apenas no **prompt** de narrativa (não muta `HealthRecord`)
  - [ ] Subtask 3.2: Criar `BiasReportWriter` → JSON agregado
  - [ ] Subtask 3.3: Flag `br.ai.bias_test.enabled`; hook opcional pós-enriquecimento

- [ ] Task 4: Integração HTML (AC: #1)
  - [ ] Subtask 4.1: Expor `writing_persona` no modelo FreeMarker quando seção IA presente
  - [ ] Subtask 4.2: Atualizar testes `HtmlExporterAiSectionTest` se necessário

- [ ] Task 5: Testes e documentação (AC: #8)
  - [ ] Subtask 5.1: `NarrativePersonaAssignerTest`, `DemographicBiasSwapTest`
  - [ ] Subtask 5.2: Nota em `docs/GUIA-DE-USO.md` — personas narrativas e teste de viés (opt-in)
  - [ ] Subtask 5.3: Rodar `./gradlew check`

## Dev Notes

### Distinção crítica — personas MAI-DxO vs personas de escrita

| Tipo | Onde | Papel atual |
|------|------|-------------|
| **Personas MAI-DxO** | `PersonaPrompts` — hypothesis, test_chooser, etc. | Debate clínico / correções |
| **Personas de escrita** (esta story) | `writing_personas/*` | Estilo da **narrativa HTML** pós-enriquecimento |

Não confundir nem reutilizar prompts — objetivos diferentes (correção vs. apresentação).

### Insight NHS England (§4.4.2)

Staff members receive a random persona; notes by the same author stay stylistically consistent across the journey. Adaptação BR:

- Persona atribuída por **paciente** (autor sintético único por prontuário no MVP)
- Conteúdo factual sempre ancorado em `PatientEnrichmentResult` + `HealthRecord` — persona altera forma, não fatos

### Teste de viés (§6.1 + Rickman 2025)

Metodologia do paper: swap gender in clinical notes → compare LLM judge scores. Extensão BR justificada pelo Epic 3 (raça/cor IBGE, UF, município):

- MVP: swap de **sexo** obrigatório nos testes
- Extensão: swap de **raça/cor** e **UF** quando atributos presentes
- Swap ocorre **somente no prompt** de narrativa — AD-2 preservado no record clínico

### Dependências

- **Depende de:** ADR-007; Story **8.1** (parsing robusto); Epic 6 (`HtmlExporter` + seção IA)
- **Complementa:** Epic 9 Story 9.4 (modo orientador) — personas afetam texto IA, não agrupamento por fase GMF
- **Não depende de:** Epic 4 (plausibilidade)

### Properties / flags (preview)

| Property | Default | Descrição |
|----------|---------|-----------|
| `br.ai.narrative.persona_mode` | `deterministic` | `deterministic` ou `random` |
| `br.ai.bias_test.enabled` | `false` | Ativa swap + relatório de viés |
| `br.ai.enrichment.enabled` | `false` | Pré-requisito |

### Project Structure Notes

```
src/main/resources/br/ai/prompts/writing_personas/
  concise.txt
  narrative.txt
  bullet_points.txt
  clinical_shorthand.txt
  abcde.txt
src/main/java/org/mitre/synthea/br/ai/
  NarrativeWritingPersona.java
  NarrativePersonaAssigner.java
  DemographicBiasSwapper.java
  BiasReportWriter.java
  AiNarrativeSummarizer.java          <- estender
src/test/java/org/mitre/synthea/br/ai/
  NarrativePersonaAssignerTest.java
  DemographicBiasSwapTest.java
```

### Testing Standards Summary

JUnit 4 + `MockLlmClient`. Teste de viés com LLM real é **manual/opt-in** (BYOK) — CI usa mocks. `./gradlew check` obrigatório.

### References

- [Source: docs/research/adr/ADR-007-ai-enrichment-maidxo.md]
- [Source: docs/research/adr/ADR-008-trajetoria-clinica-focada.md#Adendo-A]
- [Source: Poulett et al. 2026 — arXiv:2606.26879v2 §4.4.2, §6.1]
- [Source: _bmad-output/implementation-artifacts/8-1-robustez-parsing-llm-maidxo.md]
- [Source: _bmad-output/implementation-artifacts/6-1-cohort-narrative-viewer-export-html-mvp.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
