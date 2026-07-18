# ADR-005: Fonte de CID-10 BR para nomenclatura clínica (decisão provisória)

**Status:** Proposto  
**Data:** 2026-06-30  
**Autores:** Synthea-br (Story 3.3)

---

## Contexto

O PRD Synthea-br (§10, Open Question #1) e a Architecture Spine (§Deferred) deixam **aberta** a
fonte oficial de CID-10 para o Brasil: DATASUS/TabCID, WHO ICD-10 release, ou subset curado
manualmente.

A Story 3.3 (FR-6) precisa entregar interoperabilidade nacional no export FHIR R4 para a condição
piloto de câncer de mama (SNOMED `254837009`) **sem bloquear** o MVP enquanto a fonte oficial não
é decidida pelo grupo PUCPR.

Alternativas consideradas:

1. **Aguardar decisão de fonte** — bloqueia Epic 3 e validação de cohorts BR.
2. **Adotar WHO ICD-10 como definitivo** — prematuro; TabCID/DATASUS pode divergir em notação ou
   extensões nacionais.
3. **Subset piloto com curadoria manual documentada** — entrega valor de pesquisa imediato com
   natureza explicitamente provisória.

## Decisão

Adotar **provisoriamente** (Story 3.3) um subset piloto curado manualmente em
`src/main/resources/br/coding/snomed_to_cid10_breast_cancer.json`:

| SNOMED-CT  | CID-10 piloto | Justificativa |
|------------|---------------|---------------|
| 254837009  | C50.9         | Neoplasia maligna da mama, não especificada — alinhado ao SNOMED genérico de mama quando subcategoria não está especificada no módulo upstream |

O export FHIR R4 adiciona uma **segunda `Coding` CID-10** ao `Condition.code` quando
`br.profile=br`, preservando a `Coding` SNOMED original (abordagem aditiva, AD-2/AD-8).

**Condição de revisão:** qualquer expansão de FR-6 além do piloto de câncer de mama **requer**
decisão formal sobre a fonte oficial (DATASUS/TabCID vs WHO ICD-10) e atualização deste ADR para
**Aceito** ou **Substituído** antes de ampliar cobertura de condições/códigos.

TUSS/SUS billing permanece **fora de escopo** do MVP (PRD §8.2).

## Consequências

### Positivas

- Cohort piloto exportável com terminologia clínica nacional reconhecível (`C50.9`).
- Mapeamento isolado em `org.mitre.synthea.br.coding.BrCodeMapper` — troca de fonte futura não
  espalha lógica no exportador.
- Natureza provisória explícita evita falsa sensação de conformidade regulatória.

### Negativas / trade-offs

- `C50.9` pode não refletir subcategorias mamárias específicas (`C50.0`–`C50.8`) quando o módulo
  evoluir.
- Curadoria manual não substitui validação contra tabela oficial DATASUS/TabCID.
- Consumidores FHIR devem tratar a `Coding` CID-10 como complementar à SNOMED, não substitutiva.

### Ações de acompanhamento

- [ ] Grupo PUCPR decidir fonte oficial CID-10 BR e promover ADR para **Aceito** ou substituir.
- [ ] Revisar código piloto `C50.9` após decisão de fonte (pode migrar para subcategoria específica).
- [ ] Expandir data pack `br/coding/` somente após ADR atualizado.

---

## Referências

- [Story 3.3](../../_bmad-output/implementation-artifacts/3-3-nomenclatura-clinica-br-cid-10-subset-piloto.md)
- [PRD §10 Open Questions](../../_bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md)
- `src/main/resources/br/coding/snomed_to_cid10_breast_cancer.json`
- `src/main/java/org/mitre/synthea/br/coding/BrCodeMapper.java`
