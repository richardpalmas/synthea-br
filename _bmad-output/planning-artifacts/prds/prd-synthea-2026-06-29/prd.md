---
title: Synthea-br — Dados Clínicos Sintéticos Brasil
status: final
created: 2026-06-29
updated: 2026-07-08
---

# PRD: Synthea-br — Dados Clínicos Sintéticos Brasil

Projeto de pesquisa PUCPR — fork **Synthea-br**.

## 0. Document Purpose

Este PRD orienta o desenvolvimento de um **fork acadêmico do Synthea** para o grupo de pesquisa da PUCPR. Destinatários: líderes do grupo, estudantes envolvidos, orientadores e workflows downstream (arquitetura, épicos, implementação).

O documento usa vocabulário fixo na seção **Glossary**; requisitos funcionais numerados globalmente (FR-1…FR-16, FR-18…FR-25); suposições marcadas com `[ASSUMPTION]` e indexadas na seção 11.

Construído sobre o `project-context.md` (`_bmad-output/project-context.md`), que descreve o codebase brownfield Synthea. Decisões de implementação detalhadas estão em `addendum.md`.

---

## 1. Vision

Pesquisadores e estudantes da PUCPR precisam de **populações de pacientes sintéticos** para experimentos, ensino e publicações — mas o Synthea upstream gera dados centrados nos EUA, em condições genéricas e com plausibilidade clínica limitada. Isso reduz a utilidade para estudos brasileiros (epidemiologia, SUS, terminologia local) e para protocolos que exigem **casos clínicos específicos** (ex.: cohort de câncer de mama).

O **Synthea-br** adapta o motor Synthea para: (1) **geração direcionada por condição clínica**, (2) **contexto demográfico e assistencial brasileiro**, e (3) **maior coerência e credibilidade** dos registros clínicos sintéticos. Um **workflow acadêmico de documentação** garante rastreabilidade de decisões, experimentos e achados — requisito para publicação e reprodutibilidade científica.

A iniciativa começa como **projeto interno da universidade**, com **intenção de publicar** em **artigos e conferências** (SBIS, CBIS). Há **possibilidade de monetização futura**, fora do escopo do MVP — o MVP prioriza rigor acadêmico, reprodutibilidade e qualidade publicável.

---

## 2. Target User

### 2.1 Jobs To Be Done

- **Funcional:** Gerar datasets sintéticos FHIR filtrados por condição clínica e contexto BR, com parâmetros reprodutíveis (seed, config).
- **Funcional:** Validar se os registros gerados são clinicamente plausíveis para o experimento em questão.
- **Emocional:** Confiança de que os dados não contradizem a hipótese do estudo por incoerências óbvias.
- **Social:** Produzir artefatos citáveis (paper, dataset, código) aceitos por orientador e revisores.
- **Contextual:** Operar em ambiente acadêmico — budget limitado, prazos de semestre, múltiplos estudantes no mesmo fork.

### 2.2 Non-Users (v1)

- Hospitais ou operadoras buscando dados para produção clínica real.
- Usuários sem familiaridade mínima com linha de comando, Java ou FHIR.
- Equipes que precisam apenas de dados EUA genéricos (upstream Synthea atende).

### 2.3 Key User Journeys

**UJ-1. Ana configura uma cohort de câncer de mama**
- **Persona + contexto:** Ana, mestranda PUCPR, precisa de 500 pacientes sintéticos com câncer de mama.
- **Entry state:** Fork instalado; orientador definiu condição e tamanho da amostra.
- **Path:** Configura condição alvo, seed fixo, perfil `br`; executa `./run_synthea`; inspeciona export FHIR; registra parâmetros no log acadêmico.
- **Climax:** Export contém pacientes com trajetória compatível com câncer de mama; metadados de reprodutibilidade anexados.
- **Resolution:** Dataset versionado; experimento documentado com comando, seed e hash.
- **Edge case:** Nenhum paciente satisfaz a condição — erro claro com sugestão de ajuste.

