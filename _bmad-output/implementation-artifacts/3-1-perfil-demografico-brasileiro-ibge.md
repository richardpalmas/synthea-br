---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 3.1: Perfil Demográfico Brasileiro (IBGE)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero ativar um perfil `br` com distribuições demográficas brasileiras (sexo, idade, raça/cor),
para que a cohort gerada reflita o contexto epidemiológico nacional em vez do default dos EUA.

## Acceptance Criteria

1. **Given** esta é a primeira story do Epic 3 a ser implementada e nenhuma story anterior introduziu um flag de "perfil BR" genérico (a Story 2.1 introduziu apenas `br.target_condition`, específico de condição clínica, não um perfil amplo)
   **When** esta story é implementada
   **Then** ela introduz o flag fundacional `br.profile` (valores aceitos: `br` ou ausente/vazio = comportamento upstream) em `org.mitre.synthea.br.profile.BrProfile` (método estático `isActive()`), que será **reutilizado** pelas Stories 3.2, 3.3 e 3.4 — nenhuma delas deve reimplementar a checagem do flag
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-4 "Perfil selecionável via synthea.properties ou CLI"; planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-3, #AD-7]

2. **Given** data pack em `src/main/resources/br/demographics/` com distribuições IBGE simplificadas (nacional, não por município — fora de escopo MVP ter granularidade municipal)
   **When** o pesquisador ativa `br.profile=br` via `synthea.properties` ou `--br.profile=br` no CLI
   **Then** uma amostra com N≥1000 pacientes gerados apresenta distribuição etária e de sexo estatisticamente mais próxima da referência BR documentada (fonte: IBGE, citar no data pack) do que do default norte-americano (`geography/demographics.csv`)
   [Source: planning-artifacts/epics.md#Story-3.1; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-4]

3. **Given** dados demográficos não devem ser hardcoded no Java (AD-3)
   **When** o código é revisado
   **Then** todas as proporções de idade/sexo/raça-cor são carregadas de arquivo(s) CSV em `src/main/resources/br/demographics/` via um loader em `org.mitre.synthea.br.demographics.*` — nenhum literal numérico de distribuição populacional aparece em código Java
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-3]

