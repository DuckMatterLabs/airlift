#!/usr/bin/env python3
"""Deterministic structural validators for Airlift pipeline stage outputs.

Usage: validate.py <stage> <out_dir> [--repo <repo_root>]
stages: map | testscape | catalog | backfill | ir
Exit 0 = valid; exit 1 = violations (printed to stdout, one per line).

Every stage artifact must declare the schema_version it was written against
(IR-SCHEMA.md section 0); the supported version is the single line in
ir-spec/VERSION. Validators are extended across versions, never weakened.
"""
import os
import subprocess
import sys

import yaml

ROLES = {"business-logic", "plumbing", "boilerplate", "fused"}
COVERAGE = {"covered-asserted", "covered-incidental", "bare"}
OBSERVABILITY = {"direct", "indirect", "internal"}
PRIORITY = {"core", "edge", "secondary"}
KINDS = {"behavior", "invariant", "domain", "contract", "config"}
STATUS_ORDER = {"extracted": 0, "pinned": 1, "verified": 2}
ATTESTATION_TIERS = {"pinned", "verified"}
ATTESTATION_KEYS = {"tier", "evidence", "sha", "date"}

VERSION_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                            "..", "ir-spec", "VERSION")


def supported_version():
    with open(VERSION_FILE, encoding="utf-8") as fh:
        return fh.read().strip()


def parse_version(value):
    major, _, minor = str(value).partition(".")
    return int(major), int(minor)


def check_version(doc, artifact):
    """schema_version must match the supported major and not exceed the minor."""
    declared = doc.get("schema_version")
    supported = supported_version()
    try:
        s_major, s_minor = parse_version(supported)
    except ValueError:
        return [f"{artifact}: ir-spec/VERSION unparseable: {supported!r} (want major.minor)"]
    if declared is None:
        return [f"{artifact}: missing schema_version (schema v{supported} requires it)"]
    try:
        d_major, d_minor = parse_version(declared)
    except ValueError:
        return [f"{artifact}: unparseable schema_version {declared!r} (want major.minor, e.g. \"{supported}\")"]
    if d_major != s_major or d_minor > s_minor:
        return [f"{artifact}: schema_version {declared} not supported by these validators (supported: {supported})"]
    return []


def load(path):
    with open(path, encoding="utf-8") as fh:
        return yaml.safe_load(fh)


def count_lines(blob):
    return blob.count(b"\n") + (0 if blob.endswith(b"\n") or not blob else 1)


def source_line_count(repo, commit, relpath):
    """(line count, error) of relpath. Lines are evidence AT the source_binding
    commit, never a later working tree (IR-SCHEMA.md section 2): when a commit is
    declared, a failure to read the file at that commit is a violation — there is
    deliberately NO working-tree fallback. Byte-level counting: legacy sources
    need not be valid UTF-8."""
    if commit:
        try:
            probe = subprocess.run(["git", "-C", repo, "show", f"{commit}:{relpath}"],
                                   capture_output=True)
        except OSError as exc:
            return None, f"fragment-map: cannot run git in {repo}: {exc}"
        if probe.returncode != 0:
            detail = probe.stderr.decode(errors="replace").strip().splitlines()
            return None, (f"fragment-map: cannot read {relpath} at source_binding.commit "
                          f"{commit} ({detail[0] if detail else 'git show failed'})")
        return count_lines(probe.stdout), None
    path = os.path.join(repo, relpath)
    if not os.path.exists(path):
        return None, f"fragment-map: primary file not found in repo: {relpath}"
    with open(path, "rb") as fh:
        return count_lines(fh.read()), None


def v_map(out_dir, repo):
    errs = []
    doc = load(os.path.join(out_dir, "fragment-map.yaml"))
    errs += check_version(doc, "fragment-map")
    frags = doc.get("fragments") or []
    if not frags:
        return errs + ["fragment-map: no fragments"]
    commit = (doc.get("source_binding") or {}).get("commit")
    if not commit:
        errs.append("fragment-map: missing source_binding.commit")
    ids = set()
    by_file = {}
    for fr in frags:
        fid = fr.get("id")
        if not fid or fid in ids:
            errs.append(f"fragment-map: missing/duplicate id {fid}")
        ids.add(fid)
        for key in ("file", "lines", "role", "summary"):
            if not fr.get(key):
                errs.append(f"fragment-map: {fid}: missing {key}")
        if fr.get("role") not in ROLES:
            errs.append(f"fragment-map: {fid}: bad role {fr.get('role')}")
        if fr.get("role") == "fused" and not fr.get("fused_note"):
            errs.append(f"fragment-map: {fid}: fused without fused_note")
        if isinstance(fr.get("lines"), list) and len(fr["lines"]) == 2:
            by_file.setdefault(fr["file"], []).append((fr["lines"][0], fr["lines"][1], fid))
        else:
            errs.append(f"fragment-map: {fid}: lines must be [start, end]")
    # total-coverage check for the file with the most fragments (the primary file)
    if repo and by_file:
        primary = max(by_file, key=lambda f: len(by_file[f]))
        spans = sorted(by_file[primary])
        total, err = source_line_count(repo, commit, primary)
        if err:
            errs.append(err)
        else:
            cursor = 1
            for start, end, fid in spans:
                if start > cursor:
                    errs.append(f"fragment-map: {primary}: gap lines {cursor}-{start - 1} before {fid}")
                if start < cursor:
                    errs.append(f"fragment-map: {primary}: overlap at {fid} (starts {start}, expected {cursor})")
                cursor = max(cursor, end + 1)
            if cursor <= total:
                errs.append(f"fragment-map: {primary}: uncovered tail {cursor}-{total}")
    return errs


