# Experimentos — Synthea-br

Repositório versionado de experimentos reprodutíveis do grupo de pesquisa.

## Convenção de pastas

Cada experimento reside em uma pasta datada:

```
docs/research/experiments/YYYY-MM-DD-<slug>/
  experiment.md      # cópia preenchida do template
  manifest.json      # opcional: copiar de output/ após geração (Story 1.4)
  outputs/           # saídas brutas (não versionar — ver .gitignore)
```

## Como registrar um novo experimento

1. Copie [`experiment-template.md`](./experiment-template.md) para uma nova pasta `YYYY-MM-DD-<slug>/experiment.md`.
2. Execute a geração com seed e config documentados.
3. Copie `output/manifest.json` (gerado automaticamente) para a pasta do experimento.
4. Preencha resultados e conclusão.
5. Abra PR com apenas `experiment.md` e `manifest.json` — **nunca** commite dados de pacientes em `outputs/`.

## Experimentos registrados

| Data | Slug | Descrição |
|------|------|-----------|
| 2026-06-30 | piloto-template-reprodutibilidade | Piloto que valida o mecanismo de documentação (não hipótese clínica BR) |

## Referências

- [Template de experimento](./experiment-template.md)
- [Guia acadêmico](../../CONTRIBUTING-ACADEMICO.md)
- PRD — FR-11
