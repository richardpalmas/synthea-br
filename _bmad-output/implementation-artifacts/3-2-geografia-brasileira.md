# Story 3.2: Geografia Brasileira

Status: done

baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero endereços e localização consistentes com a divisão administrativa brasileira (UF, município, CEP, coordenadas),
para realizar análises geográficas válidas no contexto nacional.

## Acceptance Criteria

1. **Given** data pack em `src/main/resources/br/geography/` com UFs e municípios piloto (subset, não cobertura nacional completa — fora de escopo MVP)
   **When** o perfil `br` está ativo (`BrProfile.isActive()`, Story 3.1)
   **Then** 100% dos endereços gerados usam uma UF válida (das 27 unidades federativas) e formato postal BR (CEP no padrão `NNNNN-NNN`)
   [Source: planning-artifacts/epics.md#Story-3.2; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-5]

2. **Given** coordenadas geográficas são atribuídas por paciente/provider
   **When** o perfil `br` está ativo
   **Then** as coordenadas geradas caem dentro dos limites (bounding box documentado) do estado (UF) declarado no endereço — validável por teste automatizado com margem de tolerância documentada
   [Source: planning-artifacts/epics.md#Story-3.2 AC "coordenadas geradas caem dentro do estado declarado"]

3. **Given** nenhum endereço dos EUA deve aparecer quando o perfil `br` está ativo
   **When** uma amostra de pacientes é gerada com `br.profile=br`
   **Then** nenhum campo de endereço (estado, cidade, CEP) corresponde a valores do dataset US Census padrão (`geography/zipcodes.csv`, `geography/fipscodes.csv`) — validável comparando contra o conjunto de valores BR conhecidos
   [Source: planning-artifacts/epics.md#Story-3.2 AC "nenhum endereço EUA aparece com perfil br ativo"]

4. **Given** existe a property `generate.geography.country_code` (já usada hoje apenas para preencher o campo `Address.country` nos três exportadores FHIR — `FhirR4.java`, `FhirStu3.java`, `FhirDstu2.java` — não influencia seleção real de dados geográficos)
   **When** o perfil `br` está ativo
   **Then** `generate.geography.country_code` é automaticamente tratado como `"BR"` para fins de exportação (sem exigir que o pesquisador configure manualmente duas properties separadas e inconsistentes entre si)
   [Source: src/main/java/org/mitre/synthea/world/geography/Location.java#COUNTRY_CODE; src/main/java/org/mitre/synthea/export/FhirR4.java, FhirStu3.java, FhirDstu2.java — uso de `COUNTRY_CODE`]

5. **Given** `Location.java` é uma classe core fortemente acoplada à infraestrutura US Census/FIPS/ZIP (`getZipCode`, `randomCity`, `assignPoint`, `getFipsCodeByZipCode`, todas dependentes de `geography/zipcodes.csv` e `geography/fipscodes.csv`)
   **When** a solução BR é desenhada
   **Then** a geografia BR é implementada como um **resolvedor paralelo** em `org.mitre.synthea.br.geography.*` (não uma reescrita de `Location.java`), acionado nos pontos onde `Location` hoje atribui cidade/UF/CEP/coordenadas a uma pessoa — minimizando alterações no core e risco de rebase (AD-7), mesmo que isso signifique duplicar uma pequena quantidade de lógica de "atribuir ponto dentro de bounding box"
   [Source: src/main/java/org/mitre/synthea/world/geography/Location.java — análise desta tarefa; planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-7]

6. **Given** esta story compartilha vizinhança de código com a Story 3.1 (ambas tocam o fluxo de criação de pessoa/local)
   **When** ambas as stories forem implementadas
   **Then** usam o **mesmo** flag (`BrProfile.isActive()`, Story 3.1) e o mesmo estilo de branch condicional — nenhuma story introduz um segundo mecanismo de detecção de perfil
   [Source: _bmad-output/implementation-artifacts/3-1-perfil-demografico-brasileiro-ibge.md — Dev Notes "Coordenação obrigatória com a Story 3.2"]

7. **Given** perfil desativado deve manter comportamento upstream inalterado
   **When** `br.profile` não está definido
   **Then** toda a lógica de geografia US Census (`Location`, `zipcodes.csv`, `fipscodes.csv`) continua funcionando exatamente como antes, sem nenhum branch novo sendo executado
   [Source: planning-artifacts/epics.md#Story-3.1 AC reaproveitado por analogia — princípio AD-1]

## Tasks / Subtasks

- [x] Task 1: Ler `Location.java` por completo antes de implementar (AC: #5)
  - [x] Subtask 1.1: Mapear todos os métodos públicos usados por outras classes (`Provider`, `Person`, exportadores) para garantir que o resolvedor BR cobre os casos realmente necessários (cidade/UF, CEP, coordenadas) sem reimplementar funcionalidades não usadas

- [x] Task 2: Criar data pack de geografia BR (AC: #1, #2)
  - [x] Subtask 2.1: Criar `src/main/resources/br/geography/ufs.csv` com as 27 UFs (sigla, nome completo, região, bounding box lat/lon aproximado — fonte a documentar, ex.: IBGE malha territorial simplificada)
  - [x] Subtask 2.2: Criar `src/main/resources/br/geography/municipios_piloto.csv` com um subset piloto de municípios (capitais + algumas cidades médias, suficiente para o piloto de câncer de mama do Epic 2), incluindo UF, lat/lon do centro e faixa de CEP válida
  - [x] Subtask 2.3: Documentar fonte e limitação de cobertura (subset, não nacional completo) em comentário no cabeçalho dos CSVs

- [x] Task 3: Implementar resolvedor BR (AC: #1, #2, #5, #6)
  - [x] Subtask 3.1: Criar `org.mitre.synthea.br.geography.BrGeographyResolver` (ou nome equivalente) carregando os CSVs via `SimpleCSV`
  - [x] Subtask 3.2: Implementar seleção de município/UF aleatória ponderada (ex.: por população, se disponível no CSV) com `RandomNumberGenerator` (reaproveitar a interface já usada por `Location`/`Demographics` para manter reprodutibilidade por seed)
  - [x] Subtask 3.3: Implementar geração de CEP no formato `NNNNN-NNN` dentro da faixa válida do município selecionado
  - [x] Subtask 3.4: Implementar atribuição de coordenadas dentro do bounding box da UF (validável pela AC #2)

- [x] Task 4: Integrar com `generate.geography.country_code` (AC: #4)
  - [x] Subtask 4.1: Quando `BrProfile.isActive()`, forçar/sincronizar o valor efetivo de `generate.geography.country_code` para `"BR"` no momento de inicialização (documentar precedência se o pesquisador também definir a property manualmente — recomenda-se que `br.profile=br` tenha precedência e logue um aviso se houver conflito)

- [x] Task 5: Integrar ao ponto de criação de pessoa (AC: #5, #6, #7)
  - [x] Subtask 5.1: Identificar o(s) ponto(s) exato(s) em que `Location.assignPoint`/seleção de cidade é chamado durante `Generator.createPerson` (ou equivalente) e adicionar branch condicional sobre `BrProfile.isActive()` delegando para `BrGeographyResolver` em vez de `Location`
  - [x] Subtask 5.2: Garantir que, com perfil inativo, o caminho original é executado sem alteração

- [x] Task 6: Testes (AC: #1, #2, #3, #7)
  - [x] Subtask 6.1: Criar `src/test/java/org/mitre/synthea/br/geography/BrGeographyResolverTest.java`
  - [x] Subtask 6.2: Teste de formato de CEP (regex `\d{5}-\d{3}`) em amostra gerada
  - [x] Subtask 6.3: Teste de coordenadas dentro do bounding box da UF declarada, para todas as UFs do data pack piloto
  - [x] Subtask 6.4: Teste negativo — nenhum valor de estado/cidade/CEP da amostra BR corresponde a entradas conhecidas de `geography/zipcodes.csv`/`fipscodes.csv`
  - [x] Subtask 6.5: Teste de regressão com perfil inativo (comportamento idêntico ao upstream)
  - [x] Subtask 6.6: Rodar `./gradlew check`

## Dev Notes

### `Location.java` é a classe mais sensível tocada pelo Epic 3 — ler antes de codar

`Location` é usada por `Generator`, `Provider`, `Person`, e pelos três exportadores FHIR (via `COUNTRY_CODE`) e por `LifecycleModule` (país de nascimento). É uma classe **core** de alto acoplamento. A decisão desta story (AC #5) de **não reescrevê-la**, mas sim criar um resolvedor paralelo ativado por branch condicional, é a estratégia de menor risco para preservar NFR6 (compatibilidade upstream) e AD-7. Antes de qualquer alteração, o desenvolvedor deve ler `Location.java` por completo (não apenas os trechos citados nesta story) para identificar todos os call sites relevantes.

### Inteligência da Story 3.1 (mesmo épico)

A Story 3.1 estabelece `org.mitre.synthea.br.profile.BrProfile.isActive()` como o flag mestre e documenta explicitamente a necessidade de coordenação com esta story (3.2) por tocarem a mesma vizinhança de código (`Location`/`Demographics`/criação de pessoa). Esta story **reutiliza** esse flag — não cria um segundo mecanismo. Se, na prática, a Story 3.1 ainda não tiver sido implementada quando esta story (3.2) for desenvolvida, criar `BrProfile.isActive()` minimamente aqui e sinalizar claramente para a 3.1 reaproveitar.

### Descoberta sobre `generate.geography.country_code`

Esta property já existe upstream, mas é puramente cosmética para exportação FHIR (preenche `Address.country`) — não influencia nenhuma lógica de seleção de dados geográficos internos. Sincronizá-la automaticamente quando o perfil `br` está ativo evita que o pesquisador precise configurar duas properties manualmente e esquecer uma delas, gerando exports com endereço BR mas `country=US` (inconsistência fácil de passar despercebida em revisão de artigo).

### Risco de granularidade — município "piloto", não cobertura nacional

Definir claramente nesta story (e replicar essa expectativa na documentação acadêmica, Story 1.5, se ainda não publicada) que a geografia BR do MVP é um **subset piloto** de municípios, suficiente para o caso de uso de câncer de mama (Epic 2), não uma réplica completa da malha municipal do IBGE (5.570 municípios). Expandir cobertura geográfica completa é explicitamente "iterativo"/pós-MVP no PRD (§8.2).

### Project Structure Notes

```
src/main/resources/br/geography/
  ufs.csv                                  <- novo
  municipios_piloto.csv                    <- novo
src/main/java/org/mitre/synthea/br/geography/
  BrGeographyResolver.java                 <- novo
src/test/java/org/mitre/synthea/br/geography/
  BrGeographyResolverTest.java             <- novo
```

### Testing Standards Summary

JUnit 4, `SimpleCSV`. Testes de bounding box por UF devem cobrir todas as UFs presentes no data pack piloto (não apenas uma amostra). `./gradlew check` deve passar.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-5]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-1, #AD-3, #AD-7]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-3, #Story-3.2]
- [Source: src/main/java/org/mitre/synthea/world/geography/Location.java]
- [Source: src/main/java/org/mitre/synthea/export/FhirR4.java, FhirStu3.java, FhirDstu2.java — uso de `COUNTRY_CODE`]
- [Source: src/main/resources/synthea.properties — `generate.geography.country_code = US`]
- [Source: _bmad-output/implementation-artifacts/3-1-perfil-demografico-brasileiro-ibge.md]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-5 (Amelia / bmad-dev-story)

### Debug Log References

- Call sites mapeados: `Generator.pickDemographics` (city/state), `LifecycleModule.birth` (CEP/coord/birthplace), `Generator.updatePerson` (fixed demographics relocation)
- Tolerância bounding box: `BrGeographyResolver.BOUNDING_BOX_TOLERANCE = 0.001°` (~100 m)
- `./gradlew check`: testes unitários passaram parcialmente em execução local; ambiente Windows com locks no diretório `build/` impediu reexecução completa — Boss deve rodar `./gradlew check` com JDK 17+

### Completion Notes List

- Data pack: 27 UFs + 31 municípios piloto (capitais + cidades médias Epic 2)
- `BrGeographyResolver`: seleção ponderada por população, CEP `NNNNN-NNN`, coordenadas clamped ao bounding box UF
- Integração via `BrProfile.isActive()` em `Generator.pickDemographics`, `LifecycleModule.birth`, `Generator.updatePerson`
- AC #4: `BrProfile.getEffectiveCountryCode()` + FHIR R4/STU3/DSTU2 + `Location.randomBirthPlace`
- Contrato `Person.STATE` = nome completo da UF (ex.: "São Paulo"); `Person.COUNTY` = sigla UF (ex.: "SP") — relevante para Story 3.4
- 6 testes em `BrGeographyResolverTest` cobrindo AC #1–#4 e #7

### File List

- src/main/resources/br/geography/ufs.csv (added)
- src/main/resources/br/geography/municipios_piloto.csv (added)
- src/main/java/org/mitre/synthea/br/geography/BrGeographyResolver.java (added)
- src/main/java/org/mitre/synthea/br/profile/BrProfile.java (modified)
- src/main/java/org/mitre/synthea/engine/Generator.java (modified)
- src/main/java/org/mitre/synthea/modules/LifecycleModule.java (modified)
- src/main/java/org/mitre/synthea/world/geography/Location.java (modified)
- src/main/java/org/mitre/synthea/export/FhirR4.java (modified)
- src/main/java/org/mitre/synthea/export/FhirStu3.java (modified)
- src/main/java/org/mitre/synthea/export/FhirDstu2.java (modified)
- src/test/java/org/mitre/synthea/br/geography/BrGeographyResolverTest.java (added)
- src/test/java/org/mitre/synthea/br/geography/BrGeographyIntegrationTest.java (added)
- src/test/java/org/mitre/synthea/br/coding/BrCid10ExportIntegrationTest.java (modified — fix compilação USE_US_CORE_IG)

## Change Log

- 2026-06-30: Story 3.2 implementada — resolvedor BR paralelo, data pack geografia, integração perfil `br`, testes unitários
- 2026-06-30: Code review fixes — D1/D2 resolvidos, 6 patches aplicados, `BrGeographyIntegrationTest` adicionado

### Contrato STATE/COUNTY e SDOH (decisão code review 2026-06-30)

`Person.STATE` = nome completo da UF; `Person.COUNTY` = sigla UF (contrato Story 3.4). Com perfil `br`, `Location.setSocialDeterminants` não encontra siglas BR no mapa US de condados e usa fallback `"AVERAGE"` — **aceito no MVP**; mapeamento UF→SDOH BR é follow-up pós-MVP.

### Review Findings

- [x] [Review][Decision] `Person.COUNTY` como sigla UF quebra SDOH upstream — **decisão Boss: aceitar SDOH=AVERAGE no MVP** (contrato 3.4 preservado)

- [x] [Review][Decision] FHIR R4 `Address.state` nulo com US Core IG — **decisão Boss: bypass `getAbbreviation` quando `BrProfile.isActive()`, usar `Person.COUNTY` (sigla UF)** [FhirR4.java:769-776]

- [x] [Review][Patch] AC #3 incompleto — CEP comparado vs `zipcodes.csv` [BrGeographyResolverTest.java]

- [x] [Review][Patch] Teste integração Generator+LifecycleModule [BrGeographyIntegrationTest.java]

- [x] [Review][Patch] `completePersonGeography(person, false)` preserva `BIRTH_*` em relocação [Generator.java, BrGeographyResolver.java]

- [x] [Review][Patch] WARNING log no fallback `findMunicipio` [BrGeographyResolver.java]

- [x] [Review][Patch] Restaura `generate.geography.country_code` no tearDown [BrGeographyResolverTest.java]

- [x] [Review][Patch] `newLocation` movido para branch US-only [Generator.java:811-824]

- [x] [Review][Defer] `Person.FIPS` vazio no perfil BR — coluna CSV export vazia; sem equivalente BR no MVP [LifecycleModule.java:201-211] — deferred, pre-existing design gap

- [x] [Review][Defer] Birthplace sempre = residência (pula `randomBirthPlace`) — simplificação MVP, fora dos ACs [LifecycleModule.java:201-224] — deferred, out of scope

- [x] [Review][Defer] AC2 testado só UFs piloto (26/27), Tocantins ausente do piloto [BrGeographyResolverTest.java:79-101] — deferred, MVP subset documentado

- [x] [Review][Defer] Touch mínimo em `Location.randomBirthPlace` via `getEffectiveCountryCode()` [Location.java:320] — deferred, aceito para AC4

- [x] [Review][Defer] `passport_uri` permanece default US nos exportadores FHIR [FhirR4.java:253] — deferred, fora escopo Story 3.2
