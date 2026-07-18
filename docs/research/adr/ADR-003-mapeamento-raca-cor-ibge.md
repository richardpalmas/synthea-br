# ADR-003: Mapeamento raça/cor IBGE → categorias internas Synthea

**Status:** Proposto  
**Data:** 2026-06-30  
**Autores:** Synthea-br (Story 3.1)

---

## Contexto

O Censo IBGE classifica cor ou raça em cinco categorias: branca, preta, parda, amarela e indígena.
A categoria **parda** (~45% da população brasileira, Censo 2022) não possui equivalente direto
nas categorias de raça do US Census usadas internamente pelo Synthea (`white`, `black`, `asian`,
`native`, `other`, `hawaiian`) e alimentam exportação FHIR (`us-core-race`, OMB) em `FhirR4.java`.

A Story 3.1 precisa recalibrar proporções demográficas sem quebrar exportadores FHIR existentes
(AD-7: alterações que tocam exportadores exigem justificativa).

Alternativas consideradas:

1. Introduzir nova categoria interna `parda` — exigiria alterar exportadores DSTU2/STU3/R4.
2. Mapear `parda` integralmente para `other` — concentra ~45% em `other`, distorcendo semântica FHIR.
3. **Split proporcional interim** entre `black` e `other` — preserva chaves internas existentes.

## Decisão

Adotar **provisoriamente** (Story 3.1) o split documentado em `BrRaceMapper`:

| IBGE   | Categoria interna Synthea |
|--------|---------------------------|
| branca | white (1:1)               |
| preta  | black (1:1)               |
| amarela| asian (1:1)               |
| indígena | native (1:1)            |
| parda  | 40% → black, 60% → other  |

Chaves internas permanecem inalteradas; apenas frequências são recalibradas a partir do data pack
IBGE em `src/main/resources/br/demographics/distribuicao_nacional.csv`.

## Consequências

### Positivas

- Export FHIR R4/STU3/DSTU2 permanece compatível sem alteração imediata.
- Mapeamento isolado em `BrRaceMapper` — revisão futura não espalha lógica.
- Data pack documenta ambiguidade no cabeçalho do CSV.

### Negativas / trade-offs

- Split 40/60 de `parda` é **heurístico**, não epidemiologicamente validado.
- Distribuição interna de raça não reflete taxonomia IBGE nos exports FHIR (limitação conhecida).

### Ações de acompanhamento

- [ ] Grupo PUCPR revisar split `parda` e promover ADR para **Aceito** ou ajustar ratios.
- [ ] Avaliar extensão FHIR BR (Epic 3.3) para codificação explícita de cor/raça IBGE no export.

---

## Referências

- [Story 3.1](../../_bmad-output/implementation-artifacts/3-1-perfil-demografico-brasileiro-ibge.md)
- [IBGE Censo 2022 — cor ou raça](https://www.ibge.gov.br/estatisticas/sociais/populacao/22827-censo-demografico-2022.html)
- `src/main/java/org/mitre/synthea/br/demographics/BrRaceMapper.java`
- `src/main/java/org/mitre/synthea/world/geography/Demographics.java#pickRace`
