# Perfis de módulos de geração (`br/generation/module_profiles/`)

Data packs versionados para a Abordagem D (Story 9.5). Consumidos por
`org.mitre.synthea.br.pathway.generation.ModuleProfileConfig` (AD-3).

## Schema — `{profile_id}.json`

- `profile_version` — versão semântica do perfil (manifest/rastreabilidade).
- `profile_id` — valor de `br.generation.module_profile` (ex.: `pathway_minimal`).
- `allowed_module_paths` / `allowed_core_module_paths` — allowlist de paths GMF (top-level).
- `denied_module_paths` / `denied_core_module_paths` — denylist adicional.
- `always_include_for_target_condition` — módulos de doença/gate que nunca podem ser suprimidos
  quando `br.target_condition` está configurado.

## Arquivos

- `pathway_minimal.json` — cohort enxuta piloto (câncer de mama + infraestrutura mínima).
