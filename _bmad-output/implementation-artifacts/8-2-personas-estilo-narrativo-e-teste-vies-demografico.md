---
baseline_commit: 65c589c4dfe83b885d38e1c39af012859ea536e2
---

# Story 8.2: Personas de Estilo Narrativo e Teste de Viés Demográfico

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador apresentando cohort enriquecida por IA,
quero variação realista de **estilo de escrita** nas narrativas HTML e um teste automatizado de **viés demográfico**,
para que resumos clínicos sintéticos reflitam heterogeneidade de prontuário real sem amplificar preconceitos de gênero, raça ou região.

## Acceptance Criteria

1. **Given** ADR-007 ativo e Story 8.1 concluída
   **When** `AiNarrativeSummarizer` gera resumo de paciente ou cohort
   **Then** aceita `NarrativeWritingPersona` opcional que modifica o **estilo** sem alterar fatos clínicos do `HealthRecord`
   [Source: Poulett §4.4.2; ADR-007]

2. **Given** catálogo em resources
   **When** persona é selecionada
   **Then** suporta: `concise`, `narrative`, `bullet_points`, `clinical_shorthand`, `abcde` — prompts PT-BR em `src/main/resources/br/ai/prompts/writing_personas/`
   [Source: Poulett §4.4.2]

3. **Given** `br.ai.narrative.persona_mode=deterministic` (default)
   **When** narrativa é gerada
   **Then** persona = `hash(patientId + seed) % N` — mesma seed → mesma persona; estável entre reexecuções
   [Source: NFR1 parcial]

4. **Given** `br.ai.narrative.persona_mode=random`
   **When** narrativa é gerada
   **Then** persona amostrada com `Random` seedado por seed ⊕ patientId — reprodutível e heterogênea por paciente
   [Source: ADR-007]

5. **Given** `br.ai.bias_test.enabled=true` e ≥1 paciente enriquecido
   **When** pós-enriquecimento executa
   **Then** para cada paciente: gera par baseline vs swap de atributo protegido no **prompt** (sexo obrigatório; raça IBGE e UF quando presentes) e compara comprimento + termos-chave
   [Source: Poulett §6.1; Epic 3]

6. **Given** relatório de viés emitido
   **When** arquivo é escrito
   **Then** `output/br/ai/bias_report.json` sem PHI — ids sintéticos, atributo trocado, métricas; limiar default 20% diferença de comprimento
   [Source: NFR5]

7. **Given** `br.ai.bias_test.enabled=false` (default)
   **When** enriquecimento roda
   **Then** zero overhead — sem swap nem relatório
   [Source: opt-in]

8. **Given** suíte de testes
   **When** testes do pacote AI rodam
   **Then** cobrem: atribuição determinística; load de prompts; swap de sexo no prompt (mock); relatório JSON; flag off
   [Source: project-context]

## Tasks / Subtasks

