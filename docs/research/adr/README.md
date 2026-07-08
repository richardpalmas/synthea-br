# Architecture Decision Records (ADRs) — Synthea-br

Índice de decisões arquiteturais e de processo do fork Synthea-br (PUCPR).

## Legenda de status

| Status | Significado |
|--------|-------------|
| **Proposto** | Em discussão; ainda não adotado pelo grupo |
| **Aceito** | Decisão vigente |
| **Rejeitado** | Alternativa descartada (mantido para histórico) |
| **Substituído** | Substituído por outro ADR (indicar qual) |

## ADRs

| Número | Título | Status | Data | Link |
|--------|--------|--------|------|------|
| ADR-001 | Spike IA vs Regras Puras | Aceito | 2026-06-30 | [ADR-001-spike-ia-vs-regras.md](./ADR-001-spike-ia-vs-regras.md) |
| ADR-002 | Política de Rebase Upstream | Aceito | 2026-06-30 | [ADR-002-politica-de-rebase-upstream.md](./ADR-002-politica-de-rebase-upstream.md) |
| ADR-003 | Mapeamento raça/cor IBGE → categorias internas | Proposto | 2026-06-30 | [ADR-003-mapeamento-raca-cor-ibge.md](./ADR-003-mapeamento-raca-cor-ibge.md) |
| ADR-004 | Semântica do gate — condição ativa vs. resolvida | Proposto | 2026-06-30 | [ADR-004-semantica-do-gate-condicao-ativa-vs-resolvida.md](./ADR-004-semantica-do-gate-condicao-ativa-vs-resolvida.md) |
| ADR-005 | Fonte de CID-10 BR para nomenclatura clínica (decisão provisória) | Proposto | 2026-06-30 | [ADR-005-fonte-cid10-br.md](./ADR-005-fonte-cid10-br.md) |
| ADR-006 | Interface Web Local para Geração de Cohort (MVP) | Aceito | 2026-07-01 | [ADR-006-interface-web-localhost-mvp.md](./ADR-006-interface-web-localhost-mvp.md) |
| ADR-007 | Enriquecimento Clínico por IA (MAI-DxO) | Aceito | 2026-07-03 | [ADR-007-ai-enrichment-maidxo.md](./ADR-007-ai-enrichment-maidxo.md) |
| ADR-008 | Trajetória Clínica Focada (Epic 9) | Aceito | 2026-07-08 | [ADR-008-trajetoria-clinica-focada.md](./ADR-008-trajetoria-clinica-focada.md) |

## Como adicionar um novo ADR

1. Copie o template [`adr-template.md`](./adr-template.md).
2. Atribua o próximo número sequencial (`ADR-NNN-<titulo-em-kebab-case>.md`).
3. Preencha as seções **Contexto**, **Decisão** e **Consequências**.
4. Abra um PR com o novo arquivo e atualize a tabela acima.
5. ADRs cobrem decisões **arquiteturais, de processo ou de metodologia de pesquisa** — não apenas detalhes de implementação Java.

## Referências

- [PRD Synthea-br](../../_bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md) — FR-12
- [Architecture Spine](../../_bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md)
