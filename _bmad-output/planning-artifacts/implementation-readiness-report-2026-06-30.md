---
stepsCompleted:
  - step-01-document-discovery
  - step-02-prd-analysis
  - step-03-epic-coverage-validation
  - step-04-ux-alignment
  - step-05-epic-quality-review
  - step-06-final-assessment
project_name: synthea
assessment_date: 2026-06-30
documentsIncluded:
  prd: prds/prd-synthea-2026-06-29/prd.md
  prd_addendum: prds/prd-synthea-2026-06-29/addendum.md
  architecture: architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md
  epics: epics.md
  ux: null
  ux_note: "Aplicação CLI/terminal como Synthea upstream — sem artefato UX separado"
  project_context: ../project-context.md
---

# Implementation Readiness Assessment Report

**Date:** 2026-06-30
**Project:** synthea

---

## Document Inventory

### PRD Documents

**Whole Documents:** Nenhum na raiz

**Bundle Documents:**
- Pasta: `prds/prd-synthea-2026-06-29/`
  - `prd.md` (14.435 bytes, 2026-06-30)
  - `addendum.md` (4.293 bytes, 2026-06-30) — **incluído na análise**
  - `review-rubric.md`, `.memlog.md` — artefatos de apoio (não incluídos na análise principal)

**Seleção confirmada:** `prd.md` + `addendum.md`

### Architecture Documents

**Whole Documents:** Nenhum na raiz

**Bundle Documents:**
- Pasta: `architecture/architecture-synthea-2026-06-30/`
  - `ARCHITECTURE-SPINE.md` (7.594 bytes, 2026-06-30)

**Seleção confirmada:** `ARCHITECTURE-SPINE.md`

### Epics & Stories Documents

**Whole Documents:**
- `epics.md` (17.687 bytes, 2026-06-30)

**Seleção confirmada:** `epics.md`

### UX Design Documents

**Status:** Ausente — **aceito pelo usuário**

**Justificativa:** Aplicação opera via terminal/CLI tal como o Synthea upstream; não há GUI web em v1 (Non-Goal explícito no PRD §7).

### Duplicatas

Nenhuma duplicata whole vs. shardada identificada.

---

## PRD Analysis

### Functional Requirements

FR-1: Especificar condição clínica alvo — Pesquisador declara condição (nome ou ID de módulo GMF) via config ou CLI. Sistema rejeita condição inexistente com mensagem identificável. Documentação lista condições do MVP (mínimo: câncer de mama).

FR-2: Gerar cohort com condição garantida — População com percentual configurável (default 100%) apresenta condição ao fim da simulação. Seed e config idênticos → percentual dentro de tolerância (±0% quando 100%). Pacientes fora da condição excluíveis via flag de exportação.

FR-3: Compor múltiplas condições — **[NON-GOAL for MVP]** Fora do escopo v1.

FR-4: Perfil demográfico brasileiro — Distribuições BR (sexo, idade, raça/cor IBGE) quando perfil `br` ativo. Amostra N≥1000 com perfil `br` mais próxima de referência BR documentada que default EUA. Perfil selecionável via `synthea.properties` ou CLI.

FR-5: Geografia BR — Endereços consistentes com divisão administrativa brasileira. 100% endereços com UF válida e formato postal BR. Coordenadas dentro do estado declarado.

FR-6: Nomenclatura clínica brasileira — Códigos clínicos BR no MVP (subset CID-10 para câncer de mama). Condição piloto em FHIR referencia sistemas/códigos documentados. Mapeamento EU→BR documentado para câncer de mama.

FR-7: Providers BR — Encounters atribuídos a providers do contexto assistencial BR (CSV BR simplificado UBS/hospital genérico). Providers nos exports pertencem ao dataset BR. Nenhum provider EUA com perfil `br` ativo.