**UJ-2. Prof. Carlos avalia qualidade antes de submeter artigo**
- **Persona + contexto:** Carlos, orientador, revisa plausibilidade dos dados de orientandos.
- **Path:** Executa relatório de validação; compara métricas entre runs; consulta documentação acadêmica.
- **Climax:** Inconsistências abaixo do limiar SM-2.
- **Resolution:** Aprova dataset para paper ou abre issue de melhoria.

**UJ-3. Bruno documenta experimento para reprodutibilidade**
- **Persona + contexto:** Bruno, graduando, concluiu sprint de localização BR.
- **Path:** Preenche template de experimento; commita; referencia FR/SM.
- **Climax:** Orientador reproduz run com mesma seed e output equivalente.
- **Resolution:** Entrada pronta para seção Methods do artigo.

**UJ-4. Ana valida cohort enxuta com narrativa clínica focada para o orientador**
- **Persona + contexto:** Ana, mestranda PUCPR, precisa apresentar casos de câncer de mama ao orientador sem ruído de prontuário de vida inteira.
- **Entry state:** Epic 2 ativo (`br.target_condition=breast_cancer`); catálogo de fases publicado (FR-19).
- **Path:** Ativa `br.pathway.focus=true`; gera cohort com perfil `pathway_minimal` e, opcionalmente, `trajectory_mode=episodic`; exporta CSV/FHIR focado; abre `index.html` com `exporter.html.pathway_mode=orientador`.
- **Climax:** Timeline agrupada por fase (rastreio → diagnóstico → estadiamento → tratamento → seguimento); orientador reconhece sequência assistencial plausível.
- **Resolution:** Dataset enxuto versionado com manifest (`pathway_focus`, `pathway_catalog_version`); feedback incorporado antes do paper.
- **Edge case:** Export focus vazio — erro com sugestão de revisar catálogo ou desativar filtro.

---

## 3. Glossary

- **Synthea-br** — Fork do Synthea upstream com adaptações deste PRD.
- **Condição clínica alvo** — Doença ou cenário que a geração deve garantir (ex.: câncer de mama). Mapeia a módulos GMF ou Java customizados.
- **Contexto brasileiro** — Dados e regras que localizam a simulação para Brasil: demografia, geografia, nomenclatura, providers.
- **Cohort direcionada** — População onde percentual configurável (default 100%) satisfaz a condição clínica alvo.
- **Plausibilidade clínica** — Coerência temporal, comorbidades compatíveis, exames/tratamentos alinhados — medida por regras do grupo.
- **GMF** — Generic Module Framework do Synthea (versão 2.0).
- **Workflow acadêmico** — Registro estruturado de experimentos, decisões e achados.
- **Spike IA** — Investigação documental (sem GPU/API no lab) sobre viabilidade de IA; MVP prioriza regras/módulos.
- **Seed** — Valor que garante reprodutibilidade da geração.
- **Trajetória clínica focada** — Subconjunto enxuto e narrativamente coerente da jornada assistencial da condição-alvo (ex.: rastreio → diagnóstico → tratamento → seguimento), sem poluição de eventos irrelevantes.
- **Modo orientador** — Variante do export HTML (`exporter.html.pathway_mode=orientador`) que exibe apenas eventos da trajetória focada (+ demografia), agrupados por fase clínica, para validação com orientador.
- **Catálogo de fases** — Data pack versionado com fases canônicas da trajetória por condição-alvo: `phase_id`, ordem, allowlists de códigos e metadados `always_include`.
- **Abordagem C** — Filtro read-only de export sobre `HealthRecord` completo (`br.pathway.focus`); reduz ruído em CSV/FHIR/HTML sem alterar a simulação.
- **Abordagem D** — Geração enxuta na origem: perfil de módulos suprimidos (`pathway_minimal`) e janela temporal de simulação restrita à época relevante.
- **Abordagem E** — Módulo GMF episódico que modela a trajetória oncológica como simulação principal (`trajectory_mode=episodic`), em vez de subproduto de vida inteira.

---

## 4. Features

### 4.1 Seleção de Condição Clínica Alvo

