# Guia Acadêmico de Contribuição — Synthea-br

Guia em Português do Brasil para estudantes e pesquisadores do grupo PUCPR que contribuem com o fork **Synthea-br** — geração de dados clínicos sintéticos para pesquisa acadêmica.

## Por onde começar

Checklist mínimo para novos membros:

1. [ ] Clonar o repositório e instalar pré-requisitos (Java 17, Gradle wrapper)
2. [ ] Executar `./gradlew check` e confirmar que todos os testes passam
3. [ ] Ler o índice de ADRs em [`docs/research/adr/README.md`](research/adr/README.md)
4. [ ] Copiar o [`template de experimento`](research/experiments/experiment-template.md) para uma pasta datada
5. [ ] Rodar uma geração simples: `./run_synthea -s 42 -p 5 Massachusetts`
6. [ ] Copiar `output/manifest.json` para a pasta do experimento
7. [ ] Preencher `experiment.md` com resultados e conclusão

---

## Instalação e execução

### Pré-requisitos

| Requisito | Versão |
|-----------|--------|
| Java (JDK) | 17 ou superior |
| Gradle | 9.2.1 (via wrapper `./gradlew` — não é necessário instalar separadamente) |
| Git | Para clonar e rastreabilidade de commit |

### Comandos essenciais

```bash
# Validar o projeto (testes + checkstyle + JaCoCo)
./gradlew check

# Gerar pacientes sintéticos (exemplo: 10 pacientes em Massachusetts, seed 42)
./run_synthea -s 42 -p 10 Massachusetts

# No Windows, use run_synthea.bat em vez de ./run_synthea
```

Consulte `App.java` e `synthea.properties` para flags adicionais (`-c` para arquivo de config, `--config.chave=valor` para overrides).

### Status das features Synthea-br

As flags abaixo **dependem das Epics 2 e 3** e ainda podem não estar disponíveis no seu checkout:

| Flag | Epic | Descrição |
|------|------|-----------|
| `br.target_condition` | Epic 2 | Condição clínica alvo (ex.: câncer de mama) |
| `br.profile=br` | Epic 3 | Perfil demográfico e geográfico brasileiro |

Enquanto não implementadas, use geração padrão upstream (EUA) para validar o workflow acadêmico.

### Manifest de rastreabilidade

Cada geração produz automaticamente `output/manifest.json` com:

- `seed`, `config_hash`, `commit_sha`, `output_checksum`, `generated_at_iso8601`
- `forkName`, `version`, `profile`, `targetCondition` (proveniência Synthea-br — Story 5.2)

Desabilite temporariamente com `br.manifest.enabled = false` em `synthea.properties`.

**Importante:** ausência de manifest invalida o run para uso acadêmico oficial. Sempre copie o manifest para a pasta do experimento antes de commitar.

### Como citar a proveniência da execução

O `manifest.json` identifica de forma inequívoca o fork, a configuração e a versão do código usados na geração. Use estes campos na seção **Methods** do artigo:

| Campo | Significado | Exemplo |
|-------|-------------|---------|
| `forkName` | Nome do fork acadêmico | `"Synthea-br"` |
| `version` | Versão do fork (`Utilities.SYNTHEA_VERSION`) | `"3.2.0"` |
| `commit_sha` | Hash Git completo do código usado | `"c1247106c03fa57ace54d269af98c7833f4006a6"` |
| `profile` | Perfil geográfico/demográfico ativo; `null` = upstream (EUA) | `"br"` ou `null` |
| `targetCondition` | Condição clínica alvo (`br.target_condition`); `null` se inativa | `"breast_cancer"` ou `null` |
| `seed` | Semente de reprodutibilidade | `42` |
| `config_hash` | Hash SHA-256 da configuração efetiva | (hex) |
| `output_checksum` | Hash determinístico dos arquivos exportados | (hex) |
| `generated_at_iso8601` | Timestamp UTC da geração | `"2026-07-08T22:15:00Z"` |

**Texto sugerido para Methods:**

> Dataset gerado com Synthea-br v{version} (commit `{commit_sha}`), perfil `{profile}`, condição alvo `{targetCondition}`, seed `{seed}`. A reprodutibilidade foi verificada pelo `config_hash` e `output_checksum` registrados em `manifest.json`. Dados 100% sintéticos.

