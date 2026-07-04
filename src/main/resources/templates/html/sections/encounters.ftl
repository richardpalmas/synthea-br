<section class="nested-section">
  <h3>Encontros</h3>
  <#if patient.encounters?has_content>
    <ul class="row-list">
      <#list patient.encounters as row>
        <li>
          <span class="row-dates">${row.startDate!""} — ${row.endDate!""}</span>
          ${row.label!""}
        </li>
      </#list>
    </ul>
  <#else>
    <p class="empty">Sem registros</p>
  </#if>
</section>
