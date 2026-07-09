# ADR-008: Trajetória Clínica Focada (Epic 9)

**Status:** Aceito  
**Data:** 2026-07-08  
**Autores:** Grupo Synthea-br (PUCPR)

---

## Contexto

Pesquisadores do grupo relatam **dor recorrente** ao trabalhar com cohorts de condições específicas — em especial **câncer de mama** (`breast_cancer`): o Synthea upstream gera prontuários de **vida inteira**, repletos de comorbidades, consultas de rotina e eventos paralelos que **poluem** o dataset exportado e tornam a narrativa longitudinal **incoerente** para apresentação a orientadores, análise estatística focada ou validação de plausibilidade (SM-2).

O problema não é apenas volume de dados, mas **desalinhamento semântico**: o pesquisador pede uma trajetória oncológica (rastreio → diagnóstico → estadiamento → tratamento → seguimento), mas recebe uma simulação biográfica onde o câncer é um entre muitos episódios.

O **Epic 9 — Trajetória Clínica Focada (Cohort Enxuta)** endereça essa dor com três abordagens complementares, ancoradas em referências longitudinais documentadas, sem substituir o motor determinístico do `Generator` no MVP.

### Escopo do Epic 9

| FR | Story | Entrega |
|----|-------|---------|
| FR18 | 9.1 | Spike de referências + este ADR |
| FR19 | 9.2 | Catálogo versionado de fases por condição-alvo |
| FR20 | 9.3 | Export focado (abordagem C) |
| FR21 | 9.4 | HTML narrativo por fase / modo orientador |
| FR22 | 9.5 | Perfil de geração enxuto (abordagem D) |
| FR23 | 9.6 | Janela temporal de simulação (abordagem D) |
| FR24 | 9.7 | Módulo GMF episódico (abordagem E) |
| FR25 | 9.8 | Calibração de timings a partir de referências externas |

**Condição piloto:** câncer de mama (`breast_cancer`, SNOMED `254837009`).

### Referências de ancoragem (não motores primários no MVP)

As três famílias abaixo informam **fases, ordem canônica, intervalos macro e padrões de auditoria** — importadas como **data packs determinísticos** ou citações bibliográficas, **não** como substitutos do `Generator` ou dos exportadores:

| Referência | O que importamos | O que **não** importamos no MVP |
|------------|------------------|----------------------------------|
| **OncoSynth** (ML/difusão) | Priors estatísticos de trajetória de tratamento e sobrevida; inspiração para distribuições temporais entre fases (Story 9.8) | Motor de geração FHIR; treinamento de modelos de difusão; inferência GPU |
| **Coogee** (LLM + auditoria) | Padrões de **validação de consistência narrativa** e checklist de coerência longitudinal (referência metodológica para Epic 4 e revisão qualitativa) | Pipeline LLM como única fonte de coerência; auditoria runtime não-determinística |
| **Linhas de cuidado SUS/DATASUS** | Ordem macro de fases assistenciais e sequências de procedimentos agregados (dados públicos) | Microdados de paciente; PHI; dependência de API externa em tempo de geração |

### Fases assistenciais mínimas — mapeamento por fonte (câncer de mama piloto)

O catálogo de fases (Story 9.2) usa como ancoragem bibliográfica a jornada mínima
**rastreio → diagnóstico → estadiamento → tratamento → seguimento**. Cada fase é informada por
fontes distintas, com limitações explícitas — nenhuma fonte é usada como motor de geração:

