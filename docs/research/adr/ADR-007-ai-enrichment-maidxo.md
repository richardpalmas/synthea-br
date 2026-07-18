# ADR-007: Enriquecimento Clínico por IA (Orquestração MAI-DxO)

**Status:** Aceito  
**Data:** 2026-07-03  
**Autores:** Grupo Synthea-br (PUCPR)

---

## Contexto

O [ADR-001](ADR-001-spike-ia-vs-regras.md) decidiu que o MVP usaria **regras puras** (Abordagem A) para plausibilidade, mantendo IA apenas como spike documental. Pesquisadores do grupo solicitaram enriquecimento opcional por LLM para melhorar **coerência, credibilidade e contexto regional** dos dados sintéticos gerados.

O modelo **MAI-DxO** (Microsoft) inspira um painel de personas clínicas que debatem um caso em etapas, com um **Gatekeeper** que libera informação somente sob solicitação explícita — simulando interação clínica real.

Restrições do projeto:

- **AD-2** proíbe mutação de `HealthRecord` na camada de exportação
- **AD-1** exigia IA apenas documental até ADR futuro
- **NFR1** exige reprodutibilidade documentada (seed governa geração)
- Contexto acadêmico: **BYOK** (Bring Your Own Key) — sem API keys institucionais no MVP

## Decisão

Adotar um estágio opcional **`AiEnrichment`** pós-geração e pré-export:

| Aspecto | Decisão |
|---------|---------|
| Ativação | `br.ai.enrichment.enabled=true` (CLI ou Web UI) |
| Pré-requisito | `br.profile=br` |
| Providers MVP | OpenAI e Gemini (BYOK) |
| Orquestração | MAI-DxO adaptado: 5 personas + Gatekeeper |
| Mutação | **Autorizada** somente via `CorrectionApplicator` neste estágio (exceção documentada a AD-2) |
| Export | `deferExports=true` durante geração; enriquecimento antes de `runPostCompletionExports` |
| Auditoria | `output/br/ai/enrichment_log.json` + seção `ai_enrichment` no `manifest.json` |
| API keys | BYOK; nunca persistidas em disco, log ou manifest |
| Determinismo | Geração permanece determinística por seed; camada IA declarada `deterministic=false` |

### Personas (adaptação Synthea-br)

| Persona | Papel |
|---------|-------|
| Dr. Hypothesis | Hipóteses de incoerência clínica |
| Dr. Test-Chooser | Lacunas de exames / inconsistências laboratoriais |
| Dr. Stewardship | Adequação SUS/custo-benefício BR |
| Dr. Checklist | Completude, formato, coerência regional/IBGE |
| Dr. Challenger | Contesta correções prematuras |
| Gatekeeper | Detém prontuário completo; responde apenas ao solicitado |

### Operações de correção (v1)

`add_observation`, `fix_encounter_date`, `set_person_attribute`, `add_procedure`, `flag_unfixable`

## Consequências

### Positivas

- Enriquecimento opcional sem quebrar pipeline determinístico quando desabilitado
- Trilha de auditoria para uso acadêmico
- Complementa Epic 4 (regras) sem substituí-lo
- BYOK evita custo institucional no MVP

### Negativas / trade-offs

- Camada IA é **não-reprodutível** mesmo com seed fixo
- Latência e custo por paciente (cap `br.ai.max_patients` default 10)
- Risco de alucinação clínica — mitigado por Challenger + Checklist + whitelist de ops
- Exceção a AD-2 exige disciplina: somente `CorrectionApplicator` pode mutar pós-geração

### Revisão futura

- Expandir operações de correção após validação com cohort piloto `breast_cancer`
- Avaliar providers adicionais e modo sidecar (sem mutação) para publicações que exijam reprodutibilidade total
- **Story 8.1** — fallback LLM de parsing JSON + detecção de truncamento (Poulett et al. 2026 §4.1, §4.3)
- **Story 8.2** — personas de estilo narrativo + teste de viés demográfico (Poulett et al. 2026 §4.4.2, §6.1)

## Referências

- ADR-001 — Spike IA vs Regras
- ARCHITECTURE-SPINE AD-1, AD-2, AD-6, AD-7
- Epic 8 — Orquestração MAI-DxO (`_bmad-output/planning-artifacts/`)
