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

if [[ -z "$EXPECTED_PLUGIN_VERSION" ]]; then
    echo "Could not resolve The Fae Realm plugin version." >&2
    exit 1
fi

if [[ -z "$EXPECTED_GENERATOR_VERSION" ]]; then
    echo "Could not resolve The Fae Realm generator version." >&2
    exit 1
fi

for command in curl jq java find sha256sum; do
    command -v "$command" >/dev/null 2>&1 || { echo "Required command is unavailable: $command" >&2; exit 1; }
done

[[ -s "$PLUGIN_JAR" ]] || { echo "The Fae Realm JAR not found or empty: $PLUGIN_JAR" >&2; exit 1; }

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
PAPER_URL="$(jq -r 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".url) // empty' <<<"$BUILDS_RESPONSE")"
PAPER_BUILD="$(jq -r 'first(.[] | select(.channel == "STABLE") | .id) // empty' <<<"$BUILDS_RESPONSE")"
[[ -n "$PAPER_URL" && -n "$PAPER_BUILD" ]] || { echo "No stable Paper build is available for Minecraft ${MC_VERSION}." >&2; exit 1; }

printf 'Minecraft: %s\nPaper stable build: %s\nDownload: %s\n' "$MC_VERSION" "$PAPER_BUILD" "$PAPER_URL" > "$WORK_DIR/paper-build.txt"
curl --fail-with-body -L -sS -H "User-Agent: ${USER_AGENT}" -o "$WORK_DIR/paper.jar" "$PAPER_URL"
[[ -s "$WORK_DIR/paper.jar" ]]

stop_process() {
    local pid="$1"
    local fd="$2"
    if kill -0 "$pid" >/dev/null 2>&1; then
        printf 'stop\n' >&"$fd" || true
    fi
    local deadline=$((SECONDS + SHUTDOWN_TIMEOUT_SECONDS))
    while kill -0 "$pid" >/dev/null 2>&1 && (( SECONDS < deadline )); do sleep 1; done
    if kill -0 "$pid" >/dev/null 2>&1; then
        kill "$pid" >/dev/null 2>&1 || true
        sleep 2
    fi
}

resolve_realm_metadata() {
    find "$WORK_DIR" -type f -name 'fae-realm-generator.yml' -print -quit
}

