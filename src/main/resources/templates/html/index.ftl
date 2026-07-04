<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Visualizador Narrativo da Cohort — Synthea</title>
  <#include "styles.ftl">
</head>
<body>
  <header class="cohort-header">
    <h1>Visualizador Narrativo da Cohort</h1>
    <#if aiEnrichmentEnabled!false>
      <h2 class="ai-subtitle">Dados enriquecidos por IA<#if aiModel?has_content> - Modelo Usado: ${aiModel?html}</#if></h2>
      <section class="ai-cohort-section">
        <p class="ai-summary">${aiCohortSummary?html}</p>
      </section>
    </#if>
    <p class="meta">${patientCount} paciente<#if patientCount != 1>s</#if> · gerado em ${generatedDate}</p>
    <p class="hint">Expanda um paciente para ver timeline e seções clínicas estruturadas.</p>
  </header>

  <main>
    <#list patients as patient>
      <#include "patient-accordion.ftl">
    </#list>
  </main>

  <footer class="cohort-footer">
    <p>Artefato complementar ao export FHIR/CSV · Synthea-br Cohort Narrative Viewer (MVP)</p>
  </footer>
</body>
</html>