Permite especificar condição clínica e gerar cohort garantida. Realiza UJ-1. `[ASSUMPTION: v1 via config + CLI, sem GUI.]`

#### FR-1: Especificar condição clínica alvo

Pesquisador declara condição (nome ou ID de módulo GMF) via config ou CLI. Realiza UJ-1.

**Consequences (testable):**
- Sistema rejeita condição inexistente com mensagem identificável.
- Documentação lista condições do MVP (mínimo: câncer de mama).

#### FR-2: Gerar cohort com condição garantida

População com percentual configurável (default 100%) apresenta condição ao fim da simulação. Realiza UJ-1.

**Consequences (testable):**
- Seed e config idênticos → percentual dentro de tolerância (±0% quando 100%).
- Pacientes fora da condição excluíveis via flag de exportação.

#### FR-3: Compor múltiplas condições `[NON-GOAL for MVP]`

`[ASSUMPTION: fora do MVP.]`

---

### 4.2 Localização para Contexto Brasileiro

Substitui defaults EUA por dados BR. Realiza UJ-1, UJ-2. `[ASSUMPTION: MVP = demografia IBGE simplificada, nomes BR, geografia BR; CID-10 subset piloto.]`

#### FR-4: Perfil demográfico brasileiro

Distribuições BR (sexo, idade, raça/cor IBGE) quando perfil `br` ativo.

**Consequences (testable):**
- Amostra N≥1000 com perfil `br` mais próxima de referência BR documentada que default EUA.
- Perfil selecionável via `synthea.properties` ou CLI.

#### FR-5: Geografia BR

Endereços consistentes com divisão administrativa brasileira.

**Consequences (testable):**
- 100% endereços com UF válida e formato postal BR.
- Coordenadas dentro do estado declarado.

#### FR-6: Nomenclatura clínica brasileira

Códigos clínicos BR no MVP. `[ASSUMPTION: subset CID-10 para câncer de mama; fonte TBD — ver §10; TUSS fora do MVP.]`

**Consequences (testable):**
- Condição piloto em FHIR referencia sistemas/códigos documentados.
- Mapeamento EU→BR documentado para câncer de mama.

#### FR-7: Providers BR

`[ASSUMPTION: CSV BR simplificado (UBS/hospital genérico).]`

Encounters atribuídos a providers do contexto assistencial BR.

**Consequences (testable):**
- Providers nos exports pertencem ao dataset BR.
- Nenhum provider EUA com perfil `br` ativo.

---

### 4.3 Plausibilidade e Qualidade Clínica

Critérios mensuráveis de coerência clínica. Realiza UJ-2.

#### FR-8: Catálogo de regras de plausibilidade

Regras versionadas (JSON/YAML) com IDs estáveis.

**Consequences (testable):**
- Regras versionadas no repositório.
- Cada regra referenciável em relatórios.

#### FR-9: Relatório de validação pós-geração

Relatório com violações por paciente e agregados.

**Consequences (testable):**
- Gerado via comando documentado (`./gradlew` task ou CLI).
- Saída com contagem, severidade e exemplos anonimizados.

#### FR-10: Spike IA (documental, sem infra no lab)

Avalia se SM-2 é atingível com regras puras; revisão bibliográfica sobre IA futura. Realiza UJ-2.

**Consequences (testable):**
- ADR com métricas, lacunas e recomendação IA in/out/futuro.
- MVP implementa regras puras salvo ADR contrário.
- Spike não depende de GPU, API paga ou treinamento local.

---

### 4.4 Workflow Acadêmico de Documentação

Suporte a publicação e orientação. Realiza UJ-3.

#### FR-11: Template de experimento reprodutível

Template Markdown: hipótese, config, seed, comando, resultados, conclusão, refs FR/SM.

**Consequences (testable):**
- Template em `docs/research/`.
- Pelo menos um experimento piloto preenchido.

#### FR-12: ADRs

Decisões significativas registradas como ADRs numerados.

**Consequences (testable):**
- Formato consistente (contexto, decisão, consequências).

#### FR-13: Rastreabilidade config → output

