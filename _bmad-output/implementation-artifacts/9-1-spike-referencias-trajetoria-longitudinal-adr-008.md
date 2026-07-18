---
baseline_commit: c1247106c03fa57ace54d269af98c7833f4006a6
---

# Story 9.1: Spike — Referências de Trajetória Longitudinal + ADR-008

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador líder do Synthea-br,
quero um spike documental que compare OncoSynth, Coogee e linhas de cuidado SUS/DATASUS como **fontes de ancoragem** (fases, ordem, timings),
para decidir o que importar como data pack determinístico vs o que permanece referência bibliográfica no Epic 9.

## Acceptance Criteria

1. **Given** Epic 1 (processo de ADR, Story 1.3) concluído e pasta `docs/research/adr/` existente
   **When** o spike analisa as três famílias de referência (OncoSynth, Coogee, linhas SUS/DATASUS)
   **Then** `docs/research/adr/ADR-008-trajetoria-clinica-focada.md` é publicado com seções **contexto / decisão / consequências** e matriz explícita do que entra no fork vs o que fica deferred
   [Source: planning-artifacts/epics.md#Story-9.1; planning-artifacts/epics.md#FR18; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-12]

2. **Given** o ADR-008 está sendo redigido
   **When** a decisão arquitetural é formalizada
   **Then** o documento declara que as abordagens **C (filtro de export)**, **D (geração enxuta)** e **E (módulo GMF episódico)** são **complementares**, não mutuamente exclusivas, com tabela de escopo e onde cada uma atua no pipeline
   [Source: planning-artifacts/epics.md#Epic-9 — tabela Abordagem C/D/E]

3. **Given** câncer de mama é a condição piloto do Epic 9
   **When** o spike mapeia referências para o caso de uso
   **Then** o ADR documenta fases assistenciais mínimas: rastreio → diagnóstico → estadiamento → tratamento → seguimento, com citações e limitações explícitas por fonte (ex.: OncoSynth = estatística de sobrevida/tratamento, não prontuário FHIR; Coogee = padrão de auditoria narrativa; DATASUS/SUS = ordem macro de procedimentos)
   [Source: planning-artifacts/epics.md#Story-9.1 AC "mapeia para câncer de mama piloto"]

4. **Given** restrições do laboratório PUCPR (sem GPU, sem API paga, sem PHI no repositório)
   **When** o spike é executado
   **Then** o documento demonstra que **nenhuma etapa** depende de GPU, API paga, treinamento local ou commit de datasets com PHI — o spike é puramente documental/bibliográfico
   [Source: planning-artifacts/epics.md#Story-9.1 AC "não exige GPU"; planning-artifacts/epics.md#NFR5]

5. **Given** Story 9.8 depende de um formato de importação definido neste spike
   **When** o ADR-008 é concluído
   **Then** define formato-alvo de importação para priors temporais por fase (ex.: JSON com min/max/median ou buckets por transição de fase), **sem dados de paciente real**, com exemplo esquemático e versão inicial do schema
   [Source: planning-artifacts/epics.md#Story-9.1 AC "formato-alvo de importação para Story 9.8"]

6. **Given** o Epic 9 explicita fora de escopo substituir o `Generator` por OncoSynth/Coogee como motor primário
   **When** o ADR lista itens deferred
   **Then** registra explicitamente: motor OncoSynth/Coogee como runtime primário, treinamento de modelos de difusão, pipeline LLM não-determinístico como única fonte de coerência (conflita NFR1/ADR-001) — integrações ML/LLM futuras exigem ADR próprio pós-calibração SM-2
   [Source: planning-artifacts/epics.md#Epic-9 "Fora de escopo do Epic 9 (MVP)"]

7. **Given** o ADR é publicado
   **When** o pesquisador consulta o índice de ADRs
   **Then** `docs/research/adr/README.md` inclui entrada do ADR-008 com status (Proposto/Aceito) e link para o documento
   [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md — padrão de registro ADR-001]

8. **Given** esta story é pré-requisito de 9.2, 9.5, 9.7 e 9.8
   **When** o spike é concluído
   **Then** o ADR-008 está marcado como **Aceito** (ou equivalente acordado pelo grupo) antes de iniciar implementação das stories dependentes — stories downstream podem referenciar decisões do ADR sem ambiguidade
   [Source: planning-artifacts/epics.md#Story-9.1 Dependências "Bloqueia: 9.2, 9.5, 9.7, 9.8"]

## Tasks / Subtasks

- [x] Task 1: Preparar estrutura e revisão bibliográfica (AC: #1, #4)
  - [x] Subtask 1.1: Confirmar `docs/research/adr/` e README existentes (Story 1.3)
  - [x] Subtask 1.2: Levantar fontes primárias/secundárias sobre OncoSynth (modelagem estatística de cohorts oncológicas)
  - [x] Subtask 1.3: Levantar fontes sobre Coogee (auditoria/consistência narrativa de prontuários)
  - [x] Subtask 1.4: Levantar linhas de cuidado SUS/DATASUS e agregados públicos aplicáveis a câncer de mama (ordem macro de procedimentos, timings agregados)

- [x] Task 2: Análise comparativa e matriz fork vs deferred (AC: #2, #3, #6)
  - [x] Subtask 2.1: Documentar o que cada referência **pode** contribuir (fases, ordem, timings, validação narrativa)
  - [x] Subtask 2.2: Documentar limitações de cada referência (formato, determinismo, PHI, infraestrutura)
  - [x] Subtask 2.3: Produzir matriz "entra no fork" vs "referência bibliográfica only" vs "ADR futuro pós-SM-2"
  - [x] Subtask 2.4: Mapear fases assistenciais piloto (screening → diagnosis → staging → treatment → follow_up) com citações

- [x] Task 3: Definir formato de importação para Story 9.8 (AC: #5)
  - [x] Subtask 3.1: Especificar schema JSON esquemático de priors temporais por fase (campos, unidades, versionamento)
  - [x] Subtask 3.2: Incluir exemplo mínimo no ADR (sem dados reais de paciente)
  - [x] Subtask 3.3: Documentar como metadados bibliográficos (OncoSynth/Coogee) serão citados sem runtime ML

- [x] Task 4: Redigir e publicar ADR-008 (AC: #1, #2, #7, #8)
  - [x] Subtask 4.1: Criar `docs/research/adr/ADR-008-trajetoria-clinica-focada.md` (contexto / decisão / consequências)
  - [x] Subtask 4.2: Seção decisão: C+D+E complementares; sequenciamento recomendado 9.1→9.2→(9.3∥9.5)→9.4→9.6→9.7→9.8
  - [x] Subtask 4.3: Registrar ADR-008 em `docs/research/adr/README.md`
  - [x] Subtask 4.4: Obter aceite formal do grupo (status Aceito)

## Dev Notes

### Natureza desta story — documental, não código de produção

Esta story **não** cria classes em `org.mitre.synthea.br.pathway` nem altera exportadores ou `Generator`. Entrega exclusivamente o ADR-008 e artefatos documentais que desbloqueiam o Epic 9. Qualquer código escrito nesta story seria escopo creep.

### Abordagem arquitetural — fundação do Epic 9 (pré C/D/E)

O ADR-008 fixa a estratégia de três camadas complementares descritas no épico:

| Abordagem | Escopo | Onde atua |
|-----------|--------|-----------|
| **C — Filtro de export** | Dataset/HTML enxutos a partir do `HealthRecord` completo | `org.mitre.synthea.br.pathway` + exportadores (read-only, AD-2) |
| **D — Geração enxuta** | Menos ruído na origem (módulos suprimidos, janela temporal) | `Generator` + config `br.generation.*` |
| **E — Módulo GMF episódico** | Trajetória oncológica como simulação principal | `resources/modules/br/` + gate Epic 2 |

Stories 9.3–9.4 implementam **C**; 9.5–9.6 implementam **D**; 9.7 implementa **E**; 9.8 calibra timings com base no formato definido aqui.

### Dependências e bloqueios

- **Depende de:** Epic 1 (Story 1.3 — processo de ADR)
- **Bloqueia:** Stories 9.2, 9.5, 9.7, 9.8
- **Não depende de:** Epic 8 (MAI-DxO) — camada IA permanece opcional e complementar

### Properties / flags — nenhuma nesta story

Nenhuma property `br.pathway.*` ou `br.generation.*` é introduzida aqui. O ADR deve **nomear** as flags futuras para que stories 9.3+ implementem de forma consistente.

### Project Structure Notes

```
docs/research/adr/
  ADR-008-trajetoria-clinica-focada.md    <- entregável principal
  README.md                                <- entrada ADR-008
docs/research/experiments/                 <- referenciado por 9.8, não populado aqui
```

Pacote `org.mitre.synthea.br.pathway` será criado nas Stories 9.2+ — **fora do escopo desta story**.

### Testing Standards Summary

Não há `./gradlew check` nesta story. Validação = revisão por pares do ADR + checklist dos ACs documentais.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-9, #Story-9.1]
- [Source: _bmad-output/planning-artifacts/epics.md#FR18]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-3, #AD-7]
- [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md, 1-3-processo-de-adr-numerado.md]
- [Source: docs/research/adr/ADR-001-spike-ia-vs-regras.md — padrão de spike documental]

## Dev Agent Record

### Agent Model Used

Claude (Amelia — Dev Story workflow)

### Debug Log References

Nenhum — story documental, sem execução de `./gradlew check` (ver Testing Standards Summary).

### Completion Notes List

- ADR-008 publicado com seções contexto/decisão/consequências, matriz de escopo C/D/E,
  matriz de uso por cenário, e matriz "entra no fork vs deferred" (AC #1, #2, #6).
- Adicionada seção de mapeamento de fases assistenciais mínimas (rastreio → diagnóstico →
  estadiamento → tratamento → seguimento) com contribuição e limitação explícita por fonte
  (OncoSynth, Coogee, SUS/DATASUS) (AC #3).
- Adicionada seção explícita de conformidade com restrições do laboratório PUCPR (sem GPU,
  sem API paga, sem treinamento local, sem PHI) (AC #4).
- Adicionada seção "Formato-alvo de importação de priors temporais" com schema v1
  (`priors_version`, `condition`, `unit`, `reference_notes`, `transitions` com
  min/max/median ou buckets) e exemplo esquemático sem dados reais de paciente,
  desbloqueando a Story 9.8 (AC #5).
- `docs/research/adr/README.md` já continha entrada do ADR-008; status atualizado de
  "Proposto" para "Aceito" no ADR e no índice (AC #7, #8).
- Story marcada `review`; sprint-status.yaml atualizado (`epic-9` → in-progress,
  `9-1` → review).

### File List

- docs/research/adr/ADR-008-trajetoria-clinica-focada.md (novo conteúdo revisado/expandido)
- docs/research/adr/README.md (status ADR-008: Proposto → Aceito)
- _bmad-output/implementation-artifacts/9-1-spike-referencias-trajetoria-longitudinal-adr-008.md (este arquivo)
- _bmad-output/implementation-artifacts/sprint-status.yaml (epic-9 → in-progress; 9-1 → review)

## Change Log

- 2026-07-08: Story 9.1 implementada e movida para `review` — ADR-008 aceito com todas as
  seções exigidas pelos ACs (spike documental, sem código de produção).
- 2026-07-10: CR — patches ADR (citações, tensão OncoSynth, invariantes schema, sequenciamento, path PRD) → done.

### Senior Developer Review (AI)

**Date:** 2026-07-10 · **Outcome:** approve → done