def v_testscape(out_dir, repo):
    errs = []
    doc = load(os.path.join(out_dir, "coverage-gaps.yaml"))
    errs += check_version(doc, "coverage-gaps")
    fmap = load(os.path.join(out_dir, "fragment-map.yaml"))
    known = {fr["id"] for fr in fmap.get("fragments", [])}
    behavior_frags = {fr["id"] for fr in fmap.get("fragments", [])
                      if fr.get("role") in ("business-logic", "fused")}
    seen = set()
    for cov in doc.get("coverage") or []:
        frag = cov.get("fragment")
        seen.add(frag)
        if frag not in known:
            errs.append(f"coverage-gaps: unknown fragment {frag}")
        if cov.get("status") not in COVERAGE:
            errs.append(f"coverage-gaps: {frag}: bad status {cov.get('status')}")
    missing = behavior_frags - seen
    if missing:
        errs.append(f"coverage-gaps: behavior fragments without coverage judgment: {sorted(missing)}")
    if not doc.get("search_log"):
        errs.append("coverage-gaps: empty search_log (absence claims unauditable)")
    return errs


def v_catalog(out_dir, repo):
    errs = []
    doc = load(os.path.join(out_dir, "behavior-catalog.yaml"))
    errs += check_version(doc, "behavior-catalog")
    fmap = load(os.path.join(out_dir, "fragment-map.yaml"))
    known = {fr["id"] for fr in fmap.get("fragments", [])}
    behavior_frags = {fr["id"] for fr in fmap.get("fragments", [])
                      if fr.get("role") in ("business-logic", "fused")}
    represented = set()
    ids = set()
    for beh in doc.get("behaviors") or []:
        bid = beh.get("id", "?")
        if bid in ids:
            errs.append(f"catalog: duplicate id {bid}")
        ids.add(bid)
        for key in ("statement", "observable_outcome", "fragments"):
            if not beh.get(key):
                errs.append(f"catalog: {bid}: missing {key}")
        if beh.get("observability") not in OBSERVABILITY:
            errs.append(f"catalog: {bid}: bad observability")
        if beh.get("priority") not in PRIORITY:
            errs.append(f"catalog: {bid}: bad priority")
        for frag in beh.get("fragments") or []:
            if frag not in known:
                errs.append(f"catalog: {bid}: unknown fragment {frag}")
            represented.add(frag)
    for exc in doc.get("excluded") or []:
        represented.add(exc.get("fragment"))
        if not exc.get("reason"):
            errs.append(f"catalog: excluded {exc.get('fragment')}: no reason")
    orphans = behavior_frags - represented
    if orphans:
        errs.append(f"catalog: behavior fragments neither cataloged nor excluded: {sorted(orphans)}")
    return errs


def v_backfill(out_dir, repo):
    errs = []
    doc = load(os.path.join(out_dir, "backfill-report.yaml"))
    errs += check_version(doc, "backfill-report")
    if (doc.get("run") or {}).get("result") != "green":
        errs.append("backfill-report: run.result is not green")
    if not doc.get("pinned"):
        errs.append("backfill-report: nothing pinned")
    return errs


def v_claim_attestations(cid, claim):
    errs = []
    status = claim.get("status")
    if status not in STATUS_ORDER:
        errs.append(f"ir: {cid}: bad status {status!r} (want one of {sorted(STATUS_ORDER)})")
        return errs
    max_tier = None
    for att in claim.get("attestations") or []:
        if not isinstance(att, dict):
            errs.append(f"ir: {cid}: attestation is not a mapping")
            continue
        missing = ATTESTATION_KEYS - set(att)
        if missing:
            errs.append(f"ir: {cid}: attestation missing {sorted(missing)}")
        tier = att.get("tier")
        if tier not in ATTESTATION_TIERS:
            errs.append(f"ir: {cid}: attestation bad tier {tier!r}")
        elif max_tier is None or STATUS_ORDER[tier] > STATUS_ORDER[max_tier]:
            max_tier = tier
    if max_tier and STATUS_ORDER[status] < STATUS_ORDER[max_tier]:
        errs.append(f"ir: {cid}: status {status} below attested tier {max_tier}")
    if status in ATTESTATION_TIERS and (
            max_tier is None or STATUS_ORDER[max_tier] < STATUS_ORDER[status]):
        errs.append(f"ir: {cid}: status {status} without a supporting attestation "
                    f"at tier >= {status} (statuses are moved only by promote.py)")
    return errs


