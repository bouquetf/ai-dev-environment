#!/usr/bin/env bash
# Run the CRBPM test suite inside a DooD container.
#
# Mirror of run.ps1 for bash hosts (Linux/macOS, CI). Orchestrator for the
# `doexecute-test-dood` Cursor skill. See ../SKILL.md for the full description.
# Coverage audit / patching of this skill is owned by `run-tests-dood`.

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

IMAGE_NAME="crbpm-dood-tests:local"
REBUILD=0
KEEP_CONTAINER=0
TARGETS=()
GRADLE_ARGS=""
REPORTS_DIR=""
NO_BROWSER=0
CAMUNDA_SMOKE_WAIT_MS=""

usage() {
    cat <<'EOF'
Usage: run.sh [options]

Options:
  --targets <a,b,c>     Comma-separated scope (default: full,camunda-live,e2e).
                        `full` runs all testable modules under every engine
                        build profile (wrapper,c8 / wrapper / wrapper,stub).
                        `all` is an alias for `full`. Pass explicit module
                        names (e.g. backend,api,ui) for a faster single-profile
                        loop. `contrib` runs the isolated crbpm-connectors/
                        Gradle build (always included in `full`).
                        `camunda-live` runs Camunda 8 live ITs on the host;
                        `e2e` runs Playwright on the host. Omit host-only targets
                        with e.g. --targets full.
  --gradle-args "..."   Extra arguments forwarded to Gradle.
  --camunda-smoke-wait-ms <ms>
                        Forward -PcamundaSmokeWaitMs to :infra:camundaSmoke.
  --rebuild             Force rebuild of the test image.
  --keep-container      Do not remove the container after exit.
  --image <name>        Override the image tag (default: crbpm-dood-tests:local).
  --reports-dir <path>  Override the host reports root.
  --no-browser          Do not open the per-module HTML reports in the default
                        browser (default: open).
  -h, --help            Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --targets)
            IFS=',' read -r -a TARGETS <<< "$2"
            shift 2
            ;;
        --gradle-args)
            GRADLE_ARGS="$2"
            shift 2
            ;;
        --rebuild)
            REBUILD=1
            shift
            ;;
        --keep-container)
            KEEP_CONTAINER=1
            shift
            ;;
        --image)
            IMAGE_NAME="$2"
            shift 2
            ;;
        --reports-dir)
            REPORTS_DIR="$2"
            shift 2
            ;;
        --no-browser)
            NO_BROWSER=1
            shift
            ;;
        --camunda-smoke-wait-ms)
            CAMUNDA_SMOKE_WAIT_MS="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage
            exit 64
            ;;
    esac
done

section() { printf '\n==> %s\n' "$1"; }

assert_docker() {
    if ! docker version --format '{{.Server.Version}}' >/dev/null 2>&1; then
        echo "Docker daemon is not reachable. Start Docker or join the 'docker' group." >&2
        exit 69
    fi
}

resolve_repo_root() {
    local root
    root="$(git rev-parse --show-toplevel 2>/dev/null || true)"
    if [[ -z "$root" ]]; then
        echo "Not inside a git repository. The skill requires a working tree to mount." >&2
        exit 64
    fi
    printf '%s\n' "$root"
}

resolve_dockerfile() {
    local repo_root="$1"
    if [[ -f "$repo_root/Dockerfile.dood-tests" ]]; then
        printf '%s\n' "$repo_root/Dockerfile.dood-tests"
    else
        printf '%s\n' "$SCRIPT_DIR/Dockerfile.dood-tests"
    fi
}

ensure_image() {
    local image="$1" dockerfile="$2" force="$3"
    if [[ "$force" == "0" ]] && docker image inspect "$image" >/dev/null 2>&1; then
        echo "Reusing image '$image' (pass --rebuild to refresh)."
        return
    fi
    section "Building image '$image' from $dockerfile"
    docker build --pull -t "$image" -f "$dockerfile" "$(dirname "$dockerfile")"
}

