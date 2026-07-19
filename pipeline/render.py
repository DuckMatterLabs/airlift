#!/usr/bin/env python3
"""Render a generic pipeline prompt template with target-specific values.

Usage: render.py <template> <output> KEY=VALUE... [KEY=@/path/to/file ...]
A value starting with '@' is replaced by that file's contents.
Fails if any {{PLACEHOLDER}} remains unresolved (guards against target/prompt drift).
"""
import re
import sys


def main() -> int:
    template_path, output_path = sys.argv[1], sys.argv[2]
    subs = {}
    for pair in sys.argv[3:]:
        key, _, value = pair.partition("=")
        if value.startswith("@"):
            with open(value[1:], encoding="utf-8") as fh:
                value = fh.read().strip()
        subs[key] = value

    with open(template_path, encoding="utf-8") as fh:
        text = fh.read()
    for key, value in subs.items():
        text = text.replace("{{" + key + "}}", value)

    leftover = sorted(set(re.findall(r"\{\{([A-Z_]+)\}\}", text)))
    if leftover:
        print(f"render.py: unresolved placeholders in {template_path}: {leftover}",
              file=sys.stderr)
        return 1
    with open(output_path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"rendered {output_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
