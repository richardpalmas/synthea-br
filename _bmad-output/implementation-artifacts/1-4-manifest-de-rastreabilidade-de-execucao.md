---
baseline_commit: 0e32c32bec2a5ead6be34749782011528d18a54b
---

# Story 1.4: Manifest de Rastreabilidade de Execução

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador do grupo Synthea-br,
quero que cada run de geração produza um manifest JSON com seed, hash de configuração, commit e checksum de output,
para reproduzir a execução exata na seção Methods do artigo, sem depender de anotações manuais sujeitas a erro.

## Acceptance Criteria

1. **Given** o fork Synthea-br está instalado e uma geração é executada (`./run_synthea` ou via `Generator`/`App`)
   **When** a geração termina e os exportadores concluem (`Exporter.runPostCompletionExports`)
   **Then** um `manifest.json` é escrito em `output/manifest.json` (mesmo diretório base configurado por `exporter.baseDirectory`) com os campos: `seed`, `config_hash`, `commit_sha`, `output_checksum`, `generated_at_iso8601`
   [Source: planning-artifacts/epics.md#Story-1.4; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-13; architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-6 "Manifest em JSON com campos fixos"]

2. **Given** `generated_at_iso8601` deve ser comparável entre execuções
   **When** o manifest é gerado
   **Then** o timestamp usa ISO-8601 em UTC (consistente com a convenção definida em AD-6 "datas em ISO-8601 UTC")
   [Source: architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#Consistency-Conventions]

3. **Given** o `output_checksum` deve ser determinístico para a mesma seed/config (NFR1, SM-3)
   **When** o checksum é calculado
   **Then** o cálculo usa SHA-256 sobre a lista ordenada de (caminho relativo + hash de conteúdo) dos arquivos exportados em `output/`, **excluindo explicitamente** `output/manifest.json` (auto-referência) e `output/metadata/*.json` (contém `runStartTime`/`runTimeInSeconds` — wall-clock, não determinístico) — caso contrário a reexecução nunca produziria checksum idêntico
   [Source: prds/prd-synthea-2026-06-29/prd.md#NFR1 "Reprodutibilidade obrigatória"; prds/prd-synthea-2026-06-29/prd.md#SM-3; src/main/java/org/mitre/synthea/export/MetadataExporter.java — campos `runStartTime`/`runTimeInSeconds` não deterministicos]

4. **Given** `config_hash` deve refletir a configuração efetiva usada (incluindo overrides `-c` e `--config.*=valor`)
   **When** o hash é calculado
   **Then** usa SHA-256 sobre a serialização determinística (chaves ordenadas alfabeticamente) de todas as propriedades resolvidas via `Config.allPropertyNames()` + `Config.get(chave)`, capturada **após** todos os overrides de CLI/arquivo serem aplicados (ou seja, no momento em que `Generator` é instanciado, não antes)
   [Source: src/main/java/org/mitre/synthea/helpers/Config.java#allPropertyNames; src/main/java/App.java — ordem de aplicação de `-c` e `--config.*`]

5. **Given** `commit_sha` precisa identificar a versão exata do código que gerou o output, mesmo quando o `.git/` não está disponível em tempo de execução (ex.: JAR distribuído)
   **When** o projeto é compilado
   **Then** o `commit_sha` é capturado **em tempo de build** (não em tempo de execução) seguindo o padrão já existente em `build.gradle` (`task versionTxt` gera `version.txt` via `git describe --tags --always`, carregado por `Utilities.SYNTHEA_VERSION`) — esta story estende esse padrão com uma nova task Gradle análoga que grava o SHA completo (`git rev-parse HEAD`) em um recurso (`commit_sha.txt`), com fallback `"unknown"` e apenas um warning de build se o comando `git` falhar (não deve quebrar o build)
   [Source: build.gradle linhas ~315-339 `task versionTxt`; src/main/java/org/mitre/synthea/helpers/Utilities.java#SYNTHEA_VERSION]

6. **Given** a geração de manifest deve funcionar de forma independente das features de Epic 2/3 (ainda não implementadas)
   **When** a Story 1.4 é implementada
   **Then** o manifest funciona para **qualquer** execução do Synthea-br (com ou sem perfil `br` ativo) — não depende de `br.target_condition`, `br.profile` ou de nenhuma config introduzida pela Epic 2/3
   [Source: prds/prd-synthea-2026-06-29/prd.md#§12 Phasing — Fase 0 (FR-10–14) é anterior/paralela à Fase 1 (FR-1, FR-2)]

7. **Given** a geração de manifest pode ter custo de I/O não desejado em testes locais rápidos
   **When** o pesquisador quer desabilitar temporariamente
   **Then** existe a propriedade `br.manifest.enabled` em `synthea.properties` (default `true`) documentada com comentário inline, controlando se o manifest é escrito
   [Source: _bmad-output/project-context.md — "Ativar exportações via synthea.properties (exporter.*) — não hardcodar flags de export"]

8. **Given** o manifest é gerado automaticamente, mas o workflow acadêmico (Story 1.2) usa pastas de experimento versionadas em `docs/research/experiments/`
   **When** o pesquisador documenta um experimento
   **Then** a documentação (referenciada nesta story e formalizada na Story 1.5) instrui copiar `output/manifest.json` (gerado automaticamente, fora do controle de versão) para dentro da pasta do experimento antes de commitar — o motor de geração não escreve diretamente em `docs/research/`
   [Source: _bmad-output/implementation-artifacts/1-2-template-de-experimento-reproduzivel.md — convenção de pasta por experimento]

9. **Given** reexecução com o mesmo manifest deve produzir output equivalente (SM-3)
   **When** um teste automatizado valida a feature
   **Then** existe um teste JUnit 4 que executa o `Generator` duas vezes com a mesma seed/config, gera dois manifests, e assevera que `config_hash` e `output_checksum` são idênticos entre as duas execuções (e que `generated_at_iso8601`/`commit_sha` são consistentes em formato, mesmo que o timestamp varie)
   [Source: prds/prd-synthea-2026-06-29/prd.md#SM-3; _bmad-output/project-context.md#Testing-Rules — "JUnit 4 apenas"]

## Tasks / Subtasks

- [x] Task 1: Estender `build.gradle` para capturar `commit_sha` em tempo de build (AC: #5)
  - [x] Subtask 1.1: Criar task Gradle `commitShaTxt` análoga a `versionTxt`, executando `git rev-parse HEAD` e gravando `src/main/resources/commit_sha.txt`
  - [x] Subtask 1.2: Adicionar fallback `"unknown"` + warning de build (não falhar) se `git` não estiver disponível
  - [x] Subtask 1.3: Adicionar `compileJava.dependsOn commitShaTxt` e `uberJar.dependsOn commitShaTxt` (mesmo padrão de `versionTxt`)
  - [x] Subtask 1.4: Criar método utilitário (ex.: `Utilities.getSyntheaCommitSha()` ou constante similar a `SYNTHEA_VERSION`) para carregar `commit_sha.txt` via `Utilities.readResource`

- [x] Task 2: Criar o writer do manifest em `org.mitre.synthea.br.*` (AC: #1, #2, #6, #7)
  - [x] Subtask 2.1: Criar pacote `org.mitre.synthea.br.research` (ou equivalente) — primeira classe de produção do projeto Synthea-br
  - [x] Subtask 2.2: Implementar `ResearchManifestWriter` (nome sugerido) com método estático `writeManifest(Generator generator)`, seguindo o padrão de assinatura de `MetadataExporter.exportMetadata(Generator)`
  - [x] Subtask 2.3: Ler `br.manifest.enabled` via `Config.getAsBoolean("br.manifest.enabled", true)`; se `false`, não escrever nada
  - [x] Subtask 2.4: Capturar `seed` de `generator.options.seed`; `generated_at_iso8601` via `Instant.now()` formatado UTC
  - [x] Subtask 2.5: Adicionar propriedade `br.manifest.enabled = true` em `synthea.properties` com comentário explicativo

- [x] Task 3: Implementar `config_hash` (AC: #4)
  - [x] Subtask 3.1: Iterar `Config.allPropertyNames()` ordenado alfabeticamente, montar string `chave=valor\n` por linha
  - [x] Subtask 3.2: Calcular SHA-256 (Java `MessageDigest`) sobre a string serializada, codificar em hex
  - [x] Subtask 3.3: Capturar o hash **após** `Generator` ser inicializado (garante que overrides de `-c`/`--config.*` já foram aplicados)

- [x] Task 4: Implementar `output_checksum` (AC: #3)
  - [x] Subtask 4.1: Resolver diretório base de output via `Exporter.getOutputFolder` (mesma API usada por `MetadataExporter`)
  - [x] Subtask 4.2: Percorrer recursivamente os arquivos exportados, **excluindo** `manifest.json` e a subpasta `metadata/`
  - [x] Subtask 4.3: Para cada arquivo, calcular SHA-256 do conteúdo; ordenar por caminho relativo; concatenar `caminho:hash\n`; calcular SHA-256 final sobre a concatenação
  - [x] Subtask 4.4: Documentar explicitamente no Javadoc da classe por que `metadata/` é excluído (não determinístico)

- [x] Task 5: Integrar ao pipeline de export sem tocar `Exporter.java` (AC: #1, #6)
  - [x] Subtask 5.1: **Descoberta importante:** `Exporter.java` já expõe um ponto de extensão via SPI (`ServiceLoader.load(PostCompletionExporter.class)`, ver `org.mitre.synthea.export.PostCompletionExporter`) — qualquer classe que implemente essa interface e seja registrada em `src/main/resources/META-INF/services/org.mitre.synthea.export.PostCompletionExporter` é automaticamente executada ao final da geração, **sem exigir nenhuma alteração em `Exporter.java`**. Implementar `ResearchManifestWriter` como um `PostCompletionExporter` é a abordagem de **menor risco** (zero touch no core), preferível a inserir uma chamada manual dentro de `runPostCompletionExports`
  - [x] Subtask 5.2: Registrar a classe no arquivo de serviço SPI; o método `export(Generator, ExporterRuntimeOptions)` da interface recebe o `Generator` já finalizado, com todos os arquivos de output gravados (o checksum pode ler arquivos já finalizados, mas confirmar a ordem relativa de execução entre `PostCompletionExporter`s registrados via `ServiceLoader` antes de assumir que `MetadataExporter`/demais exportadores nativos já rodaram — `ServiceLoader` não garante ordem entre serviços de terceiros vs. chamadas nativas em `runPostCompletionExports`)
  - [x] Subtask 5.3: Garantir que falha ao escrever o manifest não derruba a geração inteira (capturar exceção, logar erro, mas não interromper o restante do pipeline) — decisão a documentar no Javadoc, dado que AD-6 diz que ausência de manifest invalida o run para uso acadêmico oficial, mas não deve invalidar a execução técnica em si

- [x] Task 6: Testes (AC: #9)
  - [x] Subtask 6.1: Criar `src/test/java/org/mitre/synthea/br/research/ResearchManifestWriterTest.java`
  - [x] Subtask 6.2: Teste de reprodutibilidade: duas execuções com a mesma seed/config pequenas (ex.: população=5) produzem `config_hash` e `output_checksum` idênticos
  - [x] Subtask 6.3: Teste de variação: seeds diferentes produzem `output_checksum` diferentes (sanity check de que o hash não é uma constante)
  - [x] Subtask 6.4: Teste de formato: `generated_at_iso8601` é parseável como ISO-8601 UTC
  - [x] Subtask 6.5: Rodar `./gradlew check` localmente (Checkstyle + JaCoCo + testes) antes de considerar a story concluída

## Dev Notes

### Esta é a única story de código de produção no Epic 1

Diferente das Stories 1.1, 1.2, 1.3 e 1.5 (documentais), esta story introduz a **primeira classe Java de produção** do Synthea-br (`org.mitre.synthea.br.*`), além de uma modificação em `build.gradle` (arquivo core/upstream). Aplicar rigorosamente as regras de `project-context.md`: pacote `org.mitre.synthea.br.*`, imports explícitos, Javadoc obrigatório em APIs públicas novas, `./gradlew check` deve passar.

### Descoberta crítica — reutilizar o padrão `versionTxt`, não inventar mecanismo novo de captura de commit

O `build.gradle` **já implementa** exatamente o problema de "capturar informação do Git em tempo de build, com fallback gracioso" via `task versionTxt` (gera `version.txt` com `git describe --tags --always`, consumido por `Utilities.SYNTHEA_VERSION`). **Não implemente** uma chamada a `git` em tempo de execução (`ProcessBuilder` dentro do `ResearchManifestWriter`) — isso falharia silenciosamente em ambientes onde o JAR é distribuído sem o diretório `.git/` (exatamente o cenário que `versionTxt` já resolve evitando). Siga o mesmo padrão: nova task Gradle, novo arquivo de recurso, carregado estaticamente.

**Atenção:** `git describe --tags --always` (usado por `SYNTHEA_VERSION`) pode retornar uma tag (ex.: `v3.2.0-10-gabcdef1`) e não o SHA completo — por isso o FR-13 exige um campo `commit_sha` **distinto**, com `git rev-parse HEAD` (SHA completo), não reaproveitar `SYNTHEA_VERSION` diretamente para esse campo.

**Risco de manutenção (referenciar ADR-002 da Story 1.3):** uma alteração em `build.gradle`, ainda que aditiva e de baixo risco, é um touchpoint a revisar durante o próximo rebase com upstream (cadência definida no ADR-002 — Story 1.3). Documentar isso no commit/PR desta story.

### Descoberta crítica — `MetadataExporter` já existe e não deve ser duplicado nem quebrado

`org.mitre.synthea.export.MetadataExporter` (já existente no upstream) já escreve um JSON de metadados por run em `output/metadata/` (via `Exporter.runPostCompletionExports`, linha ~629) contendo `seed`, `version`, `modules`, `runStartTime`, `runTimeInSeconds`, etc. **Esta story não deve alterar `MetadataExporter`** — o manifest desta story (`manifest.json`, schema FR-13/AD-6) é um artefato **complementar e com schema diferente**, focado em reprodutibilidade acadêmica (campos estáveis e citáveis), não em telemetria operacional. Reaproveite valores já calculados pelo `Generator`/`options` quando possível (ex.: `seed`) em vez de recalcular, mas escreva um arquivo JSON separado (`manifest.json`, não dentro de `metadata/`).

**Catch crítico para reprodutibilidade (NFR1/SM-3):** o JSON do `MetadataExporter` inclui `runStartTime` e `runTimeInSeconds` — campos de wall-clock, **não determinísticos** entre execuções com a mesma seed. Se o `output_checksum` desta story incluir o conteúdo de `output/metadata/*.json` no cálculo, o checksum **nunca** será idêntico entre reexecuções, violando diretamente SM-3 e NFR1. Excluir `metadata/` do cálculo do checksum é um requisito funcional, não um detalhe de implementação (ver AC #3).

### Dependência crítica de sequenciamento com Epic 2/3 — NÃO esperar por elas

Esta story (Fase 0 do PRD) deve funcionar **antes** de qualquer feature de Epic 2 (`br.target_condition`) ou Epic 3 (`br.profile=br`) existir em código. O manifest deve ser testável e funcional usando apenas uma geração padrão do Synthea (upstream, sem nenhuma flag BR). Quando Epic 2/3 forem implementadas, o manifest passará a refletir automaticamente os novos valores de config (pois `config_hash` é calculado sobre **todas** as propriedades resolvidas) sem necessidade de alterar esta story.

### Conexão com Story 1.1 (ADR-001) — revisão futura

Não há dependência de implementação direta, mas a Story 1.1 (ADR-001) registra que métricas reais de SM-2 só existirão após o Epic 4. Da mesma forma, o **AD-6** (proveniência acadêmica) só estará "fechado" como ciclo completo após a Story 5.2 (metadados de proveniência no export) reutilizar esta convenção de manifest — ver Dev Notes da Story 5.2.

### Project Structure Notes

```
build.gradle                                    <- estender (task commitShaTxt, análoga a versionTxt)
src/main/resources/
  commit_sha.txt                                <- gerado em build-time (gitignorado, como version.txt)
  synthea.properties                            <- adicionar br.manifest.enabled = true
  META-INF/services/
    org.mitre.synthea.export.PostCompletionExporter   <- novo (registra ResearchManifestWriter via SPI)
src/main/java/org/mitre/synthea/br/research/
  ResearchManifestWriter.java                   <- novo (implementa PostCompletionExporter)
src/test/java/org/mitre/synthea/br/research/
  ResearchManifestWriterTest.java                <- novo
```
Primeira vez que `org.mitre.synthea.br.*` é criado no projeto — define o padrão de pacote que as Stories de Epic 2-5 seguirão (AD-7).
Confirmar se `version.txt` está no `.gitignore` (gerado em build) e replicar o mesmo tratamento para `commit_sha.txt`.

### Testing Standards Summary

- JUnit 4 (`org.junit.Assert` estático), localização espelhando o pacote (`src/test/java/org/mitre/synthea/br/research/`).
- `./gradlew check` deve passar (Checkstyle Google Style, 100 colunas, JaCoCo).
- Teste de reprodutibilidade é o critério de aceite mais importante (AC #9) — sem ele, a story não comprova SM-3/NFR1.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-13, #NFR1, #SM-3]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-6, #AD-7, #Consistency-Conventions]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-1, #Story-1.4]
- [Source: _bmad-output/project-context.md — convenções de pacote, build e testes]
- [Source: build.gradle — `task versionTxt` (linhas ~315-339), padrão de captura de versão Git em build-time]
- [Source: src/main/java/org/mitre/synthea/helpers/Utilities.java#SYNTHEA_VERSION]
- [Source: src/main/java/org/mitre/synthea/helpers/Config.java#allPropertyNames]
- [Source: src/main/java/org/mitre/synthea/export/MetadataExporter.java — schema existente de metadados de run, não duplicar]
- [Source: src/main/java/org/mitre/synthea/export/Exporter.java#runPostCompletionExports (linha ~629)]
- [Source: _bmad-output/implementation-artifacts/1-1-spike-de-viabilidade-ia-vs-regras-puras.md, 1-3-registro-de-decisoes-arquiteturais-adrs.md — ADR-002 sobre rebase, relevante ao tocar build.gradle]
- [Source: _bmad-output/implementation-artifacts/1-2-template-de-experimento-reproduzivel.md — convenção de pasta de experimento que consumirá este manifest]

## Dev Agent Record

### Agent Model Used

Claude (Amelia dev-story)

### Completion Notes List

- `ResearchManifestWriter` implementado via SPI `PostCompletionExporter` (zero touch em Exporter.java).
- Task Gradle `commitShaTxt` + `Utilities.SYNTHEA_COMMIT_SHA` para rastreabilidade de build.
- 4 testes JUnit passando; reprodutibilidade validada com `referenceTime`/`endTime` fixos.

### File List

- build.gradle
- .gitignore
- src/main/java/org/mitre/synthea/br/research/ResearchManifestWriter.java
- src/main/java/org/mitre/synthea/helpers/Utilities.java
- src/main/resources/synthea.properties
- src/main/resources/META-INF/services/org.mitre.synthea.export.PostCompletionExporter
- src/test/java/org/mitre/synthea/br/research/ResearchManifestWriterTest.java

### Change Log

- 2026-06-30: Story 1.4 implementada — manifest JSON automático em output/manifest.json.

### Review Findings

- [x] [Review][Decision] `config_hash` inclui caminhos absolutos — **Resolvido:** `exporter.baseDirectory` excluído via `CONFIG_HASH_EXCLUDED_KEYS`; valores com newline escapados em `computeConfigHash()`.
- [x] [Review][Decision] `br.manifest.enabled = true` como default — **Aceito:** rastreabilidade sempre ativa é o objetivo do fork acadêmico.
- [x] [Review][Patch] `commitShaTxt` exit code/stdout vazio — **Corrigido:** `ProcessBuilder` com timeout 30s, verificação de exit code e fallback `"unknown"`.
- [x] [Review][Patch] `computeConfigHash()` newline collision — **Corrigido:** escape de `\n`, `\r`, `\` na serialização.
- [x] [Review][Patch] `Runtime.exec()` sem timeout/stderr — **Corrigido:** substituído por `ProcessBuilder` com `redirectErrorStream(true)` e timeout.
- [x] [Review][Defer] `ResearchManifestWriter.export()` engole `Exception` silenciosamente (apenas stderr) [ResearchManifestWriter.java:47-58] — deferred, comportamento documentado intencionalmente no Javadoc da classe; revisitar se a criticidade acadêmica exigir falha mais visível.
- [x] [Review][Defer] Exclusão hardcoded de `metadata/` em `computeOutputChecksum` é específica a um exportador conhecido [ResearchManifestWriter.java] — deferred, documentado como limitação aceita no Javadoc; pode quebrar silenciosamente se outro exportador futuro gravar timestamps não determinísticos.
- [x] [Review][Defer] Ordem do `ServiceLoader` para `PostCompletionExporter` não é garantida — deferred, hoje há apenas uma implementação registrada; revisitar se mais exportadores pós-conclusão forem adicionados.
- [x] [Review][Defer] `task commitShaTxt` não declara `inputs`/`outputs` (não cacheável pelo Gradle) — deferred, consistente com o padrão já usado por `versionTxt` (task irmã); não é regressão introduzida por esta story.