| Fase (PT-BR) | `phase_id` previsto (9.2) | Contribuição por fonte | Limitação por fonte |
|--------------|---------------------------|-------------------------|----------------------|
| Rastreio | `screening` | SUS/DATASUS: protocolo de mamografia bienal (linha de cuidado pública) | SUS/DATASUS: agregados macro, sem timing individual; não é prontuário FHIR |
| Diagnóstico | `diagnosis` | OncoSynth: intervalo estatístico rastreio→biópsia/confirmação; SUS/DATASUS: ordem exame→biópsia→anatomopatológico | OncoSynth: não gera FHIR nem prontuário determinístico, apenas estatística de sobrevida/tratamento; é priors, não runtime |
| Estadiamento | `staging` | SUS/DATASUS: sequência exames de estadiamento (imagem, marcadores) pós-diagnóstico | SUS/DATASUS: sem granularidade de subtipo tumoral; dado agregado nacional, não individualizado |
| Tratamento | `treatment` | OncoSynth: distribuições de duração/sequência de linhas de tratamento (cirurgia, quimio, radio, hormonioterapia); Coogee: checklist de consistência narrativa entre eventos de tratamento | Coogee: padrão de **auditoria narrativa** (revisão de coerência textual/LLM), não fonte de timing quantitativo; não substitui validação clínica |
| Seguimento | `follow_up` | SUS/DATASUS: periodicidade de consultas de seguimento oncológico pós-tratamento | SUS/DATASUS: não captura desfechos individuais nem eventos adversos raros |

Este mapeamento é **bibliográfico e qualitativo** nesta story; valores quantitativos (priors de
timing) são formalizados no schema da seção seguinte e populados na Story 9.8.

### Restrições de infraestrutura do laboratório — conformidade do spike

Este spike (Story 9.1) e as decisões aqui registradas cumprem as restrições do laboratório PUCPR:

- **Sem GPU** — nenhuma etapa do spike ou das Stories 9.2–9.8 planejadas executa inferência ou
  treinamento de modelo; OncoSynth/Coogee são citados apenas como literatura/priors estatísticos.
- **Sem API paga** — nenhuma chamada a serviço externo (OncoSynth, Coogee ou qualquer LLM) é
  necessária para produzir ou consumir os artefatos deste ADR; toda informação é transcrita como
  texto/JSON estático versionado no repositório.
- **Sem treinamento local** — não há pipeline de treinamento de modelo de difusão ou LLM neste
  ADR nem nas stories que ele desbloqueia.
- **Sem PHI no repositório** — o spike é puramente documental/bibliográfico; nenhum dado de
  paciente real, microdado individualizado ou identificador é referenciado ou versionado. Os
  exemplos de schema (seção seguinte) usam apenas valores esquemáticos.

### Explicitamente fora do MVP do Epic 9

- Substituir o `Generator` por OncoSynth ou Coogee como motor primário de geração
- Treinar ou executar modelos de difusão/ML em runtime
- Pipeline LLM não-determinístico como **única** fonte de coerência narrativa (conflita com **NFR1** e **ADR-001**)
- Commit de datasets com **PHI** ou microdados reais no repositório (**NFR5**)
- Integrações ML/LLM como motor primário — exigem ADR próprio pós-calibração SM-2 (revisão ADR-001)

Restrições herdadas:

- **AD-2** — exportadores e filtros são **read-only** sobre `HealthRecord`
- **NFR1** — mesma seed + config + data packs → mesmo output
- **ADR-001** — geração determinística por regras/GMF como baseline; IA apenas em estágios opcionais documentados (ADR-007)

### Formato-alvo de importação de priors temporais (pré-requisito Story 9.8)

Para que a Story 9.8 calibre transições do módulo GMF episódico (Story 9.7) com referências
externas sem incorporar PHI, define-se aqui a **versão inicial do schema** de priors temporais por
transição de fase. O arquivo reside em
`src/main/resources/br/pathways/breast_cancer_timing_priors.json` e é consumido por
`org.mitre.synthea.br.pathway.PathwayTimingLoader` (Story 9.8) — apenas leitura de estatísticas
agregadas, sem qualquer chamada a runtime ML/LLM.

**Esquema (v1) — campos:**

- `priors_version` (string): versão semântica do data pack, incrementada a cada calibração.
- `condition` (string): condição-alvo, ex. `breast_cancer`.
- `unit` (string): unidade de tempo dos valores de distribuição, ex. `days`.
- `reference_notes` (array de string): citações bibliográficas/metadados (SUS/DATASUS, OncoSynth,
  Coogee) — texto livre, sem PHI.
- `transitions` (objeto): chave `"{phase_id_origem}->{phase_id_destino}"` usando os `phase_id`
  estáveis do catálogo (Story 9.2); valor é uma distribuição com `min`, `max`, `median` **ou**
  `buckets` (lista de `{ "range_min", "range_max", "probability" }` somando 1.0).

