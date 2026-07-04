<section class="nested-section">
  <h3>Condições</h3>
  <#if patient.conditions?has_content>
    <ul class="row-list">
      <#list patient.conditions as row>
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
