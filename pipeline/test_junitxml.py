#!/usr/bin/env python3
"""Unit tests for junitxml.py (skipped tests must never count as green).

Run: ./.venv/bin/python -m unittest discover -s pipeline
"""
import os
import tempfile
import unittest

import junitxml

XML = """<?xml version="1.0" encoding="UTF-8" ?>
<testsuite errors="0" failures="1" name="demo" skipped="1" tests="4" timestamp="2026-07-19T12:00:00">
  <testcase classname="a.b.Demo" name="testPass" time="0.1" />
  <testcase classname="a.b.Demo" name="testFail" time="0.1"><failure message="boom" /></testcase>
  <testcase classname="a.b.Demo" name="testError" time="0.1"><error message="crash" /></testcase>
  <testcase classname="a.b.Demo" name="testSkip" time="0.0"><skipped /></testcase>
</testsuite>
"""


class JunitXmlTest(unittest.TestCase):
    def _parse(self, xml):
        with tempfile.NamedTemporaryFile("w", suffix=".xml", delete=False) as fh:
            fh.write(xml)
            path = fh.name
        try:
            return junitxml.parse(path)
        finally:
            os.unlink(path)

    def test_outcome_classification(self):
        doc = self._parse(XML)
        outcomes = {t["name"]: t["outcome"] for t in doc["tests"]}
        self.assertEqual(outcomes, {"testPass": "passed", "testFail": "failed",
                                    "testError": "error", "testSkip": "skipped"})
        self.assertFalse(junitxml.is_green(doc))

    def test_all_skipped_is_not_green(self):
        doc = self._parse(XML.replace('<failure message="boom" />', "<skipped />")
                             .replace('<error message="crash" />', "<skipped />")
                             .replace('<testcase classname="a.b.Demo" name="testPass" time="0.1" />',
                                      '<testcase classname="a.b.Demo" name="testPass" time="0.1"><skipped /></testcase>'))
        self.assertFalse(junitxml.is_green(doc))

    def test_empty_suite_is_not_green(self):
        doc = self._parse('<testsuite name="empty" tests="0" timestamp="t"></testsuite>')
        self.assertFalse(junitxml.is_green(doc))

    def test_green_suite(self):
        doc = self._parse('<testsuite name="g" tests="1" timestamp="t">'
                          '<testcase classname="a.B" name="testX" /></testsuite>')
        self.assertTrue(junitxml.is_green(doc))


if __name__ == "__main__":
    unittest.main()
