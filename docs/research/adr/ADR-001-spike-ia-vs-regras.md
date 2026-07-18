# ADR-001: Spike de Viabilidade — IA vs Regras Puras para Plausibilidade Clínica

**Status:** Aceito  
**Data:** 2026-06-30  
**Autores:** Grupo Synthea-br (PUCPR)

---

## Contexto

O Synthea-br define como meta de qualidade clínica a métrica **SM-2**: 0% de violações de severidade **alta** e ≤2% de severidade **média** por cohort gerada. O laboratório PUCPR confirmou que **não dispõe de GPU nem de API paga para LLMs** no MVP — qualquer abordagem baseada em IA generativa exigiria infraestrutura adicional ou custos recorrentes incompatíveis com o escopo acadêmico atual.

O PRD e o addendum propõem três abordagens para atingir plausibilidade:

| Abordagem | Descrição |
|-----------|-----------|
| **A — Regras puras** | Módulos GMF + catálogo versionado de regras (`PLAUS-###`) com engine determinístico |
| **B — Pós-processamento IA** | Geração upstream + refinamento/correção por modelo de linguagem |
| **C — Híbrido** | Regras como baseline + IA para lacunas residuais |

Este spike é **puramente documental e bibliográfico**. Nenhuma etapa depende de GPU, API paga ou treinamento local de modelos.

**Nota de escopo:** o catálogo de regras (Epic 4 / FR-8) e o relatório de validação (Epic 4 / FR-9) ainda não existem neste momento do roadmap. As "métricas de regras puras" abaixo são **estimativas qualitativas** derivadas da literatura e da arquitetura planejada (AD-5). A análise quantitativa com SM-2 real será **revisitada após a implementação do Epic 4**.

### Definição operacional de SM-2

| Severidade | Definição | Meta MVP |
|------------|-----------|----------|
| Alta | Contradição clínica grave (ex.: mastectomia sem diagnóstico de câncer) | 0% dos pacientes |
| Média | Sequência temporal improvável ou código inconsistente | ≤ 2% dos pacientes |
| Baixa | Detalhe cosmético ou terminologia subótima | Documentar; não bloqueia publicação |

### Revisão bibliográfica (3–5 fontes)

| # | Fonte | Abordagem | Infraestrutura | Plausibilidade reportada | Aplicabilidade PUCPR |
|---|-------|-----------|----------------|--------------------------|----------------------|
| 1 | Walonoski et al. (2018) — *Synthea: An approach, method, and software mechanism for generating synthetic patients and the synthetic electronic health care record* (JAMIA Open) | Módulos GMF + regras clínicas determinísticas | CPU apenas; open source | Coerência clínica adequada para pesquisa e interoperabilidade; validação por FHIR | **Alta** — baseline do fork; sem GPU/API |
| 2 | Choi et al. (2017) — *Generating Multi-label Discrete Patient Records using Generative Adversarial Networks* (MLHC) | GAN para registros discretos multi-rótulo | GPU para treinamento; inferência possível em CPU com latência | Melhora distribuição estatística; plausibilidade clínica requer validação externa | **Baixa no MVP** — exige GPU e pipeline de treinamento |
| 3 | Goncalves et al. (2020) — *Generation and evaluation of synthetic patient data* (BMC Med Res Methodol) | Comparação regras/simulação vs métodos generativos (GAN, VAE) | GAN/VAE: GPU; regras: CPU | Regras preservam coerência estrutural; GANs capturam correlações mas introduzem erros clínicos | **Média** — confirma trade-off; regras preferíveis sem infra de ML |
| 4 | Beaulieu-Jones et al. (2019) — *Privacy-preserving generative deep neural networks* (JAMIA) | DNN generativo com privacidade diferencial | GPU + expertise em deep learning | Útil para privacidade em datasets reais; não substitui validação clínica | **Baixa no MVP** — foco em privacidade, não em plausibilidade determinística |
| 5 | Tucker et al. (2020) — *Generating realistic synthetic clinical notes with transformers* (arXiv) | Transformer (GPT-style) para notas clínicas | GPU + API ou modelo local grande | Texto fluente; risco de alucinação clínica; requer revisão humana ou regras | **Baixa no MVP** — exige GPU/API; inadequado para engine determinístico |

### Síntese: quando IA se justificaria

IA (abordagens B ou C) seria reconsiderada se: (a) o catálogo de regras esgotado não atingir SM-2 após calibração no Epic 4; (b) o grupo obtiver budget/API ou GPU institucional; (c) a publicação exigir comparativo explícito com métodos generativos; ou (d) a cohort alvo envolver multi-condição complexa onde regras determinísticas não escalam.

---

## Decisão

**MVP = Abordagem A (regras puras).**

| Abordagem | Decisão MVP | Futuro |
|-----------|-------------|--------|
| A — Regras puras | **Implementar** (Epic 4: catálogo + relatório) | Manter como baseline reprodutível |
| B — Pós-processamento IA | **Spike documental only** (este ADR) | Reavaliar com budget/API |
| C — Híbrido | **Não aplicável no MVP** | Após A esgotado + infra disponível |

Estimativa qualitativa de viabilidade da Abordagem A para SM-2: **alta para severidade alta** (regras estruturais como presença de diagnóstico, sequência básica de exames são implementáveis deterministicamente); **moderada para severidade média** (casos limítrofes de temporalidade podem exigir calibração iterativa do catálogo). Lacunas residuais esperadas: interações medicamentosas raras, comorbidades não modeladas no módulo piloto (câncer de mama), e variações de terminologia CID-10 vs SNOMED no subset BR.

**Este spike não utilizou GPU, API paga nem treinamento local.**

---

## Consequências

### Positivas

- Zero custo de infraestrutura adicional (CPU, determinismo, reprodutibilidade NFR1).
- Alinhamento com AD-5 (engine de plausibilidade determinístico) e AD-2 (mutação clínica apenas no engine).
- Manifest e experimentos acadêmicos permanecem auditáveis sem dependência de modelo opaco.

### Negativas / trade-offs

- Rigor clínico depende da qualidade e cobertura do catálogo `PLAUS-###` — esforço contínuo de curadoria.
- Possível subestimação de casos extremos até calibração pós-Epic 4.
- Publicações que exijam comparação com GAN/LLM precisarão de trabalho adicional futuro.

### Revisão futura

Este ADR será **revisitado com métricas SM-2 reais** após implementação e teste do Epic 4 (catálogo de regras + relatório de validação). Se SM-2 não for atingível com regras esgotadas, o grupo reavaliará as abordagens B/C conforme disponibilidade de infraestrutura.

---

## Referências

- PRD Synthea-br — FR-10, SM-2, §12 Phasing
- Addendum — Spike IA vs Regras, Rigor de Plausibilidade
- Architecture Spine — AD-5 (Plausibility Engine Determinístico)
