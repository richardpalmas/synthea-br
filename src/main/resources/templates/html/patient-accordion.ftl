<#import "section-accordion.ftl" as section>
<details class="patient-accordion">
  <summary class="triage-header">
    <span class="patient-name">${patient.displayName!""}</span>
    <span class="triage-meta">
      ${patient.ageYears} anos · ${patient.sexLabel!""} ·
      <#if patient.primaryConditionHighlight!false>
        <span class="primary-highlight">${patient.primaryCondition!""}</span>
      <#else>
        ${patient.primaryCondition!""}
      </#if>
      · último: ${patient.lastEventDate!""} — ${patient.lastEventLabel!""}
    </span>
  </summary>

  <div class="patient-body">
    <#if patient.aiEnriched!false>
      <section class="ai-enrichment-section">
        <h2>Dados enriquecidos por IA<#if aiModel?has_content> - Modelo Usado: ${aiModel?html}</#if></h2>
        <p class="ai-meta">${patient.aiAppliedCount} correção(ões) · ${patient.aiFlagCount} limitação(ões) sinalizada(s)</p>
        <p class="ai-summary">${patient.aiSummary?html}</p>
      </section>
    </#if>

    <@section.accordion "Linha do tempo">
      <#include "sections/pathway-phases.ftl">
    </@section.accordion>

    <@section.accordion "Demografia">
      <#include "sections/demographics.ftl">
    </@section.accordion>

    <@section.accordion "Condições">
      <#include "sections/conditions.ftl">
    </@section.accordion>

    <@section.accordion "Medicamentos">
      <#include "sections/medications.ftl">
    </@section.accordion>

    <@section.accordion "Exames">
      <#include "sections/exams.ftl">
    </@section.accordion>

    <@section.accordion "Procedimentos">
      <#include "sections/procedures.ftl">
    </@section.accordion>

    <@section.accordion "Encontros">
      <#include "sections/encounters.ftl">
    </@section.accordion>

    <@section.accordion "Cobertura">
      <#include "sections/coverage.ftl">
    </@section.accordion>
  </div>
</details>
