# Matriz de marcos — trajetória câncer de mama (nível Eventos)

**Epic 10 / Story 10.1**  
**Referências:** `example1.md`, `example2.md`  
**Critério SM:** com `br.pathway.archetype` forçado + seed fixa, **100%** dos marcos obrigatórios do arquétipo presentes no export focus / HTML orientador.

## Arquétipo `remission` (example1 — luminal, desfecho favorável)

| Marco | Fase | system | code | display |
|-------|------|--------|------|---------|
| Encontro sintomático | diagnosis | SNOMED-CT | 185345009 | Encounter for symptom (procedure) |
| Mamografia diagnóstica | diagnosis | SNOMED-CT | 241055006 | Mammogram - symptomatic (procedure) |
| Ultrassonografia | diagnosis | SNOMED-CT | 1571000087109 | Ultrasonography of bilateral breasts (procedure) |
| Biópsia | diagnosis | SNOMED-CT | 122548005 | Biopsy of breast (procedure) |
| Condição-alvo | diagnosis | SNOMED-CT | 254837009 | Malignant neoplasm of breast (disorder) |
| RE positivo | diagnosis | LOINC | 85337-4 | Estrogen receptor Ag [Presence] in Breast cancer specimen by Immune stain |
| RP positivo | diagnosis | LOINC | 85339-0 | Progesterone receptor Ag [Presence] in Breast cancer specimen by Immune stain |
| HER2 negativo | diagnosis | LOINC | 85319-2 | HER2 [Presence] in Breast cancer specimen by Immune stain |
| TC tórax | staging | SNOMED-CT | 169069000 | Computed tomography of thorax (procedure) |
| TC abdome | staging | SNOMED-CT | 418714002 | Computed tomography of abdomen (procedure) |
| Cintilografia óssea | staging | SNOMED-CT | 77477000 | Radionuclide imaging of bone (procedure) |
| Estadiamento clínico | staging | LOINC | 21908-9 | Stage group.clinical Cancer |
| Quimioterapia | treatment | SNOMED-CT | 367336001 | Chemotherapy (procedure) |
| Paclitaxel | treatment | RxNorm | 583214 | Paclitaxel 100 MG Injection |
| Cirurgia conservadora | treatment | SNOMED-CT | 392021009 | Lumpectomy of breast (procedure) |
| Radioterapia | treatment | SNOMED-CT | 1287742003 | Radiotherapy (procedure) |
| Anastrozol | follow_up | RxNorm | 199224 | anastrozole 1 MG Oral Tablet |
| Mamografia seguimento | follow_up | SNOMED-CT | 71651007 | Mammography (procedure) |
| Consulta seguimento | follow_up | SNOMED-CT | 185389009 | Follow-up visit (procedure) |

## Arquétipo `progression` (example2 — triplo-negativo, metástase, óbito)

| Marco | Fase | system | code | display |
|-------|------|--------|------|---------|
| Condição-alvo | diagnosis | SNOMED-CT | 254837009 | Malignant neoplasm of breast (disorder) |
| RE negativo | diagnosis | LOINC | 85337-4 | Estrogen receptor Ag [Presence] in Breast cancer specimen by Immune stain |
| RP negativo | diagnosis | LOINC | 85339-0 | Progesterone receptor Ag [Presence] in Breast cancer specimen by Immune stain |
| HER2 negativo | diagnosis | LOINC | 85319-2 | HER2 [Presence] in Breast cancer specimen by Immune stain |
| Triplo-negativo (atributo) | diagnosis | — | `breast_cancer_triple_negative=true` | — |
| TC tórax/abdome estadiamento | staging | SNOMED-CT | 418891003 | Computed tomography of chest and abdomen (procedure) |
| Estádio III | staging | LOINC | 21908-9 | Stage group.clinical Cancer |
| Quimioterapia neoadjuvante | treatment | SNOMED-CT | 367336001 | Chemotherapy (procedure) |
| Mastectomia | treatment | SNOMED-CT | 172138000 | Modified radical mastectomy (procedure) |
| Imagem de progressão | progression | SNOMED-CT | 418891003 | Computed tomography of chest and abdomen (procedure) |
| PET-CT | progression | SNOMED-CT | 78899008 | Positron emission tomography (procedure) |
| Metástase pulmonar | progression | SNOMED-CT | 94222008 | Secondary malignant neoplasm of lung (disorder) |
| Admissão hospice | palliative | SNOMED-CT | 305336008 | Admission to hospice (procedure) |
| Óbito | palliative | — | `person.alive()==false` | — |

## Seeds de teste (fixtures)

| Arquétipo | seed | população | perfil |
|-----------|------|-----------|--------|
| remission | 100100 | 1 | pathway_minimal |
| progression | 100200 | 1 | pathway_minimal |

Configuração comum: `br.target_condition=breast_cancer`, `br.pathway.focus=true`, `exporter.html.pathway_mode=orientador`, `generate.only_alive_patients=false`.
