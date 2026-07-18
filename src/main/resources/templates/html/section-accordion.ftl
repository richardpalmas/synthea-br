<#macro accordion title>
  <details class="section-accordion">
    <summary class="section-header">${title}</summary>
    <div class="section-body">
      <#nested>
    </div>
  </details>
</#macro>