**Exemplo esquemático (sem dados de paciente real — valores ilustrativos de placeholder):**

```json
{
  "priors_version": "0.1.0-schema-preview",
  "condition": "breast_cancer",
  "unit": "days",
  "reference_notes": [
    "SUS/DATASUS: linha de cuidado oncológica — ordem macro de procedimentos (agregado público)",
    "OncoSynth: priors estatísticos de duração de tratamento (referência bibliográfica, não runtime)"
  ],
  "transitions": {
    "screening->diagnosis": { "min": 15, "max": 90, "median": 30 },
    "diagnosis->staging": { "min": 7, "max": 45, "median": 14 },
    "staging->treatment": { "min": 7, "max": 60, "median": 21 },
    "treatment->follow_up": {
      "buckets": [
        { "range_min": 90, "range_max": 180, "probability": 0.6 },
        { "range_min": 181, "range_max": 365, "probability": 0.4 }
      ]
    }
  }
}
```

Este exemplo é **esquemático** — a Story 9.8 é responsável por popular valores finais com
citações completas e revisão de privacidade (zero PHI/microdados), podendo refinar campos
opcionais (ex.: `source_id` por transição) sem quebrar a estrutura v1 acima.

## Decisão

Adotar **três abordagens complementares** (C + D + E), combináveis por configuração, com catálogo de fases (Story 9.2) como ontologia compartilhada:

| Abordagem | Nome | Onde atua | Mecanismo |
|-----------|------|-----------|-----------|
| **C** | Filtro de export | Pós-geração, pré-export | `PathwayExportFilter` + exportadores (read-only, AD-2) |
| **D** | Geração enxuta | Durante simulação | `br.generation.module_profile` + `br.generation.simulation_window` |
| **E** | Módulo GMF episódico | Origem da trajetória | `modules/br/breast_cancer_trajectory.json` + `br.generation.trajectory_mode=episodic` |

As abordagens **não são mutuamente exclusivas**. A combinação recomendada para cohort piloto de apresentação é **C + D**; **E** eleva coerência na origem quando o pesquisador aceita desviar da simulação de vida inteira.

### Matriz de uso — quando aplicar cada abordagem

| Cenário do pesquisador | C (export) | D (geração enxuta) | E (GMF episódico) |
|------------------------|:----------:|:------------------:|:-----------------:|
| Cohort já gerada; precisa limpar CSV/FHIR/HTML sem re-simular | **Sim** | Não | Não |
| Nova cohort; quer menos ruído mas manter simulação upstream | Opcional | **Sim** | Não |
| Apresentação a orientador; narrativa por fase em PT-BR | **Sim** (+ 9.4) | Recomendado | Opcional |
| Pesquisa focada em sequência oncológica; tempo de geração crítico | Sim | **Sim** (minimal + janela) | Recomendado |
| Baseline reprodutível comparável ao upstream | Não (default) | Não (`full` + `lifespan`) | Não |
| Calibração de timings com priors SUS/literatura | Sim (export) | Sim | **Sim** (9.7 → 9.8) |

### Propriedades de configuração (preview)

| Propriedade | Valores previstos | Abordagem | Default |
|-------------|-------------------|-----------|---------|
| `br.pathway.focus` | `true` / `false` | C | `false` |
| `br.generation.module_profile` | `full`, `pathway_minimal` | D | `full` |
| `br.generation.simulation_window` | ex.: `pre_onset_years:N` | D | ausente (vida inteira) |
| `br.generation.trajectory_mode` | `lifespan`, `episodic` | E | `lifespan` |
| `exporter.html.pathway_mode` | `orientador`, `pesquisador`, `full` | C (HTML) | `orientador` quando `br.pathway.focus=true`; senão `full` |

**Pré-requisitos operacionais:** `br.profile=br`, `br.target_condition=breast_cancer` (gate Epic 2), catálogo de fases carregado (Story 9.2).

### O que entra no fork vs. deferred

