#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  ./harness/bin/run-next-phase.sh
  ./harness/bin/run-next-phase.sh --dry-run
USAGE
}

fail() {
  echo "Error: $*" >&2
  exit 1
}

dry_run=0

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "${1:-}" == "--dry-run" ]]; then
  dry_run=1
  shift
fi

if [[ $# -ne 0 ]]; then
  usage >&2
  exit 2
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

phase_index="harness/phases/phase-index.tsv"
prompt_template="harness/prompts/codex-execution-prompt.template.txt"
prompt_dir="build/harness/prompts"

[[ -f "${phase_index}" ]] || fail "Phase index not found: ${phase_index}"
[[ -f "${prompt_template}" ]] || fail "Codex prompt template not found: ${prompt_template}"
[[ -x "./harness/bin/run-phase.sh" ]] || fail "Phase runner is not executable: ./harness/bin/run-phase.sh"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  fail "Runner must be executed inside a Git work tree"
fi

branch="$(git branch --show-current)"
if [[ -z "${branch}" ]]; then
  branch="DETACHED_HEAD"
fi

next_count=0
phase_id=""
blueprint_path=""
report_path=""
next_ids=()
line_number=0

while IFS=$'\t' read -r order id status blueprint report commit extra || [[ -n "${order:-}" ]]; do
  line_number=$((line_number + 1))

  if [[ "${line_number}" -eq 1 ]]; then
    continue
  fi

  [[ -n "${order}${id}${status}${blueprint}${report}${commit:-}${extra:-}" ]] || continue

  status="${status%$'\r'}"
  id="${id%$'\r'}"
  blueprint="${blueprint%$'\r'}"
  report="${report%$'\r'}"

  if [[ "${status}" == "next" ]]; then
    next_count=$((next_count + 1))
    next_ids+=("${id}")
    phase_id="${id}"
    blueprint_path="${blueprint}"
    report_path="${report}"
  fi
done <"${phase_index}"

if [[ "${next_count}" -eq 0 ]]; then
  fail "No phase marked as next in ${phase_index}"
fi

if [[ "${next_count}" -gt 1 ]]; then
  printf 'Error: Multiple phases marked as next in %s:\n' "${phase_index}" >&2
  printf '  %s\n' "${next_ids[@]}" >&2
  exit 1
fi

[[ -n "${phase_id}" ]] || fail "Next phase id is empty in ${phase_index}"
[[ -n "${blueprint_path}" ]] || fail "Next phase blueprint path is empty for ${phase_id}"
[[ -n "${report_path}" ]] || fail "Next phase report path is empty for ${phase_id}"
[[ -f "${blueprint_path}" ]] || fail "Blueprint not found for next phase ${phase_id}: ${blueprint_path}"

handoff_path="harness/handoffs/${phase_id}.handoff.md"
validation_path="harness/validations/${phase_id}.validation.md"
completion_path="harness/completion/${phase_id}.completion.md"
prompt_path="${prompt_dir}/${phase_id}.codex-prompt.txt"

if [[ "${dry_run}" -eq 1 ]]; then
  ./harness/bin/run-phase.sh --dry-run "${blueprint_path}"
else
  ./harness/bin/run-phase.sh "${blueprint_path}"
fi

mkdir -p "${prompt_dir}"

content="$(<"${prompt_template}")"
content="${content//\{\{PHASE_ID\}\}/${phase_id}}"
content="${content//\{\{PRIMARY_BLUEPRINT_PATH\}\}/${blueprint_path}}"
content="${content//\{\{HANDOFF_PATH\}\}/${handoff_path}}"
content="${content//\{\{VALIDATION_PATH\}\}/${validation_path}}"
content="${content//\{\{COMPLETION_PATH\}\}/${completion_path}}"
content="${content//\{\{REPORT_PATH\}\}/${report_path}}"
content="${content//\{\{CURRENT_BRANCH\}\}/${branch}}"
printf '%s\n' "${content}" >"${prompt_path}"

echo
echo "Next phase summary:"
echo "  next phase id: ${phase_id}"
echo "  blueprint: ${blueprint_path}"
echo "  derived handoff: ${handoff_path}"
echo "  derived validation: ${validation_path}"
echo "  derived completion: ${completion_path}"
echo "  report path: ${report_path}"
echo "  prompt path: ${prompt_path}"
echo "  branch: ${branch}"
echo
echo "Next step:"
echo "  Review ${prompt_path}, then paste it into Codex when ready to execute ${phase_id}."