Manifest com hash de config, seed, versão, commit git, checksum.

**Consequences (testable):**
- Reexecução com manifest reproduz output equivalente.

#### FR-14: Guia de contribuição acadêmica

Guia PT-BR: rodar, documentar, citar, disclaimer ético.

**Consequences (testable):**
- Guia no repositório com disclaimer de dados sintéticos.

---

### 4.5 Exportação e Compatibilidade

Realiza UJ-1, UJ-2, UJ-3.

#### FR-15: Export FHIR R4

Exportação R4 funcional para cohorts direcionadas e perfil BR. `[ASSUMPTION: R4 primário.]`

**Consequences (testable):**
- `./gradlew check` passa com testes R4.
- Amostra piloto passa validação HAPI.

#### FR-16: Metadados de proveniência

Metadados identificando Synthea-br, perfil, condição, versão.

**Consequences (testable):**
- Metadados em Bundle FHIR ou sidecar JSON documentado.

---

### 4.6 Trajetória Clínica Focada (Epic 9)

Cohorts enxutas e narrativamente coerentes para condição-alvo, ancoradas em referências longitudinais documentadas. Realiza UJ-4. `[ASSUMPTION: abordagens C, D e E complementares; OncoSynth/Coogee/SUS como ancoragem documental — sem ML/LLM como motor primário no MVP do épico.]`

#### FR-18: Spike e referências de trajetória longitudinal

Spike documental compara OncoSynth, Coogee e linhas SUS/DATASUS como fontes de ancoragem (fases, ordem, timings) para câncer de mama piloto. Realiza UJ-4.

**Consequences (testable):**
- ADR publicado com matriz do que entra no fork vs deferred; decisão C+D+E complementares.
- Spike mapeia fases assistenciais (rastreio → diagnóstico → estadiamento → tratamento → seguimento) com citações e limitações.
- Spike não exige GPU, API paga nem commit de datasets com PHI.

#### FR-19: Catálogo de fases de trajetória clínica

Catálogo versionado de fases por condição-alvo com allowlists de códigos e ordem canônica compartilhada por filtros, HTML e plausibilidade.

**Consequences (testable):**
- Data pack em `resources/br/pathways/` com `phase_id` estável, título PT-BR e `code_allowlist` por fase.
- Fases mínimas para câncer de mama: screening, diagnosis, staging, treatment, follow_up.
- Demografia e metadados de cohort marcados como `always_include`.
- API `PathwayCatalog` resolve por `br.target_condition` sem hardcode de códigos no Java.

#### FR-20: Export focado em trajetória (abordagem C)

Flag `br.pathway.focus` restringe export CSV/FHIR a eventos da trajetória (+ demografia), preservando `HealthRecord` completo na simulação.

**Consequences (testable):**
- `br.pathway.focus=false` (default) mantém export integral.
- Filtro read-only via `PathwayExportFilter` — não muta `HealthRecord`.
- `manifest.json` inclui `pathway_focus`, `pathway_catalog_version`, `pathway_condition`.
- Mesma seed + config + catálogo → mesmo output filtrado.

#### FR-21: Narrativa HTML por fase / modo orientador (abordagem C)

Export HTML agrupa timeline por fase clínica; modo orientador oculta ruído residual para validação com orientador.

**Consequences (testable):**
- `exporter.html.pathway_mode=orientador` (default quando `br.pathway.focus=true`) exibe apenas eventos da trajetória (+ demografia).
- Modos `pesquisador` (seção colapsável “Fora da trajetória”) e `full` (Epic 6) documentados.
- Fases e labels em PT-BR com perfil `br`; condição-alvo com destaque visual na timeline.
- Implementação read-only em `HtmlExporter` / templates FreeMarker.

#### FR-22: Perfil de geração enxuto (abordagem D)

`br.generation.module_profile=pathway_minimal` suprime módulos não essenciais quando `br.target_condition` está ativo.

**Consequences (testable):**
- Perfil `full` (default) mantém comportamento upstream inalterado.
- Lista allow/deny versionada em `resources/br/generation/module_profiles/pathway_minimal.json`.
- Gate de condição-alvo continua garantindo 100% da cohort (SM-1).
- Mesma seed + config + perfil → mesma cohort.