def v_ir(out_dir, repo):
    errs = []
    ir = os.path.join(out_dir, "ir")
    manifest = load(os.path.join(ir, "ir-manifest.yaml"))
    errs += check_version(manifest, "ir-manifest")
    glossary = load(os.path.join(ir, "glossary.yaml"))
    trace = load(os.path.join(ir, "traceability.yaml"))
    fmap = load(os.path.join(out_dir, "fragment-map.yaml"))
    known_frags = {fr["id"] for fr in fmap.get("fragments", [])}
    known_frags |= {fr["id"] for fr in trace.get("fragments", [])}
    terms = {t["id"].removeprefix("term.") for t in glossary.get("terms", [])} | \
            {t.get("canonical") for t in glossary.get("terms", [])}
    claim_dir = os.path.join(ir, "claims")
    claim_files = {f[:-5] for f in os.listdir(claim_dir) if f.endswith(".yaml")}
    index = {c["id"]: c for c in manifest.get("claims", [])}
    if set(index) != claim_files:
        errs.append(f"ir: manifest/claims mismatch: only-manifest={sorted(set(index) - claim_files)} "
                    f"only-files={sorted(claim_files - set(index))}")
    trace_claims = {c["id"]: c.get("fragments", []) for c in trace.get("claims", [])}
    for cid in sorted(claim_files):
        claim = load(os.path.join(claim_dir, cid + ".yaml"))
        if claim.get("id") != cid:
            errs.append(f"ir: {cid}: id/filename mismatch")
        if claim.get("kind") not in KINDS:
            errs.append(f"ir: {cid}: bad kind {claim.get('kind')}")
        if claim.get("kind") in ("behavior", "invariant") and not claim.get("behavior"):
            errs.append(f"ir: {cid}: missing behavior body")
        body = claim.get("behavior") or ""
        if claim.get("kind") == "behavior" and body and ("Given" not in body or "Then" not in body):
            errs.append(f"ir: {cid}: behavior body lacks Given/Then")
        if claim.get("observability") is not None and claim.get("observability") not in OBSERVABILITY:
            errs.append(f"ir: {cid}: bad observability {claim.get('observability')!r}")
        for term in claim.get("terms") or []:
            if term not in terms:
                errs.append(f"ir: {cid}: term '{term}' not in glossary")
        frags = claim.get("source_fragments") or []
        if not frags:
            errs.append(f"ir: {cid}: no source_fragments")
        for frag in frags:
            if frag not in known_frags:
                errs.append(f"ir: {cid}: unknown fragment {frag}")
        if cid not in trace_claims:
            errs.append(f"ir: {cid}: missing from traceability.yaml")
        errs += v_claim_attestations(cid, claim)
        if cid in index and index[cid].get("status") != claim.get("status"):
            errs.append(f"ir: {cid}: manifest status {index[cid].get('status')!r} "
                        f"!= claim file status {claim.get('status')!r}")
    for req in ("domain-model.yaml",):
        if not os.path.exists(os.path.join(ir, req)):
            errs.append(f"ir: missing {req}")
    core = [c for c in index.values() if c.get("priority") == "core"]
    for c in core:
        if c["id"] not in claim_files:
            continue
        claim = load(os.path.join(claim_dir, c["id"] + ".yaml"))
        if claim.get("kind") == "behavior" and not claim.get("scenarios"):
            errs.append(f"ir: {c['id']}: core behavior without worked scenario")
    return errs


def main():
    stage, out_dir = sys.argv[1], sys.argv[2]
    repo = None
    if "--repo" in sys.argv:
        repo = sys.argv[sys.argv.index("--repo") + 1]
    fn = {"map": v_map, "testscape": v_testscape, "catalog": v_catalog,
          "backfill": v_backfill, "ir": v_ir}[stage]
    try:
        errs = fn(out_dir, repo)
    except Exception as exc:  # structural failure IS a validation failure
        errs = [f"{stage}: cannot validate: {type(exc).__name__}: {exc}"]
    for err in errs:
        print(err)
    print(f"[validate:{stage}] {'FAIL ' + str(len(errs)) + ' violations' if errs else 'OK'}")
    return 1 if errs else 0


if __name__ == "__main__":
    sys.exit(main())