detect_targets() {
    local repo_root="$1"
    local present=()
    # Modules that currently ship automated tests (src/test or npm test).
    # engine-stub / infra have no unit tests (stub covered via :backend:stubTest).
    for name in backend-shared rbac-shared engine-spi connector-handler-spi expression expression-c8 engine-c8 \
                camunda-adapter connectors api backend ui; do
        if [[ -f "$repo_root/crbpm/$name/build.gradle.kts" ]]; then
            present+=("$name")
        fi
    done
    if [[ ${#present[@]} -eq 0 ]]; then
        echo "No known Gradle module found under crbpm/. Adjust --targets explicitly." >&2
        exit 64
    fi
    printf '%s\n' "${present[@]}"
}

run_full_verification_in_container() {
    local repo_root="$1" gradle_args="$2" image="$3" keep_container="$4" timestamp_dir="$5" phase_summary_file="$6"
    local overall_exit=0
    local -a phase_labels phase_profiles phase_tasks
    local -a phase_modules_0 phase_modules_1 phase_modules_2

    phase_labels=("full-c8" "wrapper-only" "stub")
    phase_profiles=("wrapper,c8" "wrapper" "wrapper,stub")
    phase_tasks[0]=':backend-shared:test :rbac-shared:test :engine-spi:test :connector-handler-spi:test :expression:test :expression-c8:test :engine-c8:test :camunda-adapter:test :connectors:test :api:test :ui:test :backend:test'
    phase_tasks[1]=':backend-shared:test :rbac-shared:test :engine-spi:test :connector-handler-spi:test :expression:test :camunda-adapter:test :connectors:test :backend:build :backend:test :backend:dependenciesWrapperOnly'
    phase_tasks[2]=':backend:stubTest'
    phase_modules_0=(backend-shared rbac-shared engine-spi connector-handler-spi expression expression-c8 engine-c8 camunda-adapter connectors api backend ui)
    phase_modules_1=(backend-shared rbac-shared engine-spi connector-handler-spi backend camunda-adapter connectors)
    phase_modules_2=(backend)

    local i
    for i in 0 1 2; do
        section "Full verification phase: ${phase_labels[$i]} (-Pcrbpm.engines=${phase_profiles[$i]})"
        local gradle_cmd="./gradlew ${phase_tasks[$i]} -Pcrbpm.engines=${phase_profiles[$i]} --no-daemon --console=plain --stacktrace $gradle_args"
        local container_cmd="set -eo pipefail; cd /work/crbpm && chmod +x ./gradlew && $gradle_cmd"

        local -a docker_args=(
            run --init
            -v "$repo_root:/work"
            -v "/var/run/docker.sock:/var/run/docker.sock"
            -v "crbpm-dood-gradle:/cache/gradle"
            -v "crbpm-dood-npm:/cache/npm"
            --add-host "host.docker.internal:host-gateway"
            -e "TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal"
            -e "TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true"
            -w "/work/crbpm"
        )
        if [[ "$keep_container" == "1" ]]; then
            docker_args+=(--name "crbpm-dood-tests-$(date +%Y%m%d%H%M%S)-${phase_labels[$i]}")
        else
            docker_args+=(--rm)
        fi
        docker_args+=("$image" bash -lc "$container_cmd")

        set +e
        docker "${docker_args[@]}"
        local phase_exit=$?
        set -e
        echo "Phase ${phase_labels[$i]} (-Pcrbpm.engines=${phase_profiles[$i]}) exit : $phase_exit" >> "$phase_summary_file"
        if [[ "$phase_exit" -ne 0 && "$overall_exit" -eq 0 ]]; then
            overall_exit=$phase_exit
        fi
        case "$i" in
            0) collect_phase_reports "$repo_root" "${phase_labels[$i]}" "$timestamp_dir" "${phase_modules_0[@]}" ;;
            1) collect_phase_reports "$repo_root" "${phase_labels[$i]}" "$timestamp_dir" "${phase_modules_1[@]}" ;;
            2) collect_phase_reports "$repo_root" "${phase_labels[$i]}" "$timestamp_dir" "${phase_modules_2[@]}" ;;
        esac
    done
    return "$overall_exit"
}

