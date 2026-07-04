# Story 6.1: Cohort Narrative Viewer — Export HTML MVP

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador ou estudante do Synthea-br,
quero gerar um `index.html` narrativo da cohort via flag `exporter.html.export=true`,
para apresentar casos ao orientador com timeline e seções clínicas estruturadas sem cruzar CSVs na hora da validação.

## Acceptance Criteria

1. **Given** o fork Synthea-br com Epics 2 e 3 implementados (cohort direcionada + perfil BR)
   **When** o pesquisador define `exporter.html.export=true` em `synthea.properties` (ou CLI `-Dexporter.html.export=true`) e gera cohort n=10 com seed fixa
   **Then** o arquivo `output/html/index.html` é criado, é HTML5 válido e abre offline no browser sem servidor
   **And** com `exporter.html.export=false` ou ausente, nenhum artefato é escrito em `output/html/`
   [Source: _bmad-output/specs/spec-cohort-narrative-viewer/SPEC.md#CAP-1, #CAP-2; brainstorm-intent.md#MVP-Scope]

2. **Given** `index.html` gerado para cohort n≥10
   **When** o pesquisador visualiza a página
   **Then** existe um accordion colapsável (`<details>`/`<summary>` ou equivalente sem JS obrigatório) por paciente
   **And** o header colapsado de cada accordion exibe: idade, sexo, condição principal, último evento (data + rótulo curto PT-BR)
   [Source: SPEC.md#CAP-3; ux-structure.md]

3. **Given** um accordion expandido
   **When** o pesquisador percorre o conteúdo daquele paciente
   **Then** uma timeline cronológica lista ≥1 evento clínico com timestamp
   **And** seções aninhadas estão presentes e populadas quando dados existem no `HealthRecord`: demografia, condições, medicamentos, exames, procedimentos, encounters, cobertura
   **And** seções sem dados exibem estado vazio gracioso (ocultar ou "Sem registros") sem quebrar layout
   [Source: SPEC.md#CAP-4; conventions.md]

4. **Given** `br.profile=br` ativo e mapping CID-10 da Story 3.3 disponível
   **When** condições piloto são renderizadas no HTML
   **Then** labels de seção/UI estão em português do Brasil
   **And** condições mapeadas exibem CID-10 BR quando aplicável
   [Source: SPEC.md#CAP-5; project-context.md]

5. **Given** AD-2 (export read-only) e padrão CCDA existente
   **When** `HtmlExporter` é implementado
   **Then** dados são lidos de `Person`/`HealthRecord` sem mutação clínica permanente (atributos temporários de export permitidos, como `CCDAExporter`)
   **And** templates FreeMarker residem em `src/main/resources/templates/html/` — não concatenação massiva inline no Java
   **And** integração ocorre em `Exporter.java` junto aos demais exportadores flag-driven
   [Source: ARCHITECTURE-SPINE.md#AD-2; src/main/java/org/mitre/synthea/export/CCDAExporter.java; conventions.md]

6. **Given** a suíte de testes do projeto
   **When** `./gradlew check` é executado após implementação
   **Then** novos testes em `HtmlExporterTest.java` passam, incluindo: flag off não gera arquivo; flag on gera `index.html`; presença de accordions, cabeçalho de triagem e seções esperadas (assert de substring/DOM parser leve)
   **And** testes existentes de export (FHIR, CCDA, CSV) continuam passando sem regressão
   [Source: project-context.md#Testing-Rules]

7. **Given** cohort piloto n≈500 (teste manual ou integração opcional com tag `@Ignore` se lento)
   **When** HTML único é gerado
   **Then** arquivo permanece utilizável (< limite prático documentado no Dev Agent Record) ou limitação é documentada com recomendação de split v1.1
   [Source: SPEC.md#Open-Questions; brainstorm-intent.md#Acceptance-Criteria-8]

## Tasks / Subtasks

- [x] Task 1: Config e flag de export (AC: #1)
  - [x] Subtask 1.1: Adicionar `exporter.html.export = false` com comentário em `src/main/resources/synthea.properties`
  - [x] Subtask 1.2: Garantir override via CLI `-Dexporter.html.export=true` (padrão `Config.getAsBoolean`)

- [x] Task 2: HtmlExporter + FreeMarker (AC: #2, #3, #5)
  - [x] Subtask 2.1: Criar `src/main/java/org/mitre/synthea/export/HtmlExporter.java` espelhando configuração FreeMarker de `CCDAExporter` (`templates/html`)
  - [x] Subtask 2.2: Implementar preparação de modelo: agregar encounters (padrão superEncounter), montar timeline ordenada, extrair campos de triagem
  - [x] Subtask 2.3: Criar templates: `index.ftl`, `patient-accordion.ftl`, partials por seção (`sections/demographics.ftl`, etc.)
  - [x] Subtask 2.4: CSS embutido minimalista conforme `ux-structure.md` (accordions, timeline, legibilidade)

- [x] Task 3: Montagem cohort index.html (AC: #1, #2)
  - [x] Subtask 3.1: Acumular fragmentos por paciente durante `Exporter.export` (singleton thread-safe, padrão `CSVExporter.getInstance()`)
  - [x] Subtask 3.2: Escrever `index.html` final no hook pós-cohort (`Exporter.runPostCompletionExports` ou `PostCompletionExporter` via ServiceLoader)
  - [x] Subtask 3.3: Output em `Exporter.getOutputFolder("html", null)`

- [x] Task 4: Integração Exporter.java (AC: #1, #5)
  - [x] Subtask 4.1: Adicionar bloco `if (Config.getAsBoolean("exporter.html.export"))` na região ~361-400 de `Exporter.java`
  - [x] Subtask 4.2: Resetar acumulador no início de nova execução de cohort

- [x] Task 5: Localização PT-BR (AC: #4)
  - [x] Subtask 5.1: Labels PT-BR nos templates (não hardcodar no Java salvo constantes de seção)
  - [x] Subtask 5.2: Reutilizar helpers/mappers BR existentes para CID-10 quando `br.profile=br` (`org.mitre.synthea.br.*`, Story 3.3)

- [x] Task 6: Testes (AC: #6, #7)
  - [x] Subtask 6.1: Criar `src/test/java/org/mitre/synthea/export/HtmlExporterTest.java`
  - [x] Subtask 6.2: Teste flag off — assert ausência de `output/html/index.html`
  - [x] Subtask 6.3: Teste flag on — gerar 1-3 pacientes via `TestHelper`, assert estrutura HTML (accordions, triagem, timeline, seções)
  - [x] Subtask 6.4: Rodar `./gradlew check` completo

## Dev Notes

### Escopo MVP vs v1.1 — não implementar agora

Fora desta story (non-goals da SPEC): nós timeline expansíveis inline, rodapé proveniência (seed/hash/manifest/FHIR link), destaque condição-alvo, busca/filtro, toggle orientador/pesquisador, split `patients/{id}.html`, CSS print-friendly, avisos plausibilidade Epic-4, backend, lazy-load.

### Padrão arquitetural obrigatório

- **Read-only AD-2:** exportadores leem `HealthRecord`; módulos escrevem. `CCDAExporter` coloca dados em `person.attributes` temporariamente — mesmo padrão aceito.
- **FreeMarker AD-8 complementar:** C-CDA usa `templates/ccda/`; HTML usa `templates/html/`. Não gerar XML/HTML inline extenso no Java.
- **Flag-driven:** espelhar `exporter.ccda.export` e `exporter.fhir.export` em `synthea.properties`.

### Montagem do index — decisão técnica recomendada

Arquivo único com todos os accordions exige **acumulação cross-patient**:

```
Exporter.export(person)  →  HtmlExporter.appendPatient(person, stopTime)  →  buffer
Exporter.runPostCompletionExports()  →  HtmlExporter.writeIndex()  →  output/html/index.html
```

Alternativa: `PostCompletionExporter` registrado em `META-INF/services/org.mitre.synthea.export.PostCompletionExporter` se precisar do `Generator` completo — ver `PostCompletionExporter.java` e loop em `Exporter.java:666-671`.

### Referência CCDAExporter — copiar padrão, não lógica clínica

```43:57:src/main/java/org/mitre/synthea/export/CCDAExporter.java
  private static Configuration templateConfiguration() {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
    configuration.setDefaultEncoding("UTF-8");
    // ...
    configuration.setClassLoaderForTemplateLoading(ClassLoader.getSystemClassLoader(),
        "templates/ccda");
    return configuration;
  }
```

Adaptar para `"templates/html"`. Agregação superEncounter (linhas 82-97) é o modelo para consolidar conditions/meds/procedures para templates.

### Referência Exporter — ponto de integração

```361:366:src/main/java/org/mitre/synthea/export/Exporter.java
    if (Config.getAsBoolean("exporter.ccda.export")) {
      String ccdaXml = CCDAExporter.export(person, stopTime);
      File outDirectory = getOutputFolder("ccda", person);
      Path outFilePath = outDirectory.toPath().resolve(filename(person, fileTag, "xml"));
      writeNewFile(outFilePath, ccdaXml);
    }
```

HTML MVP escreve cohort-level (não per-patient file), então usar `getOutputFolder("html", null)`.

### TextExporter — reutilizar lógica de narrativa textual

`TextExporter.java` já formata condições, meds, encounters em texto legível. **Não duplicar** parsing de `HealthRecord` — extrair ou espelhar helpers de formatação de datas (`ExportHelper.dateFromTimestamp`) e labels. Preferir FreeMarker para layout HTML; Java só prepara modelo de dados.

### Cabeçalho de triagem — heurísticas

| Campo | Fonte sugerida |
| --- | --- |
| Idade | Calculada de `Person` birthdate vs `stopTime` |
| Sexo | `Person.GENDER` attribute |
| Condição principal | `Config.get("br.target_condition")` display name se gate ativo; senão condição crônica ativa mais recente |
| Último evento | Max timestamp entre encounters/conditions/meds no record filtrado |

Documentar heurística escolhida no código (Javadoc breve).

### Timeline — construção

Unificar entries com timestamp de: encounters (start), conditions (start), medications (start), procedures (start), reports/observations (start). Ordenar ascending. Cada item: `{ date, type, label, sectionRef? }`.

### Project Structure Notes

```
src/main/java/org/mitre/synthea/export/
  HtmlExporter.java                    ← NEW
  Exporter.java                        ← UPDATE (flag + hook)

src/main/resources/templates/html/
  index.ftl                            ← NEW
  patient-accordion.ftl                ← NEW
  sections/                            ← NEW partials
  narrative.css.ftl (ou inline)        ← NEW

src/main/resources/synthea.properties  ← UPDATE

src/test/java/org/mitre/synthea/export/
  HtmlExporterTest.java                ← NEW
```

Extensões BR opcionais em `org.mitre.synthea.br.export` apenas se lógica CID-10 precisar isolar (AD-7) — preferir reutilizar mappers existentes.

### Testing Standards Summary

- JUnit 4 (`@Test`), asserts via `org.junit.Assert`
- Config de teste via `Config.set(...)` + `TestHelper` para gerar `Person`
- Limpar `output/` ou usar diretório temporário; restaurar config após teste
- `./gradlew check` = compile + testes + checkstyle + JaCoCo

### Dependências de outras stories

| Story | Dependência |
| --- | --- |
| 2.1-2.3 | Condição principal no header quando gate ativo |
| 3.3 | CID-10 BR no HTML |
| 3.1-3.2 | Demografia/endereço BR nos templates |
| 5.1-5.2 | Não bloqueante para MVP; proveniência no HTML é v1.1 |

Implementação pode progredir com fixtures `TestHelper` mesmo se Epics 4-5 incompletos; testes de integração BR completos requerem 2+3.

### Git Intelligence

Padrão recente do fork: extensões BR em `org.mitre.synthea.br.*`, export core intacto, testes espelhando pacote. Manter diff mínimo em `Exporter.java`.

### Latest Tech Information

- FreeMarker 2.3.31 (project-context) — usar `Configuration.VERSION_2_3_26` como CCDA para compatibilidade
- HTML5 `<details>/<summary>` — accordion nativo, sem dependência JS
- Nenhuma biblioteca HTML adicional necessária

### References

- [Source: _bmad-output/specs/spec-cohort-narrative-viewer/SPEC.md]
- [Source: _bmad-output/specs/spec-cohort-narrative-viewer/conventions.md]
- [Source: _bmad-output/specs/spec-cohort-narrative-viewer/ux-structure.md]
- [Source: _bmad-output/specs/spec-cohort-narrative-viewer/architecture-diagrams.md]
- [Source: _bmad-output/brainstorming/brainstorm-export-html-narrativa-clinica-2026-07-01/brainstorm-intent.md]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-6]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-2, #AD-7]
- [Source: _bmad-output/project-context.md]
- [Source: src/main/java/org/mitre/synthea/export/CCDAExporter.java]
- [Source: src/main/java/org/mitre/synthea/export/Exporter.java]
- [Source: src/main/java/org/mitre/synthea/export/TextExporter.java]

## Dev Agent Record

### Agent Model Used

Composer

### Debug Log References

- HtmlExporterTest passou após agregação clínica sem `Encounter` sintético (evita falha de cobertura no Claim).
- `./gradlew clean test --tests HtmlExporterTest` OK; checkstyle corrigido em HtmlExporter (LineLength).

### Completion Notes List

- MVP Cohort Narrative Viewer: flag `exporter.html.export`, `HtmlExporter` + templates FreeMarker PT-BR, `output/html/index.html` com accordions, timeline e seções aninhadas.
- CID-10 via `BrCodeMapper` quando `br.profile=br`; condição principal via `br.target_condition` ou heurística de condição ativa.
- Limite n≈500: arquivo único viável para cohort piloto; split `patients/{id}.html` documentado como v1.1 na SPEC.

### File List

- src/main/java/org/mitre/synthea/export/HtmlExporter.java (new)
- src/main/java/org/mitre/synthea/export/Exporter.java (modified)
- src/main/resources/synthea.properties (modified)
- src/main/resources/templates/html/index.ftl (new)
- src/main/resources/templates/html/patient-accordion.ftl (new)
- src/main/resources/templates/html/styles.ftl (new)
- src/main/resources/templates/html/sections/*.ftl (new)
- src/test/java/org/mitre/synthea/export/HtmlExporterTest.java (new)
- src/test/java/org/mitre/synthea/TestHelper.java (modified)
