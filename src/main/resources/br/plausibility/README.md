# Catálogo de Regras de Plausibilidade (Synthea-br)

Este diretório contém catálogos versionados de regras de coerência clínica para validação
read-only de `HealthRecord` após geração.

## Convenção de numeração

- Cada regra possui um ID estável no formato `PLAUS-###` (ex.: `PLAUS-001`, `PLAUS-002`).
- Novas regras devem receber o próximo número sequencial disponível no catálogo do domínio.
- O ID nunca deve ser reutilizado após publicação; alterações de critério incrementam a versão
  do catálogo, não o ID.

## Severidade operacional

| Nível  | Significado |
|--------|-------------|
| `alta` | Violação clínica grave; meta MVP SM-2: 0% dos pacientes |
| `média`| Sequência temporal ou coerência improvável; meta MVP SM-2: ≤2% |
| `baixa`| Inconsistência menor ou de contexto; calibrar conforme cohort piloto |

Definição operacional alinhada ao addendum de Rigor de Plausibilidade do PRD Synthea-br.

## Estrutura do catálogo JSON

```json
{
  "version": "1.0.0",
  "catalog": "breast_cancer",
  "rules": [
    {
      "id": "PLAUS-001",
      "severity": "alta",
      "title": "...",
      "description": "..."
    }
  ]
}
```

Implementações Java em `org.mitre.synthea.br.plausibility.rules` referenciam os metadados
deste catálogo via `PlausibilityCatalog`.
