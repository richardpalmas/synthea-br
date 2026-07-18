# Estrutura UX — Cohort Narrative Viewer

## Persona e momento de uso

**Pesquisador/estudante** apresenta um caso clínico sintético ao orientador. Sucesso = expandir um accordion, percorrer timeline e seções, obter validação **sem cruzar CSVs**.

## Hierarquia da página (`index.html`)

```
index.html
├── Cabeçalho da cohort (título, contagem de pacientes, data de geração)
├── Accordion paciente 1 [colapsado]
│   ├── Cabeçalho de triagem: idade | sexo | condição principal | último evento
│   └── Conteúdo expandido
│       ├── Timeline (fio condutor cronológico)
│       └── Seções aninhadas (colapsáveis ou agrupadas)
│           ├── Demografia
│           ├── Condições
│           ├── Medicamentos
│           ├── Exames
│           ├── Procedimentos
│           ├── Encounters
│           └── Cobertura
├── Accordion paciente 2 ...
└── (rodapé mínimo MVP — proveniência completa é v1.1)
```

## Padrões de interação MVP

| Padrão | Comportamento |
| --- | --- |
| Accordions | `<details>/<summary>` nativos HTML5 — funciona offline, sem JS obrigatório |
| Scan rápido | Todos os pacientes visíveis colapsados; triagem pelo header sem expandir |
| Timeline | Lista cronológica de marcos; cada item com data ISO + rótulo PT-BR |
| Seções vazias | Ocultar seção ou exibir "Sem registros" — nunca quebrar layout |
| Offline | Arquivo único, paths relativos, CSS inline ou `<style>` embutido |

## CSS mínimo

- Tipografia legível (system font stack)
- Contraste adequado para projeção em reunião
- Accordion/header com destaque visual da condição principal
- Timeline com linha vertical ou lista datada clara

Print-friendly e split `patients/{id}.html` ficam fora do MVP (v1.1).

## Fora do MVP (referência v1.1)

- Nós da timeline expansíveis inline (meds/labs do episódio)
- Rodapé de proveniência (seed, config hash, manifest, FHIR link)
- Destaque visual da condição-alvo da cohort ao longo da timeline
- Busca/filtro no índice
- Toggle modo orientador vs pesquisador
