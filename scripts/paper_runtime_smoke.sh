#!/usr/bin/env bash
set -euo pipefail

PLUGIN_JAR="${1:?Usage: paper_runtime_smoke.sh <fae-realm-jar> [work-directory]}"
WORK_DIR="${2:-build/paper-runtime-smoke}"
MC_VERSION="${PAPER_MINECRAFT_VERSION:-26.2}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXPECTED_PLUGIN_VERSION="${FAE_REALM_SMOKE_VERSION:-$(sed -n "s/^version = '\([^']*\)'/\1/p" "$REPO_ROOT/build.gradle" | head -n 1 | tr -d '\r')}"
GENERATOR_VERSION_SOURCE="$REPO_ROOT/src/paper/java/com/wickidcow/aetherlegacy/paper/world/FaeGeneratorVersion.java"
EXPECTED_GENERATOR_VERSION="${FAE_REALM_SMOKE_GENERATOR_VERSION:-$(sed -n 's/.*CURRENT = \([0-9][0-9]*\);.*/\1/p' "$GENERATOR_VERSION_SOURCE" | head -n 1 | tr -d '\r')}"
USER_AGENT="${PAPER_DOWNLOAD_USER_AGENT:-The-Fae-Realm-CI/${EXPECTED_PLUGIN_VERSION} (https://github.com/wickidcow/The-Fae-Realm)}"
STARTUP_TIMEOUT_SECONDS="${PAPER_SMOKE_STARTUP_TIMEOUT:-240}"
SHUTDOWN_TIMEOUT_SECONDS="${PAPER_SMOKE_SHUTDOWN_TIMEOUT:-60}"
REALM_METADATA=""
FAE_WORLDS=(fae_realm fae_realm_wildbloom fae_realm_gloam fae_realm_starfall)
SECONDARY_PLANES=(
    "Wildbloom:fae_realm_wildbloom"
    "Gloam:fae_realm_gloam"
    "Starfall:fae_realm_starfall"
)

if [[ -z "$EXPECTED_PLUGIN_VERSION" ]]; then
    echo "Could not resolve The Fae Realm plugin version." >&2
    exit 1
fi

if [[ -z "$EXPECTED_GENERATOR_VERSION" ]]; then
    echo "Could not resolve The Fae Realm generator version from $GENERATOR_VERSION_SOURCE." >&2
    exit 1
fi

for command in curl jq java find sha256sum; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "Required command is unavailable: $command" >&2
        exit 1
    fi
done

if [[ ! -s "$PLUGIN_JAR" ]]; then
    echo "The Fae Realm JAR not found or empty: $PLUGIN_JAR" >&2
    exit 1
fi

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR/plugins"
cp "$PLUGIN_JAR" "$WORK_DIR/plugins/TheFaeRealm-smoke.jar"
printf 'eula=true\n' > "$WORK_DIR/eula.txt"
cat > "$WORK_DIR/server.properties" <<'PROPERTIES'
online-mode=false
level-name=smoke-world
max-players=1
spawn-protection=0
view-distance=2
simulation-distance=2
pause-when-empty-seconds=-1
enable-query=false
enable-rcon=false
motd=The Fae Realm CI runtime smoke
PROPERTIES

BUILDS_URL="https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds"
BUILDS_RESPONSE="$(curl --fail-with-body -sS -H "User-Agent: ${USER_AGENT}" "$BUILDS_URL")"

if jq -e '.ok == false' >/dev/null 2>&1 <<<"$BUILDS_RESPONSE"; then
    jq -r '.message // "Paper downloads service returned an unknown error"' <<<"$BUILDS_RESPONSE" >&2
    exit 1
fi

PAPER_URL="$(jq -r 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".url) // empty' <<<"$BUILDS_RESPONSE")"
PAPER_BUILD="$(jq -r 'first(.[] | select(.channel == "STABLE") | .id) // empty' <<<"$BUILDS_RESPONSE")"
if [[ -z "$PAPER_URL" || -z "$PAPER_BUILD" ]]; then
    echo "No stable Paper build is available for Minecraft ${MC_VERSION}." >&2
    exit 1
fi

printf 'Minecraft: %s\nPaper stable build: %s\nDownload: %s\n' "$MC_VERSION" "$PAPER_BUILD" "$PAPER_URL" \
    > "$WORK_DIR/paper-build.txt"
curl --fail-with-body -L -sS -H "User-Agent: ${USER_AGENT}" -o "$WORK_DIR/paper.jar" "$PAPER_URL"
test -s "$WORK_DIR/paper.jar"

stop_process() {
    local pid="$1"
    local fd="$2"

    if kill -0 "$pid" >/dev/null 2>&1; then
        printf 'stop\n' >&"$fd" || true
    fi

    local deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
    while kill -0 "$pid" >/dev/null 2>&1 && (( SECONDS < deadline )); do
        sleep 1
    done

    if kill -0 "$pid" >/dev/null 2>&1; then
        echo "Paper did not stop cleanly within ${SHUTDOWN_TIMEOUT_SECONDS}s; terminating it." >&2
        kill "$pid" >/dev/null 2>&1 || true
        sleep 2
    fi
}

