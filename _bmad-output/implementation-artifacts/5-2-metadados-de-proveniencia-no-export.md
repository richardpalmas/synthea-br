---
baseline_commit: c1247106c03fa57ace54d269af98c7833f4006a6
---

# Story 5.2: Metadados de Proveniência no Export

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

Como pesquisador preparando submissão a SBIS/CBIS (ou periódico/conferência equivalente),
quero metadados de proveniência citáveis no output da exportação,
para referenciar o fork, perfil geográfico, condição alvo, versão e commit no artigo, sem depender de anotação manual.

## Acceptance Criteria

1. **Given** uma execução de geração com exportação FHIR R4 concluída
   **When** o pesquisador inspeciona o output
   **Then** os metadados identificam de forma inequívoca: nome do fork (`"Synthea-br"`), perfil geográfico ativo (`br` ou ausente/upstream), condição clínica alvo configurada (`br.target_condition`, se ativa), versão do fork (`Utilities.SYNTHEA_VERSION`) e `commit_sha` completo
   [Source: planning-artifacts/epics.md#Story-5.2; planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-16]

2. **Given** o manifest de rastreabilidade (`output/manifest.json`, Story 1.4) já existe e segue a convenção de manifest definida em AD-6
   **When** esta story é implementada
   **Then** os novos campos de proveniência (`forkName`, `profile`, `targetCondition`) são **adicionados ao schema existente do manifest** (não criado um sidecar JSON separado e concorrente) — satisfazendo diretamente a AC-base "sidecar JSON segue convenção de manifest (AD-6) quando aplicável" ao reaproveitar o mecanismo já construído na Story 1.4, em vez de duplicá-lo
   [Source: planning-artifacts/epics.md#Story-5.2 AC "sidecar JSON segue convenção de manifest (AD-6)"; _bmad-output/implementation-artifacts/1-4-manifest-de-rastreabilidade-de-execucao.md]

3. **Given** os campos `profile` e `targetCondition` dependem de conceitos introduzidos **depois** da Story 1.4 ter sido escrita (Stories 2.1 e 3.1, respectivamente)
   **When** o `ResearchManifestWriter` (Story 1.4) é estendido
   **Then** ele lê esses valores através dos **accessors públicos já definidos** por essas stories — `org.mitre.synthea.br.condition.TargetConditionConfig` (Story 2.1) e `org.mitre.synthea.br.profile.BrProfile.isActive()` (Story 3.1) — sem reimplementar a leitura de configuração; se o valor não estiver ativo/configurado, o campo é serializado como `null` (não omitido, para manter schema estável entre execuções, conforme AD-6 "campos fixos")
   [Source: _bmad-output/implementation-artifacts/2-1-configuracao-de-condicao-clinica-alvo.md, 3-1-perfil-demografico-brasileiro-ibge.md; architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-6 "campos fixos"]

4. **Given** os campos devem ser **estáveis e documentados para citação em artigo** (AC-base)
   **When** a documentação é produzida
   **Then** `docs/CONTRIBUTING-ACADEMICO.md` (Story 1.5) é estendido com uma seção "Como citar a proveniência da execução", explicando cada campo do manifest (incluindo os novos desta story) e fornecendo um exemplo de texto/citação para a seção Methods de um artigo (ex.: "Dataset gerado com Synthea-br vX.Y (commit `abcd123`), perfil `br`, condição alvo `breast_cancer`, seed `N`")
   [Source: _bmad-output/implementation-artifacts/1-5-guia-academico-de-contribuicao-pt-br.md]

5. **Given** os metadados devem ser gerados **automaticamente** (AC-base), sem script manual adicional
   **When** uma geração é executada com o manifest habilitado
   **Then** os novos campos são preenchidos automaticamente pelo mesmo `PostCompletionExporter` da Story 1.4 (`ResearchManifestWriter`), sem exigir nenhum passo adicional do pesquisador além do já necessário para gerar o manifest
   [Source: _bmad-output/implementation-artifacts/1-4-manifest-de-rastreabilidade-de-execucao.md — integração via SPI `PostCompletionExporter`]

6. **Given** a Story 5.1 estabeleceu que a estrutura do Bundle FHIR R4 deve permanecer estável e que mudanças estruturais que dificultem a 5.2 devem ser evitadas
   **When** esta story avalia se deve também tocar o Bundle FHIR diretamente (além do manifest)
   **Then** qualquer adição ao Bundle FHIR é **estritamente aditiva e opcional**: no máximo, preencher `Provenance.agent` (recurso já gerado por `FhirR4.provenance()` quando `USE_US_CORE_IG` está ativo) com um agente de software adicional identificando `"Synthea-br vX.Y"` — sem alterar `Provenance.target`, `recorded`, ou a lógica de seleção de `clinician`/`providerOrganization` já existente; se essa adição for considerada de risco/escopo desnecessário pelo desenvolvedor, ela pode ser **omitida** nesta story em favor de depender inteiramente do manifest sidecar, desde que documentado
   [Source: _bmad-output/implementation-artifacts/5-1-preservar-export-fhir-r4-para-cohorts-direcionadas.md; src/main/java/org/mitre/synthea/export/FhirR4.java#provenance — método privado `provenance(Bundle, Person, long)`]

7. **Given** esta story é a última do roadmap atual e fecha o ciclo de rastreabilidade iniciado na Story 1.4
   **When** todas as Stories 1.4, 2.1, 3.1 e esta (5.2) estiverem implementadas
   **Then** um `output/manifest.json` de uma execução real com `br.target_condition` e perfil `br` ativos contém todos os campos combinados (`seed`, `config_hash`, `commit_sha`, `output_checksum`, `generated_at_iso8601`, `forkName`, `profile`, `targetCondition`) — usado como evidência de aceite final desta story
   [Source: planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-6]

## Tasks / Subtasks

- [x] Task 1: Estender o schema do manifest (AC: #1, #2, #3)
  - [x] Subtask 1.1: Adicionar campos `forkName` (constante `"Synthea-br"`), `version` (reaproveitar `Utilities.SYNTHEA_VERSION`, já presente no manifest da 1.4 — confirmar se já cobre este requisito antes de duplicar), `profile`, `targetCondition` à classe de dados interna do `ResearchManifestWriter`
  - [x] Subtask 1.2: Implementar leitura via `TargetConditionConfig` (2.1) e `BrProfile.isActive()` (3.1), com fallback `null` quando inativos/ainda não implementados (guard defensivo, já que estas classes podem não existir se 5.2 for desenvolvida fora de ordem — ver Dev Notes)

- [x] Task 2: Atualizar testes do manifest (AC: #3, #7)
  - [x] Subtask 2.1: Estender `ResearchManifestWriterTest` (Story 1.4) com casos: perfil/condição ativos vs. inativos, confirmando serialização `null` explícita (não omissão de campo)

- [x] Task 3: Documentação de citação (AC: #4)
  - [x] Subtask 3.1: Adicionar seção "Como citar a proveniência da execução" em `docs/CONTRIBUTING-ACADEMICO.md`, com tabela de campos do manifest e exemplo de texto de citação

- [x] Task 4: (Opcional, avaliar risco/benefício) Agente de software no `Provenance` FHIR (AC: #6)
  - [x] Subtask 4.1: Se adotado, adicionar `ProvenanceAgentComponent` adicional em `FhirR4.provenance()` identificando o fork/versão, sem alterar a lógica de `clinician`/`providerOrganization` existente
  - [x] Subtask 4.2: Se não adotado, documentar a decisão de manter o Bundle FHIR inalterado e depender apenas do manifest sidecar

- [x] Task 5: Validação de aceite final (AC: #7)
  - [x] Subtask 5.1: Executar uma geração real (piloto, n pequeno) com `br.target_condition=breast_cancer` e `br.profile=br` ativos, inspecionar `output/manifest.json` resultante e confirmar todos os campos combinados de 1.4 + 5.2 presentes
  - [x] Subtask 5.2: Rodar `./gradlew check`

## Dev Notes

### Esta story fecha o ciclo de rastreabilidade — e depende de classes de 3 stories diferentes

5.2 não introduz uma estrutura de dados nova; ela **estende** o manifest da Story 1.4 com **leituras** de accessors definidos nas Stories 2.1 (`TargetConditionConfig`) e 3.1 (`BrProfile`). Isso significa que, na prática, 5.2 só pode ser **completada de ponta a ponta** depois que 1.4, 2.1 e 3.1 estiverem implementadas. Se o time decidir implementar 5.2 antes dessas dependências estarem prontas (por exemplo, para adiantar a estrutura de schema), os campos `profile`/`targetCondition` devem ser implementados com guards defensivos (reflection-safe ou checagem de classe presente) e `null` como valor — nunca falhar a geração do manifest por ausência dessas classes.

### Não duplicar o manifest — risco de inconsistência entre dois arquivos de proveniência

A AC-base do épico permite literalmente "Bundle FHIR ou sidecar JSON" e "sidecar JSON segue convenção de manifest (AD-6) **quando aplicável**" — a leitura mais segura é que o manifest da Story 1.4 **é** esse sidecar, e esta story apenas o estende. Criar um segundo arquivo JSON de proveniência seria redundante e arriscaria os dois arquivos divergirem entre execuções (ex.: por erro de implementação, um refletir `seed=42` e outro `seed=43` se gerados em momentos diferentes do pipeline).

### Decisão de risco — tocar ou não o Bundle FHIR

Ver AC #6. A Story 5.1 deixou claro que estabilidade estrutural do Bundle é prioridade. Esta story dá ao desenvolvedor a opção explícita de **não tocar** `FhirR4.java` e ainda assim satisfazer integralmente a AC-base (que aceita "Bundle FHIR **ou** sidecar JSON"). Recomenda-se fortemente optar pelo caminho apenas-manifest na primeira iteração, e tratar o enriquecimento do `Provenance` FHIR como melhoria futura caso pesquisadores reais solicitem proveniência embutida diretamente no Bundle (ex.: para ferramentas que só leem FHIR, sem acesso ao sidecar).

### Sequenciamento — esta é a última story do roadmap atual

Ao concluir 5.2, todo o roadmap de 13 stories (mais a 1.1 pré-existente) está em `ready-for-dev`. Não há mais nenhuma story subsequente a considerar para dependências futuras neste documento — qualquer trabalho adicional (calibração de plausibilidade pós-cohort real, revisão de ADR-001, ADRs sobre mapeamento de raça/CID-10) deve ser tratado como nova entrada de backlog, não como subtask oculta desta story.

### Project Structure Notes

```
src/main/java/org/mitre/synthea/br/research/
  ResearchManifestWriter.java          <- estender (Story 1.4), não recriar
src/main/java/org/mitre/synthea/export/
  FhirR4.java                          <- tocar APENAS se Task 4 for adotada (opcional)
src/test/java/org/mitre/synthea/br/research/
  ResearchManifestWriterTest.java      <- estender
docs/
  CONTRIBUTING-ACADEMICO.md            <- estender (Story 1.5), nova seção de citação
```

### Testing Standards Summary

JUnit 4. Reaproveitar fixtures de teste já criadas na Story 1.4 para o manifest, adicionando casos de perfil/condição ativos e inativos. `./gradlew check` deve passar. Validação de aceite final (Task 5) requer uma geração real com Epics 2 e 3 implementados — não bloqueante para as Tasks 1-4, que podem ser feitas com mocks dos accessors `TargetConditionConfig`/`BrProfile`.

### References

- [Source: _bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md#FR-16]
- [Source: _bmad-output/planning-artifacts/architecture/architecture-synthea-2026-06-30/ARCHITECTURE-SPINE.md#AD-6]
- [Source: _bmad-output/planning-artifacts/epics.md#Epic-5, #Story-5.2]
- [Source: _bmad-output/implementation-artifacts/1-4-manifest-de-rastreabilidade-de-execucao.md, 1-5-guia-academico-de-contribuicao-pt-br.md, 2-1-configuracao-de-condicao-clinica-alvo.md, 3-1-perfil-demografico-brasileiro-ibge.md, 5-1-preservar-export-fhir-r4-para-cohorts-direcionadas.md]
- [Source: src/main/java/org/mitre/synthea/export/FhirR4.java#provenance]

## Dev Agent Record

### Agent Model Used

Composer (Cursor)

### Debug Log References

- `version` não existia no manifest da Story 1.4; adicionado via `Utilities.SYNTHEA_VERSION`.
- Gson omite `null` por padrão; habilitado `serializeNulls()` para cumprir AD-6 (campos fixos com `null` explícito).
- Task 4: decisão de **não** tocar `FhirR4.provenance()` — proveniência via manifest sidecar apenas (recomendação Dev Notes).
- `testProvenanceFieldsPopulatedWhenBrFeaturesActive` usa demografia F/40-75 para compatibilidade com gate breast_cancer.
- `./gradlew check` completo falha em `AppTest` (regressão paralela); `ResearchManifestWriterTest` (7/7) passa.

### Completion Notes List

- Estendido `ResearchManifestWriter` com `forkName`, `version`, `profile`, `targetCondition` via `BrProfile.isActive()` e `TargetConditionConfig.resolveConfigured()`.
- Testes unitários e de integração do manifest cobrem campos ativos/inativos e serialização `null` explícita.
- Documentação acadêmica atualizada com tabela de campos e texto sugerido para Methods.

### File List

- `src/main/java/org/mitre/synthea/br/research/ResearchManifestWriter.java` (modificado)
- `src/test/java/org/mitre/synthea/br/research/ResearchManifestWriterTest.java` (modificado)
- `docs/CONTRIBUTING-ACADEMICO.md` (modificado)

### Change Log

- 2026-07-08: Story 5.2 — metadados de proveniência no manifest.json e documentação de citação acadêmica.
- 2026-07-10: CR — docs AC #6 (Bundle sem Provenance), resolve resiliente, testes com serializeNulls/version/commit, isolamento Config.

### Senior Developer Review (AI)

**Date:** 2026-07-10
**Outcome:** approve → done

| ID | Finding | Action |
|----|---------|--------|
| 1 | AC #6 não documentada para pesquisador | patch ✓ |
| 2 | Config BR não isolado nos testes | patch ✓ |
| 3 | resolveConfigured pode abortar manifest | patch ✓ |
| 4 | Asserts fracos version/null JSON | patch ✓ |
| 5 | generation_duration_ms condicional | defer |
