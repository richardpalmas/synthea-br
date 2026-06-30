# Addendum — Synthea-br PRD

Material técnico e decisões que complementam o PRD principal sem inflar o documento de requisitos.

---

## Spike IA vs Regras — Escopo de Investigação (atualizado)

**Restrição confirmada:** lab PUCPR **não possui GPU nem API** para LLMs no MVP.

**Implicação:** spike **não executa** pipeline IA. Entregáveis:

1. Cohort piloto (n=100–500) com abordagem **regras puras** (GMF + FR-8).
2. Métricas SM-2 aplicadas — calibrar regras até maior rigor viável.
3. Revisão bibliográfica: trabalhos que usam IA para dados sintéticos clínicos — quando justifica custo/infra.
4. ADR com recomendação: **MVP = regras puras**; IA como **fase futura** se SM-2 não for atingível após esgotar regras, ou se publicação exigir comparativo.

| Abordagem | MVP | Futuro |
|-----------|-----|--------|
| **A — Regras puras** | ✅ Implementar | Manter como baseline |
| **B — Pós-processamento IA** | ❌ Spike documental only | Reavaliar com budget/API |
| **C — Híbrido** | ❌ | Após A esgotado + infra |

---

## Rigor de Plausibilidade — Definição Operacional

**SM-2 (maior rigor viável):**

| Severidade | Definição exemplo | Meta MVP |
|------------|-------------------|----------|
| **Alta** | Contradição clínica grave (ex.: mastectomia sem diagnóstico de câncer; medicamento contraindicado documentado) | **0%** pacientes |
| **Média** | Sequência temporal improvável ou código inconsistente com condição | **≤ 2%** pacientes |
| **Baixa** | Detalhe cosmético ou terminologia subótima | Documentar; não bloqueia publicação |

Processo: iterar catálogo FR-8 até SM-2 passar ou ADR registrar limite atingido com justificativa para revisores.

---

## Publicação e Monetização Futura

**MVP orientado a:**
- Artigos (revistas) e **anais de conferência** — venues confirmados: **SBIS, CBIS** (ecossistema BR de informática em saúde).
- Repositório público **Synthea-br** citável (DOI/Zenodo opcional pós-MVP).
- Template de experimento → seção **Methods** do paper.

**Monetização futura (fora MVP):** arquitetura deve permitir, sem otimizar agora:
- Open-core (engine OSS + módulos/datasets premium).
- Consultoria ou geração de cohorts customizadas para parceiros.
- Licenciamento de datasets sintéticos especializados.

`[NOTE FOR PM]` Novo PRD ou addendum de produto quando monetização for prioridade — **modelo ainda sem definição**.

**CID-10 BR (TBD):** opções a avaliar no grupo — DATASUS/TabCID, WHO ICD-10 release, subset curado manualmente para câncer de mama no MVP. Decisão deve virar ADR antes de expandir FR-6 além do piloto.

---

## Localização BR — Camadas Técnicas (referência)

1. **Demografia:** substituir demographics US por CSV IBGE (sexo, idade, raça/cor).
2. **Geografia:** estados/municípios BR; quadtree ou equivalente se necessário.
3. **Nomenclatura:** mapeamento SNOMED/ICD-10 US → CID-10 para condição piloto.
4. **Providers/payers:** datasets BR em CSV (UBS, hospital, plano genérico).
5. **Nomes:** `names.yml` ou equivalente com distribuição BR.

Ordem sugerida alinhada ao PRD Fase 2.

---

## Workflow Acadêmico — Estrutura de Pastas Proposta

```
docs/research/
  experiments/
    YYYY-MM-DD-<slug>/
      experiment.md      # template FR-11
      manifest.json      # FR-13
      outputs/           # gitignored ou LFS se grande
  adr/
    ADR-001-<title>.md   # FR-12
  CONTRIBUTING-ACADEMICO.md  # FR-14
```

Integração opcional com BMad: épicos/stories referenciam experimentos; `bmad-retrospective` ao fim de cada fase.

---

## Compatibilidade Upstream

Recomendação: manter diffs localizados em:
- `src/main/resources/` (datasets BR, módulos)
- Novos pacotes `org.mitre.synthea.br.*` ou `org.mitre.synthea.pucpr.*` para extensões sem poluir core
- Properties prefixadas `synthea-br.*` ou `br.*` em `synthea.properties`

Facilita rebase periódico com `synthetichealth/synthea`.

**Política de rebase (confirmada):**
- Manter fork alinhável ao upstream — evitar divergência estrutural no core Java.
- Cadência sugerida: fim de cada fase do PRD ou semestre letivo; ADR registra versão upstream base e conflitos resolvidos.
- Responsável: a definir no grupo (orientador / maintainer do fork).
