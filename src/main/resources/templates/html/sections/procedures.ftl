<section class="nested-section">
  <h3>Procedimentos</h3>
  <#if patient.procedures?has_content>
    <ul class="row-list">
      <#list patient.procedures as row>
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
