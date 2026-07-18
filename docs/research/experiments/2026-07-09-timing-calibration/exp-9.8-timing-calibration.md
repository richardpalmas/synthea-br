# Experimento 9.8 — Calibração de timings (priors vs placeholders)

**Data:** 2026-07-09  
**Story:** 9.8  
**Pasta:** `docs/research/experiments/2026-07-09-timing-calibration/`

## Hipótese

Com a mesma seed e `br.generation.trajectory_mode=episodic`, ativar
`br.pathway.timing_priors=default` (data pack ADR-008) altera de forma **determinística**
as distribuições dos estados `Delay` do módulo `breast_cancer_trajectory_br` em relação a
`br.pathway.timing_priors=off` (placeholders UNIFORM 1–7 dias), sem introduzir PHI.

Os delays calibrados espaçam a marcação de `pathway_phase` (Abordagem E). Eventos clínicos
detalhados continuam no módulo upstream `breast_cancer.json`; a calibração 9.8 **não**
reescreve esse grafo. Melhoria em PLAUS-002/SM-2 no prontuário clínico completo é **indireta**
e deve ser reavaliada quando Epic 4 estiver estável (revisão ADR-001/008 se SM-2 não melhorar).

## Configuração

### Seed

`seed = 92008`

### Propriedades comuns

```
br.profile = br
br.target_condition = breast_cancer
br.target_condition.gate_mode = retry
br.generation.trajectory_mode = episodic
br.generation.module_profile = pathway_minimal
br.pathway.focus = true
exporter.fhir.export = true
br.manifest.enabled = true
```

### Braços

| Braço | `br.pathway.timing_priors` | Esperado |
|-------|----------------------------|----------|
| Baseline | `off` | Delays placeholder UNIFORM 1–7 dias no GMF |
| Calibrado | `default` | TRIANGULAR/buckets de `breast_cancer_timing_priors.json` v1.0.0 |

### Perfil e condição alvo

- Perfil: `br`
- Condição alvo: `breast_cancer`
- População piloto: n=10 (`-p 10 -g F -a 45-75`)

## Comando executado

```bash
# Baseline (placeholders)
run_synthea.bat -s 92008 -p 10 -g F -a 45-75 ^
  --br.profile=br ^
  --br.target_condition=breast_cancer ^
  --br.generation.trajectory_mode=episodic ^
  --br.generation.module_profile=pathway_minimal ^
  --br.pathway.timing_priors=off ^
  --br.pathway.focus=true ^
  --br.manifest.enabled=true

# Calibrado
run_synthea.bat -s 92008 -p 10 -g F -a 45-75 ^
  --br.profile=br ^
  --br.target_condition=breast_cancer ^
  --br.generation.trajectory_mode=episodic ^
  --br.generation.module_profile=pathway_minimal ^
  --br.pathway.timing_priors=default ^
  --br.pathway.focus=true ^
  --br.manifest.enabled=true
```

**Nota:** o módulo GMF é carregado uma vez por JVM; rode cada braço em processo separado
(ou reinicie a JVM) para garantir que o `ModuleSupplier` aplique o valor correto de
`br.pathway.timing_priors` no load.

## Métricas

| Métrica | Como obter | Status piloto |
|---------|------------|---------------|
| `pathway_timing_priors_version` no `manifest.json` | Braço calibrado → `1.0.0`; baseline → `off` | Verificado por teste unitário + campo manifest |
| Determinismo JSON Delay | `PathwayTimingLoaderTest.applyPriors_replacesDelayDistributionsDeterministically` | Passa no CI |
| % eventos fora de ordem / SM-2 (PLAUS-002) | `plausibility_report.json` (Epic 4) | **Pendente** — Epic 4 em review; registrar quando disponível |
| Checksum / JSON Delay calibrado ≠ baseline | Unit test: `maybeApplyPriors` com `default` produz TRIANGULAR; `off` preserva UNIFORM | Verificado no CI (`PathwayTimingLoaderTest`) |

## Resultado (piloto documental)

- Data pack `breast_cancer_timing_priors.json` v1.0.0 sem PHI (NFR5) — validado por
  `PathwayTimingLoader.assertNoPhi` + revisão manual das `reference_notes`.
- Injeção JSON pré-`Module` preserva grafo de states (só substitui `distribution`/`unit`).
- Aproximação estatística: schema ADR usa `median`; GMF TRIANGULAR usa `mode`. O loader
  mapeia `median → mode` (documentado em `PathwayTimingLoader.toGmfDistributionJson`).
  Buckets são colapsados em TRIANGULAR via média ponderada dos midpoints — MVP apenas.

## Revisão ADR pós-Epic 4

Se, com priors `default`, a média SM-2 / violações PLAUS-002 **não** melhorar vs baseline
`off` em n≥50, abrir revisão de ADR-001 e ADR-008 para decidir: (a) calibrar delays no
módulo upstream, (b) orquestrar via `CallSubmodule`/`Guard`, ou (c) manter priors só como
metadado narrativo.

## Artefatos

- Data pack: `src/main/resources/br/pathways/breast_cancer_timing_priors.json`
- Loader: `org.mitre.synthea.br.pathway.PathwayTimingLoader`
- Testes: `PathwayTimingLoaderTest`
- ADR schema: `docs/research/adr/ADR-008-trajetoria-clinica-focada.md`
