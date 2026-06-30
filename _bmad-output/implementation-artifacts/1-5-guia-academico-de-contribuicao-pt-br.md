---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 1.5: Guia Acadêmico de Contribuição PT-BR

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como estudante iniciando no projeto Synthea-br,
quero um guia em português sobre instalação, execução, documentação de experimentos e uso ético,
para contribuir e publicar resultados sem depender de suporte oral contínuo do orientador.

## Acceptance Criteria

1. **Given** o estudante clonou o repositório Synthea-br
   **When** acessa `docs/CONTRIBUTING-ACADEMICO.md` (raiz de `docs/`, distinto de `docs/research/` usado pelas Stories 1.1-1.4)
   **Then** o guia contém seções: (a) Instalação e execução, (b) Documentação de experimento, (c) Citação do fork, (d) Disclaimer de dados sintéticos/uso ético
   [Source: planning-artifacts/epics.md#Story-1.5; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-14]

2. **Given** o guia cobre "Instalação e execução"
   **When** o estudante segue o passo a passo
   **Then** inclui pré-requisitos reais do projeto (Java 17, Gradle 9.2.1 via wrapper, `./gradlew check`, `./run_synthea [opções] [estado]`) e referencia onde encontrar as flags BR quando as Epics 2-3 estiverem implementadas (sem assumir que já existem hoje)
   [Source: _bmad-output/project-context.md#Technology-Stack; src/main/java/App.java#usage]

3. **Given** o guia cobre "Documentação de experimento"
   **When** o estudante quer registrar um experimento
   **Then** referencia diretamente `docs/research/experiments/experiment-template.md` (Story 1.2) e o fluxo de copiar `output/manifest.json` (Story 1.4) para dentro da pasta do experimento antes de commitar
   [Source: _bmad-output/implementation-artifacts/1-2-template-de-experimento-reproduzivel.md; 1-4-manifest-de-rastreabilidade-de-execucao.md]

4. **Given** o guia cobre "Citação do fork"
   **When** o estudante prepara um artigo (venues confirmados: SBIS, CBIS)
   **Then** inclui um formato de citação sugerido (nome do fork "Synthea-br", instituição PUCPR, ano, e nota de que DOI/Zenodo é manual/opcional no MVP) e instrui referenciar o `commit_sha` do manifest (Story 1.4) na seção Methods para rastreabilidade exata da versão usada
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§10 "Resolvido" — Venues SBIS, CBIS; addendum.md#Publicação-e-Monetização-Futura]

5. **Given** o guia cobre "Disclaimer de dados sintéticos / uso ético"
   **When** qualquer leitor (orientador, revisor de artigo, colega de outro grupo) consulta o guia
   **Then** declara explicitamente: (a) todos os dados são 100% sintéticos, sem PHI real, (b) o Synthea-br **não** é validado para uso clínico real (Non-Goal explícito do PRD), (c) a plausibilidade clínica mede coerência para fins de pesquisa, não validade clínica certificada
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§5 NFR-Ética, #§6 Constraints, #§7 Non-Goals]

6. **Given** o guia referencia ADRs e convenções de decisão
   **When** o estudante quer entender "por que" uma decisão foi tomada (ex.: por que não há IA no MVP)
   **Then** o guia aponta para `docs/research/adr/README.md` (Story 1.3) como fonte de verdade para decisões arquiteturais, em vez de duplicar o conteúdo dos ADRs
   [Source: _bmad-output/implementation-artifacts/1-3-registro-de-decisoes-arquiteturais-adrs.md]

7. **Given** a seção Methods de um artigo deve ser preenchível a partir do template + manifest (SM-6)
   **When** o estudante chega à fase de escrita do artigo
   **Then** o guia inclui um exemplo concreto (mini seção "Methods" redigida em português, citando seed, config_hash, commit_sha de um manifest de exemplo) demonstrando a transformação de `experiment.md` + `manifest.json` em texto de artigo
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#SM-6 "Methods preenchível a partir do template"]

8. **Given** o guia é o ponto de entrada para novos membros
   **When** o estudante não sabe por onde começar
   **Then** o guia inclui uma seção "Por onde começar" com um checklist sequencial mínimo: clonar → `./gradlew check` → ler ADRs (1.3) → copiar template de experimento (1.2) → rodar geração simples → gerar manifest (1.4) → preencher experimento
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#§2.3 UJ-3]

## Tasks / Subtasks

- [x] Task 1: Estruturar o guia (AC: #1, #8)
  - [x] Subtask 1.1: Criar `docs/CONTRIBUTING-ACADEMICO.md` com sumário e as 4 seções obrigatórias do FR-14
  - [x] Subtask 1.2: Adicionar seção "Por onde começar" com checklist sequencial

- [x] Task 2: Seção Instalação e Execução (AC: #2)
  - [x] Subtask 2.1: Documentar pré-requisitos (Java 17, Gradle wrapper) e comandos básicos (`./gradlew check`, `./run_synthea`)
  - [x] Subtask 2.2: Adicionar nota "Status das features BR" indicando que flags `br.target_condition`/`br.profile=br` dependem da conclusão das Epics 2/3 — evitar instruções que assumam funcionalidade inexistente no momento da leitura

- [x] Task 3: Seção Documentação de Experimento (AC: #3)
  - [x] Subtask 3.1: Referenciar `docs/research/experiments/experiment-template.md` e o fluxo de pasta datada (`YYYY-MM-DD-<slug>/`)
  - [x] Subtask 3.2: Documentar o passo manual de copiar `output/manifest.json` para a pasta do experimento (Story 1.4 gera automaticamente em `output/`, não versionado por padrão)

- [x] Task 4: Seção Citação do Fork (AC: #4)
  - [x] Subtask 4.1: Redigir formato de citação sugerido (estilo referência de software/dataset)
  - [x] Subtask 4.2: Instruir inclusão do `commit_sha` do manifest na seção Methods

- [x] Task 5: Seção Disclaimer Ético (AC: #5)
  - [x] Subtask 5.1: Redigir disclaimer de dados sintéticos e não-validade clínica, citando PRD §7 Non-Goals
  - [x] Subtask 5.2: Reforçar proibição de PHI real no repositório (NFR5)

- [x] Task 6: Exemplo de seção Methods (AC: #7)
  - [x] Subtask 6.1: Escrever exemplo ilustrativo curto em português usando dados fictícios de seed/config_hash/commit_sha (deixar claro que são ilustrativos, não de uma execução real, já que Epic 2-5 ainda não geram cohort BR real)

- [x] Task 7: Referências cruzadas (AC: #6)
  - [x] Subtask 7.1: Linkar `docs/research/adr/README.md` em vez de duplicar conteúdo de ADRs

## Dev Notes

### Natureza desta story — documental, fecha o Epic 1

Story de workflow acadêmico (FR-14), sem código de produção. É a última story do Epic 1 e depende **conceitualmente** das Stories 1.1-1.4 para ter conteúdo real para referenciar — mas, como todas resultam em arquivos versionados (não em features de runtime), esta story pode ser escrita mesmo que 1.1-1.4 ainda não tenham sido **implementadas** (apenas criadas como story files), desde que as referências apontem para os caminhos corretos definidos por elas. Se ao implementar esta story 1.1-1.4 ainda não estiverem com código/arquivos mergeados, o guia deve ser escrito da mesma forma (os caminhos são estáveis, definidos nas stories), mas o autor deve **verificar e ajustar links** caso algum caminho tenha mudado durante a implementação real.

### Inteligência das stories anteriores no mesmo épico

- **1.1:** convenção de ADR em `docs/research/adr/`, formato Contexto/Decisão/Consequências.
- **1.2:** `docs/research/experiments/experiment-template.md` + pasta datada `YYYY-MM-DD-<slug>/`; nota de que `manifest.json` era manual até a 1.4 existir.
- **1.3:** `docs/research/adr/README.md` com tabela de status; ADR-002 sobre rebase.
- **1.4:** `output/manifest.json` gerado automaticamente (`seed`, `config_hash`, `commit_sha`, `output_checksum`, `generated_at_iso8601`); precisa ser copiado manualmente para a pasta do experimento.

Esta story (1.5) é a "cola" narrativa entre todas — não deve recriar conteúdo, apenas guiar e referenciar.

### Risco de desalinhamento de localização

Note a diferença intencional de localização: `docs/CONTRIBUTING-ACADEMICO.md` fica na **raiz** de `docs/`, enquanto as Stories 1.1-1.4 produzem conteúdo em `docs/research/`. Isso é proposital — `CONTRIBUTING-ACADEMICO.md` é o ponto de entrada (estilo `CONTRIBUTING.md` de projetos open source), e `docs/research/` é o acervo de artefatos. Não mover ou consolidar essas pastas sem um ADR.

### Disclaimer ético — não diluir

O disclaimer (AC #5) não é boilerplate legal — é um requisito de NFR5 (Privacidade/ética) e dos Non-Goals explícitos do PRD (§7: "Conformidade ANVISA/CFM para uso clínico real" está fora de escopo). Garantir que o texto não dê a entender, mesmo implicitamente, que os dados poderiam ser usados em contexto clínico real.

### Project Structure Notes

```
docs/
  CONTRIBUTING-ACADEMICO.md      <- criar nesta story (raiz de docs/)
  research/                      <- já populada pelas Stories 1.1-1.4 (apenas referenciada aqui)
```

### Testing Standards Summary

Não aplicável a testes automatizados. Validação por revisão de conteúdo:
- As 4 seções obrigatórias do FR-14 estão presentes.
- Todos os links internos (`docs/research/...`) apontam para caminhos reais definidos pelas Stories 1.1-1.4.
- Disclaimer ético presente e sem ambiguidade sobre uso clínico real.
- Documento inteiramente em Português do Brasil.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-14, #§5, #§6, #§7, #SM-6, #§10]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Publicação-e-Monetização-Futura]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-1, #Story-1.5]
- [Source: _bmad-output/project-context.md#Technology-Stack]
- [Source: src/main/java/App.java#usage]
- [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md, 1-2-template-de-experimento-reproduzivel.md, 1-3-registro-de-decisoes-arquiteturais-adrs.md, 1-4-manifest-de-rastreabilidade-de-execucao.md]

## Dev Agent Record

### Agent Model Used

Claude (Amelia dev-story)

### Completion Notes List

- Guia PT-BR com instalação, experimentos, citação, disclaimer ético, checklist e exemplo de Methods.
- Referências cruzadas para ADRs, template de experimento e manifest.

### File List

- docs/CONTRIBUTING-ACADEMICO.md

### Change Log

- 2026-06-30: Story 1.5 implementada — guia acadêmico de contribuição PT-BR.
