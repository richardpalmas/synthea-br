---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 1.1: Spike de Viabilidade IA vs Regras Puras

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero um ADR que avalie se a plausibilidade alvo (SM-2) é atingível somente com módulos/regras puras,
para que a equipe saiba se IA será necessária em fases futuras, considerando que o laboratório não possui GPU nem API para LLMs.

## Acceptance Criteria

1. **Given** o repositório Synthea-br existe com `docs/research/adr/` configurado
   **When** o pesquisador conclui o spike documental (revisão bibliográfica + análise comparativa)
   **Then** um ADR numerado (`ADR-001-spike-ia-vs-regras.md`) é publicado em `docs/research/adr/` com métricas/estimativas de regras puras, lacunas residuais e recomendação explícita (IA in/out/futuro)
   [Source: prds/prd-synthea-2026-06-29/prd.md#FR-10; epics.md#Story-1.1]

2. **Given** o ADR está sendo redigido
   **When** o conteúdo é revisado
   **Then** o ADR segue estritamente o formato **contexto / decisão / consequências**
   [Source: prds/prd-synthea-2026-06-29/prd.md#FR-12 (formato consistente de ADR aplicado também ao spike)]

3. **Given** a restrição confirmada do laboratório PUCPR (sem GPU, sem API paga)
   **When** o spike é executado
   **Then** o documento demonstra explicitamente que nenhuma etapa do spike depende de GPU, API paga ou treinamento local — é puramente documental/bibliográfico
   [Source: prds/prd-synthea-2026-06-29/addendum.md#Spike-IA-vs-Regras]

4. **Given** o ADR é publicado
   **When** o pesquisador consulta a recomendação final
   **Then** a recomendação declara explicitamente: **MVP = regras puras (Abordagem A)**; Abordagem B (pós-processamento IA) e C (híbrido) ficam marcadas como "spike documental only" / "reavaliar com budget/API futuro"
   [Source: prds/prd-synthea-2026-06-29/addendum.md#Spike-IA-vs-Regras — tabela de abordagens A/B/C]

5. **Given** o catálogo de regras de plausibilidade (Epic 4 / FR-8) ainda não existe neste momento do roadmap
   **When** o ADR apresenta "métricas de regras puras"
   **Then** o documento usa estimativas qualitativas e evidências da revisão bibliográfica (não métricas de execução real), e registra explicitamente que a análise quantitativa final com SM-2 real será revisitada após Epic 4 (catálogo + relatório de validação) estar implementado
   [Source: PRD/addendum — spike é "documental"; Architecture AD-5 define plausibility engine determinístico ainda não construído nesta story]

6. **Given** o ADR cobre a revisão bibliográfica
   **When** a seção de referências é escrita
   **Then** lista ao menos 3-5 trabalhos/fontes sobre uso de IA (ou ausência dela) para geração de dados clínicos sintéticos, com observação de quando IA se justificaria por custo/infraestrutura
   [Source: prds/prd-synthea-2026-06-29/addendum.md#Spike-IA-vs-Regras — item 3 "Revisão bibliográfica"]

## Tasks / Subtasks

- [x] Task 1: Preparar estrutura de pastas de pesquisa (AC: #1)
  - [x] Subtask 1.1: Criar `docs/research/adr/` se não existir
  - [x] Subtask 1.2: Criar `docs/research/adr/README.md` com tabela de ADRs e status (prepara terreno para Story 1.3, mas necessário aqui para o primeiro ADR ser listável)
  - [x] Subtask 1.3: Criar `docs/research/experiments/` (vazio por enquanto — usado pela Story 1.2) apenas se ainda não existir, sem conteúdo desta story

- [x] Task 2: Conduzir revisão bibliográfica (AC: #6)
  - [x] Subtask 2.1: Levantar 3-5 fontes sobre geração de dados clínicos sintéticos com/sem IA (ex.: synthetic EHR generation, GAN-based clinical data, regras/GMF-based approaches)
  - [x] Subtask 2.2: Documentar para cada fonte: abordagem, custo de infraestrutura, resultado de plausibilidade reportado, aplicabilidade ao contexto PUCPR (sem GPU/API)
  - [x] Subtask 2.3: Sintetizar quando IA se justificaria (ex.: dataset multi-condição complexo, indisponibilidade de regras determinísticas, budget disponível)

- [x] Task 3: Redigir análise comparativa de abordagens (AC: #4, #5)
  - [x] Subtask 3.1: Documentar Abordagem A (regras puras) com estimativa qualitativa de viabilidade para SM-2 (0% alta, ≤2% média) baseada na revisão bibliográfica e na arquitetura do engine de plausibilidade (AD-5)
  - [x] Subtask 3.2: Documentar Abordagem B (pós-processamento IA) e C (híbrido) como não aplicáveis ao MVP — apenas registradas para reavaliação futura
  - [x] Subtask 3.3: Identificar lacunas residuais esperadas (ex.: casos extremos de coerência temporal que regras simples podem não cobrir)

- [x] Task 4: Escrever o ADR-001 (AC: #1, #2, #3, #4)
  - [x] Subtask 4.1: Criar `docs/research/adr/ADR-001-spike-ia-vs-regras.md` com seções Contexto / Decisão / Consequências
  - [x] Subtask 4.2: Seção Contexto: restrição de infraestrutura (sem GPU/API), objetivo SM-2, fontes do PRD/addendum
  - [x] Subtask 4.3: Seção Decisão: MVP = regras puras (Abordagem A); IA fica fora do MVP, candidata a fase futura se SM-2 não for atingível após esgotar regras ou se publicação exigir comparativo
  - [x] Subtask 4.4: Seção Consequências: positivas (sem custo de infra, determinismo, reprodutibilidade), negativas/trade-offs (rigor depende de qualidade do catálogo de regras, possível necessidade de revisão pós-Epic 4)
  - [x] Subtask 4.5: Declarar explicitamente ausência de dependência de GPU/API/treinamento local
  - [x] Subtask 4.6: Adicionar nota de revisão futura: ADR será revisitado com métricas reais após Epic 4 (catálogo de regras + relatório de validação) estar implementado e testado contra SM-2

- [x] Task 5: Registrar ADR no índice (AC: #1)
  - [x] Subtask 5.1: Adicionar entrada do ADR-001 em `docs/research/adr/README.md` com status "Aceito"

## Dev Notes

### Natureza desta story — documental, não código de produção

Esta é uma story de **pesquisa/documentação acadêmica**, não de implementação de código Java. Não há mudanças em `src/main/java` ou `src/main/resources` esperadas. Os artefatos de saída são arquivos Markdown em `docs/research/`. Não se aplica `./gradlew check`, Checkstyle ou JaCoCo a esta story — mas a estrutura de pastas criada (`docs/research/adr/`, `docs/research/experiments/`) é a base para as Stories 1.2, 1.3 e 1.4 do mesmo épico, então deve seguir exatamente a convenção definida na arquitetura.

### Dependência crítica a não ignorar (evita disaster de sequenciamento)

O AD-5 da arquitetura (Plausibility Engine Determinístico) e o catálogo de regras (FR-8, Epic 4) **ainda não existem** quando esta story é implementada (Epic 1 é Fase 0, executa antes ou em paralelo a Epic 2-4 conforme PRD §12 e addendum). Portanto:
- **NÃO tente implementar ou simular execução real de cohort/regras nesta story.**
- O ADR deve ser explícito sobre usar estimativas qualitativas/bibliográficas, não métricas de execução.
- Inclua uma nota explícita no ADR de que a decisão será **revisitada** com dados reais após Epic 4. Isso é um requisito do AC #5 e evita que o ADR pareça "definitivo" com dados que não existem ainda.

### Estrutura de pastas (fonte: Architecture Spine + Addendum)

```
docs/
  research/
    adr/
      ADR-001-spike-ia-vs-regras.md   <- criar nesta story
      README.md                        <- criar nesta story (índice de ADRs)
    experiments/                       <- criar pasta vazia (conteúdo vem na Story 1.2)
```
[Source: architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md — "Structural Seed" e bloco `docs/research/` ]
[Source: prds/prd-synthea-2026-06-29/addendum.md#Workflow-Acadêmico — "Estrutura de Pastas Proposta"]

### Formato do ADR (obrigatório)

Todo ADR no projeto segue: **Contexto → Decisão → Consequências**. Use numeração sequencial `ADR-NNN-<titulo-kebab-case>.md`. Este é o primeiro ADR do projeto (`ADR-001`).
[Source: epics.md#Story-1.3 "Registro de Decisões Arquiteturais (ADRs)" — convenção reutilizada aqui pois este é o primeiro ADR criado cronologicamente]

### Conteúdo técnico de referência para a análise (não inventar — usar este resumo)

**Restrição confirmada:** lab PUCPR não possui GPU nem API para LLMs no MVP.

**Tabela de abordagens (já decidida no addendum — o ADR formaliza, não reabre a decisão):**

| Abordagem | MVP | Futuro |
|-----------|-----|--------|
| A — Regras puras | Implementar | Manter como baseline |
| B — Pós-processamento IA | Spike documental only | Reavaliar com budget/API |
| C — Híbrido | Não aplicável | Após A esgotado + infra disponível |

**Definição operacional de SM-2 (para contextualizar a viabilidade):**

| Severidade | Definição | Meta MVP |
|------------|-----------|----------|
| Alta | Contradição clínica grave (ex.: mastectomia sem diagnóstico de câncer) | 0% pacientes |
| Média | Sequência temporal improvável ou código inconsistente | ≤ 2% pacientes |
| Baixa | Detalhe cosmético/terminologia subótima | Documentar; não bloqueia |

[Source: prds/prd-synthea-2026-06-29/addendum.md#Rigor-de-Plausibilidade — Definição-Operacional]

**Entregáveis do spike conforme PRD (esta story entrega o ADR; cohort piloto real é trabalho de Epic 2+4, fora de escopo aqui):**
1. ~~Cohort piloto~~ — fora de escopo desta story (depende de Epic 2 e 4)
2. ~~Métricas SM-2 reais~~ — fora de escopo; usar estimativas bibliográficas
3. Revisão bibliográfica — **nesta story**
4. ADR com recomendação — **nesta story**

[Source: prds/prd-synthea-2026-06-29/addendum.md#Spike-IA-vs-Regras — lista de entregáveis 1-4]

### Project Structure Notes

- Esta é a primeira story do projeto a tocar `docs/research/` — a pasta não existe ainda no repositório (confirmado: nenhum arquivo em `docs/` encontrado na raiz do projeto).
- Não há conflito com a estrutura existente do Synthea upstream (que não usa `docs/research/`) — esta é uma extensão pura, sem necessidade de ADR de "core override" (AD-7 não se aplica pois não há mudança no core Java).
- Manter nomenclatura `ADR-NNN-<titulo>.md` para compatibilidade com a Story 1.3, que formalizará o índice geral de ADRs (esta story já cria o `README.md` inicial do índice para não bloquear, mas a Story 1.3 pode expandir/ajustar o template do índice).

### Testing Standards Summary

Não aplicável no sentido de testes automatizados Java (não há código de produção alterado). Validação desta story é por revisão de conteúdo:
- ADR existe no caminho correto e segue formato Contexto/Decisão/Consequências.
- Recomendação explícita presente e sem ambiguidade (IA in/out/futuro).
- Nenhuma referência a dependência de GPU/API/treinamento local como requisito do MVP.
- Revisão bibliográfica com fontes citáveis (mesmo que informais/links, desde que identificáveis).

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-10 "Spike IA (documental, sem infra no lab)"]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§9 "Success Metrics" SM-5]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Spike-IA-vs-Regras]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Rigor-de-Plausibilidade]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Workflow-Acadêmico]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-1, #AD-5, #AD-6, Structural-Seed]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-1, #Story-1.1]
- [Source: _bmad-output/project-context.md — convenções gerais do repositório (não há regra Java específica aplicável a esta story documental)]

## Dev Agent Record

### Agent Model Used

Claude (Amelia dev-story)

### Completion Notes List

- ADR-001 publicado com revisão bibliográfica (5 fontes), recomendação MVP=regras puras, nota de revisão pós-Epic 4.
- Estrutura `docs/research/adr/` e `docs/research/experiments/` criada.

### File List

- docs/research/adr/README.md
- docs/research/adr/ADR-001-spike-ia-vs-regras.md

### Change Log

- 2026-06-30: Story 1.1 implementada — spike documental IA vs regras puras (ADR-001).
