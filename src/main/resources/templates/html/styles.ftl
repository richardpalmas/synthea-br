<style>
  :root {
    --bg: #f7f9fc;
    --card: #ffffff;
    --text: #1a2a3a;
    --muted: #5a6a7a;
    --accent: #0d6e6e;
    --border: #d8e0ea;
    --timeline: #c5d4e8;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: "Segoe UI", system-ui, sans-serif;
    background: var(--bg);
    color: var(--text);
    line-height: 1.5;
  }
  .cohort-header, .cohort-footer {
    max-width: 960px;
    margin: 0 auto;
    padding: 1.5rem 1rem;
  }
  .cohort-header h1 { margin: 0 0 0.25rem; font-size: 1.75rem; color: var(--accent); }
  .ai-subtitle {
    margin: 0.75rem 0 0.35rem;
    font-size: 1.15rem;
    font-weight: 600;
    color: #2a5a8a;
  }
  .ai-cohort-section, .ai-enrichment-section {
    background: #f0f6ff;
    border: 1px solid #c8daf0;
    border-radius: 8px;
    padding: 0.85rem 1rem;
    margin: 0.5rem 0 0.75rem;
  }
  .ai-summary { margin: 0.35rem 0 0; font-size: 0.95rem; white-space: pre-line; }
  .ai-meta { margin: 0; color: var(--muted); font-size: 0.85rem; }
  .meta, .hint { color: var(--muted); margin: 0.25rem 0; }
  main { max-width: 960px; margin: 0 auto; padding: 0 1rem 2rem; }
  .patient-accordion {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 8px;
    margin-bottom: 0.75rem;
    overflow: hidden;
  }
  .triage-header {
    cursor: pointer;
    padding: 1rem 1.25rem;
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
  }
  .triage-header::-webkit-details-marker { display: none; }
  .patient-name { font-weight: 700; font-size: 1.05rem; }
  .triage-meta { color: var(--muted); font-size: 0.92rem; }
  .primary-highlight { color: var(--accent); font-weight: 600; }
  .patient-body { padding: 0 1.25rem 1.25rem; border-top: 1px solid var(--border); }
  .patient-body > section { margin-top: 1.25rem; }
  .section-accordion {
    margin-top: 0.65rem;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: #fafbfc;
  }
  .section-header {
    cursor: pointer;
    padding: 0.65rem 0.85rem;
    list-style: none;
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--accent);
    text-transform: uppercase;
    letter-spacing: 0.03em;
    display: flex;
    align-items: center;
    gap: 0.4rem;
  }
  .section-header::-webkit-details-marker { display: none; }
  .section-header::before {
    content: "▸";
    display: inline-block;
    font-size: 0.85rem;
    transition: transform 0.15s ease;
  }
  .section-accordion[open] > .section-header::before {
    transform: rotate(90deg);
  }
  .section-body { padding: 0 0.85rem 0.85rem; }
  section h2 {
    margin: 0 0 0.5rem;
    font-size: 1rem;
    color: var(--accent);
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }
  .timeline {
    list-style: none;
    margin: 0;
    padding: 0 0 0 1rem;
    border-left: 3px solid var(--timeline);
  }
  .timeline li { margin-bottom: 0.65rem; padding-left: 0.75rem; }
  .pathway-phase { margin-bottom: 1rem; }
  .phase-title {
    margin: 0 0 0.25rem;
    font-size: 1rem;
    color: var(--accent);
    font-weight: 700;
  }
  .phase-desc { margin: 0 0 0.5rem; color: var(--muted); font-size: 0.88rem; }
  .target-badge {
    color: #c45c00;
    font-weight: 700;
    margin-right: 0.25rem;
  }
  .target-condition-event .event-label { font-weight: 600; color: #8a3b00; }
  .out-of-pathway-section {
    margin-top: 1rem;
    border: 1px dashed var(--border);
    border-radius: 6px;
    background: #fafbfc;
  }
  .out-of-pathway-section > summary { list-style: none; }
  .out-of-pathway-timeline { border-left-color: #e0d8c8; }
  .timeline time { font-weight: 600; margin-right: 0.5rem; }
  .event-type {
    display: inline-block;
    background: #e8f4f4;
    color: var(--accent);
    font-size: 0.75rem;
    padding: 0.1rem 0.4rem;
    border-radius: 4px;
    margin-right: 0.35rem;
  }
  .data-table { width: 100%; border-collapse: collapse; font-size: 0.92rem; }
  .data-table th, .data-table td {
    border-bottom: 1px solid var(--border);
    padding: 0.4rem 0.5rem;
    text-align: left;
    vertical-align: top;
  }
  .data-table th { color: var(--muted); font-weight: 600; width: 7rem; }
  .row-list { margin: 0; padding: 0; list-style: none; }
  .row-list li {
    padding: 0.35rem 0;
    border-bottom: 1px solid var(--border);
    font-size: 0.92rem;
  }
  .row-dates { color: var(--muted); font-size: 0.85rem; margin-right: 0.5rem; }
  .empty { color: var(--muted); font-style: italic; margin: 0; }
  .cohort-footer { font-size: 0.85rem; color: var(--muted); text-align: center; }
</style>