resolve_world_metadata() {
    local world_name="$1"
    find "$WORK_DIR" -type f -path "*/${world_name}/fae-realm-generator.yml" -print -quit
}

assert_world_metadata() {
    local label="$1"
    local world_name="$2"
    local metadata
    metadata="$(resolve_world_metadata "$world_name")"

    if [[ -z "$metadata" || ! -s "$metadata" ]]; then
        echo "Paper runtime smoke ${label}: generator metadata missing for ${world_name}." >&2
        find "$WORK_DIR" -type f -name 'fae-realm-generator.yml' -print >&2 || true
        return 1
    fi

    if [[ "$world_name" == "fae_realm" ]]; then
        REALM_METADATA="$metadata"
    fi

    for expected in \
        "current-generator-version: ${EXPECTED_GENERATOR_VERSION}" \
        'terrain-profiles: true' \
        'first-settings-fingerprint:' \
        'current-settings-fingerprint:' \
        "world-name: ${world_name}"; do
        if ! grep -Fq "$expected" "$metadata"; then
            echo "Paper runtime smoke ${label}: ${world_name} metadata is missing '$expected'." >&2
            cat "$metadata" >&2 || true
            return 1
        fi
    done

    local world_dir
    world_dir="$(dirname "$metadata")"
    if [[ ! -d "$world_dir/region" ]]; then
        echo "Paper runtime smoke ${label}: ${world_name} region storage was not created at ${world_dir}." >&2
        find "$world_dir" -maxdepth 2 -type f -print >&2 || true
        return 1
    fi
}

assert_fae_worlds() {
    local label="$1"

    for world_name in "${FAE_WORLDS[@]}"; do
        assert_world_metadata "$label" "$world_name" || return 1
    done

    if [[ -z "$REALM_METADATA" || ! -s "$REALM_METADATA" ]]; then
        echo "Paper runtime smoke ${label}: central realm metadata could not be resolved." >&2
        return 1
    fi

    if ! grep -Fq 'structure-spacing-chunks: 10' "$REALM_METADATA"; then
        echo "Paper runtime smoke ${label}: central Fae Realm metadata lost base structure spacing." >&2
        cat "$REALM_METADATA" >&2 || true
        return 1
    fi
}

assert_plane_logs() {
    local label="$1"
    local console_log="$WORK_DIR/${label}.console.log"

    for plane_spec in "${SECONDARY_PLANES[@]}"; do
        local plane_name="${plane_spec%%:*}"
        local world_name="${plane_spec#*:}"
        if ! grep -Fq "Linked Fae plane active: ${plane_name} (${world_name}, generator v${EXPECTED_GENERATOR_VERSION})." "$console_log"; then
            echo "Paper runtime smoke ${label}: ${plane_name} did not report as an active linked plane." >&2
            cat "$console_log" >&2 || true
            return 1
        fi
    done

    for world_name in "${FAE_WORLDS[@]}"; do
        if ! grep -Fq "Async-safe Fae populator registry active for ${world_name} (" "$console_log"; then
            echo "Paper runtime smoke ${label}: async-safe populator registry was not activated for ${world_name}." >&2
            cat "$console_log" >&2 || true
            return 1
        fi
    done
}

