<#if patient.pathwayTimelineEnabled!false>
  <#list patient.pathwayPhases![] as phase>
    <section class="pathway-phase">
      <h3 class="phase-title">${phase.title!""}</h3>
      <#if phase.description?has_content>
        <p class="phase-desc">${phase.description?html}</p>
      </#if>
      <ol class="timeline pathway-timeline">
        <#list phase.events![] as event>
          <li<#if event.targetConditionHighlight!false> class="target-condition-event"</#if>>
            <time>${event.date!""}</time>
            <span class="event-type">${event.type!""}</span>
            <#if event.targetConditionHighlight!false>
              <span class="target-badge" title="Condição-alvo">★</span>
            </#if>
            <span class="event-label">${event.label!""}</span>
          </li>
        </#list>
      </ol>
    </section>
  </#list>
  <#if (patient.showOutOfPathwaySection!false)
      && (patient.outOfPathwayEvents![])?size gt 0>
    <details class="out-of-pathway-section">
      <summary class="section-header">Fora da trajetória</summary>
      <div class="section-body">
        <ol class="timeline out-of-pathway-timeline">
          <#list patient.outOfPathwayEvents as event>
            <li>
              <time>${event.date!""}</time>
              <span class="event-type">${event.type!""}</span>
              <span class="event-label">${event.label!""}</span>
            </li>
          </#list>
        </ol>
      </div>
    </details>
  </#if>
<#else>
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
</#if>