#### FR-23: Janela temporal de simulação (abordagem D)

`br.generation.simulation_window` restringe simulação à época relevante (pré-onset, diagnóstico, tratamento, seguimento).

**Consequences (testable):**
- ADR documenta impacto em demografia, seed e comorbidades antes de uso em produção.
- Simulação inicia em idade/janela documentada em vez de nascimento, preservando atributos demográficos coerentes.
- Combinações inválidas (janela incompatível com idade alvo) falham com erro claro na inicialização.
- Mesma seed + config → mesmo resultado; duração registrada no manifest ou metadata.

#### FR-24: Módulo GMF de trajetória episódica (abordagem E)

Módulo GMF BR modela jornada oncológica como trajetória principal (`br.generation.trajectory_mode=episodic`).

**Consequences (testable):**
- Fluxo episódico alinhado às fases do catálogo FR-19; compatível com GMF 2.0.
- Modo `lifespan` (default) preserva simulação upstream atual.
- States clonados por paciente — master module nunca mutado.
- Sequência piloto verificável: diagnóstico antes de procedimentos de tratamento.

#### FR-25: Calibração de timings a partir de referências externas

Priors temporais (intervalos entre fases) importáveis de referências documentadas, sem PHI.

**Consequences (testable):**
- Data pack `breast_cancer_timing_priors.json` com distribuições documentadas (min/max/median ou buckets).
- Fontes citadas: agregados SUS/DATASUS públicos; parâmetros OncoSynth/Coogee como metadado bibliográfico, não runtime ML.
- Nenhum arquivo no repositório contém PHI ou microdados de paciente real.
- `manifest.json` registra `pathway_timing_priors_version` e `pathway_reference_notes`.
- Alteração do data pack com mesma seed altera timings de forma determinística.

---

## 5. Cross-Cutting NFRs

- **Reprodutibilidade:** seed + config → output equivalente (SM-3).
- **Performance:** cohort 500 pacientes, perfil BR — ≤ 30 min em máquina 16 GB RAM `[ASSUMPTION]`.
- **Manutenibilidade:** respeitar `project-context.md`.
- **Observabilidade:** logs capturáveis para experimentos.
- **Ética:** dados 100% sintéticos; sem PHI no repositório.

---

## 6. Constraints and Guardrails

- **Privacidade:** apenas dados sintéticos e configs no repo.
- **Acadêmico:** workflow FR-11–14 obrigatório antes de publicação externa.
- **Upstream:** rebase periódico com `synthetichealth/synthea`; diffs localizados `[ASSUMPTION: mínimo por fase ou semestre]`.
- **Qualidade:** plausibilidade para pesquisa — não validade clínica real. SM-2 = maior rigor viável.
- **Publicação:** SBIS, CBIS; Methods reprodutíveis.
- **Monetização:** arquitetura não bloqueia modelo futuro; MVP não otimiza receita.

---

## 7. Non-Goals (Explicit)

- EHR ou sistema clínico de produção.
- Conformidade ANVISA/CFM para uso clínico real.
- GUI web em v1.
- Cobertura de todas doenças BR em v1.
- Billing SUS/TUSS completo no MVP.
- Monetização ou APIs LLM/GPU no MVP.

---

## 8. MVP Scope

### 8.1 In Scope

- Câncer de mama confirmado (FR-1, FR-2)
- Perfil `br` básico (FR-4, FR-5)
- Providers BR simplificados (FR-7)
- Plausibilidade v0 + regras piloto (FR-8, FR-9)
- Spike documental IA (FR-10)
- Workflow acadêmico (FR-11–14)
- Export FHIR R4 + metadados (FR-15, FR-16)

### 8.2 Out of Scope for MVP

- Múltiplas condições AND/OR (FR-3) — v2
- TUSS completo — v2
- GUI — v2+
- Pipeline IA em produção — pós-spike
- Monetização — PRD separado
- Cobertura geográfica BR completa — iterativo
- Zenodo automático — manual `[ASSUMPTION]`

