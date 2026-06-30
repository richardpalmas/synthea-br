# ADR-004: Semântica do gate de cohort — condição ativa vs. condição resolvida

**Status:** Proposto
**Data:** 2026-06-30
**Autores:** Grupo Synthea-br (PUCPR)

---

## Contexto

A Story 2.3 (Epic 2) implementa um gate de cohort que garante que pacientes gerados tenham uma condição clínica alvo (ex.: câncer de mama) antes de serem mantidos (`gate_mode=retry`) ou exportados (`gate_mode=exclude`).

A implementação atual (`GateEvaluator.hasBreastCancer`, e o módulo `keep_modules/br/breast_cancer.json` via `condition_type: "Active Condition"`) verifica exclusivamente se o código SNOMED `254837009` está **ativo** em `person.record.present` no momento da checagem (`finishTime`). Um paciente que teve a condição em algum momento da simulação, mas que foi tratado/curado antes do `finishTime`, **não** é considerado conforme pelo gate — mesmo tendo tido a condição clínica alvo durante a simulação.

Essa escolha de semântica não foi documentada em nenhum ADR ou Dev Note explícito durante a implementação da Story 2.3, e foi levantada como achado de revisão de código (Code Review Epic 1+2, 2026-06-30, camada Blind Hunter) por ter implicação direta na validade acadêmica da cohort gerada: pesquisadores podem esperar "pacientes com histórico de câncer de mama" em vez de, estritamente, "pacientes com câncer de mama ativo no momento do corte temporal da simulação".

A decisão foi explicitamente adiada para um ADR formal em vez de resolvida ad-hoc durante o code review, dado que se trata de uma escolha de modelagem clínica/epidemiológica com impacto em SM-2 (plausibilidade) e na interpretação de qualquer publicação que use essa cohort — não uma questão puramente técnica de implementação.

### Alternativas em consideração

| Alternativa | Descrição | Trade-off |
|-------------|-----------|-----------|
| **A — Apenas ativa (atual)** | Gate exige `Active Condition` no `finishTime` | Simples, já implementado; mas exclui pacientes tratados/curados — pode subrepresentar a cohort alvo em condições com alta taxa de remissão/cura |
| **B — Ativa OU resolvida** | Gate aceita histórico de diagnóstico em qualquer ponto da simulação (`record.present` OU `record.conditions` resolvidas) | Mais alinhado a "pacientes que tiveram a condição"; mas pode incluir pacientes cujo desfecho final não reflete mais a condição, afetando interpretação clínica de outros aspectos do registro (ex.: medicação atual) |
| **C — Configurável** | Adicionar um parâmetro (`br.target_condition.match_resolved`) para o pesquisador escolher por experimento | Maior flexibilidade; maior superfície de configuração e teste para o MVP |

## Decisão

_Pendente — este ADR documenta a questão para decisão formal do grupo antes da próxima iteração da Story 2.3. Até a aceitação deste ADR, o comportamento implementado (Alternativa A — apenas condição ativa) permanece como padrão de fato, mas é considerado **não validado** quanto à intenção clínica/acadêmica original._

## Consequências

### Positivas (de formalizar a decisão via ADR)

- Torna explícita e citável a semântica do gate em publicações que usem a cohort (Methods section).
- Evita divergência silenciosa entre a expectativa do pesquisador e o comportamento real do gate.

### Negativas / trade-offs

- Enquanto não decidido, qualquer cohort gerada com `br.target_condition=breast_cancer` deve ser documentada no `experiment.md` correspondente como usando a Alternativa A (apenas condição ativa), para não invalidar retroativamente experimentos já registrados caso a decisão final seja B ou C.

### Ações de acompanhamento

- [ ] Grupo Synthea-br decidir entre Alternativa A/B/C e atualizar este ADR para status "Aceito".
- [ ] Se a decisão for B ou C, atualizar `GateEvaluator`, `breast_cancer.json` (ou adicionar variante) e os testes de `GateModeIntegrationTest`/`BreastCancerKeepModuleTest` de acordo.
- [ ] Atualizar a Story 2.3 (Dev Notes/Completion Notes) referenciando este ADR.

---

## Referências

- Story 2.3 — Gate de cohort com condição garantida
- Code Review Epic 1+2 (2026-06-30) — achado da camada Blind Hunter sobre semântica `Active Condition`
- `src/main/resources/keep_modules/br/breast_cancer.json`
- `src/main/java/org/mitre/synthea/br/condition/GateEvaluator.java`