4. **Given** a taxonomia de raça/cor do IBGE (branca, preta, parda, amarela, indígena) **não tem correspondência 1:1** com as categorias de raça já usadas internamente pelo engine (`white`, `black`, `asian`, `native`, `other`, mais a `ethnicity` separada `hispanic`/`nonhispanic`), e essas categorias internas já alimentam lógica de codificação OMB/US Core nos exportadores FHIR (`FhirR4.java`)
   **When** a distribuição de raça/cor BR é aplicada
   **Then** o mapeamento IBGE → categorias internas existentes é **documentado explicitamente** em um comentário no próprio data pack e citado no ADR sugerido (ver Dev Notes) — em particular, a categoria `parda` (maior categoria do IBGE, ~45% da população) **não tem equivalente direto** nas categorias US Census; a decisão de mapeamento (ex.: distribuir proporcionalmente entre `black`/`other`, ou introduzir um novo valor de categoria) é tratada como uma **decisão a confirmar via ADR**, não resolvida silenciosamente nesta story
   [Source: src/main/java/org/mitre/synthea/world/geography/Demographics.java#pickRace, #pickEthnicity — "Uses the US Census definition for race"; ambiguidade real identificada nesta análise, sem precedente no PRD/addendum]

5. **Given** o perfil `br` precisa coexistir com o mecanismo existente de seleção de cidade/estado (`Location`/`Demographics`, usado também pela Story 3.2 — geografia)
   **When** `br.profile=br` está ativo
   **Then** o ponto de integração escolhido nesta story (ver Dev Notes — recomendado: substituir as distribuições de idade/sexo/raça do objeto `Demographics` selecionado, em vez de reescrever toda a seleção de cidade) é documentado de forma clara o suficiente para que a Story 3.2 (geografia) o reaproveite sem inventar um segundo mecanismo concorrente
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#Capability-Architecture-Map — "Localização BR... resources/br/*, loaders org.mitre.synthea.br.*"]

6. **Given** perfil desativado deve manter comportamento upstream inalterado (AD-1, princípio de compatibilidade)
   **When** `br.profile` não está definido (default)
   **Then** a geração usa exatamente as distribuições US Census originais (`geography/demographics.csv`), sem nenhuma mudança de comportamento, performance ou output em relação ao Synthea upstream
   [Source: planning-artifacts/epics.md#Story-3.1 AC "perfil desativado mantém comportamento upstream inalterado"]

7. **Given** o teste estatístico de proximidade com a referência BR precisa ser objetivo
   **When** o teste automatizado é escrito
   **Then** usa um critério quantitativo simples e documentado (ex.: diferença absoluta entre proporção observada e proporção de referência, por faixa etária e por sexo, abaixo de um limiar definido — ex.: 5 pontos percentuais — para N≥1000 com seed fixa), evitando afirmações qualitativas não verificáveis
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-4 "Consequences (testable)"]

## Tasks / Subtasks

- [x] Task 1: Introduzir o flag fundacional `br.profile` (AC: #1, #6)
  - [x] Subtask 1.1: Criar `org.mitre.synthea.br.profile.BrProfile` com `public static boolean isActive()` lendo `Config.get("br.profile")` e comparando com `"br"` (case-insensitive)
  - [x] Subtask 1.2: Adicionar `br.profile` em `synthea.properties` com comentário explicando valores aceitos e que é o flag mestre consumido por todas as Stories 3.1-3.4
  - [x] Subtask 1.3: Javadoc explícito indicando que `BrProfile.isActive()` é o **único** ponto de checagem deste flag — qualquer nova feature BR deve reutilizá-lo

- [x] Task 2: Criar data pack de demografia BR (AC: #2, #3, #4)
  - [x] Subtask 2.1: Pesquisar/documentar fonte IBGE de referência para distribuição etária/sexo nacional simplificada (ex.: Censo IBGE ou PNAD, citar fonte exata no cabeçalho do CSV)
  - [x] Subtask 2.2: Criar `src/main/resources/br/demographics/distribuicao_nacional.csv` (ou estrutura equivalente) com colunas: faixa etária, sexo, fração; e raça/cor IBGE (branca/preta/parda/amarela/indígena) com fração
  - [x] Subtask 2.3: Documentar no cabeçalho do CSV (comentário) a fonte, ano de referência, e a limitação de granularidade nacional (não municipal) para o MVP

- [x] Task 3: Implementar loader BR (AC: #3)
  - [x] Subtask 3.1: Criar `org.mitre.synthea.br.demographics.BrDemographicsLoader` (ou nome equivalente) carregando o CSV via `SimpleCSV` (mesma classe utilitária já usada por `Provider`/`Demographics` upstream) — não introduzir Jackson/outra lib de CSV
  - [x] Subtask 3.2: Expor distribuições como estruturas reutilizáveis (ex.: `Map<String, Double>` por categoria), análogas ao formato já consumido por `RandomCollection`/`buildRandomCollectionFromMap` em `Demographics.java`

- [x] Task 4: Resolver mapeamento de raça/cor IBGE → categorias internas (AC: #4)
  - [x] Subtask 4.1: Documentar a tabela de mapeamento proposta (com a ambiguidade de `parda` explicitada) em comentário no loader e no data pack
  - [x] Subtask 4.2: Registrar a decisão como candidata a ADR (seguindo a convenção da Story 1.3) — não bloquear esta story por isso, mas não silenciar a ambiguidade
  - [x] Subtask 4.3: Implementar o mapeamento escolhido (documentado) de forma isolada (um método/classe dedicado), facilitando revisão e eventual correção futura sem espalhar a lógica

- [x] Task 5: Integrar ao fluxo de seleção demográfica (AC: #5, #6)
  - [x] Subtask 5.1: Identificar o ponto exato em `Generator.randomDemographics`/`Demographics.pickAge/pickGender/pickRace` onde a substituição deve ocorrer quando `BrProfile.isActive()`
  - [x] Subtask 5.2: Implementar a substituição de forma que, com o perfil desativado, o caminho de código original seja executado sem nenhuma alteração (early-return/branch condicional claro)
  - [x] Subtask 5.3: Documentar esse ponto de integração em comentário claro, citando que a Story 3.2 (geografia) reaproveitará o mesmo padrão de branch condicional sobre `BrProfile.isActive()`

- [x] Task 6: Testes (AC: #2, #6, #7)
  - [x] Subtask 6.1: Criar `src/test/java/org/mitre/synthea/br/demographics/BrDemographicsLoaderTest.java` — validar carregamento do CSV e soma de frações ≈ 1.0 por categoria
  - [x] Subtask 6.2: Criar teste de amostra estatística (N≥1000, seed fixa) comparando proporções observadas vs. referência BR documentada, com limiar definido (AC #7)
  - [x] Subtask 6.3: Teste de regressão — com `br.profile` ausente, a saída numérica de uma amostra é idêntica à amostra equivalente gerada antes desta story (snapshot/comparação determinística)
  - [x] Subtask 6.4: Rodar `./gradlew check`

## Dev Notes

### Esta story define a fundação do "perfil br" para todo o Epic 3 (e potencialmente além)

Nenhuma story anterior (Epic 1, Epic 2) introduziu um flag de perfil genérico — `br.target_condition` (Story 2.1) é específico de condição clínica. Esta story (3.1), sendo a primeira do Epic 3 a ser processada, assume a responsabilidade de criar `BrProfile.isActive()` como o flag mestre. **Isso é uma decisão de design desta tarefa de criação de stories**, feita porque nenhuma story anterior cobria essa necessidade — ao implementar, se outra story já tiver introduzido um mecanismo equivalente por algum motivo, consolidar em um único ponto, não duplicar.

### Risco real não resolvido pelo PRD — taxonomia de raça/cor IBGE vs. US Census

Esta é uma ambiguidade real e não documentada no PRD/addendum original (que cita apenas "raça/cor IBGE" sem detalhar o mapeamento). A classe `Demographics.java` já existente usa explicitamente "US Census definition for race" (Javadoc própria da classe) e essas categorias alimentam código de exportação FHIR (mapeamento para `us-core-race` ou códigos OMB em `FhirR4.java`). Substituir os **valores** das categorias por termos em português (`"parda"`, `"indígena"`) sem ajustar os exportadores quebraria a codificação FHIR existente. A recomendação desta story é **manter as chaves internas de raça já existentes** (`white`, `black`, `asian`, `native`, `other`) e apenas recalibrar as **proporções** (frequências) usando os dados do IBGE como inspiração/aproximação, documentando a tabela de correspondência aproximada — especialmente o tratamento de `parda`, que não tem equivalente direto. **Não inventar uma quinta categoria de raça nova sem avaliar o impacto em `FhirR4.java`/`FhirStu3.java`/`FhirDstu2.java`** — se o grupo decidir que isso é necessário, deve ser via ADR explícito (AD-7: alterações que tocam os três exportadores exigem justificativa).

### Coordenação obrigatória com a Story 3.2 (mesma área de código)

`Location.java` e `Demographics.java` são compartilhados entre a seleção de **demografia** (esta story) e a seleção de **geografia/endereço** (Story 3.2). Ambas as stories tocam a mesma vizinhança de código. Para evitar dois mecanismos concorrentes de "ativar BR":
- Esta story (3.1) estabelece o padrão: checagem de `BrProfile.isActive()` como branch condicional em pontos específicos de `Generator`/`Demographics`/`Location`, preservando 100% do caminho original quando inativo.
- A Story 3.2 **deve seguir o mesmo padrão** (mesmo flag, mesmo estilo de branch condicional), não introduzir um segundo flag ou um mecanismo de "profile" paralelo.
- Se a Story 3.2 for implementada **antes** desta (3.1) por qualquer motivo de equipe, ela deve criar o `BrProfile.isActive()` mínimo necessário e esta story deve reaproveitá-lo ao invés de recriar — documentar a inversão claramente no Dev Agent Record de quem implementar primeiro.

### Limitação MVP — etnia, idioma e SES (decisão review 2026-06-30)

Com `br.profile=br`, idade/gênero/raça seguem o data pack IBGE; **etnia** (`hispanic`/`nonhispanic`), **idioma** e variáveis **socioeconômicas** (renda, educação) permanecem calibradas para a cidade US sorteada até Stories 3.x dedicadas. Documentado em `BrDemographicsLoader.createPickerForLocation` Javadoc.

### Dependência inversa com Epic 2 — não existe hoje, não é necessária

O perfil `br` (Epic 3) é conceitualmente independente de `br.target_condition` (Epic 2) — um pesquisador pode usar perfil `br` sem condição alvo, ou condição alvo sem perfil `br` (gerando câncer de mama em pacientes com demografia padrão dos EUA). Não introduzir uma dependência artificial entre os dois flags.

### Project Structure Notes

```
src/main/resources/
  synthea.properties                         <- adicionar br.profile
  br/
    demographics/
      distribuicao_nacional.csv              <- novo (fonte IBGE documentada no cabeçalho)
src/main/java/org/mitre/synthea/br/
  profile/
    BrProfile.java                           <- novo (flag mestre, reutilizado por 3.2/3.3/3.4)
  demographics/
    BrDemographicsLoader.java                <- novo
src/test/java/org/mitre/synthea/br/
  profile/BrProfileTest.java                  <- novo
  demographics/BrDemographicsLoaderTest.java  <- novo
```
Primeira vez que `org.mitre.synthea.br.profile.*` é criado — padrão a ser seguido por todo o Epic 3.

### Testing Standards Summary

JUnit 4, `SimpleCSV` para parsing (reaproveitar utilitário existente, não introduzir Jackson). `./gradlew check` deve passar. Teste de regressão com perfil desativado é obrigatório — é a garantia formal de AD-1/compatibilidade upstream.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-4]
- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/addendum.md#Localização-BR-Camadas-Técnicas]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-1, #AD-3, #AD-7, #Capability-Architecture-Map]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-3, #Story-3.1]
- [Source: src/main/java/org/mitre/synthea/world/geography/Demographics.java#pickAge, #pickGender, #pickRace, #pickEthnicity]
- [Source: src/main/java/org/mitre/synthea/engine/Generator.java#randomDemographics]
- [Source: src/main/resources/geography/demographics.csv — formato US Census existente, não reutilizável diretamente para granularidade nacional BR]
- [Source: _bmad-output/project-context.md — "Dados de referência: CSV/JSON/YAML em src/main/resources/ — não hardcodar dados demográficos"]

## Dev Agent Record

### Agent Model Used

claude-opus-4-6 (Amelia / bmad-dev-story)

### Debug Log References

- Resource path fix: `br/demographics/distribuicao_nacional.csv` (sem leading slash) para `Utilities.readResource`.
- `./gradlew check`: 666 tests, 1 falha pré-existente `PayerTest.receiveDualEligible` (não relacionada a br.profile).

### Completion Notes List

- AC #1: `BrProfile.isActive()` + property `br.profile` em synthea.properties.
- AC #2/#7: `BrDemographicsIntegrationTest` — N=1000, seed fixa, limiar 5pp por faixa etária e sexo.
- AC #3: `BrDemographicsLoader` + CSV em resources; zero literais de distribuição no Java.
- AC #4: mapeamento em `BrRaceMapper`, comentários no CSV, ADR-003 (Proposto).
- AC #5/#6: branch em `Generator.pickDemographics` via `createPickerForLocation` (instância nova, sem mutar Demographics compartilhado).
- Testes BR: 10/10 passando. Suite completa: falha isolada em PayerTest (upstream).

### File List

- src/main/java/org/mitre/synthea/br/profile/BrProfile.java
- src/main/java/org/mitre/synthea/br/demographics/BrDemographicsLoader.java
- src/main/java/org/mitre/synthea/br/demographics/BrRaceMapper.java
- src/main/resources/br/demographics/distribuicao_nacional.csv
- src/main/resources/synthea.properties
- src/main/java/org/mitre/synthea/engine/Generator.java
- src/test/java/org/mitre/synthea/br/profile/BrProfileTest.java
- src/test/java/org/mitre/synthea/br/demographics/BrDemographicsLoaderTest.java
- src/test/java/org/mitre/synthea/br/demographics/BrDemographicsIntegrationTest.java
- docs/research/adr/ADR-003-mapeamento-raca-cor-ibge.md
- docs/research/adr/README.md

## Change Log

- 2026-06-30: Story 3.1 implementada — perfil BR demográfico IBGE, flag `br.profile`, loader, mapeamento raça/cor, testes estatísticos e regressão upstream.
- 2026-06-30: Code review — 1 decision-needed, 9 patch, 5 defer, 4 dismissed.

- 2026-06-30: Code review patches aplicados — testes AC #2/#6 reforçados, CSV idade corrigido, loader hardened, limitação etnia/SES deferida para Story 3.x.

### Review Findings

- [x] [Review][Defer] Etnia/idioma/SES permanecem US com perfil BR ativo — deferido para Story 3.x (decisão Boss 2026-06-30); documentado em Dev Notes e `BrDemographicsLoader` Javadoc.

- [x] [Review][Patch] Regressão upstream incompleta (AC #6) [`BrDemographicsIntegrationTest.java`] — `testInactiveProfileIsTransparentToCityPicker` prova equivalência bit-a-bit com picker direto da cidade quando inativo.

- [x] [Review][Patch] Falta teste comparativo BR vs US (AC #2) [`BrDemographicsIntegrationTest.java`] — `testBrSampleCloserToIbgeThanToUsDefault` com distância L1 vs referência IBGE e média US ponderada por população.

- [x] [Review][Patch] Sem validação estatística de raça/cor (AC #2) [`BrDemographicsIntegrationTest.java`] — `testBrProfileSampleMatchesReferenceWithinThreshold` estende validação a `reference.getRace()`.

- [x] [Review][Patch] Frações etárias do CSV somam ~1,05 [`distribuicao_nacional.csv`] — valores renormalizados na fonte; `testRawCsvAgeFractionsSumToOneInSourceFile` valida soma bruta.

- [x] [Review][Patch] Cabeçalho CSV não documenta split interim 40/60 de `parda` (AC #4) [`distribuicao_nacional.csv:10`] — linha explícita adicionada.

- [x] [Review][Patch] Categorias IBGE obrigatórias não validadas no load [`BrDemographicsLoader.java`] — `validateRequiredRaceIbgeCategories` falha com `IOException`.

- [x] [Review][Patch] Parsing CSV frágil — NPE/NFE escapam como runtime [`BrDemographicsLoader.java`] — `parseFraction` encapsula em `IOException`.

- [x] [Review][Patch] Teste não restaura `Generator.DEFAULT_STATE` [`BrDemographicsIntegrationTest.java`] — `tearDown` restaura valor capturado no `setUp`.

- [x] [Review][Patch] `BrProfileTest` não limpa cache do loader [`BrProfileTest.java`] — `resetCacheForTest()` no `@After`.

- [x] [Review][Defer] ADR-003 permanece "Proposto" — story explicitamente não bloqueia por ADR; ação PUCPR pendente.
- [x] [Review][Defer] `BrProfile.isActive()` no hot path — overhead de `Config.get` por paciente; otimização futura (cache no `Generator` init).
- [x] [Review][Defer] `Config` global não thread-safe — padrão upstream preexistente; fora do escopo desta story.
- [x] [Review][Defer] `resetCacheForTest()` em código de produção — padrão aceitável para testes; risco operacional baixo.
- [x] [Review][Defer] Alocação de `Demographics` por paciente com perfil BR — pressão GC em runs massivos; otimização futura.
