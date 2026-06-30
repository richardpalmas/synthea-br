---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 1.3: Registro de Decisões Arquiteturais (ADRs)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como membro do grupo de pesquisa Synthea-br,
quero uma convenção e estrutura formal de ADRs (Architecture Decision Records) no repositório,
para que decisões significativas sejam consultáveis por orientandos novos sem repetir a discussão original.

## Acceptance Criteria

1. **Given** o repositório Synthea-br está configurado e `docs/research/adr/` existe (criada pela Story 1.1; idempotente se já existir)
   **When** um membro do grupo precisa registrar uma nova decisão significativa
   **Then** existe `docs/research/adr/adr-template.md` com seções obrigatórias **Contexto**, **Decisão**, **Consequências**, e instrução de numeração sequencial (`ADR-NNN-<titulo-em-kebab-case>.md`)
   [Source: planning-artifacts/epics.md#Story-1.3; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-12]

2. **Given** o template de ADR está definido
   **When** o membro segue o template para um novo ADR
   **Then** o número sequencial usado é o próximo disponível (continuação de `ADR-001`, criado pela Story 1.1) — nenhuma lacuna ou duplicação de número é permitida
   [Source: planning-artifacts/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md — "Este é o primeiro ADR do projeto (ADR-001)"]

3. **Given** `docs/research/adr/README.md` já existe com uma entrada mínima para `ADR-001` (criada pela Story 1.1)
   **When** esta story formaliza a convenção
   **Then** o README é **estendido** (não substituído) com: tabela de ADRs (número, título, status, data), legenda de status (`Proposto`, `Aceito`, `Rejeitado`, `Substituído`), e instrução de como adicionar um novo ADR
   [Source: planning-artifacts/epics.md#Story-1.3 AC3]

4. **Given** a convenção precisa ser demonstrada como reutilizável (não apenas documentada)
   **When** o grupo identifica uma decisão real pendente do PRD/Architecture Spine
   **Then** um segundo ADR real (`ADR-002-politica-de-rebase-upstream.md`) é criado, resolvendo a questão aberta de cadência de rebase com `synthetichealth/synthea` (PRD §10 Open Question #3; Architecture Spine §Deferred), registrando: versão/commit upstream de referência no momento da decisão, cadência escolhida (fim de fase do PRD ou semestre letivo, conforme addendum), e responsável a definir pelo grupo
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§10 Open-Questions item-3; planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Compatibilidade-Upstream — "Política de rebase (confirmada)"]

5. **Given** o ADR-002 é publicado
   **When** o conteúdo é revisado
   **Then** segue estritamente o formato Contexto/Decisão/Consequências e é registrado no README com status `Aceito`
   [Source: planning-artifacts/epics.md#Story-1.3 AC2]

6. **Given** múltiplos membros (estudantes, orientador) podem registrar ADRs futuramente
   **When** a convenção é consultada
   **Then** o `adr-template.md` e o `README.md` deixam explícito que ADRs cobrem decisões **arquiteturais ou de processo** (não apenas técnicas Java) — incluindo decisões de escopo, dados ou metodologia de pesquisa — para evitar que estudantes subutilizem o mecanismo
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-12 "Decisões significativas registradas como ADRs numerados"]

## Tasks / Subtasks

- [x] Task 1: Criar template formal de ADR (AC: #1, #6)
  - [x] Subtask 1.1: Criar `docs/research/adr/adr-template.md` com seções Contexto/Decisão/Consequências e placeholders comentados
  - [x] Subtask 1.2: Adicionar nota explícita de que ADRs cobrem decisões arquiteturais, de processo e de metodologia de pesquisa
  - [x] Subtask 1.3: Documentar a regra de numeração sequencial e formato de nome de arquivo

- [x] Task 2: Estender o README de ADRs (AC: #2, #3)
  - [x] Subtask 2.1: Ler `docs/research/adr/README.md` existente (criado pela Story 1.1); se ausente (1.1 ainda não implementada), criar do zero com a entrada de `ADR-001` antecipada a partir do conteúdo da própria Story 1.1
  - [x] Subtask 2.2: Adicionar tabela com colunas Número | Título | Status | Data | Link
  - [x] Subtask 2.3: Adicionar legenda de status (Proposto, Aceito, Rejeitado, Substituído)
  - [x] Subtask 2.4: Adicionar seção "Como adicionar um novo ADR" referenciando `adr-template.md`

- [x] Task 3: Redigir ADR-002 — Política de Rebase Upstream (AC: #4, #5)
  - [x] Subtask 3.1: Seção Contexto — necessidade de manter o fork alinhável a `synthetichealth/synthea`; risco de divergência estrutural; registrar commit/versão upstream de referência no momento da decisão (consultar `git log` do remote upstream se configurado, ou registrar a versão do Synthea core conforme Architecture Spine: `4.0.1-SNAPSHOT`)
  - [x] Subtask 3.2: Seção Decisão — cadência de rebase ao final de cada fase do PRD (Fases 0-4) ou a cada semestre letivo, o que ocorrer primeiro; responsável a ser nomeado pelo grupo (orientador ou maintainer do fork) — registrar como decisão aberta de governança, não bloqueante
  - [x] Subtask 3.3: Seção Consequências — positivas (reduz dívida de merge, mantém compatibilidade NFR-6), negativas (esforço periódico de revisão de conflitos, especialmente em `org.mitre.synthea.br.*` se algum dia houver overlap de nomes com upstream)
  - [x] Subtask 3.4: Registrar ADR-002 no README com status `Aceito`

- [x] Task 4: Validar consistência entre Story 1.1 e 1.3 (AC: #2, #3)
  - [x] Subtask 4.1: Confirmar que `ADR-001` (Story 1.1) não foi renumerado ou movido
  - [x] Subtask 4.2: Confirmar que o README final lista `ADR-001` e `ADR-002` sem lacunas

## Dev Notes

### Natureza desta story — documental

Story de workflow acadêmico (FR-12), sem código de produção Java. Não se aplica `./gradlew check`.

### Inteligência da Story 1.2 (anterior no mesmo épico)

A Story 1.2 trabalhou em `docs/research/experiments/` (não em `adr/`), então não há sobreposição direta de arquivos. Padrão de estilo herdado: Markdown com seções claras, placeholders comentados nos templates, README de índice por subpasta de `docs/research/`. Esta story segue o mesmo padrão para `docs/research/adr/`.

### Dependência crítica — não duplicar/sobrescrever trabalho da Story 1.1

A Story 1.1 (`1-1-spike-de-viabilidade-ia-vs-regras-puras.md`) **já cria** `docs/research/adr/README.md` (com entrada mínima de `ADR-001`) e `docs/research/adr/ADR-001-spike-ia-vs-regras.md`. Esta story **deve estender**, não recriar do zero, o README — caso contrário, se 1.1 já tiver sido implementada, esta story sobrescreveria a entrada de `ADR-001` e perderia o trabalho anterior. Se a Story 1.1 **ainda não** tiver sido implementada quando esta story (1.3) for executada, o dev desta story deve:
1. Criar a estrutura completa (`adr-template.md`, `README.md` com tabela vazia exceto pela entrada futura de `ADR-001`), e
2. Deixar uma nota clara de que a Story 1.1, ao ser implementada, deve **adicionar** sua entrada na tabela já existente, em vez de recriar o arquivo.

Recomenda-se fortemente implementar a Story 1.1 antes da 1.3 para evitar esse retrabalho, embora ambas sejam tecnicamente desacopladas o suficiente para serem feitas em qualquer ordem com a devida atenção.

### ADR-002 resolve uma lacuna real do PRD

O PRD (§10, Open Question #3) e a Architecture Spine (§Deferred) registram explicitamente a cadência de rebase como decisão pendente ("ADR de manutenção na Fase 0"). Esta story **não inventa** uma decisão nova — ela formaliza, via ADR, a recomendação já presente no addendum ("fim de cada fase do PRD ou semestre letivo"). Isso fecha o Open Question #3 do PRD oficialmente.

### Formato do ADR (obrigatório, reforça Story 1.1)

Contexto → Decisão → Consequências, numeração sequencial `ADR-NNN-<titulo-kebab-case>.md`.

### Project Structure Notes

```
docs/research/adr/
  README.md                          <- estender (criado pela Story 1.1)
  adr-template.md                    <- criar nesta story
  ADR-001-spike-ia-vs-regras.md      <- já existe (Story 1.1) — não tocar no conteúdo
  ADR-002-politica-de-rebase-upstream.md  <- criar nesta story
```
Não há conflito com a estrutura upstream do Synthea.

### Testing Standards Summary

Validação por revisão de conteúdo:
- Template existe com as 3 seções obrigatórias.
- README lista ambos os ADRs sem lacuna de numeração.
- ADR-002 segue formato e resolve explicitamente a Open Question #3 do PRD.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-12]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§10 Open-Questions]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Compatibilidade-Upstream]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#Deferred, #Stack]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-1, #Story-1.3]
- [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md — ADR-001 e README iniciais]

## Dev Agent Record

### Agent Model Used

Claude (Amelia dev-story)

### Completion Notes List

- Template `adr-template.md` e README estendido com legenda de status e instruções.
- ADR-002 publicado (política de rebase upstream), fechando Open Question #3 do PRD.

### File List

- docs/research/adr/README.md
- docs/research/adr/adr-template.md
- docs/research/adr/ADR-002-politica-de-rebase-upstream.md

### Change Log

- 2026-06-30: Story 1.3 implementada — convenção formal de ADRs e ADR-002.
