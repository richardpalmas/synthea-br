<section class="nested-section">
  <h3>Cobertura</h3>
  <#if patient.coverage?has_content>
    <ul class="row-list">
      <#list patient.coverage as row>
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
