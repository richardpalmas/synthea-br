---
id: SPEC-cohort-narrative-viewer
companions:
  - conventions.md
  - ux-structure.md
  - architecture-diagrams.md
  - ../project-context.md
sources:
  - ../../brainstorming/brainstorm-export-html-narrativa-clinica-2026-07-01/brainstorm-intent.md
---

> **Contrato canônico.** Esta SPEC e os arquivos em `companions:` definem o que construir, testar e validar. Fontes listadas em `sources:` são rastreabilidade — consultar apenas para contexto narrativo.

# Cohort Narrative Viewer

## Why

Pesquisadores do Synthea-br geram cohorts com múltiplos CSVs desconectados; reconstituir um paciente exige join manual por ID — oneroso, propenso a erro e inviável na frente de um orientador. A dor é **informação desconectada** onde deveria haver narrativa clínica associável. Oportunidade: artefato HTML estático offline que complementa FHIR/CSV e permite validação de caso em segundos (expandir accordion, mostrar timeline, obter ok do orientador).

## Capabilities

- **CAP-1**
  - **intent:** Pesquisador habilita ou desabilita a geração do visualizador narrativo via flag de configuração no mesmo padrão dos exportadores existentes.
  - **success:** Com `exporter.html.export=true`, export roda; com `false` ou ausente, nenhum artefato HTML é criado em `output/html/`.

- **CAP-2**
  - **intent:** Sistema gera HTML estático offline na mesma execução de geração/export, derivando dados diretamente de `Person`/`HealthRecord` sem join pós-CSV.
  - **success:** Cohort n=10 com flag ativa produz `output/html/index.html` abrível no browser sem servidor; dados do paciente no HTML correspondem ao HealthRecord da mesma execução (sem passo manual de merge).

- **CAP-3**
  - **intent:** Pesquisador percorre a cohort via índice com um bloco colapsável por paciente e cabeçalho de triagem antes de expandir.
  - **success:** Cada accordion exibe idade, sexo, condição principal e último evento no header colapsado; expandir revela conteúdo daquele paciente.

- **CAP-4**
  - **intent:** Pesquisador segue a trajetória clínica do paciente por timeline cronológica com seções clínicas aninhadas.
  - **success:** Conteúdo expandido inclui timeline com ≥1 evento por paciente e seções demografia, condições, medicamentos, exames, procedimentos, encounters e cobertura — populadas quando dados existem no HealthRecord.

- **CAP-5**
  - **intent:** Pesquisador lê labels e terminologia alinhados ao fork BR (PT-BR, CID-10 quando aplicável).
  - **success:** Labels de seção e UI em português; condições piloto exibem CID-10 BR quando mapping Story 3.3 disponível.

## Constraints

- Exportação read-only sobre `HealthRecord` — AD-2 da arquitetura; preparação para template não altera estado clínico permanente.
- Templates FreeMarker em `resources/templates/html/`; padrão de integração espelha `CCDAExporter` e flags `exporter.*` em `synthea.properties`.
- Artefato **complementar** a FHIR R4 e CSV — não substitui contratos de interoperabilidade existentes.
- HTML estático sem backend; abre offline no browser.
- Extensões preferencialmente em `org.mitre.synthea.export` / `org.mitre.synthea.br.*`; alterações no core upstream exigem justificativa (AD-7).

## Non-goals

- Avisos inline de plausibilidade (Epic 4) no HTML.
- Backend, lazy-load avançado ou paginação dinâmica.
- Nós de timeline expansíveis inline, rodapé de proveniência completo, busca/filtro no índice, toggle orientador/pesquisador, split `patients/{id}.html`, CSS print-friendly (todos v1.1).

## Success signal

Pesquisador gera cohort piloto (n=10 validação, n≈500 uso real), abre `output/html/index.html` offline, expande um accordion, percorre timeline e seções aninhadas, e apresenta o caso ao orientador obtendo validação **sem cruzar planilhas**.

## Assumptions

- Condição principal no cabeçalho deriva de `br.target_condition` quando gate ativo; caso contrário, heurística documentada (condição crônica mais relevante).
- HTML é gerado na mesma execução que FHIR quando ambas as flags estão ativas.
- Cohort piloto n≈500 permanece em arquivo único viável; se limite prático for atingido, documentar e apontar split v1.1.

## Open Questions

- Qual tamanho máximo aceitável de `index.html` para n=500 antes de exigir split por paciente na v1.1?
