#!/usr/bin/env python3
"""Inject a small deterministic reminder when a Codex session starts."""

import json


def main() -> None:
    """Return project-specific context using the documented SessionStart shape."""
    context = (
        "Before editing, read AGENTS.md and the requirement/architecture sources. "
        "For implementation work, finish by running mvn -B -ntp clean verify. "
        "Local MySQL is not proof of GoldenDB production compatibility."
    )
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "SessionStart",
            "additionalContext": context,
        }
    }))


if __name__ == "__main__":
    main()
