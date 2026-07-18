# ADR-002: Política de Rebase com Upstream (synthetichealth/synthea)

**Status:** Aceito  
**Data:** 2026-06-30  
**Autores:** Grupo Synthea-br (PUCPR)

---

## Contexto

O Synthea-br é um fork acadêmico de [`synthetichealth/synthea`](https://github.com/synthetichealth/synthea). A arquitetura (NFR-6) exige compatibilidade upstream periódica para reduzir dívida de merge e preservar exportação FHIR R4 validável.

O PRD (§10, Open Question #3) e a Architecture Spine (§Deferred) registravam a **cadência de rebase como decisão pendente**. O addendum propõe rebase ao final de cada fase do PRD ou a cada semestre letivo.

**Versão upstream de referência nesta decisão:**

- Versão do core Synthea: `4.0.1-SNAPSHOT` (conforme Architecture Spine)
- Commit SHA do fork Synthea-br no momento desta decisão: `0e32c32bec2a5ead6be34749782011528d18a54b`
- Remote upstream: `https://github.com/synthetichealth/synthea.git`

Sem política formal, o fork corre risco de divergência estrutural — especialmente em `build.gradle`, exportadores FHIR e convenções de `org.mitre.synthea.*` — tornando merges futuros custosos e comprometendo a claim de compatibilidade upstream.

---

## Decisão

1. **Cadência:** executar rebase (ou merge cuidadoso) com `synthetichealth/synthea` **ao final de cada fase do PRD** (Fases 0–4) **ou a cada semestre letivo**, o que ocorrer primeiro.
2. **Responsável:** a ser nomeado pelo grupo (orientador ou maintainer do fork) — decisão de governança aberta, não bloqueante para o MVP.
3. **Escopo do rebase:** priorizar `src/main/java/org/mitre/synthea/` (core), `build.gradle`, dependências e testes de exportação FHIR. Extensões em `org.mitre.synthea.br.*` devem permanecer isoladas (AD-7).
4. **Registro:** conflitos significativos ou alterações no core exigem ADR próprio (regra upstream-first do PRD).

---

## Consequências

### Positivas

- Reduz dívida de merge e mantém NFR-6 (compatibilidade upstream).
- Alinha o calendário acadêmico (semestre letivo) ao ciclo de desenvolvimento.
- Fecha formalmente a Open Question #3 do PRD.

### Negativas / trade-offs

- Esforço periódico de resolução de conflitos — especialmente se houver overlap futuro em nomes ou configurações.
- Touchpoints em `build.gradle` (ex.: tasks `versionTxt`, `commitShaTxt`) precisam ser revisados a cada rebase.
- Rebase mal executado pode quebrar `./gradlew check` temporariamente — exige validação completa pós-merge.

### Ações de acompanhamento

- [ ] Nomear responsável pelo rebase no README do grupo.
- [ ] Após cada rebase, executar `./gradlew check` e documentar commit upstream integrado.

---

## Referências

- PRD §10 Open Question #3 — Cadência de rebase
- Addendum — Compatibilidade Upstream
- Architecture Spine — Deferred, Stack (`4.0.1-SNAPSHOT`)
- NFR-6 — Compatibilidade upstream
