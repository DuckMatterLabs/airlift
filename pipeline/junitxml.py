#!/usr/bin/env python3
"""Neutral JUnit-style XML reader for Airlift's deterministic tooling.

Module: parse(path) -> {suite, timestamp, tests: [{classname, name, outcome, message}]}
CLI:    junitxml.py <results.xml>   — prints a summary plus failing tests.
        Exit 0 = green (all passed, at least one test), 1 = red, 2 = unreadable/empty.
"""
import sys
import xml.etree.ElementTree as ET


def parse(path):
    root = ET.parse(path).getroot()
    suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
    doc = {"suite": None, "timestamp": None, "tests": []}
    for suite in suites:
        if doc["suite"] is None:
            doc["suite"] = suite.get("name")
            doc["timestamp"] = suite.get("timestamp")
        for case in suite.findall("testcase"):
            node = case.find("failure")
            if node is None:
                node = case.find("error")
            if node is not None:
                outcome = "failed" if case.find("failure") is not None else "error"
            elif case.find("skipped") is not None:
                outcome = "skipped"  # not green: a skipped test attests nothing
            else:
                outcome = "passed"
            doc["tests"].append({
                "classname": case.get("classname") or "",
                "name": case.get("name") or "?",
                "outcome": outcome,
                "message": "" if node is None else (node.get("message") or "").strip()[:200],
            })
    return doc


def is_green(doc):
    return bool(doc["tests"]) and all(t["outcome"] == "passed" for t in doc["tests"])


def main():
    path = sys.argv[1]
    try:
        doc = parse(path)
    except Exception as exc:
        print(f"junitxml: cannot read {path}: {type(exc).__name__}: {exc}")
        return 2
    red = [t for t in doc["tests"] if t["outcome"] != "passed"]
    print(f"suite={doc['suite']} tests={len(doc['tests'])} red={len(red)}")
    for t in red:
        short = t["classname"].rsplit(".", 1)[-1]
        print(f"  {t['outcome'].upper()} {short}#{t['name']}: {t['message']}")
    if not doc["tests"]:
        print("junitxml: no test cases in report")
        return 2
    return 0 if not red else 1


if __name__ == "__main__":
    sys.exit(main())
