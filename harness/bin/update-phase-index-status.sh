#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  ./harness/bin/update-phase-index-status.sh start <phase-id>
  ./harness/bin/update-phase-index-status.sh validation <phase-id>
USAGE
}

fail() {
  echo "Error: $*" >&2
  exit 1
}

derive_report_path() {
  local phase_id="$1"
  local report_slug
  report_slug="$(printf '%s' "${phase_id}" | tr '[:lower:]' '[:upper:]' | tr '-' '_')"
  printf 'harness/reports/runs/%s_REPORT.md' "${report_slug}"
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

if [[ $# -ne 2 ]]; then
  usage >&2
  exit 2
fi

transition="$1"
phase_id="$2"

case "${transition}" in
  start)
    expected_status="next"
    target_status="in_progress"
    ;;
  validation)
    expected_status="in_progress"
    target_status="validation"
    ;;
  *)
    fail "Unknown transition: ${transition}. Expected start or validation."
    ;;
esac

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
cd "${repo_root}"

phase_index="harness/phases/phase-index.tsv"
[[ -f "${phase_index}" ]] || fail "Phase index not found: ${phase_index}"

if [[ "${transition}" == "validation" ]]; then
  report_path="$(derive_report_path "${phase_id}")"
  [[ -f "${report_path}" ]] || fail "Cannot move ${phase_id} to validation before report exists: ${report_path}"
fi

tmp_index="$(mktemp "${TMPDIR:-/tmp}/phase-index-status.XXXXXX")"
trap 'rm -f "${tmp_index}"' EXIT

awk -v phase_id="${phase_id}" \
  -v expected_status="${expected_status}" \
  -v target_status="${target_status}" '
BEGIN {
  FS = OFS = "\t"
  found = 0
  failed = 0
}

function fail(message) {
  print "Error: " message > "/dev/stderr"
  failed = 1
}

NR == 1 {
  if ($0 != "order\tid\tstatus\tcommit") {
    fail("Invalid phase index header. Expected: order<TAB>id<TAB>status<TAB>commit")
  }
  print
  next
}

NF == 0 {
  next
}

NF != 4 {
  fail("Invalid TSV row at line " NR ". Expected exactly 4 tab-separated columns.")
  next
}

$2 == phase_id {
  found += 1
  if ($3 != expected_status) {
    fail("Cannot update " phase_id " from status " $3 ". Expected " expected_status ".")
  }
  $3 = target_status
  $4 = "TBD"
}

{
  print
}

END {
  if (found == 0) {
    fail("Phase id not found in phase index: " phase_id)
  }
  if (found > 1) {
    fail("Duplicate phase id in phase index: " phase_id)
  }
  if (failed) {
    exit 1
  }
}
' "${phase_index}" >"${tmp_index}"

mv "${tmp_index}" "${phase_index}"
trap - EXIT

echo "Updated ${phase_index}: ${phase_id} ${expected_status} -> ${target_status}, commit TBD"
