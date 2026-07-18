# Data packs de trajetoria clinica (`br/pathways/`)

Catalogos versionados de fases da trajetoria clinica por condicao-alvo, usados pelo
`org.mitre.synthea.br.pathway.PathwayCatalog` (Story 9.2) como fonte unica de verdade
(AD-3) para filtros de export (9.3), narrativa HTML por fase (9.4) e modulo GMF
episodico (9.7). Ver `docs/research/adr/ADR-008-trajetoria-clinica-focada.md`.

## Schema — `{condition}_phases.json`

- `catalog_version` (string): versao semantica do data pack.
- `condition` (string): chave da condicao-alvo (`br.target_condition`), ex. `breast_cancer`.
- `source_module` / `source_reference`: rastreabilidade de onde os codigos foram extraidos.
- `phases` (array): cada fase possui:
  - `phase_id` (string, estavel): identificador usado por Stories 9.3/9.4/9.7/9.8.
  - `order` (int): ordem canonica de exibicao/transicao (1-indexed).
  - `title_pt_br` / `description_pt_br`: rotulos em portugues do Brasil.
  - `encounter_types` (array opcional): tipos de encontro tipicamente associados a fase.
  - `code_allowlist` (array): entradas `{ "system", "code", "display" }` — subconjunto
    SNOMED/LOINC/RxNorm/CPT extraido do modulo GMF piloto (`modules/breast_cancer.json`
    e submodulos). E um subconjunto curado para o MVP do Epic 9, extensivel em
    iteracoes futuras sem quebrar o schema.
- `always_include` (objeto): `attributes` sempre relevantes independente da fase
  (demografia, metadados de cohort BR) — nao sao codigos clinicos, mas atributos de
  `Person`/contexto de execucao.

## Arquivos

- `breast_cancer_phases.json` — catalogo piloto (condicao `breast_cancer`, SNOMED
  `254837009`).
- `breast_cancer_timing_priors.json` — priors temporais entre fases (Story 9.8). Schema
  ADR-008: `priors_version`, `condition`, `unit`, `reference_notes`, `transitions` com
  chaves `{phase_id}->{phase_id}` e distribuicoes `min`/`max`/`median` ou `buckets`.
  Consumido por `PathwayTimingLoader` quando `br.pathway.timing_priors=default` e
  `br.generation.trajectory_mode=episodic`. Apenas agregados/citacoes — zero PHI (NFR5).
