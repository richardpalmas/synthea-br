# ADR-006: Interface Web Local para Geração de Cohort (MVP)

**Status:** Aceito  
**Data:** 2026-07-01  
**Decisores:** PO Synthea-br (PUCPR)

## Contexto

O PRD original (`prd.md` §7) listava **GUI web em v1** como non-goal, assumindo que pesquisadores usariam CLI e `synthea.properties`. Em lab acadêmico, estudantes e orientadores encontraram barreira de entrada alta (CMD, flags, ordem de argumentos).

O Epic 6 (Story 6.1) cobre **export HTML narrativo** da cohort já gerada — não substitui a configuração/execução via terminal.

## Decisão

Adotar uma **interface web local MVP** (Epic 7, Story 7.1):

- Servidor HTTP embarcado JDK (`com.sun.net.httpserver`) no mesmo processo JVM que o `Generator` (AD-1).
- Bind padrão `127.0.0.1` — uso local/lab, sem autenticação no MVP.
- Formulário PT-BR para parâmetros principais; geração assíncrona com um job por vez.
- CLI (`run_synthea`, `App.main`) permanece caminho avançado e preferido para reprodutibilidade documentada em papers.
- Entry points: `./gradlew runWeb` e `run_synthea.bat --web`.

## Consequências

**Positivas:**

- Reduz fricção para cohorts piloto em aulas e validação com orientadores.
- Reutiliza `Generator` + `Config` — sem `Runtime.exec` do batch.

**Negativas / limites:**

- Reverte non-goal GUI do PRD §7 para escopo MVP local (não deploy remoto).
- Sem fila paralela, auth ou upload de properties customizadas na v1.
- Expor `0.0.0.0` exige ADR/revisão explícita — risco em redes compartilhadas.

## Referências

- Story 7.1: `_bmad-output/implementation-artifacts/7-1-interface-web-geracao-sintetica-mvp.md`
- PRD §7 (non-goal superseded para MVP local)
- ARCHITECTURE-SPINE AD-1, AD-7
