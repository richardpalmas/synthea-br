---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 1.2: Template de Experimento Reprodutível

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como estudante ou pesquisador do grupo Synthea-br,
quero um template padronizado para documentar experimentos (hipótese, config, seed, comando, resultados, conclusão),
para que qualquer membro do grupo reproduza e cite o experimento em artigos sem depender de explicação oral.

## Acceptance Criteria

1. **Given** o repositório está clonado localmente e `docs/research/` existe (criado pela Story 1.1; se ainda não implementada, esta story cria a pasta)
   **When** o usuário acessa `docs/research/experiments/`
   **Then** existe `experiment-template.md` na raiz de `docs/research/experiments/` com seções: Hipótese, Configuração (seed, properties relevantes, comando exato), Data de execução, Resultados, Conclusão, Referências (FR/SM aplicáveis)
   [Source: planning-artifacts/epics.md#Story-1.2; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-11]

2. **Given** o template está definido
   **When** o pesquisador inicia um novo experimento
   **Then** a convenção de pastas segue `docs/research/experiments/YYYY-MM-DD-<slug>/` contendo `experiment.md` (cópia preenchida do template) e, quando aplicável, `manifest.json` (produzido pela Story 1.4 — ver Dev Notes sobre sequenciamento) e `outputs/` (saídas brutas, não versionadas via Git LFS ou `.gitignore`)
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Workflow-Acadêmico — "Estrutura de Pastas Proposta"]

3. **Given** o template existe
   **When** o conteúdo é revisado
   **Then** cada campo do template inclui instrução inline (comentário ou placeholder) explicando o que deve ser preenchido, para reduzir ambiguidade para estudantes sem experiência prévia em documentação de experimentos
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§2.3 UJ-3 "Bruno documenta experimento para reprodutibilidade"]

4. **Given** o template está versionado
   **When** o pesquisador busca por experimentos anteriores
   **Then** existe pelo menos um experimento piloto preenchido em `docs/research/experiments/<data>-<slug>/experiment.md` seguindo rigorosamente o template, com seed e comando reais e executáveis no estado atual do repositório
   [Source: planning-artifacts/epics.md#Story-1.2 AC2]

5. **Given** o experimento piloto preenchido referencia "comando" e "seed"
   **When** o conteúdo do piloto é avaliado quanto à viabilidade
   **Then** o piloto usa um comando Synthea **já executável hoje** (ex.: `./run_synthea -s <seed> -p <n> <Estado>`), pois as features BR (Epic 2-5) ainda não existem no código — o piloto não pode pressupor flags `br.target_condition` ou perfil `br` funcionais
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§12 Phasing — Fase 0 (workflow) executa antes/paralelo à Fase 1+ (cohort direcionada)]

6. **Given** o template referencia campos de rastreabilidade (seed, hash de config)
   **When** o template é usado antes da Story 1.4 estar implementada
   **Then** o template documenta explicitamente que o campo `manifest.json` é opcional/manual até a Story 1.4 entregar a geração automática, e inclui um exemplo de schema manual mínimo (`seed`, `config_hash`, `commit_sha`, `output_checksum`, `generated_at_iso8601`) para uso provisório
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-6; planning-artifacts/epics.md#Story-1.4]

