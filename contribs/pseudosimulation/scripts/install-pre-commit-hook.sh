#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null) || {
	printf 'Hook installer: cannot locate the Git repository.\n' >&2
	exit 2
}
HOOKS_DIR=$(git -C "$REPO_ROOT" rev-parse --git-path hooks)
HOOK_SOURCE="$SCRIPT_DIR/pre-commit"
HOOK_TARGET="$HOOKS_DIR/pre-commit"

mkdir -p -- "$HOOKS_DIR"

if [[ -e "$HOOK_TARGET" || -L "$HOOK_TARGET" ]]; then
	if [[ -L "$HOOK_TARGET" && "$(readlink -- "$HOOK_TARGET")" == "$HOOK_SOURCE" ]]; then
		printf 'PSim pre-commit hook is already installed.\n'
		exit 0
	fi
	printf 'Hook installer: refusing to overwrite existing hook: %s\n' "$HOOK_TARGET" >&2
	printf 'Move it aside or make it invoke %s, then retry.\n' "$HOOK_SOURCE" >&2
	exit 1
fi

ln -s -- "$HOOK_SOURCE" "$HOOK_TARGET"
printf 'Installed PSim pre-commit hook at %s\n' "$HOOK_TARGET"
