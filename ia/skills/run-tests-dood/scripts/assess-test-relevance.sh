#!/usr/bin/env bash
# Map git changes to CRBPM modules, related tests, and suggested DooD targets.
# Helper for run-tests-dood coverage gate. Does not run tests.
set -euo pipefail

REPO_ROOT="${1:-}"
BASE_REF="${2:-}"

if [[ -z "$REPO_ROOT" ]]; then
  REPO_ROOT="$(git rev-parse --show-toplevel)"
fi
REPO_ROOT="$(cd "$REPO_ROOT" && pwd)"

collect_changed() {
  local paths
  paths="$( (
    if [[ -n "$BASE_REF" ]]; then
      git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR "${BASE_REF}...HEAD" 2>/dev/null || true
      git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR "$BASE_REF" 2>/dev/null || true
    fi
    git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR HEAD 2>/dev/null || true
    git -C "$REPO_ROOT" diff --name-only --diff-filter=ACMR --cached 2>/dev/null || true
    git -C "$REPO_ROOT" ls-files --others --exclude-standard 2>/dev/null || true
  ) | sed 's|\\|/|g' | sort -u )"
  printf '%s\n' "$paths"
}

map_module() {
  local p="$1"
  case "$p" in
    crbpm/connectors/*) echo "connectors|connectors" ;;
    crbpm/backend-shared/*) echo "backend-shared|backend-shared,backend" ;;
    crbpm/backend/*) echo "backend|backend" ;;
    crbpm/api/*) echo "api|api" ;;
    crbpm/ui/e2e/*) echo "ui|e2e,ui" ;;
    crbpm/ui/*) echo "ui|ui" ;;
    crbpm/engine-c8/*) echo "engine-c8|engine-c8,backend,camunda-live" ;;
    crbpm/expression-c8/*) echo "expression-c8|expression-c8,backend" ;;
    crbpm/expression/*) echo "expression|expression" ;;
    crbpm/engine-spi/*) echo "engine-spi|engine-spi,backend" ;;
    crbpm/engine-stub/*) echo "engine-stub|backend" ;;
    crbpm/camunda-adapter/*) echo "camunda-adapter|camunda-adapter,camunda-live" ;;
    crbpm/infra/*) echo "infra|camunda-live" ;;
    crbpm-connectors/*) echo "crbpm-connectors|contrib,connectors" ;;
    docs/testplan/*|docs/*) echo "docs|" ;;
    .cursor/skills/run-tests-dood/*|.cursor/skills/doexecute-test-dood/*) echo "skill-dood|" ;;
    *) echo "other|" ;;
  esac
}

echo "=== run-tests-dood coverage gate: change → module map ==="
echo "RepoRoot: $REPO_ROOT"
if [[ -n "$BASE_REF" ]]; then echo "BaseRef: $BASE_REF"; else echo "Scope: working tree (staged + unstaged + untracked)"; fi

mapfile -t CHANGED < <(collect_changed | sed '/^$/d')
echo "Changed files: ${#CHANGED[@]}"
echo

if [[ ${#CHANGED[@]} -eq 0 ]]; then
  echo "No local file changes detected."
  echo "SuggestedTargets: full,camunda-live,e2e"
  echo "Note: without a diff, the agent must rely on conversation feature scope only."
  exit 0
fi

declare -A MODULE_FILES
declare -A MODULE_HINTS
ALL_HINTS=()

for p in "${CHANGED[@]}"; do
  mapped="$(map_module "$p")"
  module="${mapped%%|*}"
  hints="${mapped#*|}"
  MODULE_FILES["$module"]+="$p"$'\n'
  if [[ -n "$hints" ]]; then
    MODULE_HINTS["$module"]="$hints"
    IFS=',' read -ra hs <<< "$hints"
    for h in "${hs[@]}"; do ALL_HINTS+=("$h"); done
  fi
done

echo "## Modules touched"
for module in $(printf '%s\n' "${!MODULE_FILES[@]}" | sort); do
  files="$(printf '%s' "${MODULE_FILES[$module]}" | sed '/^$/d')"
  count="$(printf '%s\n' "$files" | sed '/^$/d' | wc -l | tr -d ' ')"
  echo
  echo "### $module ($count file(s))"
  printf '%s\n' "$files" | sed '/^$/d' | sed 's/^/  - /'

  if [[ "$module" =~ ^(docs|skill-dood|other)$ ]]; then
    echo "  RelatedTests: (n/a for this path class)"
    continue
  fi

  stems=()
  while IFS= read -r fp; do
    [[ -z "$fp" ]] && continue
    base="$(basename "$fp")"
    stem="${base%.*}"
    stems+=("$stem")
    stems+=("${stem%Resource}")
    stems+=("${stem%Service}")
    stems+=("${stem%Client}")
    stems+=("${stem%Worker}")
    stems+=("${stem%Executor}")
    stems+=("${stem%Handler}")
  done <<< "$files"

  test_roots=("crbpm/$module/src/test" "crbpm/$module/src/c8Test" "crbpm/$module/src/stubTest")
  if [[ "$module" == "ui" ]]; then
    test_roots+=("crbpm/ui/src" "crbpm/ui/e2e")
  fi
  if [[ "$module" == "crbpm-connectors" ]]; then
    test_roots=()
    for contrib_dir in "$REPO_ROOT"/crbpm-connectors/modules/*; do
      [[ -d "$contrib_dir" ]] || continue
      test_roots+=("crbpm-connectors/modules/$(basename "$contrib_dir")/src/test")
    done
  fi

  related=()
  for root in "${test_roots[@]}"; do
    abs="$REPO_ROOT/$root"
    [[ -d "$abs" ]] || continue
    while IFS= read -r -d '' f; do
      rel="${f#"$REPO_ROOT"/}"
      name="$(basename "$f")"
      for stem in "${stems[@]}"; do
        [[ ${#stem} -lt 4 ]] && continue
        if [[ "$name" == *"$stem"* || "$rel" == *"$stem"* ]]; then
          if [[ "$name" =~ (Test|IT|spec)\. || "$rel" == *"/e2e/"* || "$name" =~ \.test\.(js|jsx)$ ]]; then
            related+=("$rel")
            break
          fi
        fi
      done
    done < <(find "$abs" -type f \( -name '*.java' -o -name '*.js' -o -name '*.jsx' \) -print0 2>/dev/null)
  done

  mapfile -t related_u < <(printf '%s\n' "${related[@]:-}" | sed '/^$/d' | sort -u | head -n 40)
  if [[ ${#related_u[@]} -eq 0 ]]; then
    echo "  RelatedTests: NONE matched by name heuristic — agent must search manually"
  else
    echo "  RelatedTests:"
    printf '%s\n' "${related_u[@]}" | sed 's/^/    - /'
  fi
done

mapfile -t suggested < <(printf '%s\n' "${ALL_HINTS[@]:-}" | sed '/^$/d' | sort -u)
if [[ ${#suggested[@]} -eq 0 ]]; then
  suggested=(full camunda-live e2e)
fi

echo
echo "## Suggested DooD targets (heuristic)"
echo "SuggestedTargets: $(IFS=,; echo "${suggested[*]}")"
echo "DefaultFullStillValid: yes (regression gate); prefer SuggestedTargets for focused feature loops after agent completeness check."
echo
echo "## Agent next steps"
echo "1. Cross-check conversation features / story acceptance criteria against RelatedTests."
echo "2. Flag completeness gaps (changed production code with no test, missing IT/e2e for user-facing flows)."
echo "3. Confirm targets with the user when narrowing below full, or when gaps require new tests before running."
