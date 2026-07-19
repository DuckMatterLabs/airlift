#!/usr/bin/env python3
"""Airlift verification spine: map test failures back to violated IR claims.

Usage: report-claims.py --results <junit.xml> --tests-dir <java-src-dir> [--ir <ir-dir>]
                        [--spine-out <spine.yaml>]
Prints a claim-level verdict; exit 0 if green, 1 if any claim violated.
--spine-out additionally writes the machine-readable spine file of IR-SCHEMA.md
section 8 (suite, results_file, sha, timestamp, per-test claim binding + outcome) —
the target-neutral form consumed by pipeline/promote.py.
"""
import argparse
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

CLAIM_RE = re.compile(
    r'@AirliftClaim\("([^"]+)"\)\s*(?:@\w+(?:\([^)]*\))?\s*)*public\s+void\s+(\w+)\s*\(',
    re.MULTILINE)


def method_claims(tests_dir):
    mapping = {}
    for dirpath, _dirs, files in os.walk(tests_dir):
        for fname in files:
            if fname.endswith(".java"):
                with open(os.path.join(dirpath, fname), encoding="utf-8") as fh:
                    for claim, method in CLAIM_RE.findall(fh.read()):
                        mapping[method] = claim
    return mapping


def claim_title(ir_dir, claim_id):
    if not ir_dir:
        return ""
    path = os.path.join(ir_dir, "claims", claim_id + ".yaml")
    if os.path.exists(path):
        with open(path, encoding="utf-8") as fh:
            for line in fh:
                if line.startswith("title:"):
                    return line.partition(":")[2].strip()
    return ""


def write_spine(path, results_path, tests_dir, mapping, suites):
    """Machine-readable spine (IR-SCHEMA.md section 8) for deterministic consumers."""
    sha = subprocess.run(["git", "-C", tests_dir, "rev-parse", "HEAD"],
                         capture_output=True, text=True, check=True).stdout.strip()
    suite_name = timestamp = None
    rows, unbound = [], []
    for suite in suites:
        if suite_name is None:
            suite_name = suite.get("name")
            timestamp = suite.get("timestamp")
        for case in suite.findall("testcase"):
            name = case.get("name", "?")
            short = (case.get("classname") or "").rsplit(".", 1)[-1]
            if case.find("failure") is not None:
                outcome = "failed"
            elif case.find("error") is not None:
                outcome = "error"
            elif case.find("skipped") is not None:
                outcome = "skipped"  # not a pass: a skipped test attests nothing
            else:
                outcome = "passed"
            claim = mapping.get(name)
            if claim is None:
                unbound.append(f"{short}#{name}")
            else:
                rows.append((f"{short}#{name}", claim, outcome))
    q = json.dumps  # JSON strings are valid YAML flow scalars — no hand-escaping
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(f"suite: {q(suite_name)}\n")
        fh.write(f"results_file: {q(results_path)}\n")
        fh.write(f"sha: {q(sha)}\n")
        fh.write(f"timestamp: {q(timestamp)}\n")
        fh.write("tests:\n")
        for test, claim, outcome in rows:
            fh.write(f"  - {{test: {q(test)}, claim: {q(claim)}, outcome: {outcome}}}\n")
        fh.write(f"unbound: [{', '.join(q(u) for u in unbound)}]\n")
    print(f"spine written: {path} ({len(rows)} bound, {len(unbound)} unbound)")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", required=True)
    parser.add_argument("--tests-dir", required=True)
    parser.add_argument("--ir")
    parser.add_argument("--spine-out")
    args = parser.parse_args()

    mapping = method_claims(args.tests_dir)
    root = ET.parse(args.results).getroot()
    suites = [root] if root.tag == "testsuite" else root.findall("testsuite")

    if args.spine_out:
        write_spine(args.spine_out, args.results, args.tests_dir, mapping, suites)

    total, failed = 0, []
    for suite in suites:
        for case in suite.findall("testcase"):
            total += 1
            if case.find("failure") is not None or case.find("error") is not None:
                name = case.get("name", "?")
                node = case.find("failure")
                if node is None:
                    node = case.find("error")
                failed.append((name, (node.get("message") or "").strip()[:200]))

    print(f"=== Airlift verification spine: {total} tests, {len(failed)} red ===")
    if not failed:
        print("ALL CLAIMS HELD: every bound test is green.")
        return 0

    violated = {}
    for name, message in failed:
        claim = mapping.get(name, "<unbound test — no @AirliftClaim>")
        violated.setdefault(claim, []).append((name, message))
    print(f"\nVIOLATED CLAIMS ({len(violated)}):")
    for claim in sorted(violated):
        title = claim_title(args.ir, claim)
        print(f"\n  ✗ {claim}" + (f" — {title}" if title else ""))
        for name, message in violated[claim]:
            print(f"      test {name}: {message}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