Substitua os valores entre chaves pelos campos reais do seu `manifest.json`. Quando `profile` ou `targetCondition` forem `null`, indique explicitamente no texto (ex.: "perfil upstream (EUA)", "sem condição alvo configurada").

**Bundle FHIR R4:** nesta versão a proveniência citável fica **apenas** no sidecar `manifest.json`. O recurso `Provenance` do Bundle FHIR **não** recebe agente de software adicional identificando Synthea-br (decisão Story 5.2 / AC #6 — estabilidade estrutural do Bundle priorizada). Ferramentas que leem só FHIR não verão esses metadados; use o manifest.

---

## Documentação de experimento

1. Copie [`docs/research/experiments/experiment-template.md`](research/experiments/experiment-template.md) para `docs/research/experiments/YYYY-MM-DD-<slug>/experiment.md`.
2. Documente hipótese, seed, propriedades alteradas e comando exato.
3. Após a geração, copie `output/manifest.json` para a pasta do experimento.
4. Preencha resultados e conclusão.
5. **Não commite** dados de pacientes em `outputs/` — a pasta `output/` do repositório está no `.gitignore`.

Veja o piloto de referência: [`2026-06-30-piloto-template-reprodutibilidade`](research/experiments/2026-06-30-piloto-template-reprodutibilidade/experiment.md).

---

## Citação do fork

Ao preparar artigos para **SBIS**, **CBIS** ou outras venues:

### Formato sugerido (software/dataset)

> Grupo Synthea-br (PUCPR). *Synthea-br: fork acadêmico do Synthea para geração de cohorts sintéticas com contexto brasileiro.* Ano de publicação. Repositório: [URL do fork]. Commit: `<commit_sha>`.

O campo `commit_sha` do `manifest.json` identifica a versão exata do código usada na geração — **inclua-o na seção Methods**.

DOI/Zenodo é opcional no MVP; o grupo pode registrar posteriormente.

### Decisões arquiteturais

Para entender *por que* uma decisão foi tomada (ex.: por que não há IA no MVP), consulte [`docs/research/adr/README.md`](research/adr/README.md) — **não duplique** o conteúdo dos ADRs neste guia.

---

## Disclaimer de dados sintéticos e uso ético

1. **Todos os dados gerados são 100% sintéticos.** Não contêm PHI (Protected Health Information) real. **Nunca** commite dados de pacientes reais no repositório (NFR5).

2. **O Synthea-br não é validado para uso clínico real.** Não deve ser usado para diagnóstico, tratamento ou decisões assistenciais. Conformidade ANVISA/CFM para uso clínico está explicitamente fora de escopo (PRD §7 Non-Goals).

3. **Plausibilidade clínica mede coerência para fins de pesquisa**, não validade clínica certificada. Métricas SM-2 (0% violações alta, ≤2% média) são critérios de qualidade para publicação acadêmica, não certificação regulatória.

4. Use os dados apenas em ambientes de pesquisa, ensino e desenvolvimento de métodos — com citação adequada do fork e do manifest de rastreabilidade.

---

## Exemplo de seção Methods (ilustrativo)

> **Nota:** os valores abaixo são **fictícios/ilustrativos** para demonstrar o formato. Substitua pelos valores reais do seu `manifest.json` e `experiment.md`.

### Métodos

Geramos uma cohort sintética de 500 pacientes com Synthea-br v3.2.0 (commit `a1b2c3d4e5f6789012345678abcdef0123456789`), perfil `br`, condição alvo `breast_cancer`, seed 42. A reprodutibilidade foi verificada pelo `config_hash` e `output_checksum` registrados em `manifest.json`. Exportação FHIR R4; proveniência citável apenas no sidecar (Bundle sem agente Provenance adicional). Dados 100% sintéticos; o pipeline não foi validado para uso clínico.

---

## Referências internas

- [ADRs](research/adr/README.md) — decisões arquiteturais
- [Template de experimento](research/experiments/experiment-template.md)
- [PRD](../_bmad-output/planning-artifacts/prds/prd-synthea-2026-06-29/prd.md)
- [Project context](../_bmad-output/project-context.md) — convenções de código

---

*Última atualização: 2026-07-08*