7. **Given** o template é referenciado pelo guia acadêmico (Story 1.5, ainda não criada)
   **When** esta story é concluída
   **Then** o template e o experimento piloto usam nomenclatura e caminhos estáveis (`docs/research/experiments/experiment-template.md`) para que a Story 1.5 possa referenciá-los sem necessidade de retrabalho
   [Source: planning-artifacts/epics.md#Story-1.5 AC3 "Methods de um rascunho pode ser preenchida a partir do template + manifest (SM-6)"]

## Tasks / Subtasks

- [x] Task 1: Garantir estrutura de pastas base (AC: #1, #2)
  - [x] Subtask 1.1: Verificar se `docs/research/experiments/` existe (criada pela Story 1.1); criar se ausente (idempotente — não falhar se já existir)
  - [x] Subtask 1.2: Documentar a convenção `docs/research/experiments/YYYY-MM-DD-<slug>/` em comentário no topo do template ou em `docs/research/experiments/README.md` (criar este README nesta story, pois a Story 1.1 não o cria)

- [x] Task 2: Criar `experiment-template.md` (AC: #1, #3, #6)
  - [x] Subtask 2.1: Seção "Hipótese" — o que se espera observar/provar
  - [x] Subtask 2.2: Seção "Configuração" — seed, propriedades relevantes de `synthea.properties` alteradas, perfil (`br`/default), condição alvo se aplicável
  - [x] Subtask 2.3: Seção "Comando executado" — comando exato copiável (bloco de código)
  - [x] Subtask 2.4: Seção "Data de execução" — ISO-8601
  - [x] Subtask 2.5: Seção "Resultados" — métricas observadas, caminho para `outputs/` (se aplicável), link para `manifest.json` quando existir
  - [x] Subtask 2.6: Seção "Conclusão" — confirma/refuta hipótese, próximos passos
  - [x] Subtask 2.7: Seção "Referências FR/SM" — lista FRs e Success Metrics do PRD relacionados ao experimento
  - [x] Subtask 2.8: Adicionar nota inline explicando que `manifest.json` é manual até a Story 1.4 (AC #6) com exemplo de schema mínimo em bloco de código

- [x] Task 3: Criar experimento piloto preenchido (AC: #4, #5)
  - [x] Subtask 3.1: Escolher um comando Synthea já funcional hoje (sem dependências de Epic 2-5) — ex.: geração padrão de N pacientes com seed fixo em um estado dos EUA (upstream), usado apenas para validar a mecânica do template e da reprodutibilidade (não é um experimento científico do escopo Synthea-br ainda)
  - [x] Subtask 3.2: Executar o comando localmente (ou documentar como seria executado, se a execução não for viável no ambiente desta tarefa) e preencher o template com resultado real ou claramente identificado como ilustrativo
  - [x] Subtask 3.3: Criar pasta `docs/research/experiments/2026-06-30-piloto-template-reprodutibilidade/` com `experiment.md` preenchido
  - [x] Subtask 3.4: Deixar claro no campo "Conclusão" que este piloto valida o mecanismo de documentação, não uma hipótese clínica/BR (que dependerá de Epic 2-5)

- [x] Task 4: Atualizar índice/README de experimentos (AC: #2, #7)
  - [x] Subtask 4.1: Criar/atualizar `docs/research/experiments/README.md` listando o template e o experimento piloto, com instrução de uso (copiar template, criar pasta datada, preencher)

## Dev Notes

### Natureza desta story — documental

Story de workflow acadêmico (FR-11), sem código de produção Java. Toda a saída é Markdown em `docs/research/`. Não se aplica `./gradlew check`/Checkstyle/JaCoCo.

### Inteligência da Story 1.1 (anterior no mesmo épico)

A Story 1.1 (`1-1-spike-de-viabilidade-ia-vs-regras-puras.md`, status atual `ready-for-dev`, ainda não implementada) estabelece:
- `docs/research/adr/` e `docs/research/adr/README.md` (índice de ADRs).
- `docs/research/experiments/` deve ser criada **vazia** pela Story 1.1, "usada pela Story 1.2" — ou seja, esta story (1.2) é a dona do conteúdo dessa pasta.
- Convenção de nomenclatura `ADR-NNN-<titulo-kebab-case>.md` (não diretamente aplicável aqui, mas mantém consistência de estilo Markdown entre as stories do Epic 1).

**Risco de sequenciamento:** se a Story 1.1 ainda não tiver sido implementada quando esta story for executada, `docs/research/` pode não existir. Esta story deve ser **idempotente**: criar toda a árvore de pastas necessária (`docs/research/experiments/`) sem assumir que 1.1 já rodou, e sem sobrescrever/conflitar com o que a 1.1 criará depois (mesma convenção de nomenclatura kebab-case e mesmo diretório-raiz `docs/research/`).

### Dependência crítica a não ignorar — piloto não pode simular features inexistentes

Nenhuma feature de Epic 2-5 (condição alvo, perfil `br`, plausibilidade, export com proveniência) existe em código neste ponto do roadmap (Epic 1 é Fase 0, PRD §12). O experimento piloto desta story **deve usar apenas funcionalidade já existente no Synthea upstream** (ex.: geração padrão com seed fixo) — citar isso explicitamente no próprio arquivo do experimento para não induzir confusão futura sobre "quando isso passou a funcionar". Quando a Epic 2 estiver implementada, um **novo** experimento (não esta story) deve documentar a primeira cohort BR real.

### Dependência futura com Story 1.4 (manifest)

A Story 1.4 (ainda `backlog`) implementará a geração automática de `manifest.json` (FR-13, AD-6). Como ela ainda não existe como código:
- O template deve referenciar o campo `manifest.json` como **opcional/manual por enquanto**.
- O experimento piloto desta story pode incluir um `manifest.json` **escrito manualmente** como exemplo do schema esperado (não gerado por automação), deixando claro que é um exemplo, não um manifest real validado.
- Ao implementar a Story 1.4, os autores devem revisar se o template precisa de ajuste fino (ex.: caminho exato do manifest dentro da pasta do experimento).

### Estrutura de pastas (fonte: Addendum)

```
docs/research/
  experiments/
    README.md                                    <- criar nesta story
    experiment-template.md                        <- criar nesta story
    2026-06-30-piloto-template-reprodutibilidade/
      experiment.md                                <- criar nesta story (piloto preenchido)
      manifest.json                                 <- opcional, exemplo manual (ver nota acima)
  adr/                                             <- criada pela Story 1.1
```
[Source: planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Workflow-Acadêmico]

### Project Structure Notes

- Não há conflito com a estrutura upstream do Synthea (que não usa `docs/research/`).
- Mantém a convenção de nomenclatura iniciada pela Story 1.1 (Markdown em `docs/research/`, prefixo de data ISO no formato `YYYY-MM-DD` para pastas de experimento).
- Esta é a primeira story a popular `docs/research/experiments/` com conteúdo — a Story 1.5 (guia acadêmico) referenciará este template e este piloto como exemplo, portanto os caminhos definidos aqui devem ser tratados como estáveis.

### Testing Standards Summary

Não aplicável a testes JUnit. Validação por revisão de conteúdo:
- Template existe com todas as seções exigidas e instruções inline.
- Experimento piloto preenchido com comando real e executável hoje (sem dependência de features futuras).
- Nenhuma referência a flags/perfis que ainda não existem como se já funcionassem.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-11]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§2.3 UJ-3]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§12 Phasing]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Workflow-Acadêmico]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-6]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-1, #Story-1.2, #Story-1.4, #Story-1.5]
- [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md — estrutura de pastas e convenções herdadas]

## Dev Agent Record

### Agent Model Used

Claude (Amelia dev-story)

### Completion Notes List

- Template `experiment-template.md` com todas as seções e instruções inline.
- Piloto `2026-06-30-piloto-template-reprodutibilidade` preenchido com comando upstream executável.
- README de experimentos criado.

### File List

- docs/research/experiments/README.md
- docs/research/experiments/experiment-template.md
- docs/research/experiments/2026-06-30-piloto-template-reprodutibilidade/experiment.md
- docs/research/experiments/2026-06-30-piloto-template-reprodutibilidade/manifest.json

### Change Log

- 2026-06-30: Story 1.2 implementada — template e piloto de experimento reprodutível.
