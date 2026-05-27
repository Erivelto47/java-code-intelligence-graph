# Validations

Validations registram checks esperados para uma fase. No harness
blueprint-driven, validation checklists sao derivados do blueprint, dos padroes
do projeto e das validacoes existentes.

Elas devem combinar comandos Git, build/testes e verificacoes especificas do
contrato em andamento. Sempre que possivel, devem conter comandos concretos.
Validations nao devem ampliar escopo e devem confirmar explicitamente o fora de
escopo definido pelo blueprint.

Use `validation-checklist.template.md` como base. Checks nao executados devem
ser justificados no report. Se houver conflito entre validation checklist e
blueprint, o blueprint vence e a checklist deve ser corrigida antes da
execucao.

## Pacotes versionados

- `phase-4-2-java-decision-unresolved-early-return.validation.md`: checklist
  derivada e revisavel para a execucao futura da Fase 4.2.