FR-8: Catálogo de regras de plausibilidade — Regras versionadas (JSON/YAML) com IDs estáveis. Regras versionadas no repositório. Cada regra referenciável em relatórios.

FR-9: Relatório de validação pós-geração — Relatório com violações por paciente e agregados. Gerado via comando documentado (`./gradlew` task ou CLI). Saída com contagem, severidade e exemplos anonimizados.

FR-10: Spike IA (documental, sem infra no lab) — Avalia se SM-2 é atingível com regras puras; revisão bibliográfica sobre IA futura. ADR com métricas, lacunas e recomendação IA in/out/futuro. MVP implementa regras puras salvo ADR contrário. Spike não depende de GPU, API paga ou treinamento local.

FR-11: Template de experimento reprodutível — Template Markdown: hipótese, config, seed, comando, resultados, conclusão, refs FR/SM. Template em `docs/research/`. Pelo menos um experimento piloto preenchido.

FR-12: ADRs — Decisões significativas registradas como ADRs numerados. Formato consistente (contexto, decisão, consequências).

FR-13: Rastreabilidade config → output — Manifest com hash de config, seed, versão, commit git, checksum. Reexecução com manifest reproduz output equivalente.

FR-14: Guia de contribuição acadêmica — Guia PT-BR: rodar, documentar, citar, disclaimer ético. Guia no repositório com disclaimer de dados sintéticos.

FR-15: Export FHIR R4 — Exportação R4 funcional para cohorts direcionadas e perfil BR. `./gradlew check` passa com testes R4. Amostra piloto passa validação HAPI.

FR-16: Metadados de proveniência — Metadados identificando Synthea-br, perfil, condição, versão. Metadados em Bundle FHIR ou sidecar JSON documentado.

**Total FRs:** 16 (14 in-scope MVP; FR-3 explicitamente fora do MVP)

### Non-Functional Requirements

NFR-1 (Reprodutibilidade): seed + config → output equivalente (SM-3).

NFR-2 (Performance): cohort 500 pacientes, perfil BR — ≤ 30 min em máquina 16 GB RAM [ASSUMPTION].

NFR-3 (Manutenibilidade): respeitar `project-context.md`.

NFR-4 (Observabilidade): logs capturáveis para experimentos.

NFR-5 (Ética): dados 100% sintéticos; sem PHI no repositório.

**Total NFRs:** 5

### Additional Requirements

**Constraints and Guardrails (§6):**
- Privacidade: apenas dados sintéticos e configs no repo.
- Acadêmico: workflow FR-11–14 obrigatório antes de publicação externa.
- Upstream: rebase periódico com `synthetichealth/synthea`; diffs localizados.
- Qualidade: plausibilidade para pesquisa — não validade clínica real. SM-2 = maior rigor viável.
- Publicação: SBIS, CBIS; Methods reprodutíveis.
- Monetização: arquitetura não bloqueia modelo futuro; MVP não otimiza receita.

**Success Metrics (SM-1 a SM-6 + counter-metrics):**
- SM-1: 100% pacientes da cohort piloto (câncer de mama, n=500, seed fixo) com condição verificável.
- SM-2: 0% violações severidade alta; ≤2% severidade média.
- SM-3: Reexecução com manifest → checksum idêntico.
- SM-4: Orientador reproduz experimento sem suporte oral.
- SM-5: ADR do spike antes de expandir condições.
- SM-6: Methods preenchível a partir do template.
- SM-C1: Não sacrificar plausibilidade por throughput.
- SM-C2: Não expandir catálogo de módulos à custa de qualidade.

**Addendum — Requisitos técnicos complementares:**
- Spike IA: MVP = regras puras (Abordagem A); B e C apenas futuro.
- SM-2 operacional: Alta 0%, Média ≤2%, Baixa documentada.
- Localização BR em 5 camadas: demografia, geografia, nomenclatura, providers/payers, nomes.
- Compatibilidade upstream: diffs em `src/main/resources/`, pacotes `org.mitre.synthea.br.*` ou `org.mitre.synthea.pucpr.*`, properties `synthea-br.*` ou `br.*`.
- Estrutura de pastas proposta para workflow acadêmico em `docs/research/`.

