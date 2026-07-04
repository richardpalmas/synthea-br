<#if patient.exams?has_content>
  <ul class="row-list">
    <#list patient.exams as row>
      <li>
        <span class="row-dates">${row.startDate!""}</span>
        ${row.label!""}<#if row.detail??> — ${row.detail}</#if>
      </li>
    </#list>
  </ul>
<#else>
  <p class="empty">Sem registros</p>
</#if>
