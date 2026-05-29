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

warn() {
  echo "Warning: $*" >&2
}

is_allowed_status() {
  case "$1" in
    planned|next|in_progress|validation|implemented|approved|blocked|skipped) return 0 ;;
    *) return 1 ;;
  esac
}

derive_blueprint_path() {
  printf 'harness/blueprints/%s.blueprint.md' "$1"
}

derive_handoff_path() {
  printf 'harness/handoffs/%s.handoff.md' "$1"
}

derive_validation_path() {
  printf 'harness/validations/%s.validation.md' "$1"
}

derive_completion_path() {
  printf 'harness/completion/%s.completion.md' "$1"
}

derive_prompt_path() {
  printf 'harness/bin/build/prompts/%s.codex-prompt.txt' "$1"
}

derive_report_path() {
  local phase_id="$1"
  local report_slug
  report_slug="$(printf '%s' "${phase_id}" | tr '[:lower:]' '[:upper:]' | tr '-' '_')"
  printf 'harness/reports/runs/%s_REPORT.md' "${report_slug}"
}

index_of_id() {
  local search="$1"
  local i

  for ((i = 0; i < ${#ids[@]}; i++)); do
    if [[ "${ids[$i]}" == "${search}" ]]; then
      printf '%s\n' "${i}"
      return 0
    fi
  done

  return 1
}

write_phase_index() {
  local target="$1"
  local i

  {
    printf 'order\tid\tstatus\tcommit\n'
    for ((i = 0; i < ${#ids[@]}; i++)); do
      printf '%s\t%s\t%s\t%s\n' "${orders[$i]}" "${ids[$i]}" "${statuses[$i]}" "${commits[$i]}"
    done
  } >"${target}"
}

sync_phase_index() {
  local tmp_index="$1"
  local line_number=0
  local header_format=""
  local col1 col2 col3 col4 col5 col6 col7
  local order id status commit idx
  local max_order=0
  local blueprint_file blueprint_id next_order

  orders=()
  ids=()
  statuses=()
  commits=()

  while IFS=$'\t' read -r col1 col2 col3 col4 col5 col6 col7 || [[ -n "${col1:-}" ]]; do
    line_number=$((line_number + 1))

    col1="${col1%$'\r'}"
    col2="${col2%$'\r'}"
    col3="${col3%$'\r'}"
    col4="${col4%$'\r'}"
    col5="${col5%$'\r'}"
    col6="${col6%$'\r'}"
    col7="${col7%$'\r'}"

    if [[ "${line_number}" -eq 1 ]]; then
      if [[ "${col1}" == "order" && "${col2}" == "id" && "${col3}" == "status" && "${col4}" == "commit" && -z "${col5}${col6}${col7}" ]]; then
        header_format="minimal"
      elif [[ "${col1}" == "order" && "${col2}" == "id" && "${col3}" == "status" && "${col4}" == "blueprint" && "${col5}" == "report" && "${col6}" == "commit" && -z "${col7}" ]]; then
        header_format="legacy"
      else
        fail "Invalid phase index header in ${phase_index}. Expected: order<TAB>id<TAB>status<TAB>commit"
      fi
      continue
    fi

    [[ -n "${col1}${col2}${col3}${col4}${col5}${col6}${col7}" ]] || continue

    if [[ "${header_format}" == "minimal" ]]; then
      [[ -z "${col5}${col6}${col7}" ]] || fail "Unexpected extra columns in ${phase_index} at line ${line_number}"
      order="${col1}"
      id="${col2}"
      status="${col3}"
      commit="${col4:-TBD}"
    else
      [[ -z "${col7}" ]] || fail "Unexpected extra columns in ${phase_index} at line ${line_number}"
      order="${col1}"
      id="${col2}"
      status="${col3}"
      commit="${col6:-TBD}"
    fi

    [[ "${order}" =~ ^[0-9]+$ ]] || fail "Invalid order at line ${line_number}: ${order}"
    [[ -n "${id}" ]] || fail "Missing phase id at line ${line_number}"
    is_allowed_status "${status}" || fail "Invalid status for ${id}: ${status}"
    if idx="$(index_of_id "${id}")"; then
      fail "Duplicate phase id in ${phase_index}: ${id}"
    fi

    orders+=("${order}")
    ids+=("${id}")
    statuses+=("${status}")
    commits+=("${commit}")

    if ((order > max_order)); then
      max_order="${order}"
    fi
  done <"${phase_index}"

  while IFS= read -r blueprint_file; do
    [[ -n "${blueprint_file}" ]] || continue
    blueprint_id="$(basename "${blueprint_file}")"
    blueprint_id="${blueprint_id%.blueprint.md}"

    if ! idx="$(index_of_id "${blueprint_id}")"; then
      next_order=$((max_order + 1))
      orders+=("${next_order}")
      ids+=("${blueprint_id}")
      statuses+=("planned")
      commits+=("TBD")
      max_order="${next_order}"
      echo "Synced new blueprint as planned: ${blueprint_id}"
    fi
  done < <(find harness/blueprints -maxdepth 1 -type f -name '*.blueprint.md' | sort)

  write_phase_index "${tmp_index}"
  if ! cmp -s "${tmp_index}" "${phase_index}"; then
    mv "${tmp_index}" "${phase_index}"
    echo "Synchronized phase index: ${phase_index}"
  fi

  for ((idx = 0; idx < ${#ids[@]}; idx++)); do
    if [[ ! -f "$(derive_blueprint_path "${ids[$idx]}")" ]]; then
      warn "Phase index entry has no matching blueprint: ${ids[$idx]}"
    fi
  done
}

render_prompt() {
  local phase_id="$1"
  local blueprint_path="$2"
  local handoff_path="$3"
  local validation_path="$4"
  local completion_path="$5"
  local report_path="$6"
  local prompt_path="$7"
  local content

  mkdir -p "${prompt_dir}"

  content="$(<"${prompt_template}")"
  content="${content//\{\{PHASE_ID\}\}/${phase_id}}"
  content="${content//\{\{PRIMARY_BLUEPRINT_PATH\}\}/${blueprint_path}}"
  content="${content//\{\{HANDOFF_PATH\}\}/${handoff_path}}"
  content="${content//\{\{VALIDATION_PATH\}\}/${validation_path}}"
  content="${content//\{\{COMPLETION_PATH\}\}/${completion_path}}"
  content="${content//\{\{REPORT_PATH\}\}/${report_path}}"
  content="${content//\{\{CURRENT_BRANCH\}\}/${branch}}"
  content="${content//\{\{PHASE_INDEX_PATH\}\}/${phase_index}}"
  printf '%s\n' "${content}" >"${prompt_path}"
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
prompt_dir="harness/bin/build/prompts"

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

tmp_index="$(mktemp "${TMPDIR:-/tmp}/phase-index.XXXXXX")"
trap 'rm -f "${tmp_index}"' EXIT
sync_phase_index "${tmp_index}"

next_count=0
validation_count=0
planned_index=-1
phase_id=""
next_ids=()
validation_ids=()
idx=0

for ((idx = 0; idx < ${#ids[@]}; idx++)); do
  case "${statuses[$idx]}" in
    next)
      next_count=$((next_count + 1))
      next_ids+=("${ids[$idx]}")
      phase_id="${ids[$idx]}"
      ;;
    validation)
      validation_count=$((validation_count + 1))
      validation_ids+=("${ids[$idx]}")
      ;;
    planned)
      if [[ "${planned_index}" -lt 0 ]]; then
        planned_index="${idx}"
      fi
      ;;
  esac
done

if [[ "${next_count}" -gt 1 ]]; then
  printf 'Error: Multiple phases marked as next in %s:\n' "${phase_index}" >&2
  printf '  %s\n' "${next_ids[@]}" >&2
  exit 1
fi

if [[ "${validation_count}" -gt 0 ]]; then
  echo "Cannot run next phase." >&2
  echo >&2
  echo "Phase waiting for validation:" >&2
  printf '  %s\n' "${validation_ids[@]}" >&2
  echo >&2
  echo "Review the report first:" >&2
  for phase_id in "${validation_ids[@]}"; do
    printf '  %s\n' "$(derive_report_path "${phase_id}")" >&2
  done
  echo >&2
  echo "This prevents stacking phase executions without Human Reviewer approval." >&2
  exit 1
fi

if [[ "${next_count}" -eq 0 ]]; then
  if [[ "${planned_index}" -lt 0 ]]; then
    echo "No next phase is available in ${phase_index}."
    echo "There are no planned phases to promote."
    exit 0
  fi

  statuses[$planned_index]="next"
  phase_id="${ids[$planned_index]}"
  write_phase_index "${phase_index}"
  echo "Promoted first planned phase to next: ${phase_id}"
fi

[[ -n "${phase_id}" ]] || fail "Next phase id is empty in ${phase_index}"

blueprint_path="$(derive_blueprint_path "${phase_id}")"
handoff_path="$(derive_handoff_path "${phase_id}")"
validation_path="$(derive_validation_path "${phase_id}")"
completion_path="$(derive_completion_path "${phase_id}")"
prompt_path="$(derive_prompt_path "${phase_id}")"
report_path="$(derive_report_path "${phase_id}")"

[[ -f "${blueprint_path}" ]] || fail "Blueprint not found for next phase ${phase_id}: ${blueprint_path}"

if [[ -f "${report_path}" ]]; then
  cat >&2 <<MESSAGE
Report already exists for next phase:
${report_path}

This phase may already be waiting for validation.
Set status to validation or update the phase index before continuing.
MESSAGE
  exit 1
fi

if [[ "${dry_run}" -eq 1 ]]; then
  ./harness/bin/run-phase.sh --dry-run "${blueprint_path}"
else
  ./harness/bin/run-phase.sh "${blueprint_path}"
fi

render_prompt "${phase_id}" "${blueprint_path}" "${handoff_path}" "${validation_path}" "${completion_path}" "${report_path}" "${prompt_path}"

echo
echo "Next phase summary:"
echo "  next phase id: ${phase_id}"
echo "  blueprint: ${blueprint_path}"
echo "  derived handoff: ${handoff_path}"
echo "  derived validation: ${validation_path}"
echo "  derived completion: ${completion_path}"
echo "  report path: ${report_path}"
echo "  prompt path: ${prompt_path}"
echo "  phase index: ${phase_index}"
echo "  branch: ${branch}"
echo
echo "Next step:"
echo "  Review ${prompt_path}, then paste it into Codex when ready to execute ${phase_id}."
