#!/usr/bin/env python3
"""Airlift verification spine: map test failures back to violated IR claims.

Usage: report-claims.py --results <junit.xml> --tests-dir <java-src-dir> [--ir <ir-dir>]
Prints a claim-level verdict; exit 0 if green, 1 if any claim violated.
"""
import argparse
import os
import re
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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--results", required=True)
    parser.add_argument("--tests-dir", required=True)
    parser.add_argument("--ir")
    args = parser.parse_args()

    mapping = method_claims(args.tests_dir)
    root = ET.parse(args.results).getroot()
    suites = [root] if root.tag == "testsuite" else root.findall("testsuite")

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
