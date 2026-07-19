#!/usr/bin/env python3
"""Stamp the external runner's verdict into a backfill report (stage-4 runner).

Usage: stamp-backfill.py <backfill-report.yaml> <results.xml> <target_repo> <command>

Replaces (or appends) the report's top-level `run:` block with the deterministic
verdict derived from the results file: result, results_file, sha (target-repo HEAD),
timestamp (from the results file). The agent never writes `run:` — the runner owns it.
Exit 0 = green, 1 = red, 2 = structural error.
"""
import json
import subprocess
import sys

import yaml

import junitxml


def replace_top_level_block(lines, key, block_lines):
    """Drop the existing top-level `key:` block (if any) and append block_lines at EOF."""
    out, skipping = [], False
    for line in lines:
        if line.startswith(f"{key}:"):
            skipping = True
            continue
        if skipping and line[:1] not in (" ", "\t", "#", "\n", ""):
            skipping = False
        if not skipping:
            out.append(line)
    while out and out[-1].strip() == "":
        out.pop()
    if out and not out[-1].endswith("\n"):
        out[-1] += "\n"
    return out + ["\n"] + block_lines


def main():
    report_path, results_path, repo, command = sys.argv[1:5]
    try:
        doc = junitxml.parse(results_path)
    except Exception as exc:
        print(f"stamp-backfill: cannot read {results_path}: {type(exc).__name__}: {exc}")
        return 2
    green = junitxml.is_green(doc)
    sha = subprocess.run(["git", "-C", repo, "rev-parse", "HEAD"],
                         capture_output=True, text=True, check=True).stdout.strip()
    block = [
        "run:\n",
        f"  command: {json.dumps(command)}\n",  # JSON string = valid YAML double-quoted scalar
        f"  result: {'green' if green else 'red'}\n",
        f"  results_file: {results_path}\n",
        f"  sha: {sha}\n",
        f"  timestamp: \"{doc['timestamp']}\"\n",
        "  notes: stamped by the pipeline external runner (stage-4 external-runner variant)\n",
    ]
    with open(report_path, encoding="utf-8") as fh:
        lines = fh.readlines()
    lines = replace_top_level_block(lines, "run", block)
    text = "".join(lines)
    yaml.safe_load(text)  # must still parse — fail loudly before writing otherwise
    with open(report_path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"stamp-backfill: run.result={'green' if green else 'red'} "
          f"tests={len(doc['tests'])} sha={sha[:12]}")
    return 0 if green else 1


if __name__ == "__main__":
    sys.exit(main())
