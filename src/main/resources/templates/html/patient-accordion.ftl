<details class="patient-accordion">
  <summary class="triage-header">
    <span class="patient-name">${patient.displayName!""}</span>
    <span class="triage-meta">
      ${patient.ageYears} anos · ${patient.sexLabel!""} · ${patient.primaryCondition!""}
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

    <section class="timeline-section">
      <h2>Linha do tempo</h2>
      <#if patient.timeline?has_content>
        <ol class="timeline">
          <#list patient.timeline as event>
            <li>
              <time>${event.date!""}</time>
              <span class="event-type">${event.type!""}</span>
              <span class="event-label">${event.label!""}</span>
            </li>
          </#list>
        </ol>
      <#else>
        <p class="empty">Sem registros</p>
      </#if>
    </section>

    <#include "sections/demographics.ftl">
    <#include "sections/conditions.ftl">
    <#include "sections/medications.ftl">
    <#include "sections/exams.ftl">
    <#include "sections/procedures.ftl">
    <#include "sections/encounters.ftl">
    <#include "sections/coverage.ftl">
  </div>
</details>