| Entra no fork (Epic 9 MVP) | Deferred (ADR futuro / pós-SM-2) |
|----------------------------|----------------------------------|
| Catálogo `breast_cancer_phases.json` + API `PathwayCatalog` | Substituição do Generator por OncoSynth |
| Filtro de export read-only (C) | Coogee como auditor runtime LLM |
| Perfil `pathway_minimal` e janela temporal (D) | Treinamento de modelos de difusão |
| Módulo GMF episódico BR (E) | PHI ou microdados reais no repo |
| Data pack `breast_cancer_timing_priors.json` (agregados, sem PHI) | Integração ML/LLM como motor primário de coerência |

## Consequências

### Positivas

- Pesquisadores recebem cohorts **enxutas e narrativamente coerentes** sem abandonar o pipeline determinístico do Synthea
- Abordagem C respeita **AD-2** (export read-only) e permite re-filtrar cohorts já geradas
- Abordagem D reduz tempo de simulação e ruído **na origem**, não só na visualização
- Abordagem E alinha trajetória oncológica às fases do catálogo, melhorando SM-2 em sequências temporais
- Referências OncoSynth/Coogee/SUS ficam **documentadas e rastreáveis** sem custo de GPU/API no MVP
- Mesma seed + config preserva reprodutibilidade (**NFR1**) em todas as combinações suportadas

### Negativas / trade-offs

- Três camadas (C+D+E) aumentam **superfície de configuração** e matriz de testes
- Filtro C sozinho **não corrige** incoerências já geradas — apenas oculta na exportação
- Janela temporal (D) pode impactar demografia, comorbidades e compatibilidade com `-a`/IBGE — requer validação explícita
- Modo episódico (E) desvia da semântica “vida inteira” do upstream; comparabilidade com cohorts upstream exige documentação
- Priors de timing (9.8) são **aproximações** de agregados públicos — não substituem validação clínica local
- Calibração fina contra SM-2 real depende do Epic 4 (ainda em roadmap)

### Ações de acompanhamento

- [ ] Publicar catálogo de fases `breast_cancer_phases.json` (Story 9.2)
- [ ] Implementar `PathwayExportFilter` e `br.pathway.focus` (Story 9.3)
- [ ] Implementar HTML por fase e `exporter.html.pathway_mode` (Story 9.4)
- [ ] Definir perfil `pathway_minimal.json` e testes de integração (Story 9.5)
- [ ] Documentar semântica de `br.generation.simulation_window` e combinações inválidas (Story 9.6)
- [ ] Criar módulo `breast_cancer_trajectory.json` com `trajectory_mode=episodic` (Story 9.7)
- [ ] Importar `breast_cancer_timing_priors.json` com citações SUS/DATASUS e notas OncoSynth/Coogee (Story 9.8)
- [ ] Registrar experimento piloto em `docs/research/experiments/` (cohort calibrada vs. não calibrada)
- [ ] Revisar ADR-001 após SM-2 real (Epic 4) para avaliar integrações ML/LLM adicionais

---

## Adendo A — Validação externa (NHS England, jun/2026)

