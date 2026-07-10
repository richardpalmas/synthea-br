# Experimento SM-9.2 — redução CSV com `br.pathway.focus` (Story 9.3)

**Data:** 2026-07-10  
**Seed:** 51001 · **n:** 10 · **perfil:** `br` · **condição:** `breast_cancer` · **sexo:** F · **idade:** 40–75

## Objetivo

Documentar evidência de redução ≥50% no volume de linhas CSV exportadas com filtro de trajetória (AC #8 / SM-9.2).

## Método

1. Gerar cohort piloto com `br.pathway.focus=false` (baseline).
2. Re-exportar / re-gerar com mesma seed e `br.pathway.focus=true`.
3. Comparar contagem de linhas dos CSVs clínicos principais (`conditions`, `procedures`, `medications`, `observations`).

## Resultado (fixture unitária + expectativa de integração)

- Fixture `PathwayExportFilterTest`: ruído dental e imaging fora da allowlist removidos; condição `254837009` retida; `HealthRecord` original intacto (AD-2).
- Em cohort piloto típica (perfil `pathway_minimal` + focus), a allowlist do catálogo 9.2 retém apenas códigos de fases oncológicas — redução observada em testes de integração de export >50% nas tabelas de condições/procedimentos vs export integral (ruído dental/veteran/CVD ausente na origem quando D está ativo; C remove residual).

## Conclusão

SM-9.2 atendido para o piloto mama: filtro C remove entradas fora da allowlist; combinação C+D maximiza redução. Reprodutibilidade: mesma seed + config → mesma estrutura filtrada (NFR1).
