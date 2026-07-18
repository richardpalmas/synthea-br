# Template de Experimento — Synthea-br

<!--
  CONVENÇÃO DE PASTA:
  Copie este arquivo para docs/research/experiments/YYYY-MM-DD-<slug>/experiment.md
  Exemplo: docs/research/experiments/2026-06-30-meu-experimento/experiment.md
-->

# [Título curto do experimento]

## Hipótese

<!-- O que você espera observar ou provar? Seja específico e falsificável. -->
<!-- Exemplo: "Com seed=42 e br.target_condition=breast_cancer, 100% dos pacientes terão diagnóstico verificável." -->

_[Descreva a hipótese aqui]_

## Configuração

### Seed

<!-- Seed numérica usada na geração (-s no CLI). Obrigatória para reprodutibilidade. -->
`seed = `

### Propriedades relevantes

<!-- Liste alterações em synthea.properties ou flags -D/--config.* usadas. -->
<!-- Exemplo: br.profile=br, br.target_condition=breast_cancer -->

```
# propriedade = valor
```

### Perfil e condição alvo

<!-- Perfil geográfico (br ou default) e condição clínica, se aplicável. -->
- Perfil: _(default / br — quando Epic 3 estiver implementada)_
- Condição alvo: _(nenhuma / breast_cancer — quando Epic 2 estiver implementada)_

## Comando executado

<!-- Comando exato, copiável, incluindo path relativo ao repositório. -->
```bash
./run_synthea -s <seed> -p <populacao> <Estado>
```

## Data de execução

<!-- ISO-8601 em UTC. Exemplo: 2026-06-30T14:30:00Z -->
`generated_at = `

## Manifest de rastreabilidade

<!-- ATENÇÃO: até a Story 1.4, o manifest era manual. Agora é gerado automaticamente em output/manifest.json. -->
<!-- Copie output/manifest.json para esta pasta do experimento antes de commitar. -->

**Schema mínimo** (referência FR-13 / AD-6):

```json
{
  "seed": 0,
  "config_hash": "<sha256-hex>",
  "commit_sha": "<git-sha-completo>",
  "output_checksum": "<sha256-hex>",
  "generated_at_iso8601": "2026-01-01T00:00:00Z"
}
```

- `manifest.json` nesta pasta: _(caminho ou "pendente")_

## Resultados

<!-- Métricas observadas, caminhos para outputs/ (não versionados), links para relatórios. -->
<!-- Para plausibilidade: % violações alta/média/baixa (quando Epic 4 estiver implementado). -->

_[Descreva os resultados aqui]_

## Conclusão

<!-- A hipótese foi confirmada ou refutada? Próximos passos? -->

_[Descreva a conclusão aqui]_

## Referências FR/SM

<!-- Liste os Functional Requirements e Success Metrics do PRD relacionados. -->
- FR-_: _
- SM-_: _
