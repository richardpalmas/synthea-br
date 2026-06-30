# Deferred Work

Itens identificados em code reviews, válidos mas não bloqueantes para a story em revisão. Acompanhar como follow-up.

## Deferred from: code review of Epic 1 + Epic 2 (2026-06-30)

- `ResearchManifestWriter.export()` engole `Exception` silenciosamente (apenas stderr) — comportamento documentado intencionalmente no Javadoc; revisitar se a criticidade acadêmica exigir falha mais visível. [Story 1.4]
- Exclusão hardcoded de `metadata/` em `computeOutputChecksum` é específica a um exportador conhecido — documentado como limitação aceita; pode quebrar silenciosamente se outro exportador futuro gravar timestamps não determinísticos. [Story 1.4]
- Ordem do `ServiceLoader` para `PostCompletionExporter` não é garantida — hoje há apenas uma implementação registrada; revisitar se mais exportadores pós-conclusão forem adicionados. [Story 1.4]
- `task commitShaTxt` não declara `inputs`/`outputs` (não cacheável pelo Gradle) — consistente com o padrão já usado por `versionTxt`; não é regressão desta story. [Story 1.4]
- `KeepModulePaths` resolve caminho de dev relativo ao diretório de trabalho atual (não ao classpath); fallback via classloader nunca é exercitado pelos testes atuais (todos rodam a partir da árvore fonte) — frágil para cenário de JAR empacotado. [Story 2.2]
- Nenhuma documentação/aviso sobre o custo do modo `retry` sem restrições demográficas — **resolvido** na code review Epic 1+2 (WARNING em synthea.properties). [Story 2.3]
- **ADR-004 (Proposto):** semântica do gate — condição ativa vs. resolvida. Decisão formal pendente do grupo; comportamento atual (apenas ativa) permanece até aceitação do ADR. [Story 2.3]

## Deferred from: code review of 3-1-perfil-demografico-brasileiro-ibge (2026-06-30)

- ADR-003 permanece "Proposto" — story não bloqueia; revisão PUCPR pendente para promover split `parda` 40/60.
- `BrProfile.isActive()` no hot path — cache no `Generator` init como otimização futura para runs massivos.
- `Config` global não thread-safe — padrão upstream preexistente.
- `resetCacheForTest()` em código de produção — padrão aceitável para testes.
- Alocação de `Demographics` por paciente com perfil BR — otimização futura (template imutável clonado).

## Deferred from: code review of 3-1-perfil-demografico-brasileiro-ibge (2026-06-30) — decision C

- Etnia (`hispanic`/`nonhispanic`), idioma e variáveis socioeconômicas (renda, educação) permanecem calibrados para cidade US quando `br.profile=br` — deferido para Story 3.x (decisão Boss 2026-06-30).

## Deferred from: code review of 3-2-geografia-brasileira (2026-06-30)

- `Person.FIPS` vazio no perfil BR — coluna CSV export vazia; sem equivalente BR no MVP [LifecycleModule.java:201-211]
- Birthplace sempre = residência (pula `randomBirthPlace`/`randomBirthplaceByLanguage`) — simplificação MVP, fora dos ACs [LifecycleModule.java:201-224]
- AC2 testado só UFs piloto (26/27); Tocantins ausente do piloto [BrGeographyResolverTest.java:79-101] — MVP subset documentado
- Touch mínimo em `Location.randomBirthPlace` via `getEffectiveCountryCode()` [Location.java:320] — aceito para AC4
- `passport_uri` permanece default US nos exportadores FHIR [FhirR4.java:253] — fora escopo Story 3.2

## Deferred from: code review of 3-4-providers-do-contexto-assistencial-br (2026-06-30)

- Estado estático `Provider` compartilhado entre runs no mesmo JVM — padrão upstream; testes usam `Provider.clear()`.
- `parsed.location` = Generator US vs campos IBGE no CSV — trade-off documentado; exports usam campos do provider object.
- Paths hardcoded em `BrProviderConfig` sem `Config.get` — MVP piloto; override externo é follow-up.
- `stripCsvCommentLines` descarta linhas `#` linha-a-linha — CSVs BR controlados; risco baixo no MVP.

## Deferred from: code review of 3-3-nomenclatura-clinica-br-cid-10-subset-piloto (2026-06-30)

- `./gradlew check` não verificado nesta sessão de review — ambiente local com JVM 8; gate real é CI com JDK 17.
- `condition.codes.get(0)` sem guard `isEmpty()` em `FhirR4.java:1703` — padrão upstream preexistente, não introduzido pela Story 3.3.
- Round-trip `getSystemFromURI` perde identidade `CID-10` (retorna `ICD10`) — sem consumer atual; relevante se Story futura fizer round-trip de codings BR.
- `BrCodeMapper.lookup()` usa sempre `entries.get(0)` e ignora `weight` — aceitável para piloto single-entry; documentar invariante antes de expandir data pack.

## Deferred from: code review of 3-4-providers-do-contexto-assistencial-br (2026-06-30) — P7

- Sem teste FHIR export AC #1 (`Organization`/`Encounter.serviceProvider` no bundle R4 com `br.profile=br`) — defer Epic 5 / story export; wiring `Generator` → `BrProviderLoader` validado por testes de registry.
