#!/usr/bin/env python3
"""Deny a narrow set of clearly destructive shell commands."""

import json
import re
import sys
from typing import Any, Dict, Pattern, Tuple


BLOCKED_PATTERNS: Tuple[Tuple[Pattern[str], str], ...] = (
    (re.compile(r"(^|[;&|]\s*)rm\s+(-[^\s]*[rf][^\s]*\s+)*(/|~|\$HOME)(\s|$)"),
     "recursive removal of a broad filesystem target"),
    (re.compile(r"\bgit\s+reset\s+--hard\b"), "git reset --hard can discard user work"),
    (re.compile(r"\bgit\s+clean\s+-[^\s]*[fdx][^\s]*"), "git clean can permanently remove untracked work"),
    (re.compile(r"\bgit\s+checkout\s+--\s"), "git checkout -- can discard user changes"),
    (re.compile(r"\bgit\s+push\b[^\n]*(--force|-f)(\s|$)"), "force-push can rewrite shared history"),
    (re.compile(r"\bdocker\s+system\s+prune\b"), "docker system prune has a broad deletion scope"),
    (re.compile(r"\bkubectl\s+delete\s+(namespace|ns)\b"), "namespace deletion is a broad destructive action"),
    (re.compile(r"\bDROP\s+(DATABASE|SCHEMA)\b", re.IGNORECASE), "database/schema deletion is destructive"),
)


def denied(reason: str) -> Dict[str, Any]:
    """Build the documented PreToolUse denial response."""
    return {
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": f"Blocked by repository policy: {reason}.",
        }
    }


def main() -> None:
    """Read one hook event from stdin and emit JSON only when denying it."""
    event = json.load(sys.stdin)
    command = str(event.get("tool_input", {}).get("command", ""))
    for pattern, reason in BLOCKED_PATTERNS:
        if pattern.search(command):
            print(json.dumps(denied(reason)))
            return


if __name__ == "__main__":
    main()
