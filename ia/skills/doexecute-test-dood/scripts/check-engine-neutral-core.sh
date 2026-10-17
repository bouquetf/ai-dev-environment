#!/usr/bin/env bash
# Fails when core modules contain disallowed Camunda/Zeebe references.
# Allowed: historical Flyway scripts under db/migration; forbidden vendor URI fragments in BpmnVendorExtensionValidator.
set -euo pipefail

CRBPM_ROOT="${1:?Usage: check-engine-neutral-core.sh <crbpm-root>}"

scan_roots=(
  "${CRBPM_ROOT}/backend/src/main"
  "${CRBPM_ROOT}/backend-shared/src/main"
  "${CRBPM_ROOT}/rbac-shared/src/main"
  "${CRBPM_ROOT}/engine-spi/src/main"
)
api_root="${CRBPM_ROOT}/api/src/main"

is_allowlisted() {
  local line="$1"
  [[ "${line}" =~ BpmnVendorExtensionValidator\.java:[0-9]+:.*\"(camunda\.org|zeebe\.io)\" ]]
}

collect_hits() {
  local root="$1"
  local -a hits=()
  if [[ ! -d "${root}" ]]; then
    return 0
  fi
  while IFS= read -r -d '' file; do
    local line_number=0
    while IFS= read -r line || [[ -n "${line}" ]]; do
      line_number=$((line_number + 1))
      if echo "${line}" | grep -qiE 'camunda|zeebe'; then
        local relative="${file#${CRBPM_ROOT}/}"
        relative="${relative//\\//}"
        hits+=("${relative}:${line_number}:${line}")
      fi
    done < "${file}"
  done < <(find "${root}" -type f ! -path '*/db/migration/*' -print0)
  if ((${#hits[@]} > 0)); then
    printf '%s\n' "${hits[@]}"
  fi
}

violations=()
while IFS= read -r hit; do
  [[ -z "${hit}" ]] && continue
  if is_allowlisted "${hit}"; then
    continue
  fi
  violations+=("${hit}")
done < <(
  for root in "${scan_roots[@]}"; do
    collect_hits "${root}"
  done
)

if ((${#violations[@]} > 0)); then
  echo "Engine-neutral core check failed. Disallowed camunda/zeebe references:" >&2
  printf '%s\n' "${violations[@]}" >&2
  exit 1
fi

api_violations=()
while IFS= read -r hit; do
  [[ -z "${hit}" ]] && continue
  api_violations+=("${hit}")
done < <(collect_hits "${api_root}")

if ((${#api_violations[@]} > 0)); then
  echo "API module must stay engine-neutral. Disallowed references:" >&2
  printf '%s\n' "${api_violations[@]}" >&2
  exit 1
fi

echo "Engine-neutral core check passed."