assert_realm_files() {
    local label="$1"
    REALM_METADATA="$(resolve_realm_metadata)"
    [[ -n "$REALM_METADATA" && -s "$REALM_METADATA" ]] || {
        echo "Paper runtime smoke ${label}: Fae Realm generator metadata was not created." >&2
        return 1
    }

    local realm_dir
    realm_dir="$(dirname "$REALM_METADATA")"
    [[ "$(basename "$realm_dir")" == "fae_realm" ]] || {
        echo "Paper runtime smoke ${label}: metadata is not inside a fae_realm folder: $REALM_METADATA" >&2
        return 1
    }

    for expected in \
        "current-generator-version: ${EXPECTED_GENERATOR_VERSION}" \
        'current-preset: radiant_end' \
        'terrain-profiles: true' \
        'radiant-end-layout: true' \
        'growth-density: 1.45' \
        'structure-spacing-chunks: 10' \
        'landmark-spacing-chunks: 28' \
        'first-settings-fingerprint:' \
        'current-settings-fingerprint:'; do
        if ! grep -Fq "$expected" "$REALM_METADATA"; then
            echo "Paper runtime smoke ${label}: metadata is missing '$expected'." >&2
            cat "$REALM_METADATA" >&2 || true
            return 1
        fi
    done

    [[ -d "$realm_dir/region" ]] || {
        echo "Paper runtime smoke ${label}: Fae Realm region storage was not created at $realm_dir." >&2
        return 1
    }
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
    local deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
    local started=false

    while kill -0 "$server_pid" >/dev/null 2>&1 && (( SECONDS < deadline )); do
        if grep -Fq 'Done (' "$console_log" 2>/dev/null; then
            started=true
            break
        fi
        if grep -Eq 'Error occurred while enabling TheFaeRealm|Could not load .*TheFaeRealm|Unable to create or load the Fae Realm' "$console_log" 2>/dev/null; then
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
    assert_realm_files "$label" || {
        stop_process "$server_pid" 3
        wait "$server_pid" >/dev/null 2>&1 || true
        exec 3>&-
        cat "$console_log" >&2 || true
        return 1
    }

    printf 'fae info\n' >&3
    printf 'fae locate crystal_woods\n' >&3
    sleep 2
    stop_process "$server_pid" 3

    local status=0
    wait "$server_pid" || status=$?
    exec 3>&-
    (( status == 0 )) || { echo "Paper runtime smoke ${label}: Paper exited with status ${status}." >&2; return 1; }

    grep -Eq 'Enabling TheFaeRealm v|The Fae Realm .* enabled on Minecraft' "$console_log" || {
        echo "Paper runtime smoke ${label}: The Fae Realm was not observed enabling." >&2; return 1;
    }
    grep -Fq 'Installed concurrent BlockPopulator guard for fae_realm' "$console_log" || {
        echo "Paper runtime smoke ${label}: Chunky concurrency guard was not installed." >&2
        cat "$console_log" >&2 || true
        return 1
    }
    grep -Fq "Generator: v${EXPECTED_GENERATOR_VERSION} / radiant_end" "$console_log" || {
        echo "Paper runtime smoke ${label}: /fae info did not report generator v${EXPECTED_GENERATOR_VERSION}/radiant_end." >&2; return 1;
    }
    grep -Fq 'Nearest Crystal Woods region sample:' "$console_log" || {
        echo "Paper runtime smoke ${label}: /fae locate did not return a Crystal Woods result." >&2; return 1;
    }
    if grep -Eq 'Error occurred while enabling TheFaeRealm|Could not load .*TheFaeRealm|Unable to create or load the Fae Realm|Exception in server tick loop|java\.lang\.(NoSuchMethodError|NoClassDefFoundError|LinkageError)' "$console_log"; then
        echo "Paper runtime smoke ${label}: fatal runtime/linkage failure detected." >&2
        cat "$console_log" >&2 || true
        return 1
    fi
    grep -Fq 'Stopping server' "$console_log" || {
        echo "Paper runtime smoke ${label}: a normal server shutdown was not observed." >&2; return 1;
    }
}

run_cycle "first"
FIRST_METADATA_HASH="$(sha256sum "$REALM_METADATA" | awk '{print $1}')"
FIRST_REALM_PATH="${REALM_METADATA#"$WORK_DIR"/}"
grep -Fq 'Initialized the Fae Realm arrival island and return portal.' "$WORK_DIR/first.console.log" || {
    echo "First boot did not report initial arrival-area setup." >&2; exit 1;
}

run_cycle "second"
SECOND_METADATA_HASH="$(sha256sum "$REALM_METADATA" | awk '{print $1}')"
SECOND_REALM_PATH="${REALM_METADATA#"$WORK_DIR"/}"
grep -Fq 'Existing Fae Realm detected; preserving player changes around realm spawn.' "$WORK_DIR/second.console.log" || {
    echo "Second boot did not preserve the initialized arrival area." >&2; exit 1;
}
[[ "$FIRST_REALM_PATH" == "$SECOND_REALM_PATH" ]] || {
    echo "Fae Realm storage moved between boot cycles: $FIRST_REALM_PATH -> $SECOND_REALM_PATH" >&2; exit 1;
}
[[ -n "$FIRST_METADATA_HASH" && -n "$SECOND_METADATA_HASH" ]]

cat > "$WORK_DIR/smoke-result.txt" <<EOF
The Fae Realm Paper runtime smoke: PASS
The Fae Realm: ${EXPECTED_PLUGIN_VERSION}
Minecraft: ${MC_VERSION}
Paper stable build: ${PAPER_BUILD}
Cycles: 2
Fae Realm dimension created: yes
Fae Realm storage: ${SECOND_REALM_PATH}
Generator metadata present: yes
Generator version: ${EXPECTED_GENERATOR_VERSION}
Preset: radiant_end
Radiant End layout: yes
Growth ecology: 1.45
Landmark spacing: 28 chunks
Chunky concurrency guard: installed
Console /fae info: pass
Console /fae locate: pass
Second boot loaded persisted realm: yes
Arrival area preserved on second boot: yes
EOF
cat "$WORK_DIR/smoke-result.txt"
