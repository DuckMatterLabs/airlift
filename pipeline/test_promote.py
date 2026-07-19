#!/usr/bin/env python3
"""Unit tests for promote.py (deterministic claim-status promotion).

Run: ./.venv/bin/python -m unittest discover -s pipeline
"""
import os
import tempfile
import unittest

import yaml

import promote

MANIFEST = """\
schema_version: "1.0"
seam: test-seam
claims:
  - id: T.A.ONE
    kind: behavior
    title: "One."
    status: extracted
    priority: core
  - id: T.A.TWO
    kind: behavior
    title: "Two."
    status: pinned
    priority: edge
  - id: T.A.THREE
    kind: config
    title: "Three."
    status: extracted
    priority: core
pinned_claims:
  - id: T.A.TWO
    tests: [Backfill#testTwo]
"""

CLAIM = """\
id: {cid}
kind: behavior
title: {title}
status: {status}
priority: core
terms: [thing]
behavior: |
  Rule: something
    Given a thing
    Then an outcome
notes: >
  Trailing prose block."""


def spine(path, tier_suite, results_file, sha, timestamp, tests, unbound=()):
    with open(path, "w") as fh:
        fh.write(f"suite: {tier_suite}\nresults_file: {results_file}\n")
        fh.write(f"sha: {sha}\ntimestamp: \"{timestamp}\"\ntests:\n")
        for test, claim, outcome in tests:
            fh.write(f"  - {{test: {test}, claim: {claim}, outcome: {outcome}}}\n")
        fh.write(f"unbound: [{', '.join(unbound)}]\n")


class PromoteTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.ir = os.path.join(self.tmp.name, "ir")
        os.makedirs(os.path.join(self.ir, "claims"))
        with open(os.path.join(self.ir, "ir-manifest.yaml"), "w") as fh:
            fh.write(MANIFEST)
        self._claim("T.A.ONE", "extracted")
        self._claim("T.A.TWO", "pinned")
        self._claim("T.A.THREE", "extracted", trailing_newline=False)
        self.blind = os.path.join(self.tmp.name, "blind-spine.yaml")
        self.backfill = os.path.join(self.tmp.name, "backfill-spine.yaml")
        spine(self.blind, "blind", "runs/blind.xml", "b" * 40, "2026-07-19T11:17:14",
              [("Blind#one", "T.A.ONE", "passed"), ("Blind#two", "T.A.TWO", "passed")])
        spine(self.backfill, "backfill", "runs/backfill.xml", "b" * 40, "2026-07-19T09:44:56",
              [("Backfill#two", "T.A.TWO", "passed"), ("Backfill#three", "T.A.THREE", "passed")])

    def tearDown(self):
        self.tmp.cleanup()

    def _claim(self, cid, status, trailing_newline=True):
        text = CLAIM.format(cid=cid, title=f'"{cid} title."', status=status)
        if trailing_newline:
            text += "\n"
        with open(os.path.join(self.ir, "claims", cid + ".yaml"), "w") as fh:
            fh.write(text)

    def _run(self, argv):
        old = os.sys.argv
        os.sys.argv = ["promote.py"] + argv
        try:
            return promote.main()
        finally:
            os.sys.argv = old

    def _load(self, cid):
        with open(os.path.join(self.ir, "claims", cid + ".yaml")) as fh:
            return yaml.safe_load(fh)

    def _bytes(self):
        out = {}
        for root, _dirs, files in os.walk(self.ir):
            for f in files:
                p = os.path.join(root, f)
                with open(p, "rb") as fh:
                    out[p] = fh.read()
        return out

    def test_promotion_and_attestations(self):
        rc = self._run(["--ir", self.ir, "--pinned-from", self.backfill,
                        "--verified-from", self.blind])
        self.assertEqual(rc, 0)
        one, two, three = self._load("T.A.ONE"), self._load("T.A.TWO"), self._load("T.A.THREE")
        # blind-green: verified, even straight from extracted
        self.assertEqual(one["status"], "verified")
        self.assertEqual(one["attestations"], [{"tier": "verified", "evidence": "runs/blind.xml",
                                                "sha": "b" * 40, "date": "2026-07-19T11:17:14"}])
        # green in both suites: both attestations, status = strongest
        self.assertEqual(two["status"], "verified")
        self.assertEqual({a["tier"] for a in two["attestations"]}, {"pinned", "verified"})
        # backfill-only: pinned
        self.assertEqual(three["status"], "pinned")
        self.assertEqual(three["attestations"][0]["tier"], "pinned")
        # manifest synced to claim files
        with open(os.path.join(self.ir, "ir-manifest.yaml")) as fh:
            manifest = yaml.safe_load(fh)
        statuses = {c["id"]: c["status"] for c in manifest["claims"]}
        self.assertEqual(statuses, {"T.A.ONE": "verified", "T.A.TWO": "verified",
                                    "T.A.THREE": "pinned"})

    def test_idempotent_second_run(self):
        self._run(["--ir", self.ir, "--pinned-from", self.backfill,
                   "--verified-from", self.blind])
        before = self._bytes()
        rc = self._run(["--ir", self.ir, "--pinned-from", self.backfill,
                        "--verified-from", self.blind])
        self.assertEqual(rc, 0)
        self.assertEqual(before, self._bytes(), "second run must be a byte-level no-op")

    def test_dry_run_writes_nothing(self):
        before = self._bytes()
        rc = self._run(["--ir", self.ir, "--verified-from", self.blind, "--dry-run"])
        self.assertEqual(rc, 0)
        self.assertEqual(before, self._bytes())

    def test_red_blocks_promotion(self):
        red = os.path.join(self.tmp.name, "red-spine.yaml")
        spine(red, "blind", "runs/red.xml", "c" * 40, "2026-07-19T12:00:00",
              [("Blind#one", "T.A.ONE", "failed"), ("Blind#two", "T.A.TWO", "passed")])
        rc = self._run(["--ir", self.ir, "--verified-from", red])
        self.assertEqual(rc, 2)  # conflicts present
        self.assertEqual(self._load("T.A.ONE")["status"], "extracted")  # blocked
        self.assertEqual(self._load("T.A.TWO")["status"], "verified")   # unaffected claim promoted

    def test_red_in_one_suite_blocks_even_if_green_in_other(self):
        redback = os.path.join(self.tmp.name, "redback-spine.yaml")
        spine(redback, "backfill", "runs/redback.xml", "c" * 40, "2026-07-19T12:00:00",
              [("Backfill#one", "T.A.ONE", "error")])
        rc = self._run(["--ir", self.ir, "--verified-from", self.blind,
                        "--pinned-from", redback])
        self.assertEqual(rc, 2)
        self.assertEqual(self._load("T.A.ONE")["status"], "extracted")

    def test_forward_only(self):
        self._run(["--ir", self.ir, "--verified-from", self.blind])
        self.assertEqual(self._load("T.A.TWO")["status"], "verified")
        # pinned-tier evidence on an already-verified claim: attestation recorded,
        # status never lowered
        rc = self._run(["--ir", self.ir, "--pinned-from", self.backfill])
        self.assertEqual(rc, 0)
        two = self._load("T.A.TWO")
        self.assertEqual(two["status"], "verified")
        self.assertIn("pinned", {a["tier"] for a in two["attestations"]})

    def test_unknown_claim_warns_without_crash(self):
        odd = os.path.join(self.tmp.name, "odd-spine.yaml")
        spine(odd, "blind", "runs/odd.xml", "d" * 40, "2026-07-19T13:00:00",
              [("Blind#ghost", "T.A.GHOST", "passed")])
        rc = self._run(["--ir", self.ir, "--verified-from", odd])
        self.assertEqual(rc, 0)
        self.assertFalse(os.path.exists(os.path.join(self.ir, "claims", "T.A.GHOST.yaml")))

    def test_unbound_test_is_an_error(self):
        bad = os.path.join(self.tmp.name, "bad-spine.yaml")
        spine(bad, "blind", "runs/bad.xml", "e" * 40, "2026-07-19T14:00:00",
              [("Blind#one", "T.A.ONE", "passed")], unbound=["Blind#loose"])
        rc = self._run(["--ir", self.ir, "--verified-from", bad])
        self.assertEqual(rc, 1)
        self.assertEqual(self._load("T.A.ONE")["status"], "extracted")

    def test_no_trailing_newline_file_stays_valid_yaml(self):
        self._run(["--ir", self.ir, "--pinned-from", self.backfill])
        three = self._load("T.A.THREE")
        self.assertEqual(three["status"], "pinned")
        self.assertEqual(three["notes"].strip(), "Trailing prose block.")
        self.assertEqual(len(three["attestations"]), 1)

    def test_dry_run_reports_same_changes_as_real_run(self):
        import contextlib
        import io

        def capture(argv):
            buf = io.StringIO()
            with contextlib.redirect_stdout(buf):
                self._run(argv)
            return sorted(line.strip() for line in buf.getvalue().splitlines()
                          if "->" in line or "+ attestation" in line)

        argv = ["--ir", self.ir, "--pinned-from", self.backfill,
                "--verified-from", self.blind]
        dry = capture(argv + ["--dry-run"])
        real = capture(argv)
        self.assertEqual(dry, real, "dry-run must print exactly the real run's diff, "
                                    "manifest edits included")
        self.assertTrue(any(line.startswith("manifest:") for line in dry))

    def test_comment_inside_attestations_block_survives(self):
        self._run(["--ir", self.ir, "--verified-from", self.blind])
        path = os.path.join(self.ir, "claims", "T.A.ONE.yaml")
        with open(path) as fh:
            text = fh.read()
        text = text.replace("attestations:\n", "attestations:\n# ratified by DE\n")
        with open(path, "w") as fh:
            fh.write(text)
        later = os.path.join(self.tmp.name, "later-spine.yaml")
        spine(later, "blind", "runs/later.xml", "f" * 40, "2026-08-01T00:00:00",
              [("Blind#one", "T.A.ONE", "passed")])
        rc = self._run(["--ir", self.ir, "--verified-from", later])
        self.assertEqual(rc, 0)
        with open(path) as fh:
            text = fh.read()
        self.assertIn("# ratified by DE", text)
        self.assertEqual(len(self._load("T.A.ONE")["attestations"]), 2)

    def test_skipped_outcome_blocks_promotion(self):
        skipped = os.path.join(self.tmp.name, "skipped-spine.yaml")
        spine(skipped, "blind", "runs/skip.xml", "a" * 40, "2026-07-19T15:00:00",
              [("Blind#one", "T.A.ONE", "skipped")])
        rc = self._run(["--ir", self.ir, "--verified-from", skipped])
        self.assertEqual(rc, 2)
        self.assertEqual(self._load("T.A.ONE")["status"], "extracted")


if __name__ == "__main__":
    unittest.main()
