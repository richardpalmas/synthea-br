# Convenções de Implementação — Cohort Narrative Viewer

## Stack e padrões do projeto

| Item | Convenção |
| --- | --- |
| Linguagem | Java 17, pacote `org.mitre.synthea.export` (classe `HtmlExporter` ou equivalente) |
| Templates | FreeMarker 2.3.31 em `src/main/resources/templates/html/` |
| Config | `exporter.html.export=true/false` em `src/main/resources/synthea.properties` com comentário inline |
| Output | `output/html/index.html` via `Exporter.getOutputFolder("html", null)` |
| Testes | JUnit 4 em `src/test/java/org/mitre/synthea/export/HtmlExporterTest.java` |
| Qualidade | `./gradlew check` obrigatório antes de concluir |

## Padrão de referência: CCDAExporter

Seguir o modelo de `CCDAExporter.java`:

- `Configuration` estática com `setClassLoaderForTemplateLoading(..., "templates/html")`
- Preparar modelo de dados a partir de `Person`/`HealthRecord` **sem mutar** o record clínico de forma permanente (atributos temporários de export, como CCDA faz com `person.attributes.put("ehr_*", ...)`)
- Processar template FreeMarker e retornar `String` HTML
- Integrar em `Exporter.java` na mesma região dos demais exportadores (`exporter.ccda.export`, `exporter.fhir.export`)

## Montagem do index.html (cohort)

Para MVP com arquivo único e accordions por paciente, preferir **acumulação durante export + escrita final**:

1. **Por paciente** (`Exporter.export`): gerar fragmento HTML do accordion via FreeMarker (`patient-accordion.ftl` ou similar) e acumular em buffer/singleton thread-safe (padrão análogo a `CSVExporter.getInstance()`).
2. **Pós-cohort** (`PostCompletionExporter` ou hook existente em `Exporter.runPostCompletionExports`): renderizar `index.ftl` com lista de pacientes e escrever `output/html/index.html`.

Alternativa aceitável: `PostCompletionExporter` registrado via `ServiceLoader` (META-INF/services) se a montagem exigir acesso ao `Generator` completo.

## Dados clínicos a expor (HealthRecord)

Seções MVP derivadas de `HealthRecord` (via encounters agregados, padrão superEncounter do CCDA):

| Seção | Fonte |
| --- | --- |
| Demografia | `Person.attributes` (nome, idade, sexo, nascimento, endereço BR se perfil ativo) |
| Condições | `Encounter.conditions` agregadas |
| Medicamentos | `Encounter.medications` |
| Exames | `Encounter.reports` + `Observation` (labs) |
| Procedimentos | `Encounter.procedures` |
| Encounters | `person.record.encounters` |
| Cobertura | `person.coverage` / plan records |

Timeline: eventos ordenados cronologicamente a partir de entries com timestamp (condições, meds, encounters, exames, procedimentos).

## Cabeçalho de triagem (accordion collapsed)

Campos obrigatórios no `<summary>`/header:

- Idade (ou faixa etária calculada no export time)
- Sexo
- Condição principal (preferir `br.target_condition` quando gate ativo; senão condição crônica mais recente)
- Último evento (data + rótulo curto PT-BR)

## Localização PT-BR

- Labels de seção e UI em português do Brasil
- Códigos clínicos: usar CID-10 quando mapping BR disponível (Story 3.3); fallback para display name em inglês upstream documentado
- Reutilizar helpers existentes de export (`ExportHelper`, mappers BR em `org.mitre.synthea.br.*`) — não duplicar lógica de coding

## O que NÃO fazer

- Não gerar HTML via concatenação inline extensa no Java — usar FreeMarker
- Não exigir servidor HTTP ou JavaScript de backend
- Não mutar `HealthRecord` para efeito clínico
- Não quebrar export FHIR/CSV existentes quando flag HTML desligada