**Open Questions (§10):**
- Fonte CID-10 BR (DATASUS, WHO, subset manual) — TBD antes de Fase 2 completa.
- Modelo monetização futura — pós-MVP.
- Frequência rebase upstream — ADR de manutenção na Fase 0.

**Phasing (§12):**
- Fase 0: FR-10–14 (Workflow + spike)
- Fase 1: FR-1, FR-2 (Cohort direcionada)
- Fase 2: FR-4–7 (Localização BR)
- Fase 3: FR-8, FR-9 (Plausibilidade)
- Fase 4: FR-15, FR-16 (Export + hardening)

### PRD Completeness Assessment

O PRD está **completo e bem estruturado** para iniciar implementação:
- 16 FRs numerados com consequências testáveis.
- 5 NFRs cross-cutting claros.
- MVP scope delimitado (câncer de mama, perfil BR básico, CLI-only).
- Non-goals explícitos (GUI, TUSS completo, múltiplas condições, IA em produção).
- Métricas de sucesso rastreáveis aos FRs.
- Addendum técnico complementa sem duplicar requisitos.
- Questões abertas identificadas com impacto e timing de resolução.
- **Gap menor:** FR-6 depende de decisão CID-10 (TBD) — subset piloto aceito para MVP.

---

## Epic Coverage Validation

### Epic FR Coverage Extracted

| FR | Epic | Story(s) |
|----|------|----------|
| FR-1 | Epic 2 | 2.1 |
| FR-2 | Epic 2 | 2.2, 2.3 |
| FR-3 | Não-MVP | — (v2 explícito) |
| FR-4 | Epic 3 | 3.1 |
| FR-5 | Epic 3 | 3.2 |
| FR-6 | Epic 3 | 3.3 |
| FR-7 | Epic 3 | 3.4 |
| FR-8 | Epic 4 | 4.1 |
| FR-9 | Epic 4 | 4.2 |
| FR-10 | Epic 1 | 1.1 |
| FR-11 | Epic 1 | 1.2 |
| FR-12 | Epic 1 | 1.3 |
| FR-13 | Epic 1 | 1.4 |
| FR-14 | Epic 1 | 1.5 |
| FR-15 | Epic 5 | 5.1 |
| FR-16 | Epic 5 | 5.2 |

**Total FRs in epics:** 16 (15 MVP + 1 non-MVP explícito)

### Coverage Matrix

| FR | PRD Requirement | Epic Coverage | Status |
|----|-----------------|---------------|--------|
| FR-1 | Especificar condição clínica alvo via config/CLI | Epic 2 Story 2.1 | ✓ Covered |
| FR-2 | Gerar cohort com condição garantida | Epic 2 Stories 2.2, 2.3 | ✓ Covered |
| FR-3 | Compor múltiplas condições | Marcado não-MVP v2 | ✓ Out of scope |
| FR-4 | Perfil demográfico brasileiro | Epic 3 Story 3.1 | ✓ Covered |
| FR-5 | Geografia BR | Epic 3 Story 3.2 | ✓ Covered |
| FR-6 | Nomenclatura clínica brasileira (CID-10) | Epic 3 Story 3.3 | ✓ Covered |
| FR-7 | Providers BR | Epic 3 Story 3.4 | ✓ Covered |
| FR-8 | Catálogo de regras de plausibilidade | Epic 4 Story 4.1 | ✓ Covered |
| FR-9 | Relatório de validação pós-geração | Epic 4 Story 4.2 | ✓ Covered |
| FR-10 | Spike IA documental | Epic 1 Story 1.1 | ✓ Covered |
| FR-11 | Template de experimento reprodutível | Epic 1 Story 1.2 | ✓ Covered |
| FR-12 | ADRs | Epic 1 Story 1.3 | ✓ Covered |
| FR-13 | Rastreabilidade config → output | Epic 1 Story 1.4 | ✓ Covered |
| FR-14 | Guia de contribuição acadêmica | Epic 1 Story 1.5 | ✓ Covered |
| FR-15 | Export FHIR R4 | Epic 5 Story 5.1 | ✓ Covered |
| FR-16 | Metadados de proveniência | Epic 5 Story 5.2 | ✓ Covered |

