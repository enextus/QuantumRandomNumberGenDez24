#!/usr/bin/env bash
set -euo pipefail

show_help() {
    cat <<'HELP'
Usage:
  scripts/make-chatgpt-context-zip.sh [output.zip]

Creates a ChatGPT context ZIP from the project root.
Designed for Git Bash/MSYS2. Does not call PowerShell.

If output.zip is omitted, the archive is created in the project root with
name rep-qrng-chaos-game_context_YYYY-MM-DD_HH-MM-SS.zip.
HELP
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    show_help
    exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd -P)"
TIMESTAMP="$(date '+%Y-%m-%d_%H-%M-%S')"
OUTPUT_PATH="${1:-"$PROJECT_ROOT/rep-qrng-chaos-game_context_$TIMESTAMP.zip"}"

if command -v cygpath >/dev/null 2>&1 && [[ "$OUTPUT_PATH" =~ ^[A-Za-z]:[\\/].* ]]; then
    OUTPUT_PATH="$(cygpath -u "$OUTPUT_PATH")"
elif [[ "$OUTPUT_PATH" != /* ]]; then
    OUTPUT_PATH="$PROJECT_ROOT/$OUTPUT_PATH"
fi

OUTPUT_DIR="$(dirname "$OUTPUT_PATH")"
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_PATH"

if ! command -v zip >/dev/null 2>&1 && ! command -v jar >/dev/null 2>&1; then
    cat >&2 <<'ERROR'
Neither 'zip' nor 'jar' was found.
Install zip for Git Bash/MSYS2 or make sure the JDK 'jar' tool is on PATH.
ERROR
    exit 1
fi

HAS_TTY=false
if [[ -t 1 && "${TERM:-}" != "dumb" ]]; then
    HAS_TTY=true
fi

clear_line() {
    if [[ "$HAS_TTY" == true ]]; then
        printf '\r\033[K'
    else
        printf '\r'
    fi
}

print_banner() {
    cat <<'BANNER'
╭──────────────────────────────────────────────╮
│  ChatGPT Context ZIP                         │
│  rep-qrng-chaos-game                         │
╰──────────────────────────────────────────────╯
BANNER
}

render_progress() {
    local current="$1"
    local total="$2"
    local message="$3"
    local width=28
    local percent=$(( current * 100 / total ))
    local filled=$(( current * width / total ))
    local empty=$(( width - filled ))
    local bar=""

    for ((i = 0; i < filled; i++)); do
        bar+="█"
    done
    for ((i = 0; i < empty; i++)); do
        bar+="░"
    done

    clear_line
    printf '  [%s] %3d%%  %s' "$bar" "$percent" "$message"
}

finish_progress() {
    local message="$1"
    clear_line
    printf '  ✓ %s\n' "$message"
}

run_with_spinner() {
    local message="$1"
    shift

    local frames=("⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏")
    local frame_index=0

    "$@" &
    local pid=$!

    while kill -0 "$pid" 2>/dev/null; do
        clear_line
        printf '  %s %s' "${frames[$((frame_index % ${#frames[@]}))]}" "$message"
        frame_index=$((frame_index + 1))
        sleep 0.08
    done

    if wait "$pid"; then
        clear_line
        printf '  ✓ %s\n' "$message"
    else
        local exit_code=$?
        clear_line
        printf '  ✗ %s\n' "$message"
        return "$exit_code"
    fi
}

STAGING_DIR="$(mktemp -d)"
cleanup() {
    rm -rf "$STAGING_DIR"
}
trap cleanup EXIT

copy_file() {
    local source_path="$1"
    local target_path="$STAGING_DIR/$source_path"
    mkdir -p "$(dirname "$target_path")"
    cp -p "$PROJECT_ROOT/$source_path" "$target_path"
}

copy_directory() {
    local source_path="$1"
    (
        cd "$PROJECT_ROOT"
        tar \
            --exclude='target' \
            --exclude='.git' \
            --exclude='logs' \
            --exclude='*.zip' \
            --exclude='*.class' \
            --exclude='*.jar' \
            --exclude='.idea' \
            --exclude='*.iml' \
            -cf - "$source_path"
    ) | (
        cd "$STAGING_DIR"
        tar -xf -
    )
}

create_archive_with_zip() {
    (cd "$STAGING_DIR" && zip -qr "$OUTPUT_PATH" .)
}

create_archive_with_jar() {
    jar cMf "$OUTPUT_PATH" -C "$STAGING_DIR" .
}

verify_archive() {
    if command -v unzip >/dev/null 2>&1; then
        unzip -tq "$OUTPUT_PATH" >/dev/null
    elif command -v jar >/dev/null 2>&1; then
        jar tf "$OUTPUT_PATH" >/dev/null
    fi
}

INCLUDED_PATHS=()
CANDIDATE_PATHS=(
    "pom.xml"
    "Readme.md"
    "README.md"
    "TEST_STATUS.md"
    ".env.example"
    ".gitignore"
    "src"
    "scripts/make-chatgpt-context-zip.sh"
)

print_banner

candidate_index=0
candidate_total=${#CANDIDATE_PATHS[@]}
for candidate_path in "${CANDIDATE_PATHS[@]}"; do
    candidate_index=$((candidate_index + 1))
    render_progress "$candidate_index" "$candidate_total" "Collecting context files"

    if [[ ! -e "$PROJECT_ROOT/$candidate_path" ]]; then
        continue
    fi

    INCLUDED_PATHS+=("$candidate_path")

    if [[ -d "$PROJECT_ROOT/$candidate_path" ]]; then
        copy_directory "$candidate_path"
    else
        copy_file "$candidate_path"
    fi
done
finish_progress "Context files collected"

if [[ ${#INCLUDED_PATHS[@]} -eq 0 ]]; then
    echo "No context files found. Run this script from the project root or scripts directory." >&2
    exit 1
fi

if command -v zip >/dev/null 2>&1; then
    run_with_spinner "Compressing archive with zip" create_archive_with_zip
else
    run_with_spinner "Compressing archive with jar fallback" create_archive_with_jar
fi

run_with_spinner "Verifying ZIP integrity" verify_archive

ARCHIVE_SIZE_BYTES="$(wc -c < "$OUTPUT_PATH" | tr -d ' ')"
ARCHIVE_SIZE_KB="$(( (ARCHIVE_SIZE_BYTES + 1023) / 1024 ))"

printf '\nCreated ChatGPT context archive:\n'
printf '  %s\n' "$OUTPUT_PATH"
printf 'Size: %s KB\n' "$ARCHIVE_SIZE_KB"
printf 'Included:\n'
printf ' - %s\n' "${INCLUDED_PATHS[@]}"