---

## 9. Success Metrics

**Primary**

- **SM-1:** 100% pacientes da cohort piloto (câncer de mama, n=500, seed fixo) com condição verificável. Valida FR-2.
- **SM-2:** 0% violações severidade alta; ≤2% severidade média. Valida FR-8, FR-9.
- **SM-3:** Reexecução com manifest → checksum idêntico. Valida FR-13.

**Secondary**

- **SM-4:** Orientador reproduz experimento sem suporte oral. Valida FR-11, FR-14.
- **SM-5:** ADR do spike antes de expandir condições. Valida FR-10.
- **SM-6:** Methods preenchível a partir do template. Valida FR-11, FR-14.

**Epic 9 — Trajetória clínica focada**

- **SM-9.1:** ≥ 80% dos eventos exportados classificados como “trajetória” no modo focus (câncer de mama, n=10–50). Valida FR-19, FR-20.
- **SM-9.2:** ≥ 50% de redução de linhas CSV vs export full na mesma cohort. Valida FR-20.
- **SM-9.3:** Violações PLAUS-002 (ordem temporal) com modo episódico + calibração ≤ limiar SM-2 média. Valida FR-24, FR-25.
- **SM-9.4:** Redução documentada de tempo de geração (`pathway_minimal` + `simulation_window` vs baseline); não regredir NFR2 em n=500. Valida FR-22, FR-23.

**Counter-metrics**

- **SM-C1:** Não sacrificar plausibilidade por throughput.
- **SM-C2:** Não expandir catálogo de módulos à custa de qualidade.

---

## 10. Open Questions

| # | Questão | Impacto | Revisitar quando |
|---|---------|---------|------------------|
| 1 | Fonte CID-10 BR (DATASUS, WHO, subset manual) | FR-6 expansão | ADR antes de Fase 2 completa; subset piloto ok para MVP |
| 2 | Modelo monetização futura | Produto pós-MVP | Após MVP ou paper aceito |
| 3 | Frequência rebase upstream | Manutenção | ADR de manutenção na Fase 0 |

### Resolvido

| Decisão | Valor |
|---------|-------|
| Nome | **Synthea-br** |
| Condição piloto | **Câncer de mama** |
| Rigor | SM-2: 0% alta, ≤2% média |
| Infra IA | Sem GPU/API; MVP regras puras |
| Publicação | Artigos + conferências |
| Venues | **SBIS, CBIS** |
| Rebase | Sim, política confirmada |

---

## 11. Assumptions Index

- **A1:** CLI/config, sem GUI (§4.1)
- **A2:** Câncer de mama confirmado (§8.1)
- **A3:** Perfil BR = IBGE simplificado + UF + nomes (§4.2)
- **A4:** CID-10 subset piloto; fonte TBD (§10)
- **A5:** Providers = CSV simplificado (§4.2)
- **A6:** FHIR R4 primário (§4.5)
- **A7:** Performance ≤30 min / n=500 (§5)
- **A8:** Publicação dataset manual no MVP (§8.2)
- **A9:** Múltiplas condições fora do MVP (§4.1 FR-3)
- **A10:** Spike documental; MVP regras puras (§4.3)
- **A11:** Monetização fora do MVP (§1, §7)
- **A12:** Venues SBIS, CBIS (§10)
- **A13:** Rebase confirmado; frequência TBD (§6, §10)
- **A14:** Modelo monetização TBD (§10)
- **A15:** Fase 0 paralela a Fase 1 (§12)

---

## 12. Phasing

| Fase | Foco | FRs |
|------|------|-----|
| **0** | Workflow + spike | FR-10–14 |
| **1** | Cohort direcionada | FR-1, FR-2 |
| **2** | Localização BR | FR-4–7 |
| **3** | Plausibilidade | FR-8, FR-9 |
| **4** | Export + hardening | FR-15, FR-16 |

---

*PRD finalizado em 2026-06-30. Próximo passo recomendado: `bmad-architecture`.*
