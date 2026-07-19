#!/usr/bin/env python3
"""Deterministic structural validators for Airlift pipeline stage outputs.

Usage: validate.py <stage> <out_dir> [--repo <repo_root>]
stages: map | testscape | catalog | backfill | ir
Exit 0 = valid; exit 1 = violations (printed to stdout, one per line).
"""
import os
import sys

import yaml

ROLES = {"business-logic", "plumbing", "boilerplate", "fused"}
COVERAGE = {"covered-asserted", "covered-incidental", "bare"}
OBSERVABILITY = {"direct", "indirect", "internal"}
PRIORITY = {"core", "edge", "secondary"}
KINDS = {"behavior", "invariant", "domain", "contract", "config"}


def load(path):
    with open(path, encoding="utf-8") as fh:
        return yaml.safe_load(fh)


def file_line_count(path):
    with open(path, encoding="utf-8", errors="replace") as fh:
        return sum(1 for _ in fh)


def v_map(out_dir, repo):
    errs = []
    doc = load(os.path.join(out_dir, "fragment-map.yaml"))
    frags = doc.get("fragments") or []
    if not frags:
        return ["fragment-map: no fragments"]
    if not (doc.get("source_binding") or {}).get("commit"):
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
        path = os.path.join(repo, primary)
        if os.path.exists(path):
            total = file_line_count(path)
            cursor = 1
            for start, end, fid in spans:
                if start > cursor:
                    errs.append(f"fragment-map: {primary}: gap lines {cursor}-{start - 1} before {fid}")
                if start < cursor:
                    errs.append(f"fragment-map: {primary}: overlap at {fid} (starts {start}, expected {cursor})")
                cursor = max(cursor, end + 1)
            if cursor <= total:
                errs.append(f"fragment-map: {primary}: uncovered tail {cursor}-{total}")
        else:
            errs.append(f"fragment-map: primary file not found in repo: {primary}")
    return errs


def v_testscape(out_dir, repo):
    errs = []
    doc = load(os.path.join(out_dir, "coverage-gaps.yaml"))
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
    if (doc.get("run") or {}).get("result") != "green":
        errs.append("backfill-report: run.result is not green")
    if not doc.get("pinned"):
        errs.append("backfill-report: nothing pinned")
    return errs


def v_ir(out_dir, repo):
    errs = []
    ir = os.path.join(out_dir, "ir")
    manifest = load(os.path.join(ir, "ir-manifest.yaml"))
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
    for req in ("domain-model.yaml",):
        if not os.path.exists(os.path.join(ir, req)):
            errs.append(f"ir: missing {req}")
    core = [c for c in index.values() if c.get("priority") == "core"]
    for c in core:
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
