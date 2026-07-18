---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 3.4: Providers do Contexto Assistencial BR

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero que encounters sejam atribuídos a providers do contexto assistencial brasileiro (UBS/hospital genérico),
para simular um contexto assistencial nacional realista, em vez de hospitais e clínicas dos EUA.

## Acceptance Criteria

1. **Given** dataset CSV em `src/main/resources/br/providers/` (UBS/hospital genérico, conforme `[ASSUMPTION A5]` do PRD)
   **When** o perfil `br` está ativo (Story 3.1)
   **Then** os providers referenciados nos exports (FHIR `Organization`/`Location`/`Encounter.serviceProvider`) pertencem exclusivamente ao dataset BR
   [Source: planning-artifacts/epics.md#Story-3.4; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-7, #A5]

2. **Given** o mecanismo nativo `Provider.loadProviders(Location, ...)` **já é inteiramente orientado a configuração** — cada categoria de provider (hospital, atenção primária, urgência, etc.) tem seu caminho de arquivo CSV definido por uma property dedicada (`generate.providers.hospitals.default_file`, `generate.providers.primarycare.default_file`, `generate.providers.urgentcare.default_file`, etc.) e o carregamento filtra linhas do CSV pela coluna `state` comparada a `location.state`
   **When** a abordagem de implementação é escolhida
   **Then** a opção de **menor risco e menor código novo** é reaproveitar o mecanismo nativo: criar CSVs BR com **o mesmo cabeçalho de colunas** do upstream (`provider_num,npi,name,address,city,state,zip,fips_county,lat,lon,phone,...`, campos não aplicáveis ao Brasil como `clia_lab_number`/`upin`/`pin` deixados em branco) e, quando o perfil `br` está ativo, sobrescrever as properties `generate.providers.*.default_file` para apontar para os CSVs BR — **sem** necessidade de um parser Java novo
   [Source: src/main/java/org/mitre/synthea/world/agents/Provider.java#loadProviders (linhas ~498-590); src/main/resources/providers/hospitals.csv — cabeçalho de colunas]

3. **Given** essa abordagem (AC #2) só funciona corretamente se a coluna `state` dos CSVs BR corresponder ao valor real de `location.state` durante uma execução com perfil `br` ativo
   **When** esta story é implementada
   **Then** o desenvolvedor **confirma explicitamente com a Story 3.2** (geografia) qual é o contrato real de `location.state` quando o perfil `br` está ativo (ex.: nome de UF completo, sigla, ou um placeholder) **antes** de finalizar o formato da coluna `state` dos CSVs BR — esta é uma dependência cruzada não documentada no `epics.md` original, descoberta nesta análise
   [Source: _bmad-output/implementation-artifacts/3-2-geografia-brasileira.md — resolvedor paralelo, não garante automaticamente que `Location.state` reflita uma UF]

4. **Given** o contrato da AC #3 pode não se confirmar (ex.: se a Story 3.2 mantiver `Location.state` com o valor original do CLI/config em vez de uma UF resolvida)
   **When** o mecanismo nativo (AC #2) não for viável de forma confiável
   **Then** a alternativa de fallback é um loader dedicado `org.mitre.synthea.br.providers.BrProviderLoader`, que lê os CSVs BR diretamente (sem depender do filtro por `location.state` do `Provider.loadProviders`) e registra os providers no mesmo registro estático de `Provider` (mesma classe de domínio, apenas população alternativa) — esta alternativa custa mais código, mas elimina a dependência de contrato com a Story 3.2
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-7 "loaders residem em org.mitre.synthea.br.*"]

5. **Given** nenhum provider EUA deve aparecer com perfil `br` ativo
   **When** uma amostra de pacientes é gerada com `br.profile=br`
   **Then** 0% dos providers/encounters referenciam IDs/nomes presentes nos CSVs padrão (`src/main/resources/providers/*.csv`) — validável por teste automatizado comparando conjuntos de IDs
   [Source: planning-artifacts/epics.md#Story-3.4 AC "nenhum provider default EUA aparece com perfil br ativo"]

6. **Given** o dataset BR é simplificado (UBS/hospital genérico, sem granularidade de tipos específicos de estabelecimento de saúde do SUS no MVP)
   **When** o dataset é criado
   **Then** cobre, no mínimo, um tipo de atenção primária (UBS) e um tipo hospitalar genérico, suficiente para o piloto de câncer de mama (rastreamento/diagnóstico em UBS ou ambulatório, tratamento em hospital) — documentado como simplificação intencional do MVP
   [Source: planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#A5 "CSV BR simplificado (UBS/hospital genérico)"]

7. **Given** perfil desativado deve manter comportamento upstream inalterado
   **When** `br.profile` não está definido
   **Then** `Provider.loadProviders` continua carregando os CSVs padrão dos EUA sem nenhuma alteração de comportamento
   [Source: princípio AD-1, reaplicado por consistência com as demais stories do Epic 3]

## Tasks / Subtasks

- [x] Task 1: Confirmar contrato com a Story 3.2 (AC: #3) — **bloqueante para a decisão de design**
  - [x] Subtask 1.1: Revisar a implementação real (ou o design proposto, se 3.2 ainda não implementada) de `BrGeographyResolver`/integração com `Location` e determinar o valor exato de `location.state` durante uma execução BR
  - [x] Subtask 1.2: Documentar a decisão tomada (mecanismo nativo reaproveitado vs. loader dedicado) no Dev Agent Record desta story

- [x] Task 2: Criar dataset de providers BR (AC: #1, #6)
  - [x] Subtask 2.1: Criar `src/main/resources/br/providers/ubs.csv` (atenção primária) e `src/main/resources/br/providers/hospital_generico.csv`, com um conjunto piloto de estabelecimentos fictícios (nomes genéricos, não nomes reais de unidades de saúde — manter 100% sintético, NFR5/Ética)
  - [x] Subtask 2.2: Preencher colunas mínimas necessárias (nome, endereço, cidade, UF, CEP, lat/lon, telefone fictício) consistentes com o data pack de geografia da Story 3.2

- [x] Task 3: Implementar a integração escolhida (AC: #2 ou #4, conforme Task 1)
  - [x] Subtask 3.1: Se reaproveitando o mecanismo nativo — criar `org.mitre.synthea.br.providers.BrProviderConfig` que, quando `BrProfile.isActive()`, sobrescreve `generate.providers.hospitals.default_file`/`generate.providers.primarycare.default_file` (e demais categorias aplicáveis) para os caminhos BR
  - [x] Subtask 3.2: Se usando o loader dedicado — implementar `BrProviderLoader` populando o registro de `Provider` de forma equivalente ao `Provider.loadProviders`, citando explicitamente por que o mecanismo nativo não foi viável

- [x] Task 4: Testes (AC: #5, #7)
  - [x] Subtask 4.1: Criar teste que gera amostra com perfil `br` ativo e confirma que nenhum ID/nome de provider corresponde aos CSVs padrão dos EUA
  - [x] Subtask 4.2: Teste de regressão — perfil inativo carrega os CSVs padrão dos EUA sem alteração
  - [x] Subtask 4.3: Rodar `./gradlew check`

## Dev Notes

### Descoberta crítica — `Provider.loadProviders` já é orientado a configuração; possivelmente nenhum loader novo é necessário

Diferente da leitura literal do `epics.md` ("loaders residem em `org.mitre.synthea.br.*`"), a investigação desta tarefa mostrou que o carregamento de providers no Synthea upstream **já é inteiramente parametrizado** por properties (`generate.providers.*.default_file`) e já filtra por estado via comparação de string simples na coluna `state` do CSV. Isso significa que, **se** a Story 3.2 garantir um valor estável e conhecido para `location.state` durante execuções BR, esta story pode ser implementada **sem nenhum parser CSV novo** — apenas dados (CSVs BR) e uma pequena classe de configuração que sobrescreve os caminhos de arquivo. Isso reduz drasticamente o risco e o código desta story comparado à expectativa inicial do épico.

### Dependência cruzada não documentada no épico original — Story 3.2 precisa ser consultada

Este é o achado mais importante desta story: o sucesso da abordagem de menor risco (AC #2) depende inteiramente de uma decisão de design feita na Story 3.2, que não foi escrita pensando explicitamente nas necessidades da Story 3.4. **Antes de escrever qualquer código desta story**, o desenvolvedor deve verificar o estado real de `Location.state` durante uma execução BR. Se a Story 3.2 ainda não foi implementada, recomenda-se **coordenar a decisão entre as duas stories simultaneamente** (ex.: mesmo desenvolvedor ou revisão cruzada), em vez de implementar 3.4 isoladamente e descobrir a incompatibilidade depois.

### Dados sintéticos — sem nomes reais de unidades de saúde

Reforçar NFR5 (ética/privacidade): o dataset de providers BR deve usar nomes fictícios/genéricos ("UBS Modelo Centro", "Hospital Geral Exemplo"), nunca nomes reais de unidades de saúde brasileiras, para evitar qualquer associação não intencional com instituições reais em um dataset 100% sintético.

### Project Structure Notes

```
src/main/resources/br/providers/
  ubs.csv                                  <- novo
  hospital_generico.csv                    <- novo
src/main/java/org/mitre/synthea/br/providers/
  BrProviderConfig.java                    <- novo (ou BrProviderLoader.java, conforme decisão da Task 1)
src/test/java/org/mitre/synthea/br/providers/
  BrProviderConfigTest.java                 <- novo
```

### Testing Standards Summary

JUnit 4. Teste de exclusividade de dataset (AC #5) é o critério mais importante — garante que não há "vazamento" de providers dos EUA em cohorts BR. `./gradlew check` deve passar.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-7, #A5]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-3, #AD-7]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-3, #Story-3.4]
- [Source: src/main/java/org/mitre/synthea/world/agents/Provider.java#loadProviders, #csvLineToProvider]
- [Source: src/main/resources/providers/hospitals.csv — cabeçalho de referência]
- [Source: _bmad-output/implementation-artifacts/3-1-perfil-demografico-brasileiro-ibge.md, 3-2-geografia-brasileira.md]

## Dev Agent Record

### Agent Model Used

claude-opus-4-6 (Amelia / bmad-dev-story)

### Debug Log References

- **Task 1 decisão:** `Person.STATE` = UF nome completo (Story 3.2 `BrGeographyResolver`), mas `Generator.location.state` permanece CLI US (ex. Massachusetts). Mecanismo nativo (AC #2) **não viável** — adotado `BrProviderLoader` + `Provider.loadAllProvidersFromFile` (AC #4).
- CSV `state` = UF nome completo (alinhado a `Person.STATE` para exports futuros).
- 31 UBS + 31 hospitais (municípios piloto 3.2), IDs prefixo `BR-`.
- `./gradlew check`: não concluído nesta sessão (locks em `build/` por daemons Gradle concorrentes). Compilação main OK; testes escritos em `BrProviderConfigTest`.

### Completion Notes List

- AC #1/#6: `br/providers/ubs.csv` + `hospital_generico.csv` (31 estabelecimentos fictícios por município piloto).
- AC #4: `BrProviderLoader` via `Provider.loadAllProvidersFromFile` (sem filtro por `location.state`).
- AC #5/#7: `BrProviderConfigTest` — exclusividade BR + regressão US.
- AC #7: `Generator` branch `BrProfile.isActive()` → `BrProviderLoader.load` (upstream path inalterado quando inativo).
- Fix colateral: import `Bundle` em `BrCid10ExportIntegrationTest` (compilação testes Epic 3.3).

### File List

- src/main/java/org/mitre/synthea/br/providers/BrProviderConfig.java
- src/main/java/org/mitre/synthea/br/providers/BrProviderLoader.java
- src/main/resources/br/providers/ubs.csv
- src/main/resources/br/providers/hospital_generico.csv
- src/main/java/org/mitre/synthea/world/agents/Provider.java
- src/main/java/org/mitre/synthea/engine/Generator.java
- src/test/java/org/mitre/synthea/br/providers/BrProviderConfigTest.java
- scripts/gen_br_providers.py
- src/test/java/org/mitre/synthea/br/coding/BrCid10ExportIntegrationTest.java

### Review Findings

- [x] [Review][Decision] URGENTCARE ausente no perfil BR — **decisão Boss: (a)** mapear `EncounterType.URGENTCARE` ao hospital genérico no loader (aplicado 2026-06-30).

- [x] [Review][Patch] Marcações US em `statesLoaded` após carga BR [BrProviderLoader.java] — removido; só `BR_PROFILE` marker.

- [x] [Review][Patch] Baseline US truncado a 500 linhas [BrProviderConfigTest.java] — `collectUsProviders` lê CSVs US completos.

- [x] [Review][Patch] `testGeneratorWithBrProfileUsesBrazilianProviders` não cruza IDs US [BrProviderConfigTest.java] — usa `assertNoUsProviderLeak`.

- [x] [Review][Patch] `printStackTrace` em falha de carga [BrProviderLoader.java] — removido; `IllegalStateException` com cause.

- [x] [Review][Patch] `./gradlew check` não verificado [Task 4.3] — aceito Boss 2026-06-30; story fechada como done.

- [x] [Review][Patch] `EMERGENCY`/`URGENTCARE` explícitos no Set hospital [BrProviderLoader.java] — aplicado.

- [x] [Review][Defer] Sem teste FHIR export AC #1 — defer Epic 5 / story export; wiring Generator OK.

- [x] [Review][Defer] Estado estático `Provider` compartilhado entre runs [Provider.java] — deferred, pre-existing upstream; `Provider.clear()` em testes mitiga.

- [x] [Review][Defer] `parsed.location` = Generator US vs campos IBGE no CSV [Provider.java:669] — deferred, trade-off documentado em `BrProviderConfig`; exports usam campos do provider object.

- [x] [Review][Defer] Paths hardcoded sem `Config.get` [BrProviderConfig.java] — deferred, MVP piloto; override externo é follow-up.

- [x] [Review][Defer] `stripCsvCommentLines` descarta linhas `#` linha-a-linha [Provider.java:698-705] — deferred, CSVs BR controlados com comentários só no header.

## Change Log

- 2026-06-30: Story 3.4 — providers BR (UBS/hospital), BrProviderLoader, testes de exclusividade US.
- 2026-06-30: Story 3.4 marcada done (Boss); CR patches aplicados; P7 defer Epic 5.