### Missing Requirements

**Nenhum FR MVP sem cobertura identificado.**

FR-3 está explicitamente marcado como não-MVP em PRD, épicos e stories — alinhado.

### Coverage Statistics

- **Total PRD FRs:** 16
- **FRs MVP in-scope:** 15 (excl. FR-3)
- **FRs covered in epics:** 15/15 MVP (100%)
- **Coverage percentage:** 100% (MVP)

**Observação:** Épicos incluem NFR6–NFR10 derivados da arquitetura (upstream, SM-2, FHIR, determinismo, publicação) — enriquecimento válido além dos 5 NFRs do PRD §5.

---

## UX Alignment Assessment

### UX Document Status

**Not Found** — confirmado pelo usuário e pelo PRD.

### Contexto Validado

- PRD §7 lista **GUI web em v1** como Non-Goal explícito.
- PRD §4.1 [A1]: v1 via config + CLI, sem GUI.
- User journeys (UJ-1 a UJ-3) operam via `./run_synthea`, `./gradlew`, inspeção de exports FHIR e documentação acadêmica.
- `epics.md` confirma: "Nenhum contrato UX encontrado para este ciclo."
- Arquitetura: pipeline CLI in-process (Modular Monolith) — sem componentes web/mobile.

### Alignment Issues

Nenhum. Ausência de UX é **consistente e intencional** entre PRD, Architecture e Epics.

### Warnings

Nenhum warning crítico. Interface = terminal/CLI como Synthea upstream — alinhado com expectativa do usuário.

**Recomendação menor:** Documentar explicitamente no `CONTRIBUTING-ACADEMICO.md` (Story 1.5) que não há GUI e que todos os fluxos são CLI — evita confusão de novos estudantes.

---

## Epic Quality Review

### Epic Structure Validation

| Epic | User Value | Independence | Status |
|------|-----------|--------------|--------|
| Epic 1 — Infraestrutura Acadêmica | ✓ Pesquisadores documentam/reproduzem experimentos | ✓ Standalone (Fase 0 PRD) | ✓ Pass |
| Epic 2 — Cohort Direcionada | ✓ Especificar condição e obter 100% cohort | ✓ Não requer Epic 3+ | ✓ Pass |
| Epic 3 — Contexto BR | ✓ Perfil brasileiro autêntico | ✓ Não requer Epic 4/5 | ✓ Pass |
| Epic 4 — Plausibilidade | ✓ Auditar qualidade clínica | ✓ Requer cohort (Epic 2), não Epic 5 | ✓ Pass |
| Epic 5 — Export FHIR | ✓ Export citável para publicação | ✓ Requer cohort, não Epic 4 | ✓ Pass |

**Ordem de épicos alinhada ao phasing PRD §12:** 0→1→2→3→4 ✓

### Story Quality

- **Formato BDD:** Todas as 14 stories usam Given/When/Then ✓
- **Critérios testáveis:** Referências a SM-1, SM-2, SM-3, NFRs e ADs ✓
- **Tamanho:** Stories focadas (1 FR ou sub-capacidade por story) ✓
- **Brownfield:** Não exige story de "starter template" — fork Synthea existente ✓

### Dependency Analysis