- [x] Task 1: Modelo e catálogo (AC: #1, #2)
  - [x] Subtask 1.1: `NarrativeWritingPersona` enum + loader de `writing_personas/*.txt`
  - [x] Subtask 1.2: 5 prompts PT-BR (concise, narrative, bullet_points, clinical_shorthand, abcde)
  - [x] Subtask 1.3: Overloads `summarizePatient/summarizeCohort` com persona; compor system = base + estilo

- [x] Task 2: Atribuição (AC: #3, #4)
  - [x] Subtask 2.1: `NarrativePersonaAssigner.assign(patientId, seed, mode)`
  - [x] Subtask 2.2: Properties + getters em `AiEnrichmentConfig`
  - [x] Subtask 2.3: Integrar em `AiEnrichmentService` com `person.populationSeed` / `generator.options.seed`

- [x] Task 3: Teste de viés (AC: #5, #6, #7)
  - [x] Subtask 3.1: `DemographicBiasSwapper` — swap GENDER / RACE_IBGE / STATE só no prompt demográfico
  - [x] Subtask 3.2: `BiasReportWriter` → `output/br/ai/bias_report.json`
  - [x] Subtask 3.3: Hook pós-loop em `AiEnrichmentService` se flag on; métricas comprimento + keywords clínicas

- [x] Task 4: HTML (AC: #1)
  - [x] Subtask 4.1: Campo `writingPersona` no log row + `PatientNarrative.aiWritingPersona`
  - [x] Subtask 4.2: Exibir no `patient-accordion.ftl`; atualizar `HtmlExporterAiSectionTest`

- [x] Task 5: Testes e docs (AC: #8)
  - [x] Subtask 5.1: `NarrativePersonaAssignerTest`, `DemographicBiasSwapTest`, estender summarizer tests
  - [x] Subtask 5.2: GUIA §10.1 — personas + bias test
  - [x] Subtask 5.3: `./gradlew test --tests org.mitre.synthea.br.ai.*`

## Dev Notes

### Distinção crítica

| Tipo | Onde | Papel |
|------|------|-------|
| Personas MAI-DxO | `PersonaPrompts` | Debate/correções (8.1) |
| Personas de escrita | `writing_personas/*` | Estilo narrativa HTML (**esta story**) |

### Architecture

| AD | Implicação |
|----|------------|
| AD-2 | Swap só no prompt |
| AD-6 | bias_report sem API key/PHI |
| AD-7 | Classes em `org.mitre.synthea.br.ai.*` |
| ADR-007 | BYOK; `deterministic=false` no conteúdo LLM |

## Dev Agent Record

### Agent Model Used

Cursor Grok 4.5

### Debug Log References

- Review H1: random mode ignored patientId — fixed with seed ⊕ patientId hash
- Review H2: sanitize collapsed newlines — preserve `\n` + CSS `pre-line`
- Review M1: demoBlock only when bias_test enabled
- Review M2: patients without corrections recorded with empty swaps + skippedReason

### Completion Notes List

- Ultimate context engine analysis completed (2026-07-09)
- Implemented writing personas + assigner + bias swap/report + HTML persona label
- Post-review fixes for random heterogeneity, newline preservation, opt-in demo block
- ✅ Resolved review finding [High]: random mode per-patient variation
- ✅ Resolved review finding [High]: bullet/abcde structure in HTML
- ✅ Resolved review finding [Med]: demoBlock only with bias_test
- ✅ Resolved review finding [Med]: document skip without corrections in report

### File List

- src/main/java/org/mitre/synthea/br/ai/NarrativeWritingPersona.java
- src/main/java/org/mitre/synthea/br/ai/NarrativePersonaAssigner.java
- src/main/java/org/mitre/synthea/br/ai/DemographicBiasSwapper.java
- src/main/java/org/mitre/synthea/br/ai/BiasReportWriter.java
- src/main/java/org/mitre/synthea/br/ai/AiNarrativeSummarizer.java
- src/main/java/org/mitre/synthea/br/ai/AiEnrichmentService.java
- src/main/java/org/mitre/synthea/br/ai/AiEnrichmentConfig.java
- src/main/java/org/mitre/synthea/br/ai/PatientEnrichmentResult.java
- src/main/java/org/mitre/synthea/br/ai/CohortEnrichmentLog.java
- src/main/java/org/mitre/synthea/export/HtmlExporter.java
- src/main/resources/br/ai/prompts/writing_personas/concise.txt
- src/main/resources/br/ai/prompts/writing_personas/narrative.txt
- src/main/resources/br/ai/prompts/writing_personas/bullet_points.txt
- src/main/resources/br/ai/prompts/writing_personas/clinical_shorthand.txt
- src/main/resources/br/ai/prompts/writing_personas/abcde.txt
- src/main/resources/templates/html/patient-accordion.ftl
- src/main/resources/templates/html/styles.ftl
- src/main/resources/synthea.properties
- docs/GUIA-DE-USO.md
- src/test/java/org/mitre/synthea/br/ai/NarrativePersonaAssignerTest.java
- src/test/java/org/mitre/synthea/br/ai/DemographicBiasSwapTest.java
- src/test/java/org/mitre/synthea/br/ai/HtmlExporterAiSectionTest.java
- _bmad-output/implementation-artifacts/8-2-personas-estilo-narrativo-e-teste-vies-demografico.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-07-09: Story 8.2 implemented — writing personas, bias test, HTML persona label
- 2026-07-09: Addressed code review findings — 2 High + 2 Med resolved
- 2026-07-09: Status → done

## Senior Developer Review (AI)

**Outcome:** Changes Requested → addressed in-session  
**Review date:** 2026-07-09  
**Reviewer model:** Claude Sonnet (adversarial; separate from implementer)

### Action Items

- [x] [High] Random mode must vary by patientId
- [x] [High] Preserve newlines for bullet_points/abcde in HTML
- [x] [Med] demoBlock only when bias_test enabled
- [x] [Med] Document/record patients without corrections in bias report
- [ ] [Med] Full enrichCohort integration test for bias report (deferred; unit coverage present)
- [ ] [Low] Stronger swap pairing / temperature=0 for paired calls (débito)

### Severity breakdown

- High: 2 (resolvidos)
- Med: 4 (2 resolvidos; 1 deferred; 1 partial via docs)
- Low: 4 (débito)

## Tasks / Subtasks → Review Follow-ups (AI)

- [x] [AI-Review][High] Random per-patient
- [x] [AI-Review][High] Newline preservation
- [x] [AI-Review][Med] Opt-in demographic block
- [x] [AI-Review][Med] Skip reason in bias report