contrib_modules() {
    local repo_root="$1"
    local modules_root="$repo_root/crbpm-connectors/modules"
    [[ -d "$modules_root" ]] || return 0
    local dir
    for dir in "$modules_root"/*; do
        if [[ -f "$dir/build.gradle.kts" ]]; then
            basename "$dir"
        fi
    done
}

run_contrib_tests_in_container() {
    local repo_root="$1" image="$2" keep_container="$3"
    section "Contribution tree tests (crbpm-connectors)"
    echo "Note: crbpm-connectors/ is an isolated Gradle build (no dependency"
    echo "      from crbpm/*). Its handler and CLI modules resolve"
    echo "      com.moodys.crbpm:connector-handler-spi from mavenLocal, so the"
    echo "      SPI is published inside the container before running the tests."
    echo "      --gradle-args is not forwarded here (task/test filters of the"
    echo "      platform build do not apply to this build)."

    local publish_cmd='./gradlew :connector-handler-spi:publishToMavenLocal --no-daemon --console=plain'
    local contrib_cmd='./gradlew test --no-daemon --console=plain --stacktrace'
    local container_cmd="set -eo pipefail; cd /work/crbpm && chmod +x ./gradlew && $publish_cmd"
    container_cmd+=" && cd /work/crbpm-connectors && chmod +x ./gradlew && $contrib_cmd"

    local docker_args=(
        run --init
        -v "$repo_root:/work"
        -v "/var/run/docker.sock:/var/run/docker.sock"
        -v "crbpm-dood-gradle:/cache/gradle"
        -v "crbpm-dood-npm:/cache/npm"
        --add-host "host.docker.internal:host-gateway"
        -e "TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal"
        -e "TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true"
        -w "/work/crbpm-connectors"
    )
    if [[ "$keep_container" == "1" ]]; then
        docker_args+=(--name "crbpm-dood-tests-$(date +%Y%m%d%H%M%S)-contrib")
    else
        docker_args+=(--rm)
    fi
    docker_args+=("$image" bash -lc "$container_cmd")

    docker "${docker_args[@]}"
}

copy_if_exists() {
    local src="$1" dest="$2"
    if [[ -e "$src" ]]; then
        mkdir -p "$(dirname "$dest")"
        cp -R "$src" "$dest"
    fi
}

generate_junit_html_report() {
    local junit_file="$1" output_file="$2" title="$3"
    [[ -f "$junit_file" ]] || return 1
    local generator="$SCRIPT_DIR/generate-junit-html-report.py"
    if [[ ! -f "$generator" ]]; then
        echo "Missing JUnit HTML generator: $generator" >&2
        return 1
    fi
    local python_bin
    python_bin="$(command -v python3 || command -v python || true)"
    if [[ -z "$python_bin" ]]; then
        echo "python3 not available; cannot generate static HTML report from $junit_file" >&2
        return 1
    fi
    "$python_bin" "$generator" --junit "$junit_file" --output "$output_file" --title "$title"
}

collect_module_reports() {
    local repo_root="$1" module="$2" dest_dir="$3" title_suffix="${4:-}"
    mkdir -p "$dest_dir"
    copy_if_exists "$repo_root/crbpm/$module/build/test-results" "$dest_dir/test-results"
    copy_if_exists "$repo_root/crbpm/$module/build/reports/tests" "$dest_dir/reports"
    if [[ "$module" == "ui" ]]; then
        local junit_file="$dest_dir/test-results/test/junit.xml"
        local report_html="$dest_dir/report.html"
        local title="Vitest (ui) - DooD run"
        if [[ -n "$title_suffix" ]]; then
            title="Vitest (ui) - $title_suffix"
        fi
        generate_junit_html_report "$junit_file" "$report_html" "$title" || true
    fi
}

collect_reports() {
    local repo_root="$1" timestamp_dir="$2"
    shift 2
    local modules=("$@")
    for module in "${modules[@]}"; do
        collect_module_reports "$repo_root" "$module" "$timestamp_dir/$module"
    done
}

collect_phase_reports() {
    local repo_root="$1" phase_label="$2" timestamp_dir="$3"
    shift 3
    local modules=("$@")
    for module in "${modules[@]}"; do
        collect_module_reports "$repo_root" "$module" "$timestamp_dir/$phase_label/$module" "$phase_label"
    done
}

normalize_targets() {
    local out=()
    for t in "$@"; do
        if [[ "$t" == "all" ]]; then
            out+=("full")
        else
            out+=("$t")
        fi
    done
    printf '%s\n' "${out[@]}"
}

collect_contrib_reports() {
    local repo_root="$1" timestamp_dir="$2"
    shift 2
    local modules=("$@")
    local module module_build
    for module in "${modules[@]}"; do
        module_build="$repo_root/crbpm-connectors/modules/$module/build"
        mkdir -p "$timestamp_dir/contrib/$module"
        copy_if_exists "$module_build/test-results" "$timestamp_dir/contrib/$module/test-results"
        copy_if_exists "$module_build/reports/tests" "$timestamp_dir/contrib/$module/reports"
    done
}

collect_e2e_reports() {
    local repo_root="$1" timestamp_dir="$2"
    local module_dir="$timestamp_dir/e2e"
    mkdir -p "$module_dir"

    # JUnit XML produced by the Playwright `junit` reporter
    # (see crbpm/ui/playwright.config.js).
    copy_if_exists "$repo_root/crbpm/ui/build/test-results/e2e" "$module_dir/test-results/e2e"

    # Playwright artifacts (screenshots, videos, traces) for failing tests.
    copy_if_exists "$repo_root/crbpm/ui/test-results" "$module_dir/artifacts"

    # Playwright's built-in HTML report is a SPA that needs
    # `npx playwright show-report` (or a static server) to open; we keep a
    # copy for users who know that command, but the primary viewer is the
    # self-contained report.html below.
    copy_if_exists "$repo_root/crbpm/ui/playwright-report" "$module_dir/playwright-report"

    local junit_file="$module_dir/test-results/e2e/junit.xml"
    local report_html="$module_dir/report.html"
    generate_junit_html_report "$junit_file" "$report_html" "Playwright (e2e) - DooD run" || true
}

collect_camunda_live_reports() {
    local repo_root="$1" timestamp_dir="$2"
    local module_dir="$timestamp_dir/camunda-live/backend"
    mkdir -p "$module_dir"
    copy_if_exists "$repo_root/crbpm/backend/build/test-results/c8Test" "$module_dir/test-results/c8Test"
    copy_if_exists "$repo_root/crbpm/backend/build/reports/tests/c8Test" "$module_dir/reports/c8Test"
}

run_engine_neutral_core_check() {
    local repo_root="$1"
    local crbpm_root="$repo_root/crbpm"
    local check_script="$SCRIPT_DIR/check-engine-neutral-core.sh"
    section "Engine-neutral core check (host)"
    echo "bash $check_script $crbpm_root"
    bash "$check_script" "$crbpm_root"
}

wait_http_ok() {
    local url="$1"
    local timeout_seconds="${2:-180}"
    local elapsed=0
    while (( elapsed < timeout_seconds )); do
        if curl -fsS --max-time 5 "$url" >/dev/null 2>&1; then
            return 0
        fi
        sleep 3
        elapsed=$((elapsed + 3))
    done
    echo "Timed out waiting for $url" >&2
    return 1
}

connectors_live_compose_args() {
    printf '%s\n' \
        -f docker-compose.connectors-standalone.yml \
        -f docker-compose.connectors-live.yml
}

start_connector_execution_live_prereqs() {
    local repo_root="$1"
    local crbpm_dir="$repo_root/crbpm"
    local gradlew="$crbpm_dir/gradlew"
    echo "Building the connectors image for the live connector-execution stack."
    (
        cd "$crbpm_dir"
        "$gradlew" :connectors:dockerBuild :connector-handler-spi:publishToMavenLocal \
            --no-daemon --console=plain --stacktrace
    ) || return $?

    echo "Preparing contributed connector handlers for JVM discovery."
    (
        cd "$repo_root/crbpm-connectors"
        chmod +x ./gradlew
        ./gradlew prepareJvmDiscovery --no-daemon --console=plain --stacktrace
    ) || return $?

    echo "Starting connectors standalone on host port 18083 (QuarkusTest backend catalog URL)."
    local -a compose_args
    mapfile -t compose_args < <(connectors_live_compose_args)
    (
        cd "$repo_root/crbpm/docker"
        export CRBPM_CONNECTORS_PORT=18083
        docker compose "${compose_args[@]}" up -d --force-recreate
    ) || return $?
    if ! wait_http_ok "http://127.0.0.1:18083/health"; then
        echo "Connectors standalone did not become healthy; last container logs:" >&2
        docker logs --tail 40 crbpm-connectors-standalone >&2 2>&1 || true
        return 1
    fi

    echo "Recreating camunda-adapter with the connector job-worker overlay."
    (
        cd "$repo_root/crbpm/docker"
        docker compose \
            -f docker-compose.yml \
            -f docker-compose.c8.yml \
            -f docker-compose.adapter-live.yml \
            -f docker-compose.adapter-connector-worker.yml \
            up -d --build --force-recreate camunda-adapter
    ) || return $?
    wait_http_ok "http://127.0.0.1:18082/health"
}

stop_connector_execution_live_prereqs() {
    local repo_root="$1"
    echo "Stopping connectors standalone stack used by connector-execution live ITs."
    local -a compose_args
    mapfile -t compose_args < <(connectors_live_compose_args)
    (
        cd "$repo_root/crbpm/docker" || exit 0
        docker compose "${compose_args[@]}" down -v >/dev/null 2>&1 || true
    )
}

run_camunda_live_on_host() {
    local repo_root="$1" gradle_args="$2" camunda_smoke_wait_ms="$3"
    section "Camunda 8 live tests on host (infra + *LiveIT)"
    echo "Note: camunda-live runs OUTSIDE the DooD container. It starts the"
    echo "      local Camunda 8 stack + camunda-adapter on the host Docker"
    echo "      daemon, then runs gated backend live ITs (deploy/start/sync/"
    echo "      CEL routing + connector execution). Requires ~8 GB RAM and Java 21 on the host."
    local crbpm_dir="$repo_root/crbpm"
    local gradlew="$crbpm_dir/gradlew"
    if [[ ! -x "$gradlew" ]]; then
        chmod +x "$gradlew" 2>/dev/null || true
    fi
    if [[ ! -f "$gradlew" ]]; then
        echo "Gradle wrapper not found at $gradlew." >&2
        return 1
    fi
    local live_exit=0
    (
        cd "$crbpm_dir"
        local -a smoke_args=(:infra:camundaSmoke --no-daemon --console=plain --stacktrace)
        if [[ -n "$camunda_smoke_wait_ms" ]]; then
            smoke_args+=(-PcamundaSmokeWaitMs="$camunda_smoke_wait_ms")
        fi
        "$gradlew" "${smoke_args[@]}"
    ) || live_exit=$?

    if [[ "$live_exit" -eq 0 ]]; then
        start_connector_execution_live_prereqs "$repo_root" || live_exit=$?
    fi

    if [[ "$live_exit" -eq 0 ]]; then
        (
            cd "$crbpm_dir"
            export CRBPM_CAMUNDA_LIVE=true
            export CRBPM_CONNECTOR_EXECUTION_LIVE=true
            # shellcheck disable=SC2086
            "$gradlew" :backend:c8Test -Pcrbpm.engines=wrapper,c8 --no-daemon --console=plain --stacktrace \
                --tests 'com.moodys.crbpm.engine.c8.zeebe.*LiveIT' $gradle_args
        ) || live_exit=$?
        if [[ "$live_exit" -ne 0 ]]; then
            echo "Camunda live tests failed; adapter and connectors logs:" >&2
            docker logs --tail 200 crbpm-camunda-adapter >&2 2>&1 || true
            docker logs --tail 200 crbpm-connectors-standalone >&2 2>&1 || true
        fi
        unset CRBPM_CAMUNDA_LIVE
        unset CRBPM_CONNECTOR_EXECUTION_LIVE
    fi

    section "Stopping Camunda 8 test stack"
    stop_connector_execution_live_prereqs "$repo_root"
    (
        cd "$crbpm_dir"
        "$gradlew" :infra:camundaAdapterDown :infra:camundaDown --no-daemon --console=plain >/dev/null 2>&1 || true
    )
    return "$live_exit"
}

run_e2e_on_host() {
    local repo_root="$1" gradle_args="$2"
    section "Running e2e suite on host (./gradlew :ui:e2e)"
    echo "Note: e2e runs OUTSIDE the DooD container because it boots a"
    echo "      docker-compose stack (crbpm-e2e: api, backend, ui,"
    echo "      connectors, camunda-adapter, postgres-e2e) and drives"
    echo "      Chromium via Playwright in crbpm/ui/. Host ports:"
    echo "      15173 / 18080 / 18081 / 18083 / 15432. Java 21 and"
    echo "      Node.js must be available on the host."
    local crbpm_dir="$repo_root/crbpm"
    local gradlew="$crbpm_dir/gradlew"
    if [[ ! -x "$gradlew" ]]; then
        chmod +x "$gradlew" 2>/dev/null || true
    fi
    if [[ ! -f "$gradlew" ]]; then
        echo "Gradle wrapper not found at $gradlew." >&2
        return 1
    fi
    (
        cd "$crbpm_dir"
        # shellcheck disable=SC2086
        "$gradlew" :ui:e2e --no-daemon --console=plain --stacktrace $gradle_args
    )
}

update_latest_pointer() {
    local reports_root="$1" timestamp_dir="$2"
    local latest="$reports_root/latest"
    rm -rf "$latest"
    if ln -s "$timestamp_dir" "$latest" 2>/dev/null; then
        return
    fi
    cp -R "$timestamp_dir" "$latest"
}

open_in_browser() {
    local path="$1"
    [[ -e "$path" ]] || return 1
    if command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$path" >/dev/null 2>&1 &
    elif command -v open >/dev/null 2>&1; then
        open "$path" >/dev/null 2>&1 &
    elif [[ -n "${BROWSER:-}" ]]; then
        "$BROWSER" "$path" >/dev/null 2>&1 &
    else
        echo "No browser opener found (xdg-open / open / \$BROWSER). Skipping: $path"
        return 1
    fi
    echo "Opened: $path"
}

stop_legacy_vitest_preview_server() {
    # Backwards compat: kill the detached `vite preview` server from earlier
    # versions of this skill, if it is still hanging around.
    local pid_file="$1/.vitest-preview.pid"
    [[ -f "$pid_file" ]] || return 0
    local old_pid
    old_pid="$(tr -d '[:space:]' <"$pid_file")"
    if [[ "$old_pid" =~ ^[0-9]+$ ]]; then
        kill "$old_pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
}

open_reports_in_browser() {
    local repo_root="$1" reports_root="$2"
    shift 2
    local modules=("$@")
    for module in "${modules[@]}"; do
        local candidates=(
            "$reports_root/latest/$module/report.html"
            "$reports_root/latest/$module/reports/c8Test/index.html"
            "$reports_root/latest/$module/reports/test/index.html"
            "$reports_root/latest/$module/reports/index.html"
        )
        local hit=""
        for c in "${candidates[@]}"; do
            if [[ -f "$c" ]]; then hit="$c"; break; fi
        done
        if [[ -z "$hit" ]]; then
            echo "No HTML report found for module '$module' under tmp/dood-tests/latest/."
            continue
        fi
        open_in_browser "$hit" || true
    done
}

# ---- main ----------------------------------------------------------------

assert_docker
REPO_ROOT="$(resolve_repo_root)"
echo "Repository root : $REPO_ROOT"

if [[ -z "$REPORTS_DIR" ]]; then
    REPORTS_DIR="$REPO_ROOT/tmp/dood-tests"
fi
mkdir -p "$REPORTS_DIR"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
TIMESTAMP_DIR="$REPORTS_DIR/$TIMESTAMP"
mkdir -p "$TIMESTAMP_DIR"
echo "Reports dir     : $TIMESTAMP_DIR"

DOCKERFILE="$(resolve_dockerfile "$REPO_ROOT")"
ensure_image "$IMAGE_NAME" "$DOCKERFILE" "$REBUILD"

if [[ ${#TARGETS[@]} -eq 0 ]]; then
    TARGETS=(full camunda-live e2e)
fi
mapfile -t TARGETS < <(normalize_targets "${TARGETS[@]}")

# Split host-only meta-targets from DooD-runnable Gradle modules.
GRADLE_TARGETS=()
WANTS_E2E=0
WANTS_CAMUNDA_LIVE=0
WANTS_FULL=0
WANTS_CONTRIB=0
for t in "${TARGETS[@]}"; do
    if [[ "$t" == "e2e" ]]; then
        WANTS_E2E=1
    elif [[ "$t" == "camunda-live" ]]; then
        WANTS_CAMUNDA_LIVE=1
    elif [[ "$t" == "full" ]]; then
        WANTS_FULL=1
    elif [[ "$t" == "contrib" ]]; then
        WANTS_CONTRIB=1
    else
        GRADLE_TARGETS+=("$t")
    fi
done
if [[ "$WANTS_FULL" == "1" ]]; then
    WANTS_CONTRIB=1
fi

if [[ "$WANTS_FULL" == "1" ]]; then
    echo "Gradle targets  : full (all engine profiles)"
elif [[ ${#GRADLE_TARGETS[@]} -gt 0 ]]; then
    echo "Gradle targets  : ${GRADLE_TARGETS[*]}"
else
    echo "Gradle targets  : (none)"
fi
if [[ "$WANTS_CONTRIB" == "1" ]]; then
    echo "Contrib tree    : enabled (crbpm-connectors isolated Gradle build)"
fi
if [[ "$WANTS_CAMUNDA_LIVE" == "1" ]]; then
    echo "Camunda live    : enabled (host-side :infra:camundaSmoke + *LiveIT)"
fi
if [[ "$WANTS_E2E" == "1" ]]; then
    echo "E2E             : enabled (host-side ./gradlew :ui:e2e)"
fi

GRADLE_EXIT=0
PHASE_SUMMARY_FILE="$TIMESTAMP_DIR/phase-summary.txt"
: > "$PHASE_SUMMARY_FILE"
ENGINE_NEUTRAL_EXIT=0

if [[ "$WANTS_FULL" == "1" ]]; then
    set +e
    run_engine_neutral_core_check "$REPO_ROOT"
    ENGINE_NEUTRAL_EXIT=$?
    set -e
fi

if [[ "$WANTS_FULL" == "1" && "$ENGINE_NEUTRAL_EXIT" -eq 0 ]]; then
    set +e
    run_full_verification_in_container "$REPO_ROOT" "$GRADLE_ARGS" "$IMAGE_NAME" "$KEEP_CONTAINER" "$TIMESTAMP_DIR" "$PHASE_SUMMARY_FILE"
    GRADLE_EXIT=$?
    set -e
elif [[ "$WANTS_FULL" == "1" ]]; then
    echo "Skipping DooD full verification because engine-neutral core check failed." >&2
    GRADLE_EXIT=$ENGINE_NEUTRAL_EXIT
elif [[ ${#GRADLE_TARGETS[@]} -gt 0 ]]; then
    GRADLE_TASKS=""
    for t in "${GRADLE_TARGETS[@]}"; do
        GRADLE_TASKS+=" :$t:test"
    done
    GRADLE_TASKS="${GRADLE_TASKS# }"

    GRADLE_CMD="./gradlew $GRADLE_TASKS --no-daemon --console=plain --stacktrace $GRADLE_ARGS"
    CONTAINER_CMD="set -eo pipefail; cd /work/crbpm && chmod +x ./gradlew && $GRADLE_CMD"

    DOCKER_ARGS=(
        run --init
        -v "$REPO_ROOT:/work"
        -v "/var/run/docker.sock:/var/run/docker.sock"
        -v "crbpm-dood-gradle:/cache/gradle"
        -v "crbpm-dood-npm:/cache/npm"
        --add-host "host.docker.internal:host-gateway"
        -e "TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal"
        -e "TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true"
        -w "/work/crbpm"
    )

    if [[ "$KEEP_CONTAINER" == "1" ]]; then
        DOCKER_ARGS+=(--name "crbpm-dood-tests-$(date +%Y%m%d%H%M%S)")
    else
        DOCKER_ARGS+=(--rm)
    fi

    DOCKER_ARGS+=("$IMAGE_NAME" bash -lc "$CONTAINER_CMD")

    section "Running tests in DooD container"
    echo "docker ${DOCKER_ARGS[*]}"

    set +e
    docker "${DOCKER_ARGS[@]}"
    GRADLE_EXIT=$?
    set -e

    section "Collecting DooD reports"
    collect_reports "$REPO_ROOT" "$TIMESTAMP_DIR" "${GRADLE_TARGETS[@]}"
else
    echo "No Gradle modules requested - skipping DooD test run."
fi

CONTRIB_EXIT=0
CONTRIB_MODULES=()
if [[ "$WANTS_CONTRIB" == "1" ]]; then
    mapfile -t CONTRIB_MODULES < <(contrib_modules "$REPO_ROOT")
    if [[ ${#CONTRIB_MODULES[@]} -eq 0 ]]; then
        echo "No crbpm-connectors module found - skipping contribution tree tests."
    else
        set +e
        run_contrib_tests_in_container "$REPO_ROOT" "$IMAGE_NAME" "$KEEP_CONTAINER"
        CONTRIB_EXIT=$?
        set -e
        echo "Collecting contribution tree reports..."
        collect_contrib_reports "$REPO_ROOT" "$TIMESTAMP_DIR" "${CONTRIB_MODULES[@]}"
    fi
fi

CAMUNDA_LIVE_EXIT=0
if [[ "$WANTS_CAMUNDA_LIVE" == "1" ]]; then
    set +e
    run_camunda_live_on_host "$REPO_ROOT" "$GRADLE_ARGS" "$CAMUNDA_SMOKE_WAIT_MS"
    CAMUNDA_LIVE_EXIT=$?
    set -e
fi

E2E_EXIT=0
if [[ "$WANTS_E2E" == "1" ]]; then
    set +e
    run_e2e_on_host "$REPO_ROOT" "$GRADLE_ARGS"
    E2E_EXIT=$?
    set -e
fi

section "Collecting reports"
# Full verification snapshots each phase inside run_full_verification_in_container.
# Explicit DooD module runs snapshot Gradle output before camunda-live can overwrite build/.
if [[ "$WANTS_E2E" == "1" ]]; then
    collect_e2e_reports "$REPO_ROOT" "$TIMESTAMP_DIR"
fi
if [[ "$WANTS_CAMUNDA_LIVE" == "1" ]]; then
    collect_camunda_live_reports "$REPO_ROOT" "$TIMESTAMP_DIR"
fi

SUMMARY="$TIMESTAMP_DIR/summary.txt"
{
    echo "Run at        : $(date -Iseconds)"
    echo "Image         : $IMAGE_NAME"
    echo "Targets       : ${TARGETS[*]}"
    echo "Gradle args   : $GRADLE_ARGS"
    if [[ "$WANTS_FULL" == "1" ]]; then
        echo "Engine-neutral: $ENGINE_NEUTRAL_EXIT"
    else
        echo "Engine-neutral: n/a"
    fi
    echo "Gradle exit   : $GRADLE_EXIT"
    if [[ "$WANTS_CONTRIB" == "1" ]]; then
        echo "Contrib exit  : $CONTRIB_EXIT"
    else
        echo "Contrib exit  : n/a"
    fi
    if [[ "$WANTS_CAMUNDA_LIVE" == "1" ]]; then
        echo "Camunda live  : $CAMUNDA_LIVE_EXIT"
    else
        echo "Camunda live  : n/a"
    fi
    if [[ "$WANTS_E2E" == "1" ]]; then
        echo "E2E exit      : $E2E_EXIT"
    else
        echo "E2E exit      : n/a"
    fi
    if [[ "$WANTS_FULL" == "1" && -f "$PHASE_SUMMARY_FILE" ]]; then
        cat "$PHASE_SUMMARY_FILE"
    fi
    echo "Repo root     : $REPO_ROOT"
    echo "Reports dir   : $TIMESTAMP_DIR"
} > "$SUMMARY"

update_latest_pointer "$REPORTS_DIR" "$TIMESTAMP_DIR"

section "Summary"
cat "$SUMMARY"

# Stop any leftover `vite preview` server from previous skill versions so the
# user does not see a stale http://127.0.0.1:4175/ page after this run.
stop_legacy_vitest_preview_server "$REPORTS_DIR"

if [[ "$NO_BROWSER" != "1" ]]; then
    section "Opening reports in default browser"
    BROWSER_MODULES=()
    if [[ "$WANTS_FULL" == "1" ]]; then
        BROWSER_MODULES+=(full-c8/backend full-c8/connectors full-c8/ui wrapper-only/backend wrapper-only/connectors stub/backend)
    else
        BROWSER_MODULES=("${GRADLE_TARGETS[@]}")
    fi
    # Contribution modules are small CLI/handler suites: only surface them when
    # they fail, to keep the number of browser tabs reasonable.
    if [[ "$WANTS_CONTRIB" == "1" && "$CONTRIB_EXIT" -ne 0 ]]; then
        for module in "${CONTRIB_MODULES[@]}"; do
            BROWSER_MODULES+=("contrib/$module")
        done
    fi
    if [[ "$WANTS_CAMUNDA_LIVE" == "1" ]]; then
        BROWSER_MODULES+=("camunda-live/backend")
    fi
    if [[ "$WANTS_E2E" == "1" ]]; then
        BROWSER_MODULES+=("e2e")
    fi
    if [[ ${#BROWSER_MODULES[@]} -gt 0 ]]; then
        open_reports_in_browser "$REPO_ROOT" "$REPORTS_DIR" "${BROWSER_MODULES[@]}"
    fi
fi

if [[ "$ENGINE_NEUTRAL_EXIT" -ne 0 ]]; then
    echo "Tests reported failures (engine-neutral=$ENGINE_NEUTRAL_EXIT, gradle=$GRADLE_EXIT, camunda-live=$CAMUNDA_LIVE_EXIT, e2e=$E2E_EXIT). See $TIMESTAMP_DIR." >&2
    exit "$ENGINE_NEUTRAL_EXIT"
fi
FAILURE_SUMMARY="gradle=$GRADLE_EXIT, contrib=$CONTRIB_EXIT, camunda-live=$CAMUNDA_LIVE_EXIT, e2e=$E2E_EXIT"
if [[ "$GRADLE_EXIT" -ne 0 ]]; then
    echo "Tests reported failures ($FAILURE_SUMMARY). See $TIMESTAMP_DIR." >&2
    exit "$GRADLE_EXIT"
fi
if [[ "$CONTRIB_EXIT" -ne 0 ]]; then
    echo "Tests reported failures ($FAILURE_SUMMARY). See $TIMESTAMP_DIR." >&2
    exit "$CONTRIB_EXIT"
fi
if [[ "$CAMUNDA_LIVE_EXIT" -ne 0 ]]; then
    echo "Tests reported failures ($FAILURE_SUMMARY). See $TIMESTAMP_DIR." >&2
    exit "$CAMUNDA_LIVE_EXIT"
fi
if [[ "$E2E_EXIT" -ne 0 ]]; then
    echo "Tests reported failures ($FAILURE_SUMMARY). See $TIMESTAMP_DIR." >&2
    exit "$E2E_EXIT"
fi

echo "All requested tests passed. Reports: $TIMESTAMP_DIR"