**Within-epic (sequência natural, sem forward refs proibidas):**
- Epic 2: 2.1 → 2.2 → 2.3 (sequencial lógico)
- Epic 4: 4.1 → 4.2 (catálogo antes de relatório)
- Epic 5: 5.1 → 5.2 (export antes de metadados)

**Cross-epic:** Nenhuma violação Epic N → Epic N+1. Epic 4 e 5 dependem de cohort gerada (Epic 2) — dependência backward válida.

### Quality Findings by Severity

#### 🔴 Critical Violations

Nenhuma.

#### 🟠 Major Issues

Nenhuma.

#### 🟡 Minor Concerns

1. **Epic 1 título** — "Infraestrutura Acadêmica" soa técnico; valor ao usuário está claro nas stories. Considerar renomear para "Workflow Acadêmico e Reprodutibilidade" (cosmético).

2. **Story 1.4 (Manifest)** — Integração com pipeline de geração. Pode ser implementada como hook no runner Synthea existente antes de Epic 2 estar completo; documentar essa estratégia na story.

3. **FR-6 / Story 3.3** — Fonte CID-10 TBD. Story já prevê ADR e subset piloto; registrar ADR na Fase 0/Epic 1 antes de expandir nomenclatura.

4. **Epic 1 Story 1.1 vs PRD Fase 0** — Spike documental pode concluir antes de regras piloto (Epic 4); ADR inicial pode ser preliminar e revisado após calibração SM-2 — aceitável se documentado.

### Best Practices Compliance Checklist

| Critério | E1 | E2 | E3 | E4 | E5 |
|----------|----|----|----|----|-----|
| Entrega valor ao usuário | ✓ | ✓ | ✓ | ✓ | ✓ |
| Independência de épicos futuros | ✓ | ✓ | ✓ | ✓ | ✓ |
| Stories dimensionadas | ✓ | ✓ | ✓ | ✓ | ✓ |
| Sem forward dependencies | ✓ | ✓ | ✓ | ✓ | ✓ |
| ACs claros e testáveis | ✓ | ✓ | ✓ | ✓ | ✓ |
| Rastreabilidade FR | ✓ | ✓ | ✓ | ✓ | ✓ |

---

## Summary and Recommendations

### Overall Readiness Status

**READY** — Os artefatos de planejamento estão alinhados e completos o suficiente para iniciar a Fase 4 (implementação).

### Critical Issues Requiring Immediate Action

Nenhum bloqueador crítico identificado.

### Recommended Next Steps

1. **Registrar ADR de fonte CID-10** (subset piloto câncer de mama) antes ou durante Story 3.3 — resolve open question §10 do PRD.
2. **Iniciar Epic 1 (Fase 0)** em paralelo com Epic 2 — conforme PRD §12 e assumption A15; spike + workflow acadêmico desbloqueiam publicação cedo.
3. **Documentar interface CLI-only** no guia acadêmico (Story 1.5) — reforça expectativa de terminal para novos contribuidores.
4. **Opcional:** Renomear Epic 1 para linguagem mais centrada no pesquisador (cosmético, não bloqueante).

### Assessment Summary

| Categoria | Resultado |
|-----------|-----------|
| Documentos | PRD + Addendum, Architecture, Epics confirmados; UX N/A (CLI) |
| Cobertura FR | 100% MVP (15/15) |
| Alinhamento UX | Consistente — sem gaps |
| Qualidade Épicos | Aprovado — 0 críticos, 4 menores |
| PRD Completeness | Completo com 1 open question gerenciável (CID-10) |

### Final Note

Esta avaliação identificou **0 issues críticos** e **4 concerns menores** em 4 categorias. O planejamento está pronto para implementação. Os pontos menores podem ser endereçados durante Epic 1 ou ignorados sem risco ao MVP.

---

**Assessor:** Implementation Readiness Workflow (BMAD)  
**Assessed for:** Boss  
**Report:** `_bmad-output/planning-artifacts/implementation-readiness-report-2026-06-30.md`