run_cycle() {
    local label="$1"
    local console_log="$WORK_DIR/${label}.console.log"
    local input_fifo="$WORK_DIR/${label}.stdin"

    rm -f "$input_fifo"
    mkfifo "$input_fifo"
    exec 3<>"$input_fifo"

    (
        cd "$WORK_DIR"
        java -Xms512M -Xmx2G -jar paper.jar --nogui <&3 > "${label}.console.log" 2>&1
    ) &
    local server_pid=$!
    local startup_deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
    local started=false

    while kill -0 "$server_pid" >/dev/null 2>&1 && (( SECONDS < startup_deadline )); do
        if grep -Fq 'Done (' "$console_log" 2>/dev/null; then
            started=true
            break
        fi
        if grep -Eq 'Error occurred while enabling TheFaeRealm|Could not load .*TheFaeRealm' "$console_log" 2>/dev/null; then
            break
        fi
        sleep 2
    done

    if [[ "$started" != true ]]; then
        echo "Paper runtime smoke ${label}: server did not reach Done." >&2
        stop_process "$server_pid" 3
        wait "$server_pid" >/dev/null 2>&1 || true
        exec 3>&-
        cat "$console_log" >&2 || true
        return 1
    fi

    sleep 3
    if ! assert_fae_worlds "$label"; then
        stop_process "$server_pid" 3
        wait "$server_pid" >/dev/null 2>&1 || true
        exec 3>&-
        cat "$console_log" >&2 || true
        return 1
    fi

    if ! assert_plane_logs "$label"; then
        stop_process "$server_pid" 3
        wait "$server_pid" >/dev/null 2>&1 || true
        exec 3>&-
        return 1
    fi

    printf 'fae info\n' >&3
    printf 'fae planes\n' >&3
    printf 'fae locate crystal_woods\n' >&3
    sleep 2

    stop_process "$server_pid" 3

    local server_status=0
    wait "$server_pid" || server_status=$?
    exec 3>&-

    if (( server_status != 0 )); then
        echo "Paper runtime smoke ${label}: Paper exited with status ${server_status}." >&2
        cat "$console_log" >&2 || true
        return 1
    fi

    if ! grep -Eq 'Enabling TheFaeRealm v|The Fae Realm .* enabled on Minecraft' "$console_log"; then
        echo "Paper runtime smoke ${label}: The Fae Realm was not observed enabling." >&2
        cat "$console_log" >&2 || true
        return 1
    fi

    if ! grep -Fq "Generator: v${EXPECTED_GENERATOR_VERSION} / balanced" "$console_log"; then
        echo "Paper runtime smoke ${label}: /fae info did not report generator v${EXPECTED_GENERATOR_VERSION}/balanced." >&2
        cat "$console_log" >&2 || true
        return 1
    fi

    if ! grep -Fq 'Linked planes: 4 / 4' "$console_log"; then
        echo "Paper runtime smoke ${label}: /fae info did not report all four Fae planes." >&2
        cat "$console_log" >&2 || true
        return 1
    fi

    for world_name in fae_realm_wildbloom fae_realm_gloam fae_realm_starfall; do
        if ! grep -Fq "$world_name" "$console_log"; then
            echo "Paper runtime smoke ${label}: /fae planes did not report ${world_name}." >&2
            cat "$console_log" >&2 || true
            return 1
        fi
    done

    if ! grep -Fq 'Nearest Crystal Woods region sample' "$console_log"; then
        echo "Paper runtime smoke ${label}: /fae locate did not return a Crystal Woods result." >&2
        cat "$console_log" >&2 || true
        return 1
    fi

    if grep -Eq 'Error occurred while enabling TheFaeRealm|Could not load .*TheFaeRealm|Unable to create or load the Fae Realm' "$console_log"; then
        echo "Paper runtime smoke ${label}: The Fae Realm reported an enable/load failure." >&2
        cat "$console_log" >&2 || true
        return 1
    fi

    if grep -Eq 'Exception in server tick loop|Failed to save player data|java\.lang\.(NoSuchMethodError|NoClassDefFoundError|LinkageError)' "$console_log"; then
        echo "Paper runtime smoke ${label}: fatal runtime/linkage failure detected." >&2
        cat "$console_log" >&2 || true
        return 1
    fi

    if ! grep -Fq 'Stopping server' "$console_log"; then
        echo "Paper runtime smoke ${label}: a normal server shutdown was not observed." >&2
        cat "$console_log" >&2 || true
        return 1
    fi
}

run_cycle "first"
FIRST_METADATA_HASH="$(sha256sum "$REALM_METADATA" | awk '{print $1}')"
FIRST_REALM_PATH="${REALM_METADATA#"$WORK_DIR"/}"

if ! grep -Fq 'Initialized the Fae Realm arrival island and return portal.' "$WORK_DIR/first.console.log"; then
    echo "First boot did not report initial arrival-area setup." >&2
    cat "$WORK_DIR/first.console.log" >&2 || true
    exit 1
fi

run_cycle "second"
SECOND_METADATA_HASH="$(sha256sum "$REALM_METADATA" | awk '{print $1}')"
SECOND_REALM_PATH="${REALM_METADATA#"$WORK_DIR"/}"

if ! grep -Fq 'Existing Fae Realm detected; preserving player changes around realm spawn.' "$WORK_DIR/second.console.log"; then
    echo "Second boot did not preserve the initialized central arrival area." >&2
    cat "$WORK_DIR/second.console.log" >&2 || true
    exit 1
fi

if [[ -z "$FIRST_METADATA_HASH" || -z "$SECOND_METADATA_HASH" ]]; then
    echo "Could not hash persisted generator metadata." >&2
    exit 1
fi

if [[ "$FIRST_REALM_PATH" != "$SECOND_REALM_PATH" ]]; then
    echo "Fae Realm storage moved between boot cycles: $FIRST_REALM_PATH -> $SECOND_REALM_PATH" >&2
    exit 1
fi

cat > "$WORK_DIR/smoke-result.txt" <<EOF
The Fae Realm Paper runtime smoke: PASS
The Fae Realm: ${EXPECTED_PLUGIN_VERSION}
Minecraft: ${MC_VERSION}
Paper stable build: ${PAPER_BUILD}
Cycles: 2
Fae worlds created: 4
Central realm: fae_realm
Wildbloom: fae_realm_wildbloom
Gloam: fae_realm_gloam
Starfall: fae_realm_starfall
Central metadata storage: ${SECOND_REALM_PATH}
Generator metadata present for all planes: yes
Generator version: ${EXPECTED_GENERATOR_VERSION}
Terrain profiles persisted: yes
Settings provenance persisted: yes
Async-safe populator registry: pass (4 / 4 worlds)
Console /fae info: pass
Console /fae planes: pass
Console /fae locate: pass
Second boot loaded persisted Fae worlds: yes
Central arrival area preserved on second boot: yes
EOF
cat "$WORK_DIR/smoke-result.txt"
