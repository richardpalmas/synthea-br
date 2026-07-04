# ADR-006: Terminologia clínica PT-BR na exportação

**Status:** Aceito  
**Data:** 2026-07-02  
**Relacionado:** ADR-005 (CID-10 BR), Story 3.3, Epic 6 (HTML narrativo)

## Contexto

Com `br.profile=br`, demografia, geografia e providers já refletem o contexto brasileiro, mas os **textos clínicos** (condições, medicamentos, procedimentos, exames) permaneciam em inglês — herdados do campo `display` dos módulos GMF upstream. Pesquisadores e estudantes precisam ler cohorts em português do Brasil nos exportadores HTML, FHIR R4 e CSV.

## Decisão

1. **Camada de resolução na exportação** — `BrTerminologyResolver` em `org.mitre.synthea.br.terminology` resolve `display_pt` a partir de data packs JSON em `src/main/resources/br/terminology/`, **sem mutar** `HealthRecord` (AD-2).

2. **Ativação** — somente quando `BrProfile.isActive()` (`br.profile=br`).

3. **Fallback** — sem mapeamento por `(system, code)`, tenta `resolveDisplayText(display)` (labels LABEL), depois CID-10 piloto para SNOMED, e por fim mantém inglês upstream. Relatório incremental em `output/.../br/terminology/_unmapped_report.csv` via `BrUnmappedCodeReporter` (PostCompletionExporter).

4. **Fonte das traduções** — curadoria manual acadêmica + descritores LOINC em português + descrições CID-10 (WHO). **Não** redistribuir SNOMED CT BR licenciado no repositório.

5. **Codificação preservada** — códigos SNOMED/RxNorm/LOINC nos exports permanecem inalterados; apenas `display`/`text` human-readable são localizados. CID-10 aditivo (Story 3.3) continua em FHIR R4.

6. **Datas** — HTML usa `dd/MM/yyyy` com perfil BR; CSV mantém ISO `yyyy-MM-dd` para análise estatística.

7. **Declaração de Óbito (DO)** — `BrDeathCertification` substitui displays US (`69409-1`, `69453-9`) por conceito brasileiro (SIM/MS) na simulação quando `br.profile=br`, preservando códigos LOINC upstream.

## Consequências

- Expansão incremental: editar/criar JSON em `br/terminology/`, reexecutar geração.
- Cobertura cohort câncer de mama (2026-07): **~250+ termos** em 11 data packs; pilot run ~**84%** descrições clínicas em PT-BR nos CSV (wellness, SDOH, dental, dor; TNM/oncologia avançada ainda parcial).
- Termos não mapeados continuam em inglês — expandir via `_unmapped_report.csv`.

## Alternativas rejeitadas

| Alternativa | Motivo |
|-------------|--------|
| Duplicar módulos GMF em PT-BR | Alto custo de manutenção e rebase upstream |
| Tradução automática via LLM | Fora do escopo acadêmico MVP; sem API no lab |
| Import bulk SNOMED CT BR | Licenciamento e tamanho do dataset |
