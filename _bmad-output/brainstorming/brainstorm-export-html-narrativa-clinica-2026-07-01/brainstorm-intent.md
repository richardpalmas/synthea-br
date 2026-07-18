# Intent: Cohort Narrative Viewer

**Origem:** brainstorm export-html-narrativa-clinica (2026-07-01)  
**Destino:** bmad-prd, bmad-spec, bmad-create-story

---

## Problem / Pain

Pesquisadores geram cohorts Synthea com CSVs multi-paciente e multi-arquivo. Reconstituir um paciente exige join manual por ID — oneroso, propenso a erro e inviável na frente de um orientador. A dor central: **informação desconectada** onde deveria haver narrativa clínica associável.

## Job to Be Done

> Como estudante/pesquisador, quero gerar HTML narrativo por paciente via flag de export, para substituir planilhas desconectadas por informação estruturada e apresentável.

**Momento de sucesso:** abrir o laptop, expandir um accordion, mostrar um caso ao orientador e **obter validação dele** — sem cruzar CSV na hora.

## Proposed Solution: Cohort Narrative Viewer

Artefato HTML estático offline que complementa FHIR/CSV (não substitui). Um `index.html` com accordions por paciente; ao expandir, revela narrativa clínica estruturada com todos os dados Synthea daquele paciente — timeline como fio condutor com seções aninhadas.

## MVP Scope (Must)

| Item | Descrição |
|------|-----------|
| Flag de export | `exporter.html.export=true` em `synthea.properties`, espelhando `exporter.fhir.export` |
| HTML estático | Gerado na mesma passada do export FHIR; sem backend |
| Accordions por paciente | Um bloco colapsável por paciente no índice |
| Cabeçalho de triagem | Idade, sexo, condição principal, último evento — escolher paciente antes de expandir |
| Timeline + seções | Cronologia como fio condutor; seções aninhadas: demografia, condições, medicamentos, exames, procedimentos, encounters, cobertura |
| Geração via HealthRecord | A partir de `Person`/`HealthRecord` — zero join pós-CSV |
| FreeMarker template | `resources/templates/html/` alinhado ao padrão C-CDA existente |
| PT-BR | Labels e CID-10 alinhados ao fork BR |
| Cohort piloto | n≈500, arquivo único viável |

## Should / Could / Won't

**Should (v1.1):** nós timeline expansíveis inline (meds/labs/encounter do episódio); rodapé de proveniência (seed, config hash, versão módulos, link manifest.json e bundle FHIR); destaque visual da condição-alvo da cohort; CSS print-friendly; split `index.html` + `patients/{id}.html` para cohorts grandes.

**Could:** busca/filtro no índice (id, idade, sexo, condição); toggle "modo orientador" (narrativa limpa) vs "modo pesquisador" (dados brutos colapsáveis).

**Won't (MVP):** avisos inline de plausibilidade Epic-4; lazy-load avançado; backend.

## Technical Approach

```
synthea.properties
  exporter.html.export=true
        ↓
Exporter (mesma passada FHIR)
  Person / HealthRecord
        ↓
FreeMarker → resources/templates/html/
        ↓
output/html/
  index.html              (cohort pequena)
  patients/{id}.html      (v1.1, cohort grande)
```

- **Zero join pós-CSV** — dados já associados no modelo interno.
- **Offline, estático** — abrir no browser sem servidor.
- **Complementar** — convive com FHIR R4 e CSV existentes.

## UX Essentials

1. **Accordions** — scan rápido da cohort; expandir só o paciente de interesse.
2. **Cabeçalho de triagem** — decisão em segundos sem abrir narrativa completa.
3. **Timeline** — cada evento clínico como marco temporal; fio condutor da narrativa.
4. **Seções aninhadas** — demografia → condições → meds → exames → procedimentos → encounters → cobertura.
5. **Destaque condição-alvo** — condição da cohort (gate) visível ao longo da timeline (v1.1).
6. **Proveniência** — rodapé com seed, hash, versão, links para manifest e FHIR (v1.1).

## Acceptance Criteria (sketch)

1. Com `exporter.html.export=true`, geração de cohort n=10 produz `output/html/index.html` válido e abrível offline.
2. Cada accordion exibe cabeçalho de triagem: idade, sexo, condição principal, último evento.
3. Expandir accordion revela timeline cronológica com pelo menos um evento por paciente.
4. Seções aninhadas presentes e populadas quando dados existem: demografia, condições, medicamentos, exames, procedimentos, encounters, cobertura.
5. Labels em PT-BR; códigos CID-10 quando aplicável no fork BR.
6. HTML gerado na mesma execução do export FHIR, sem passo manual de join.
7. Flag `exporter.html.export=false` (ou ausente) não gera artefato HTML.
8. Cohort piloto n=500 gera arquivo utilizável (< limite prático de tamanho ou split v1.1 documentado).
