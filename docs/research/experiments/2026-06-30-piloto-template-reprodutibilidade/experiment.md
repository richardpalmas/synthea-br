# Piloto: Validação do Template de Experimento Reprodutível

## Hipótese

O template de experimento e o fluxo de documentação permitem que qualquer membro do grupo reproduza uma execução Synthea upstream com seed fixa, sem depender de features Synthea-br ainda não implementadas (Epics 2–5).

> **Nota:** Este piloto valida o **mecanismo de documentação**, não uma hipótese clínica ou demográfica brasileira. Cohorts BR reais serão documentadas em experimentos futuros após Epic 2–5.

## Configuração

### Seed

`seed = 42`

### Propriedades relevantes

Configuração padrão de `synthea.properties` (sem overrides BR). Exportação FHIR R4 habilitada (`exporter.fhir.export = true`).

```
exporter.fhir.export = true
exporter.metadata.export = true
generate.default_population = 1
```

### Perfil e condição alvo

- Perfil: **default** (upstream EUA — `br.profile` não existe ainda)
- Condição alvo: **nenhuma** (`br.target_condition` não existe ainda)

## Comando executado

```bash
./run_synthea.bat -s 42 -p 2 Massachusetts
```

## Data de execução

`generated_at = 2026-06-30T12:00:00Z`

## Manifest de rastreabilidade

Exemplo manual do schema esperado (pré-automação Story 1.4). Após implementação do manifest automático, copiar `output/manifest.json` para esta pasta.

```json
{
  "seed": 42,
  "config_hash": "<gerado-automaticamente-pela-Story-1.4>",
  "commit_sha": "0e32c32bec2a5ead6be34749782011528d18a54b",
  "output_checksum": "<gerado-automaticamente-pela-Story-1.4>",
  "generated_at_iso8601": "2026-06-30T12:00:00Z"
}
```

- `manifest.json` nesta pasta: exemplo manual em [`manifest.json`](./manifest.json)

## Resultados

- Comando executável com funcionalidade **upstream existente** (geração padrão EUA).
- População solicitada: 2 pacientes em Massachusetts.
- Saída esperada em `output/fhir/` (bundles FHIR R4) e `output/metadata/` (metadados de run).
- O template foi preenchido integralmente seguindo [`experiment-template.md`](../experiment-template.md).

## Conclusão

O piloto confirma que o template cobre os campos necessários para reprodutibilidade acadêmica (hipótese, seed, comando, resultados, conclusão, refs FR/SM). Features Synthea-br (`br.profile`, `br.target_condition`, plausibilidade) serão documentadas em **novos** experimentos quando as Epics 2–5 estiverem implementadas — não retroagir a este piloto.

## Referências FR/SM

- FR-11: Template de experimento reprodutível
- SM-6: Methods preenchível a partir do template + manifest
- SM-3: Reprodutibilidade (seed + config → output equivalente)
