#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  ./harness/bin/run-phase.sh harness/blueprints/<phase>.blueprint.md
  ./harness/bin/run-phase.sh --dry-run harness/blueprints/<phase>.blueprint.md
USAGE
}

fail() {
  echo "Error: $*" >&2
  exit 1
}

dry_run=0

if [[ $# -eq 0 ]]; then
  usage >&2
  exit 2
fi

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ "${1:-}" == "--dry-run" ]]; then
  dry_run=1
  shift
fi

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 2
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

blueprint_path="$1"

case "${blueprint_path}" in
  harness/blueprints/*.blueprint.md) ;;
  *)
    fail "Blueprint path must match harness/blueprints/<phase>.blueprint.md"
    ;;
esac

if [[ ! -f "${blueprint_path}" ]]; then
  fail "Blueprint not found: ${blueprint_path}"
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  fail "Runner must be executed inside a Git work tree"
fi

branch="$(git branch --show-current)"
if [[ -z "${branch}" ]]; then
  branch="DETACHED_HEAD"
fi

echo "Branch:"
echo "  ${branch}"
echo
echo "git status --short:"
status_short="$(git status --short)"
if [[ -z "${status_short}" ]]; then
  echo "  (clean)"
else
  printf '%s\n' "${status_short}" | sed 's/^/  /'
fi
echo

if [[ "${branch}" == "master" && "${HARNESS_ALLOW_MASTER:-0}" != "1" ]]; then
  fail "Refusing to prepare phase from master. Use a working branch unless Human Reviewer explicitly approved otherwise. To override, set HARNESS_ALLOW_MASTER=1."
fi

blueprint_file="$(basename "${blueprint_path}")"
phase_id="${blueprint_file%.blueprint.md}"

if [[ -z "${phase_id}" || "${phase_id}" == "${blueprint_file}" ]]; then
  fail "Could not derive phase id from blueprint path: ${blueprint_path}"
fi

handoff_path="harness/handoffs/${phase_id}.handoff.md"
validation_path="harness/validations/${phase_id}.validation.md"
completion_path="harness/completion/${phase_id}.completion.md"
report_slug="$(printf '%s' "${phase_id}" | tr '[:lower:]' '[:upper:]' | tr '-' '_')"
report_path="harness/reports/runs/${report_slug}_REPORT.md"

handoff_template="harness/handoffs/handoff.template.md"
validation_template="harness/validations/validation-checklist.template.md"
completion_template="harness/completion/completion-criteria.template.md"

for template in "${handoff_template}" "${validation_template}" "${completion_template}"; do
  if [[ ! -f "${template}" ]]; then
    fail "Required template not found: ${template}"
  fi
done

directories=(
  "harness/handoffs"
  "harness/validations"
  "harness/completion"
  "harness/reports/runs"
)

if [[ "${dry_run}" -eq 1 ]]; then
  echo "Dry run: no files or directories will be created."
  for directory in "${directories[@]}"; do
    if [[ -d "${directory}" ]]; then
      echo "directory exists: ${directory}"
    else
      echo "would create directory: ${directory}"
    fi
  done
  echo
else
  mkdir -p "${directories[@]}"
fi

render_template() {
  local template_path="$1"
  local target_path="$2"
  local content

  content="$(<"${template_path}")"
  content="${content//\{\{PHASE_ID\}\}/${phase_id}}"
  content="${content//\{\{PRIMARY_BLUEPRINT_PATH\}\}/${blueprint_path}}"
  content="${content//\{\{HANDOFF_PATH\}\}/${handoff_path}}"
  content="${content//\{\{VALIDATION_PATH\}\}/${validation_path}}"
  content="${content//\{\{COMPLETION_PATH\}\}/${completion_path}}"
  content="${content//\{\{REPORT_PATH\}\}/${report_path}}"

  printf '%s\n' "${content}" >"${target_path}"
}

prepare_artifact() {
  local template_path="$1"
  local target_path="$2"

  if [[ -f "${target_path}" ]]; then
    echo "exists, left unchanged: ${target_path}"
    return
  fi

  if [[ "${dry_run}" -eq 1 ]]; then
    echo "would create: ${target_path}"
    return
  fi

  render_template "${template_path}" "${target_path}"
  echo "created: ${target_path}"
}

prepare_artifact "${handoff_template}" "${handoff_path}"
prepare_artifact "${validation_template}" "${validation_path}"
prepare_artifact "${completion_template}" "${completion_path}"

echo
echo "Summary:"
echo "  primary blueprint: ${blueprint_path}"
echo "  derived handoff: ${handoff_path}"
echo "  derived validation: ${validation_path}"
echo "  derived completion: ${completion_path}"
echo "  runtime report path: ${report_path}"
echo "  branch: ${branch}"
echo
echo "Next step:"
echo "  Review derived artifacts, then execute the phase using the primary blueprint as source of truth. Runtime report should be written to ${report_path}."
