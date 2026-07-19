#!/usr/bin/env python3
"""Unit tests for validate.py's v1.0 additions (version gate, attestation checks).

Run: ./.venv/bin/python -m unittest discover -s pipeline
"""
import unittest

import validate


class VersionGateTest(unittest.TestCase):
    # ir-spec/VERSION is the real supported version; these tests assume major 1
    def test_supported_is_1_x(self):
        major, minor = validate.parse_version(validate.supported_version())
        self.assertEqual(major, 1)

    def test_missing_version_rejected(self):
        errs = validate.check_version({}, "x")
        self.assertEqual(len(errs), 1)
        self.assertIn("missing schema_version", errs[0])

    def test_exact_match_accepted(self):
        self.assertEqual(validate.check_version(
            {"schema_version": validate.supported_version()}, "x"), [])

    def test_float_scalar_accepted(self):
        # YAML writers may emit 1.0 unquoted; normalize rather than nitpick
        major, minor = validate.parse_version(validate.supported_version())
        self.assertEqual(validate.check_version(
            {"schema_version": float(f"{major}.{minor}")}, "x"), [])

    def test_older_minor_accepted_newer_minor_rejected(self):
        major, minor = validate.parse_version(validate.supported_version())
        newer = f"{major}.{minor + 1}"
        errs = validate.check_version({"schema_version": newer}, "x")
        self.assertEqual(len(errs), 1)
        self.assertIn("not supported", errs[0])

    def test_major_mismatch_rejected(self):
        for bad in ("0.9", "2.0"):
            errs = validate.check_version({"schema_version": bad}, "x")
            self.assertEqual(len(errs), 1, bad)

    def test_garbage_rejected(self):
        errs = validate.check_version({"schema_version": "one"}, "x")
        self.assertEqual(len(errs), 1)
        self.assertIn("unparseable", errs[0])


class AttestationCheckTest(unittest.TestCase):
    ATT = {"tier": "verified", "evidence": "e.xml", "sha": "a" * 40,
           "date": "2026-07-19T11:17:14"}

    def test_bad_status_flagged(self):
        errs = validate.v_claim_attestations("C", {"status": "golden"})
        self.assertEqual(len(errs), 1)

    def test_valid_attestation_ok(self):
        errs = validate.v_claim_attestations(
            "C", {"status": "verified", "attestations": [self.ATT]})
        self.assertEqual(errs, [])

    def test_missing_keys_flagged(self):
        errs = validate.v_claim_attestations(
            "C", {"status": "verified", "attestations": [{"tier": "verified"}]})
        self.assertEqual(len(errs), 1)
        self.assertIn("missing", errs[0])

    def test_status_below_attested_tier_flagged(self):
        errs = validate.v_claim_attestations(
            "C", {"status": "extracted", "attestations": [self.ATT]})
        self.assertTrue(any("below attested tier" in e for e in errs))

    def test_bad_tier_flagged(self):
        att = dict(self.ATT, tier="blessed")
        errs = validate.v_claim_attestations(
            "C", {"status": "verified", "attestations": [att]})
        self.assertTrue(any("bad tier" in e for e in errs))

    def test_status_without_supporting_attestation_flagged(self):
        # pinned/verified statuses must be earned via promote.py, never hand-written
        errs = validate.v_claim_attestations("C", {"status": "pinned"})
        self.assertTrue(any("without a supporting attestation" in e for e in errs))
        pinned_att = dict(self.ATT, tier="pinned")
        errs = validate.v_claim_attestations(
            "C", {"status": "verified", "attestations": [pinned_att]})
        self.assertTrue(any("without a supporting attestation" in e for e in errs))
        errs = validate.v_claim_attestations(
            "C", {"status": "extracted"})
        self.assertEqual(errs, [])


if __name__ == "__main__":
    unittest.main()