**Fonte:** Poulett et al., *A Pipeline for Generating Longitudinal Synthetic Clinical Notes Using Large Language Models*, arXiv [2606.26879v2](https://arxiv.org/html/2606.26879v2) (NHS England Data Science and Applied AI Team, CC BY 4.0).

Este adendo registra evidência externa independente que **reforça** as decisões deste ADR, sem alterar o status **Aceito** nem o escopo do Epic 9.

### O que o paper faz (resumo)

A NHS England publicou um pipeline modular em cinco estágios para gerar **notas clínicas hospitalares longitudinais** (70 pacientes × 20–50 notas) usando LLMs:

1. **Pacientes** — Synthea UK (adaptação NHS) **apenas para demografia** (nome, idade, sexo); LLM complementa contatos e alergias.
2. **Admissões** — motivo de internação amostrado de tabela curada (SNOMED/ICD possível); LLM gera especialidade, ward, medicações, histórico.
3. **Jornada do paciente** — **100% LLM**: jornada simples → detecção de truncamento → validador de realismo → enriquecimento por evento.
4. **Notas clínicas** — uma nota por evento; **personas de escrita** por membro de staff (Concise, Narrative, Bullet Points, etc.); validador de fidelidade em loop.
5. **Augmentação** — abreviações clínicas e typos por staff.

Eles **não usam** o motor completo do Synthea para trajetória hospitalar. Motivo explícito no paper:

> *"Synthea UK is focused on primary care — to use it later in our pipeline we would require secondary care journeys and development of a secondary care simulation engine was not possible in the time available."*

### Implicações para o Synthea-br (Epic 9)

| Insight do paper | Decisão já tomada neste ADR | Ação no fork |
|------------------|----------------------------|--------------|
| Synthea sozinho insuficiente para jornada hospitalar/secundária coerente | Abordagem **E** (GMF episódico BR) como motor determinístico de trajetória | Story 9.7 — `breast_cancer_trajectory.json` |
| LLM como **única** fonte de coerência longitudinal é frágil (alucinações, truncamento, viés, casos "médios") | Explicitamente **fora do MVP** (conflita NFR1/ADR-001) | Manter deferred; ADR próprio pós-SM-2 se necessário |
| Coerência temporal = timestamps em ordem por paciente | PLAUS-002 / SM-9.3 no Epic 4 e experimento 9.8 | Reutilizar métrica `% pacientes 100% em ordem` do paper |
| Jornada em duas passadas (esqueleto → complexidade) melhora qualidade | Sequenciamento 9.7 (GMF placeholder) → 9.8 (priors calibrados) | Confirmado — não unificar em uma única story |
| MedSyn-like: injetar dados estruturados no prompt LLM | Abordagem **C** exporta `HealthRecord` determinístico; IA opcional via ADR-007 | Epic 8 complementar, não motor primário |

### O que **não** importamos do paper (regressão arquitetural)

- Geração de admissão, red flags e exames obrigatórios **via LLM** — o GMF + catálogo 9.2 já modelam isso deterministicamente.
- Dataset Bronze/Silver/Gold com revisão clínica NHS — fora do escopo acadêmico PUCPR no MVP.
- GPT-4o como motor de jornada — conflita com restrição **sem API paga** no laboratório para geração core.

### Personas e viés — encaminhamento Epic 8

O paper valida dois padrões reaproveitáveis na **camada opcional de IA** (ADR-007), não na geração core:

- **Personas de estilo de escrita** consistentes por autor sintético → Story **8.2** (personas narrativas + integração HTML/Epic 6).
- **Teste de viés por troca de gênero** (Rickman, citado no paper) → Story **8.2** (extensão BR: sexo/raça/UF).

### Conclusão do adendo

O paper da NHS England é evidência **a favor** da arquitetura C+D+E deste ADR: quando a trajetória clínica precisa ser longitudinal e coerente, **simulação determinística na origem** (GMF episódico) supera pipeline LLM-heavy como fonte primária. A camada LLM permanece útil para **narrativa, validação opcional e augmentação** — exatamente onde o ADR-007 posiciona o MAI-DxO.

## Referências

- [ADR-001](ADR-001-spike-ia-vs-regras.md) — Spike IA vs Regras Puras (NFR1, determinismo)
- [ADR-007](ADR-007-ai-enrichment-maidxo.md) — Enriquecimento opcional pós-geração (complementar, não substituto)
- Epic 9 — `_bmad-output/planning-artifacts/epics.md` (FR18–FR25, Stories 9.1–9.8)
- ARCHITECTURE-SPINE — AD-2 (read-only export), AD-3 (data packs vs. hardcode)
- OncoSynth — referência bibliográfica para priors estatísticos de trajetória oncológica (não motor FHIR)
- Coogee — referência metodológica para padrões de auditoria/consistência narrativa LLM (MVP: documental)
- Linhas de cuidado SUS/DATASUS — ordem macro de fases e procedimentos (dados agregados públicos)
- PRD Synthea-br — FR-19 a FR-25 (`docs/_bmad-output/planning-artifacts/`)
- Poulett et al. (2026) — *A Pipeline for Generating Longitudinal Synthetic Clinical Notes Using Large Language Models*, arXiv:2606.26879v2 — [HTML](https://arxiv.org/html/2606.26879v2) (validação externa; ver Adendo A)
