# Análise comparativa — PDF externo vs estado atual do Synthea-br

**Data:** 2026-07-10  
**Base de comparação:** `analise_synthea_llm_e_benchmark_nhs.pdf` (HAILAB) vs implementação atual do repositório Synthea-br.

---

## 1) Resumo executivo

O diagnóstico externo e a arquitetura atual do Synthea-br convergem no ponto central: **abordagem Synthea-first híbrida**.

- O motor determinístico (GMF + export estruturado) permanece fonte de verdade.
- LLM fica como camada opcional de narrativa/augmentação, não motor primário de trajetória.
- Coorte por doença exige **contrato de fenótipo** (gate/keep), não apenas seleção de módulo.

Em termos práticos, o Synthea-br já implementa boa parte dos fundamentos recomendados pelo PDF. As lacunas mais relevantes estão na camada de orquestração de produto: **perfil declarativo versionado**, **quotas/estratos**, **calibração epidemiológica quantitativa** e **verificação factual robusta da camada de IA**.

---

## 2) Convergências fortes (o que já está alinhado)

## 2.1 Coorte não é `-m`

O PDF reforça que `-m` não garante caso clínico e cita o contraexemplo NHS (hipertensão ~51,2%).  
No Synthea-br, a direção já foi corrigida:

- uso de `br.target_condition` + keep/gate
- perfil `pathway_minimal` com observação explícita de que substitui uso ad hoc de `-m`

**Insight:** essa decisão reduz risco metodológico e aproxima o fork de uma arquitetura de coorte reproduzível.

## 2.2 Gate e fenótipo mínimo operacional

O PDF recomenda keep module + contrato de fenótipo.  
O Synthea-br já possui:

- `keep_modules/br/breast_cancer.json`
- `gate_mode` (`retry` e `exclude`)
- testes de integração para conformidade da coorte

**Insight:** para câncer de mama piloto, a fundação de “100% casos conformes” está tecnicamente presente.

## 2.3 Estratégia de fork (anti-poda)

O PDF critica o modelo NHS por poda massiva do núcleo e remoção de testes.  
O Synthea-br segue estratégia de overlay:

- perfil BR por feature flag (`br.profile=br`)
- data packs BR em `src/main/resources/br/...`
- manutenção de suíte de testes

**Insight:** maior chance de sustentabilidade e rebase contínuo com upstream.

## 2.4 Híbrido com IA subordinada ao estruturado

No PDF, “LLM puro” não é recomendado como motor.  
No Synthea-br, ADRs já apontam para:

- baseline determinístico por regras
- IA opcional pós-geração
- uso de LLM sem substituir o motor longitudinal

**Insight:** alinhamento conceitual maduro com literatura e benchmark NHS.

## 2.5 Proveniência e rastreabilidade

O PDF exige manifest por lote (seed, commit, hash etc.).  
No Synthea-br isso já existe com `manifest.json` e campos de proveniência.

**Insight:** excelente base para ciência reprodutível e auditoria acadêmica.

---

## 3) Gaps reais frente ao plano proposto no PDF

## 3.1 Perfil de coorte declarativo versionado (falta)

O PDF propõe perfil formal (ex.: `oncology.lung.nsclc.v1`) com fenótipo, estratos, validação e provenance no mesmo artefato.  
No Synthea-br, os parâmetros ainda estão dispersos entre properties, UI, flags e data packs.

**Impacto:** dificulta escalar para múltiplas doenças/subtipos mantendo contrato único e auditável.

## 3.2 Quotas e estratos epidemiológicos (falta)

Ainda não há engine explícito de quotas por faixa etária, estágio, subtipo, desfecho ou tratamento.

**Impacto:** a coorte pode ser 100% “caso” sem refletir composição epidemiológica desejada.

## 3.3 Fenótipo avançado por subtipo/estágio (parcial)

No piloto atual, o gate de mama está centrado no caso principal (SNOMED alvo).  
Ainda falta contrato mais rico para:

- subtipo molecular
- regras de estágio
- janela temporal clínica por critério de aceite

**Impacto:** limita precisão para estudos de subcoortes clínicas.

## 3.4 Calibração quantitativa com tolerâncias (início)

Há priors e catálogos para trajetória, mas ainda não um loop completo de calibração epidemiológica com metas numéricas e tolerâncias formais.

**Impacto:** forte para plausibilidade qualitativa; parcial para validação estatística robusta.

## 3.5 Verificador factual para narrativa LLM (falta)

A camada de IA já existe de forma opcional, porém ainda sem mecanismo completo de “cada afirmação rastreável ao FHIR”.

**Impacto:** risco de alucinação clínica em uso narrativo avançado.

---

## 4) Riscos e oportunidades

## 4.1 Riscos se manter como está

- crescimento de features sem contrato unificado de perfil de coorte
- variabilidade de resultados entre runs por configuração distribuída
- dificuldade de reproduzir exatamente estudos multi-doença/subtipo
- narrativa IA evoluir mais rápido que validação factual

## 4.2 Oportunidades imediatas

- transformar o conhecimento atual em **catálogo de perfis versionados**
- consolidar gate + trajetória + simulação em um único artefato de execução
- criar trilha de qualidade com critérios de aceite por doença

---

## 5) Backlog recomendado (priorizado)

## P0 — Fundação de produto de coorte

1. Definir schema de **Perfil de Coorte v1** (YAML/JSON):
   - id/version
   - condição/subtipo
   - gate/fenótipo
   - parâmetros de geração
   - export
   - critérios de validação
   - proveniência
2. Implementar parser/runner desse perfil (CLI e web).
3. Gerar perfil piloto: `oncology.breast.default.v1`.

## P1 — Qualidade epidemiológica e clínica

1. Adicionar **quotas/estratos** por idade/sexo/estágio.
2. Definir métricas com tolerâncias (incidência, estágio, tratamento, desfecho).
3. Expandir fenótipo para subtipos e critérios temporais explícitos.

## P2 — IA segura e auditável

1. Criar verificador factual FHIR→narrativa (assertions suportadas).
2. Marcar explicitamente texto inferido vs texto comprovado.
3. Integrar validação factual no pipeline de aceitação.

---

## 6) Critérios de aceite sugeridos para próximo marco

- 100% dos pacientes aceitos satisfazem fenótipo do perfil.
- 100% dos bundles FHIR válidos no validador adotado.
- 0 violações críticas de cronologia no catálogo de plausibilidade.
- Reprodutibilidade por `(commit + perfil + seed)` com saída lógica estável.
- Manifest completo em 100% dos lotes.
- (Se IA ativa) 0 fatos críticos não suportados por FHIR na amostra auditada.

---

## 7) Conclusão

O Synthea-br está **bem posicionado** em relação ao benchmark e às recomendações do PDF: acertou a espinha dorsal (determinismo, gate, proveniência, estratégia de fork).  
O próximo salto não é “trocar de motor”, e sim **industrializar a camada de coorte**: perfil declarativo versionado, quotas, calibração formal e validação factual da narrativa.

Essa evolução preserva o que já funciona e reduz risco metodológico para escalar de um piloto de câncer de mama para um catálogo multi-doença com qualidade acadêmica.
