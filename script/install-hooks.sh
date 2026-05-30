#!/bin/bash
# Install git hooks into .git/hooks/
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOOKS_DIR="$ROOT/.git/hooks"

cp "$ROOT/script/pre-push-hook.sh" "$HOOKS_DIR/pre-push"
chmod +x "$HOOKS_DIR/pre-push"
echo "Installed pre-push hook to .git/hooks/pre-push"
echo "To remove: rm .git/hooks/pre-push"
